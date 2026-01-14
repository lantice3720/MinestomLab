package command

import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.display.BlockDisplayMeta
import net.minestom.server.entity.metadata.display.ItemDisplayMeta
import net.minestom.server.entity.metadata.display.TextDisplayMeta
import net.minestom.server.instance.Instance
import net.minestom.server.utils.location.RelativeVec

class SummonDisplay : Command("summondisplay") {
    init {
        // Define literal arguments for display type
        val textLiteral = ArgumentType.Literal("text")
        val blockLiteral = ArgumentType.Literal("block")
        val itemLiteral = ArgumentType.Literal("item")

        // Define common arguments
        val positionArgument = ArgumentType.RelativeVec3("position")

        // Define type-specific arguments
        val componentArgument = ArgumentType.Component("component")
        val blockArgument = ArgumentType.BlockState("blockstate")
        val itemArgument = ArgumentType.ItemStack("itemstack")

        // Define optional arguments for transformations (translation-rotation-scale order)
        val translationXArgument = ArgumentType.Float("translationX")
        val translationYArgument = ArgumentType.Float("translationY")
        val translationZArgument = ArgumentType.Float("translationZ")

        val rotXArgument = ArgumentType.Float("rotX")
        val rotYArgument = ArgumentType.Float("rotY")
        val rotZArgument = ArgumentType.Float("rotZ")
        val rotWArgument = ArgumentType.Float("rotW")

        val scaleXArgument = ArgumentType.Float("scaleX")
        val scaleYArgument = ArgumentType.Float("scaleY")
        val scaleZArgument = ArgumentType.Float("scaleZ")

        positionArgument.setCallback { sender, _ ->
            sender.sendMessage(Component.text("Invalid position coordinates"))
        }

        componentArgument.setCallback { sender, exception ->
            sender.sendMessage(Component.text("Invalid JSON text component: ${exception.input}"))
        }

        arrayOf(translationXArgument, translationYArgument, translationZArgument).forEach { arg ->
            arg.setCallback { sender, _ ->
                sender.sendMessage(Component.text("Invalid translation value"))
            }
        }

        arrayOf(rotXArgument, rotYArgument, rotZArgument, rotWArgument).forEach { arg ->
            arg.setCallback { sender, _ ->
                sender.sendMessage(Component.text("Invalid rotation value"))
            }
        }

        arrayOf(scaleXArgument, scaleYArgument, scaleZArgument).forEach { arg ->
            arg.setCallback { sender, _ ->
                sender.sendMessage(Component.text("Invalid scale value"))
            }
        }

        // TEXT DISPLAY - Minimal syntax: /summondisplay text <pos> <component>
        addSyntax({ sender, context ->
            val position = context.get(positionArgument)
            val component = context.get(componentArgument)
            handleTextDisplay(sender, position, component)
        }, textLiteral, positionArgument, componentArgument)

        // TEXT DISPLAY - With translation: /summondisplay text <pos> <component> <tx> <ty> <tz>
        addSyntax({ sender, context ->
            val position = context.get(positionArgument)
            val component = context.get(componentArgument)
            val translationX = context.get(translationXArgument)
            val translationY = context.get(translationYArgument)
            val translationZ = context.get(translationZArgument)
            handleTextDisplay(
                sender,
                position,
                component,
                Vec(translationX.toDouble(), translationY.toDouble(), translationZ.toDouble())
            )
        }, textLiteral, positionArgument, componentArgument, translationXArgument, translationYArgument, translationZArgument)

        // TEXT DISPLAY - With translation + rotation: /summondisplay text <pos> <component> <tx> <ty> <tz> <rotX> <rotY> <rotZ> <rotW>
        addSyntax({ sender, context ->
            val position = context.get(positionArgument)
            val component = context.get(componentArgument)
            val translationX = context.get(translationXArgument)
            val translationY = context.get(translationYArgument)
            val translationZ = context.get(translationZArgument)
            val rotX = context.get(rotXArgument)
            val rotY = context.get(rotYArgument)
            val rotZ = context.get(rotZArgument)
            val rotW = context.get(rotWArgument)
            handleTextDisplay(
                sender,
                position,
                component,
                Vec(translationX.toDouble(), translationY.toDouble(), translationZ.toDouble()),
                floatArrayOf(rotX, rotY, rotZ, rotW)
            )
        }, textLiteral, positionArgument, componentArgument, translationXArgument, translationYArgument, translationZArgument,
            rotXArgument, rotYArgument, rotZArgument, rotWArgument)

        // TEXT DISPLAY - Full: /summondisplay text <pos> <component> <tx> <ty> <tz> <rotX> <rotY> <rotZ> <rotW> <sx> <sy> <sz>
        addSyntax({ sender, context ->
            val position = context.get(positionArgument)
            val component = context.get(componentArgument)
            val translationX = context.get(translationXArgument)
            val translationY = context.get(translationYArgument)
            val translationZ = context.get(translationZArgument)
            val rotX = context.get(rotXArgument)
            val rotY = context.get(rotYArgument)
            val rotZ = context.get(rotZArgument)
            val rotW = context.get(rotWArgument)
            val scaleX = context.get(scaleXArgument)
            val scaleY = context.get(scaleYArgument)
            val scaleZ = context.get(scaleZArgument)
            handleTextDisplay(
                sender,
                position,
                component,
                Vec(translationX.toDouble(), translationY.toDouble(), translationZ.toDouble()),
                floatArrayOf(rotX, rotY, rotZ, rotW),
                Vec(scaleX.toDouble(), scaleY.toDouble(), scaleZ.toDouble())
            )
        }, textLiteral, positionArgument, componentArgument, translationXArgument, translationYArgument, translationZArgument,
            rotXArgument, rotYArgument, rotZArgument, rotWArgument, scaleXArgument, scaleYArgument, scaleZArgument)

        // BLOCK DISPLAY - Minimal syntax: /summondisplay block <pos> <blockstate>
        addSyntax({ sender, context ->
            val position = context.get(positionArgument)
            val block = context.get(blockArgument)
            handleBlockDisplay(sender, position, block)
        }, blockLiteral, positionArgument, blockArgument)

        // BLOCK DISPLAY - With translation: /summondisplay block <pos> <blockstate> <tx> <ty> <tz>
        addSyntax({ sender, context ->
            val position = context.get(positionArgument)
            val block = context.get(blockArgument)
            val translationX = context.get(translationXArgument)
            val translationY = context.get(translationYArgument)
            val translationZ = context.get(translationZArgument)
            handleBlockDisplay(
                sender,
                position,
                block,
                Vec(translationX.toDouble(), translationY.toDouble(), translationZ.toDouble())
            )
        }, blockLiteral, positionArgument, blockArgument, translationXArgument, translationYArgument, translationZArgument)

        // BLOCK DISPLAY - With translation + rotation: /summondisplay block <pos> <blockstate> <tx> <ty> <tz> <rotX> <rotY> <rotZ> <rotW>
        addSyntax({ sender, context ->
            val position = context.get(positionArgument)
            val block = context.get(blockArgument)
            val translationX = context.get(translationXArgument)
            val translationY = context.get(translationYArgument)
            val translationZ = context.get(translationZArgument)
            val rotX = context.get(rotXArgument)
            val rotY = context.get(rotYArgument)
            val rotZ = context.get(rotZArgument)
            val rotW = context.get(rotWArgument)
            handleBlockDisplay(
                sender,
                position,
                block,
                Vec(translationX.toDouble(), translationY.toDouble(), translationZ.toDouble()),
                floatArrayOf(rotX, rotY, rotZ, rotW)
            )
        }, blockLiteral, positionArgument, blockArgument, translationXArgument, translationYArgument, translationZArgument,
            rotXArgument, rotYArgument, rotZArgument, rotWArgument)

        // BLOCK DISPLAY - Full: /summondisplay block <pos> <blockstate> <tx> <ty> <tz> <rotX> <rotY> <rotZ> <rotW> <sx> <sy> <sz>
        addSyntax({ sender, context ->
            val position = context.get(positionArgument)
            val block = context.get(blockArgument)
            val translationX = context.get(translationXArgument)
            val translationY = context.get(translationYArgument)
            val translationZ = context.get(translationZArgument)
            val rotX = context.get(rotXArgument)
            val rotY = context.get(rotYArgument)
            val rotZ = context.get(rotZArgument)
            val rotW = context.get(rotWArgument)
            val scaleX = context.get(scaleXArgument)
            val scaleY = context.get(scaleYArgument)
            val scaleZ = context.get(scaleZArgument)
            handleBlockDisplay(
                sender,
                position,
                block,
                Vec(translationX.toDouble(), translationY.toDouble(), translationZ.toDouble()),
                floatArrayOf(rotX, rotY, rotZ, rotW),
                Vec(scaleX.toDouble(), scaleY.toDouble(), scaleZ.toDouble())
            )
        }, blockLiteral, positionArgument, blockArgument, translationXArgument, translationYArgument, translationZArgument,
            rotXArgument, rotYArgument, rotZArgument, rotWArgument, scaleXArgument, scaleYArgument, scaleZArgument)

        // ITEM DISPLAY - Minimal syntax: /summondisplay item <pos> <itemstack>
        addSyntax({ sender, context ->
            val position = context.get(positionArgument)
            val item = context.get(itemArgument)
            handleItemDisplay(sender, position, item)
        }, itemLiteral, positionArgument, itemArgument)

        // ITEM DISPLAY - With translation: /summondisplay item <pos> <itemstack> <tx> <ty> <tz>
        addSyntax({ sender, context ->
            val position = context.get(positionArgument)
            val item = context.get(itemArgument)
            val translationX = context.get(translationXArgument)
            val translationY = context.get(translationYArgument)
            val translationZ = context.get(translationZArgument)
            handleItemDisplay(
                sender,
                position,
                item,
                Vec(translationX.toDouble(), translationY.toDouble(), translationZ.toDouble())
            )
        }, itemLiteral, positionArgument, itemArgument, translationXArgument, translationYArgument, translationZArgument)

        // ITEM DISPLAY - With translation + rotation: /summondisplay item <pos> <itemstack> <tx> <ty> <tz> <rotX> <rotY> <rotZ> <rotW>
        addSyntax({ sender, context ->
            val position = context.get(positionArgument)
            val item = context.get(itemArgument)
            val translationX = context.get(translationXArgument)
            val translationY = context.get(translationYArgument)
            val translationZ = context.get(translationZArgument)
            val rotX = context.get(rotXArgument)
            val rotY = context.get(rotYArgument)
            val rotZ = context.get(rotZArgument)
            val rotW = context.get(rotWArgument)
            handleItemDisplay(
                sender,
                position,
                item,
                Vec(translationX.toDouble(), translationY.toDouble(), translationZ.toDouble()),
                floatArrayOf(rotX, rotY, rotZ, rotW)
            )
        }, itemLiteral, positionArgument, itemArgument, translationXArgument, translationYArgument, translationZArgument,
            rotXArgument, rotYArgument, rotZArgument, rotWArgument)

        // ITEM DISPLAY - Full: /summondisplay item <pos> <itemstack> <tx> <ty> <tz> <rotX> <rotY> <rotZ> <rotW> <sx> <sy> <sz>
        addSyntax({ sender, context ->
            val position = context.get(positionArgument)
            val item = context.get(itemArgument)
            val translationX = context.get(translationXArgument)
            val translationY = context.get(translationYArgument)
            val translationZ = context.get(translationZArgument)
            val rotX = context.get(rotXArgument)
            val rotY = context.get(rotYArgument)
            val rotZ = context.get(rotZArgument)
            val rotW = context.get(rotWArgument)
            val scaleX = context.get(scaleXArgument)
            val scaleY = context.get(scaleYArgument)
            val scaleZ = context.get(scaleZArgument)
            handleItemDisplay(
                sender,
                position,
                item,
                Vec(translationX.toDouble(), translationY.toDouble(), translationZ.toDouble()),
                floatArrayOf(rotX, rotY, rotZ, rotW),
                Vec(scaleX.toDouble(), scaleY.toDouble(), scaleZ.toDouble())
            )
        }, itemLiteral, positionArgument, itemArgument, translationXArgument, translationYArgument, translationZArgument,
            rotXArgument, rotYArgument, rotZArgument, rotWArgument, scaleXArgument, scaleYArgument, scaleZArgument)

        // Default executor for usage message
        setDefaultExecutor { sender, _ ->
            sender.sendMessage(Component.text("Usage: /summondisplay <text|block|item> <pos> <type-specific> [translation] [rotation] [scale]"))
        }
    }

