package event

import net.kyori.adventure.nbt.CompoundBinaryTag
import net.minestom.server.component.DataComponents
import net.minestom.server.event.player.PlayerBlockPlaceEvent
import net.minestom.server.instance.block.Block

fun onPlayerBlockPlace(event: PlayerBlockPlaceEvent) {
    when (event.block) {
        Block.OAK_SIGN -> {
            val item = event.player.itemInMainHand
            var block = Block.OAK_SIGN.withProperty("rotation", ((event.player.position.yaw + 180) / 22.5).toInt().toString())

            // Try to get block entity data from the item
            val blockEntityData = item.get(DataComponents.BLOCK_ENTITY_DATA)
            if (blockEntityData != null) {
                // Extract the NBT compound from the typed custom data
                val nbt = blockEntityData.nbt()

                // Create sign NBT with front_text from item's block entity data
                val signNbt = CompoundBinaryTag.builder()

                // If the item has front_text, use it
                if (nbt.get("front_text") != null) {
                    signNbt.put("front_text", nbt.get("front_text")!!)
                }

                // If the item has back_text, use it
                if (nbt.get("back_text") != null) {
                    signNbt.put("back_text", nbt.get("back_text")!!)
                }

                block = block.withNbt(signNbt.build())
            } else {
                // Fallback: Try to get custom data and look for sign text there
//                val customData = item.get(DataComponents.CUSTOM_DATA)
//                if (customData != null) {
//                    val nbt = customData.nbt()
//                    val signNbt = CompoundBinaryTag.builder()
//
//                    if (nbt.get("front_text") != null) {
//                        signNbt.put("front_text", nbt.get("front_text")!!)
//                    }
//
//                    if (nbt.get("back_text") != null) {
//                        signNbt.put("back_text", nbt.get("back_text")!!)
//                    }
//
//                    block = block.withNbt(signNbt.build())
//                }
            }

            event.block = block
        }
        else -> {}
    }
}