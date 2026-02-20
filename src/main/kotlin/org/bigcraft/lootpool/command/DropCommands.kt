package org.bigcraft.lootpool.command

import dev.jorel.commandapi.CommandAPIBukkit
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.EntitySelectorArgument
import dev.jorel.commandapi.arguments.LocationArgument
import dev.jorel.commandapi.arguments.LocationType
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.CommandExecutor
import me.wyne.wutils.common.kotlin.inventory.addOrDrop
import me.wyne.wutils.i18n.kotlin.placeholderComponent
import me.wyne.wutils.i18n.kotlin.replace
import org.bigcraft.lootpool.api.CommonLootProvider
import org.bigcraft.lootpool.api.LootPool
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Container
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class GiveCommand(commonLootProvider: CommonLootProvider) : SubCommand("give") {
    override val command: CommandAPICommand = super.command
        .withPermission("lootpool.give")
        .withArguments(lootKey("key", commonLootProvider))
        .withArguments(EntitySelectorArgument.ManyPlayers("targets"))
        .executes(CommandExecutor { sender, args ->
            val key = args.getOrDefaultRaw("key", "")
            assertLootExists(key, sender, commonLootProvider)
            val targets = args.getByClass("targets", List::class.java) as List<Player>
            val lootList = commonLootProvider.getLootList(key)!!
            val lootPool = LootPool("dummy", lootList)
            targets.forEach { target ->
                val populated = lootPool.populate(lootList.size)
                target.addOrDrop(*populated.toTypedArray())
            }
            sender.placeholderComponent("success-loot-drop", "key" replace key, "amount" replace lootList.size).sendMessage(sender)
        })
}

class DropCommand(commonLootProvider: CommonLootProvider) : SubCommand("drop") {
    override val command: CommandAPICommand = super.command
        .withPermission("lootpool.drop")
        .withArguments(lootKey("key", commonLootProvider))
        .withArguments(LocationArgument("location", LocationType.PRECISE_POSITION))
        .executes(CommandExecutor { sender, args ->
            val key = args.getOrDefaultRaw("key", "")
            assertLootExists(key, sender, commonLootProvider)
            val location = args.getByClass("location", Location::class.java)!!
            val lootList = commonLootProvider.getLootList(key)!!
            val lootPool = LootPool("dummy", lootList)
            val populated = lootPool.populate(lootList.size)
            populated.forEach {
                location.world.dropItem(location, it)
            }
            sender.placeholderComponent("success-loot-drop", "key" replace key, "amount" replace lootList.size).sendMessage(sender)
        })
}

class InsertCommand(commonLootProvider: CommonLootProvider) : SubCommand("insert") {
    override val command: CommandAPICommand = super.command
        .withPermission("lootpool.insert")
        .withArguments(lootKey("key", commonLootProvider))
        .withArguments(LocationArgument("location"))
        .executes(CommandExecutor { sender, args ->
            val key = args.getOrDefaultRaw("key", "")
            assertLootExists(key, sender, commonLootProvider)
            val location = args.getByClass("location", Location::class.java)!!
            val container = location.block.state as? Container
                ?: throw CommandAPIBukkit.failWithBaseComponents(
                    *sender.placeholderComponent(
                        "error-not-a-container",
                        "x" replace location.blockX,
                        "y" replace location.blockY,
                        "z" replace location.blockZ).bungee()
                )
            val lootList = commonLootProvider.getLootList(key)!!
                .filterNot { it.item().type == Material.AIR }
            val lootPool = LootPool("dummy", lootList)
            lootPool.populate(container.inventory, lootList.size)
            sender.placeholderComponent("success-loot-drop", "key" replace key, "amount" replace lootList.size).sendMessage(sender)
        })
}

class FillCommand(commonLootProvider: CommonLootProvider) : SubCommand("fill") {
    override val command: CommandAPICommand = super.command
        .withPermission("lootpool.fill")
        .withArguments(lootKey("key", commonLootProvider))
        .withArguments(LocationArgument("location"))
        .executes(CommandExecutor { sender, args ->
            val key = args.getOrDefaultRaw("key", "")
            assertLootExists(key, sender, commonLootProvider)
            val location = args.getByClass("location", Location::class.java)!!
            val container = location.block.state as? Container
                ?: throw CommandAPIBukkit.failWithBaseComponents(
                    *sender.placeholderComponent(
                        "error-not-a-container",
                        "x" replace location.blockX,
                        "y" replace location.blockY,
                        "z" replace location.blockZ).bungee()
                )
            val lootList = commonLootProvider.getLootList(key)!!
            val lootPool = LootPool("dummy", lootList)
            lootPool.populateRandomly(container.inventory, container.inventory.size)
            sender.placeholderComponent("success-loot-drop", "key" replace key, "amount" replace container.inventory.size).sendMessage(sender)
        })
}

fun lootKey(nodeName: String, commonLootProvider: CommonLootProvider): Argument<String> =
    StringArgument(nodeName)
        .replaceSuggestions(ArgumentSuggestions.stringCollection { _ -> commonLootProvider.lootKeys })

fun assertLootExists(key: String, sender: CommandSender, commonLootProvider: CommonLootProvider) {
    if (commonLootProvider.lootKeys.contains(key)) return
    throw CommandAPIBukkit.failWithBaseComponents(
        *sender.placeholderComponent("error-lootpool-not-found", "key" replace key).bungee()
    )
}
