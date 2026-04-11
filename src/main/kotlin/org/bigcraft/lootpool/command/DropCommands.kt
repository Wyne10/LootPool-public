package org.bigcraft.lootpool.command

import dev.jorel.commandapi.CommandAPIBukkit
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.BooleanArgument
import dev.jorel.commandapi.arguments.EntitySelectorArgument
import dev.jorel.commandapi.arguments.IntegerArgument
import dev.jorel.commandapi.arguments.LocationArgument
import dev.jorel.commandapi.arguments.LocationType
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.CommandExecutor
import me.wyne.wutils.common.kotlin.inventory.addOrDrop
import me.wyne.wutils.common.kotlin.item.isNotNullOrAir
import me.wyne.wutils.i18n.kotlin.placeholderComponent
import me.wyne.wutils.i18n.kotlin.replace
import org.bigcraft.lootpool.api.Loot
import org.bigcraft.lootpool.api.BasicLootPool
import org.bigcraft.lootpool.api.KeyedLoot
import org.bigcraft.lootpool.api.LootPool
import org.bigcraft.lootpool.core.LootPoolManager
import org.bukkit.Location
import org.bukkit.block.Container
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

class GiveCommand(lootPoolManager: LootPoolManager) : SubCommand("give") {
    override val command: CommandAPICommand = super.command
        .withPermission("lootpool.give")
        .withArguments(key("key", lootPoolManager))
        .withArguments(EntitySelectorArgument.OnePlayer("target"))
        .withOptionalArguments(amountArgument("amount"), BooleanArgument("unique"), IntegerArgument("slots", 0))
        .executes(CommandExecutor { sender, args ->
            val key = args.getOrDefaultRaw("key", "")
            assertExists(key, sender, lootPoolManager)
            val target = args.getByClass("target", Player::class.java)!!
            val amount = args.getByClass("amount", String::class.java)
            val lootList = lootPoolManager.getLootPool(key)!!.lootList
            val unique = args.getByClass("unique", Boolean::class.java) ?: false
            val slots = args.getByClass("slots", Int::class.java) ?: lootList.size
            val lootPool = BasicLootPool("dummy", lootList.toMutableList())
            val populated = List(slots) {
                lootPool.random.let {
                    if (unique) lootPool.lootPool.remove(it)
                    it!!.item.clone().apply { this.amount = getAmount(amount, it) }
                }
            }
            target.addOrDrop(true, *populated.toTypedArray())
            sender.placeholderComponent("success-loot-drop", "key" replace key, "amount" replace slots).sendMessagePlayer(sender)
        })
}

class DropCommand(lootPoolManager: LootPoolManager) : SubCommand("drop") {
    override val command: CommandAPICommand = super.command
        .withPermission("lootpool.drop")
        .withArguments(key("key", lootPoolManager))
        .withArguments(LocationArgument("location", LocationType.PRECISE_POSITION))
        .withOptionalArguments(amountArgument("amount"), BooleanArgument("unique"), IntegerArgument("slots", 0))
        .executes(CommandExecutor { sender, args ->
            val key = args.getOrDefaultRaw("key", "")
            assertExists(key, sender, lootPoolManager)
            val location = args.getByClass("location", Location::class.java)!!
            val amount = args.getByClass("amount", String::class.java)
            val lootList = lootPoolManager.getLootPool(key)!!.lootList
            val unique = args.getByClass("unique", Boolean::class.java) ?: false
            val slots = args.getByClass("slots", Int::class.java) ?: lootList.size
            val lootPool = BasicLootPool("dummy", lootList.toMutableList())
            val populated = List(slots) {
                lootPool.random.let {
                    if (unique) lootPool.lootPool.remove(it)
                    it!!.item.clone().apply { this.amount = getAmount(amount, it) }
                }
            }
            populated
                .filter { it.isNotNullOrAir() }
                .forEach {
                    location.world.dropItem(location, it)
                }
            sender.placeholderComponent("success-loot-drop", "key" replace key, "amount" replace slots).sendMessagePlayer(sender)
        })
}

