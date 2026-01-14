import io.klogging.Klogging
import io.klogging.noCoLogger
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.header
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.io.FileOutputStream
import java.nio.file.FileSystems
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardWatchEventKinds
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.util.UUID
import java.net.URI
import java.security.MessageDigest
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

object ResourcePackHost: Klogging {
    data class ResourcePackMetadata(
        val uuid: UUID,
        val uri: URI,
        val sha1Hash: ByteArray
    )

    private data class PackState(
        val zipFile: File,
        val isStale: MutableStateFlow<Boolean> = MutableStateFlow(true)
    )

    private val cacheDir = File("cache").apply { mkdirs() }
    private val resourcePacksDir = File("resourcepacks").apply { mkdirs() }
    private val watchService = FileSystems.getDefault().newWatchService()
    private val watchedPaths = ConcurrentHashMap<Path, String>() // Path -> packName
    private val packStates = ConcurrentHashMap<String, PackState>()
    private val zipMutex = Mutex()
    private val syncLogger = noCoLogger<ResourcePackHost>()

    // Configuration
    private val serverHostname = System.getProperty("server.hostname", "localhost")
    private val serverPort = System.getProperty("server.port", "8080").toInt()
    private val metadataCache = ConcurrentHashMap<String, ResourcePackMetadata>()

    fun watchFileSystemEvents(): Flow<java.nio.file.WatchKey> = flow {
        while (currentCoroutineContext().isActive) {
            val key = withContext(Dispatchers.IO) {
                watchService.poll(100, TimeUnit.MILLISECONDS)
            } ?: continue
            emit(key)
        }
    }.flowOn(Dispatchers.IO)
        .catch { e ->
            if (e is CancellationException) throw e
            syncLogger.error(e, "Watch service error")
        }

    fun handleWatchEvent(key: java.nio.file.WatchKey) {
        val path = key.watchable() as? Path ?: return
        val packName = watchedPaths[path] ?: return

        packStates[packName.substringBeforeLast('.')]?.isStale?.value = true

        key.pollEvents()
        if (!key.reset()) {
            watchedPaths.remove(path)
            syncLogger.warn("Watch key no longer valid for $path")
        }
    }

    suspend fun getResourcePack(packName: String): File? = withContext(Dispatchers.IO) {
        val resourcePack = File(resourcePacksDir, packName)
        if (!resourcePack.exists() || !resourcePack.canonicalPath.startsWith(resourcePacksDir.canonicalPath)) {
            return@withContext null
        }

        val zipFile = File(cacheDir, "${resourcePack.name}.zip")
        logger.info(packStates)
        val state = packStates.getOrPut(packName) { PackState(zipFile, MutableStateFlow(true)) }
        logger.info(state.isStale.value)

        if (state.isStale.value) {
            zipMutex.withLock {
                // Double-check pattern for thread safety
                if (state.isStale.value) {
                    logger.info("Rebuilding stale zip for $packName")
                    if (zipFile.exists()) zipFile.delete()
                    createZip(resourcePack, zipFile)
                    state.isStale.value = false
                    metadataCache.remove(packName)
                }
            }
        }

        return@withContext zipFile
    }

    suspend fun getResourcePackMetadata(packName: String): ResourcePackMetadata? {
        val zipFile = getResourcePack(packName) ?: return null

        // Simple cache check - no timestamp validation
        val cached = metadataCache[packName]
        if (cached != null) {
            return ResourcePackMetadata(cached.uuid, cached.uri, cached.sha1Hash)
        }

        // Recalculate with double-check
        return zipMutex.withLock {
            metadataCache[packName]?.let {
                return@withLock ResourcePackMetadata(it.uuid, it.uri, it.sha1Hash)
            }

            val hash = calculateSha1(zipFile)
            val uuid = generateUuidFromHash(hash)
            val uri = URI("http://$serverHostname:$serverPort/resourcepack/$packName")

            metadataCache[packName] = ResourcePackMetadata(uuid, uri, hash)
            ResourcePackMetadata(uuid, uri, hash)
        }
    }

    private fun calculateSha1(file: File): ByteArray {
        val digest = MessageDigest.getInstance("SHA-1")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest()
    }

    private fun generateUuidFromHash(hash: ByteArray): UUID {
        val buffer = ByteBuffer.wrap(hash)
        val mostSigBits = buffer.getLong()
        val leastSigBits = buffer.getLong()
        return UUID(mostSigBits, leastSigBits)
    }

    private fun createZip(source: File, destination: File) {
        try {
            ZipOutputStream(FileOutputStream(destination)).use { zos ->
                source.walkTopDown().forEach { file ->
                    if (file.isFile) {
                        val relativePath = source.toPath().relativize(file.toPath()).toString().replace("\\", "/")
                        zos.putNextEntry(ZipEntry(relativePath))
                        file.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }
            registerWatcher(source, destination)
        } catch (e: Exception) {
            syncLogger.error(e, "Error creating zip file")
            destination.delete()
        }
    }

    private fun registerWatcher(root: File, zipFile: File) {
        try {
            Files.walkFileTree(root.toPath(), object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (!watchedPaths.containsKey(dir)) {
                        syncLogger.info("Registering watcher for ${dir.toAbsolutePath()}")
                        dir.register(
                            watchService,
                            StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_DELETE,
                            StandardWatchEventKinds.ENTRY_MODIFY
                        )
                        watchedPaths[dir] = zipFile.name
                    }
                    return FileVisitResult.CONTINUE
                }
            })
        } catch (e: Exception) {
            syncLogger.error(e, "Error registering watcher for ${root.name}")
        }
    }
}

fun CoroutineScope.startResourcePackHost() {
    launch(Dispatchers.IO) {
        ResourcePackHost.watchFileSystemEvents()
            .collect { key -> ResourcePackHost.handleWatchEvent(key) }
    }

    launch(Dispatchers.IO) {
        embeddedServer(CIO, port = 8080) {
            routing {
                get("/resourcepack/{packname}") {
                    val packname = call.parameters["packname"]
                    if (packname == null) {
                        call.respondText("Missing packname", status = BadRequest)
                        return@get
                    }
                    val file = ResourcePackHost.getResourcePack(packname)
                    if (file != null && file.exists()) {
                        call.response.header("Content-Disposition", "attachment; filename=\"${file.name}\"")
                        call.response.header("Content-Type", "application/octet-stream")
                        call.respondFile(file)
                    } else {
                        call.respondText("File not found", status = HttpStatusCode.NotFound)
                    }
                }
            }
        }.start(wait = true)
    }
}
