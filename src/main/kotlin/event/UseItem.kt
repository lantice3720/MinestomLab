package event

import net.minestom.server.event.player.PlayerUseItemEvent
import net.minestom.server.item.Material
import net.minestom.server.network.packet.server.play.OpenBookPacket

fun onUseItem(event: PlayerUseItemEvent) {
    when (event.itemStack.material()) {
        Material.WRITTEN_BOOK -> {
            event.player.sendPacket(OpenBookPacket(event.hand))
        }
        else -> {}
    }
}