class InsertCommand(lootPoolManager: LootPoolManager) : SubCommand("insert") {
    override val command: CommandAPICommand = super.command
        .withPermission("lootpool.insert")
        .withArguments(key("key", lootPoolManager))
        .withArguments(LocationArgument("location", LocationType.BLOCK_POSITION))
        .withOptionalArguments(
            amountArgument("amount"), BooleanArgument("random"),
            BooleanArgument("unique"), IntegerArgument("slots", 0))
        .executes(CommandExecutor { sender, args ->
            val key = args.getOrDefaultRaw("key", "")
            assertExists(key, sender, lootPoolManager)
            val location = args.getByClass("location", Location::class.java)!!
            val amount = args.getByClass("amount", String::class.java)
            val container = location.block.state as? Container
                ?: throw CommandAPIBukkit.failWithAdventureComponent(
                    sender.placeholderComponent(
                        "error-not-a-container",
                        "x" replace location.blockX,
                        "y" replace location.blockY,
                        "z" replace location.blockZ).get()
                )
            val lootList = lootPoolManager.getLootPool(key)!!.lootList
            val unique = args.getByClass("unique", Boolean::class.java) ?: false
            val slots = args.getByClass("slots", Int::class.java) ?: lootList.size
            val random = args.getByClass("random", Boolean::class.java) ?: false
            val lootPool = BasicLootPool("dummy", lootList.toMutableList())
            val populated = List(slots) {
                lootPool.random.let {
                    if (unique) lootPool.lootPool.remove(it)
                    it!!.item.clone().apply { this.amount = getAmount(amount, it) }
                }
            }
            if (random)
                LootPool.populateRandomly(populated, container.inventory)
            else
                LootPool.populate(populated, container.inventory)
            sender.placeholderComponent("success-loot-drop", "key" replace key, "amount" replace slots).sendMessagePlayer(sender)
        })
}

class FillCommand(lootPoolManager: LootPoolManager) : SubCommand("fill") {
    override val command: CommandAPICommand = super.command
        .withPermission("lootpool.fill")
        .withArguments(key("key", lootPoolManager))
        .withArguments(LocationArgument("location", LocationType.BLOCK_POSITION))
        .executes(CommandExecutor { sender, args ->
            val key = args.getOrDefaultRaw("key", "")
            assertExists(key, sender, lootPoolManager)
            val location = args.getByClass("location", Location::class.java)!!
            val container = location.block.state as? Container
                ?: throw CommandAPIBukkit.failWithAdventureComponent(
                    sender.placeholderComponent(
                        "error-not-a-container",
                        "x" replace location.blockX,
                        "y" replace location.blockY,
                        "z" replace location.blockZ).get()
                )
            val lootPool = lootPoolManager.getLootPool(key)!!
            lootPool.populate(container.inventory)
            sender.placeholderComponent("success-loot-drop", "key" replace key, "amount" replace container.inventory.size).sendMessagePlayer(sender)
        })
}

class ProjectCommand(lootPoolManager: LootPoolManager) : SubCommand("project") {
    override val command: CommandAPICommand = super.command
        .withPermission("lootpool.project")
        .withArguments(key("key", lootPoolManager))
        .withArguments(EntitySelectorArgument.OnePlayer("target"))
        .withOptionalArguments(amountArgument("amount"))
        .executes(CommandExecutor { sender, args ->
            val key = args.getOrDefaultRaw("key", "")
            assertExists(key, sender, lootPoolManager)
            val target = args.getByClass("target", Player::class.java)!!
            val amount = args.getByClass("amount", String::class.java)
            val lootList = lootPoolManager.getLootPool(key)!!.lootList
            val populated = mutableListOf<ItemStack>()
            lootList.forEach {
                populated.add(
                    it.item.clone().apply { this.amount = getAmount(amount, it) }
                )
            }
            target.addOrDrop(*populated.toTypedArray())
            sender.placeholderComponent("success-loot-drop", "key" replace key, "amount" replace lootList.size).sendMessagePlayer(sender)
        })
}

fun amountArgument(nodeName: String): Argument<String> =
    StringArgument(nodeName)
        .replaceSuggestions(ArgumentSuggestions.strings("min-amount", "max-amount", "random-amount", "<amount>"))

fun getAmount(amountArgument: String?, loot: Loot): Int =
    when(amountArgument) {
        "min-amount" -> loot.minAmount
        "max-amount" -> loot.maxAmount
        "random-amount" -> Random.nextInt(loot.minAmount, loot.maxAmount + 1)
        null -> Random.nextInt(loot.minAmount, loot.maxAmount + 1)
        else -> amountArgument.toIntOrNull() ?: 1
    }

fun key(nodeName: String, lootPoolManager: LootPoolManager): Argument<String> =
    StringArgument(nodeName)
        .replaceSuggestions(ArgumentSuggestions.stringCollection { _ -> lootPoolManager.mapKeys })

private fun assertExists(key: String, sender: CommandSender, lootPoolManager: LootPoolManager) {
    if (lootPoolManager.mapKeys.contains(key)) return
    throw CommandAPIBukkit.failWithAdventureComponent(
        sender.placeholderComponent("error-lootpool-not-found", "key" replace key).get()
    )
}
