import command.ReloadResource
import command.Stop
import command.SummonDisplay
import event.onPlayerBlockPlace
import event.onUseItem
import io.klogging.config.CONSOLE_INFO
import io.klogging.config.loggingConfiguration
import io.klogging.logger
import kotlinx.coroutines.runBlocking
import net.minestom.server.Auth
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.GameMode
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerBlockPlaceEvent
import net.minestom.server.event.player.PlayerUseItemEvent
import net.minestom.server.instance.LightingChunk
import net.minestom.server.instance.anvil.AnvilLoader
import net.minestom.server.instance.block.Block

fun main() = runBlocking {
    loggingConfiguration { CONSOLE_INFO() }
    val logger = logger("main")

    startResourcePackHost()

    val server = MinecraftServer.init(Auth.Online())

    val instanceManager = MinecraftServer.getInstanceManager()
    val instanceContainer = instanceManager.createInstanceContainer()

    instanceContainer.setGenerator { unit ->
        unit.modifier().fillHeight(0, 40, Block.GRASS_BLOCK)
    }

    instanceContainer.setChunkSupplier(::LightingChunk)
    instanceContainer.chunkLoader = AnvilLoader("test_world")

    val globalEventHandler = MinecraftServer.getGlobalEventHandler()
    globalEventHandler.addListener(AsyncPlayerConfigurationEvent::class.java) { event ->
        val player = event.player
        event.spawningInstance = instanceContainer
        player.respawnPoint = Pos(0.0, 42.0, 0.0)

        player.gameMode = GameMode.CREATIVE
    }
    globalEventHandler.addListener(PlayerBlockPlaceEvent::class.java, ::onPlayerBlockPlace)
    globalEventHandler.addListener(PlayerUseItemEvent::class.java, ::onUseItem)

    val commandManager = MinecraftServer.getCommandManager()

    commandManager.register(ReloadResource())
    commandManager.register(Stop())
    commandManager.register(SummonDisplay())

    server.start("0.0.0.0", 25565)
}