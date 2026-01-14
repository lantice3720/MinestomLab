package command

import ResourcePackHost
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.resource.ResourcePackInfo
import net.kyori.adventure.resource.ResourcePackRequest
import net.kyori.adventure.text.Component
import net.minestom.server.command.builder.Command

class ReloadResource: Command {
    constructor() : super("reloadresource") {
        setDefaultExecutor { sender, _ ->
            sender.clearResourcePacks()

            val metadata = runBlocking { ResourcePackHost.getResourcePackMetadata("test") }
            if (metadata == null) {
                sender.sendMessage(Component.text("Resource pack 'test' not found"))
                return@setDefaultExecutor
            }

            val resourcePackRequest = ResourcePackRequest.resourcePackRequest()
                .packs(ResourcePackInfo.resourcePackInfo(
                    metadata.uuid,
                    metadata.uri,
                    metadata.sha1Hash.joinToString("") { "%02x".format(it) }
                ))
                .required(true)
                .callback { _, status, audience ->
                    audience.sendMessage(Component.text("Resource pack status: $status"))
                }
                .build()

            sender.sendResourcePacks(resourcePackRequest)
        }
    }
}