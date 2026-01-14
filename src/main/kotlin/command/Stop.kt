package command

import net.minestom.server.MinecraftServer
import net.minestom.server.command.builder.Command

class Stop: Command {
    constructor() : super("stop") {
        setDefaultExecutor { sender, _ ->
            sender.sendMessage("Shutting down server...")
            MinecraftServer.getInstanceManager().instances.forEach {
                it.saveInstance().join()
                it.saveChunksToStorage().join()
            }
            MinecraftServer.stopCleanly()
        }
    }
}