    private fun handleTextDisplay(
        sender: CommandSender,
        position: RelativeVec,
        component: Component,
        translation: Vec = Vec(0.0, 0.0, 0.0),
        rotation: FloatArray = floatArrayOf(0f, 0f, 0f, 1f),
        scale: Vec = Vec(1.0, 1.0, 1.0)
    ) {
        val instance = getInstanceFromSender(sender) ?: return
        val finalPosition = position.fromSender(sender)

        val entity = Entity(EntityType.TEXT_DISPLAY)
        val metadata = entity.entityMeta as TextDisplayMeta
        metadata.text = component
        metadata.translation = translation
        metadata.leftRotation = rotation
        metadata.scale = scale
        entity.setNoGravity(true)
        entity.setInstance(instance, Pos(finalPosition))

        sender.sendMessage(Component.text("Spawned text display at ${finalPosition.x()}, ${finalPosition.y()}, ${finalPosition.z()}"))
    }

    private fun handleBlockDisplay(
        sender: CommandSender,
        position: RelativeVec,
        block: net.minestom.server.instance.block.Block,
        translation: Vec = Vec(0.0, 0.0, 0.0),
        rotation: FloatArray = floatArrayOf(0f, 0f, 0f, 1f),
        scale: Vec = Vec(1.0, 1.0, 1.0)
    ) {
        val instance = getInstanceFromSender(sender) ?: return
        val finalPosition = position.fromSender(sender)

        val entity = Entity(EntityType.BLOCK_DISPLAY)
        val metadata = entity.entityMeta as BlockDisplayMeta
        metadata.setBlockState(block)
        metadata.translation = translation
        metadata.leftRotation = rotation
        metadata.scale = scale
        entity.setNoGravity(true)
        entity.setInstance(instance, Pos(finalPosition))

        sender.sendMessage(Component.text("Spawned block display at ${finalPosition.x()}, ${finalPosition.y()}, ${finalPosition.z()}"))
    }

    private fun handleItemDisplay(
        sender: CommandSender,
        position: RelativeVec,
        item: net.minestom.server.item.ItemStack,
        translation: Vec = Vec(0.0, 0.0, 0.0),
        rotation: FloatArray = floatArrayOf(0f, 0f, 0f, 1f),
        scale: Vec = Vec(1.0, 1.0, 1.0)
    ) {
        val instance = getInstanceFromSender(sender) ?: return
        val finalPosition = position.fromSender(sender)

        val entity = Entity(EntityType.ITEM_DISPLAY)
        val metadata = entity.entityMeta as ItemDisplayMeta
        metadata.itemStack = item
        metadata.translation = translation
        metadata.leftRotation = rotation
        metadata.scale = scale
        entity.setNoGravity(true)
        entity.setInstance(instance, Pos(finalPosition))

        sender.sendMessage(Component.text("Spawned item display at ${finalPosition.x()}, ${finalPosition.y()}, ${finalPosition.z()}"))
    }

    private fun getInstanceFromSender(sender: CommandSender): Instance? {
        val instance = (sender as? Player)?.instance
            ?: MinecraftServer.getInstanceManager().instances.firstOrNull()

        if (instance == null) {
            sender.sendMessage(Component.text("No instance available"))
        }

        return instance
    }
}
