package org.bigcraft.lootpool.command

import dev.jorel.commandapi.CommandAPIBukkit
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.IntegerArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.CommandExecutor
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import me.wyne.wutils.common.kotlin.item.isNotNullOrAir
import me.wyne.wutils.i18n.kotlin.placeholderComponent
import me.wyne.wutils.i18n.kotlin.replace
import me.wyne.wutils.i18n.kotlin.replaceComponent
import org.bigcraft.lootpool.api.KeyedLoot
import org.bigcraft.lootpool.api.Loot
import org.bigcraft.lootpool.core.LootPoolManager
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CreateLootCommand(lootPoolManager: LootPoolManager) : SubCommand("create") {
    override val command: CommandAPICommand = super.command
        .withPermission("lootpool.create")
        .withArguments(StringArgument("key"))
        .withOptionalArguments(IntegerArgument("minAmount", 1, 64))
        .withOptionalArguments(IntegerArgument("maxAmount", 1, 64))
        .withOptionalArguments(IntegerArgument("weight", 0))
        .executesPlayer(PlayerCommandExecutor { sender, args ->
            val key = args.getOrDefaultRaw("key", "")
            val minAmount = args.getByClassOrDefault("minAmount", Int::class.java, Loot.EMPTY.minAmount)
            val maxAmount = args.getByClassOrDefault("maxAmount", Int::class.java, Loot.EMPTY.maxAmount)
            val weight = args.getByClassOrDefault("weight", Int::class.java, Loot.EMPTY.weight)
            assertLootNotNull(key, sender)
            if (lootPoolManager.mapKeys.contains(key)) {
                if (sender.hasPermission("lootpool.modify"))
                    sender.placeholderComponent("info-loot-already-exists", "key" replace key).sendMessage(sender)
                else
                    sender.placeholderComponent("error-loot-already-exists", "key" replace key).sendMessage(sender)
                return@PlayerCommandExecutor
            }
            lootPoolManager.writeLootPool(KeyedLoot(key, Loot(sender.inventory.itemInMainHand.clone().apply { amount = 1 }, weight,
                        minAmount.coerceAtMost(maxAmount), maxAmount.coerceAtLeast(minAmount))))
            sender.placeholderComponent("success-loot-create", "key" replace key).sendMessage(sender)
        })
}

class ModifyLootCommand(lootPoolManager: LootPoolManager) : SubCommand("modify") {
    override val command: CommandAPICommand = super.command
        .withPermission("lootpool.modify")
        .withArguments(lootKey("key", lootPoolManager))
        .withOptionalArguments(IntegerArgument("minAmount", 1, 64))
        .withOptionalArguments(IntegerArgument("maxAmount", 1, 64))
        .withOptionalArguments(IntegerArgument("weight", 0))
        .executesPlayer(PlayerCommandExecutor { sender, args ->
            val key = args.getOrDefaultRaw("key", "")
            assertLootExists(key, sender, lootPoolManager)
            assertLootNotNull(key, sender)
            val loot = lootPoolManager.getLootPool(key)!! as KeyedLoot
            val minAmount = args.getByClassOrDefault("minAmount", Int::class.java, loot.loot.minAmount)
            val maxAmount = args.getByClassOrDefault("maxAmount", Int::class.java, loot.loot.maxAmount)
            val weight = args.getByClassOrDefault("weight", Int::class.java, 1)
            lootPoolManager.writeLootPool(KeyedLoot(key, Loot(sender.inventory.itemInMainHand.clone().apply { amount = 1 }, weight,
                        minAmount.coerceAtMost(maxAmount), maxAmount.coerceAtLeast(minAmount))))
            sender.placeholderComponent("success-loot-create", "key" replace key).sendMessage(sender)
        })
}

class RemoveLootCommand(lootPoolManager: LootPoolManager) : SubCommand("remove") {
    override val command: CommandAPICommand = super.command
        .withPermission("lootpool.remove")
        .withArguments(lootKey("key", lootPoolManager))
        .executes(CommandExecutor { sender, args ->
            val key = args.getOrDefaultRaw("key", "")
            assertLootExists(key, sender, lootPoolManager)
            lootPoolManager.removeLootPool(key)
            sender.placeholderComponent("success-loot-remove", "key" replace key).sendMessage(sender)
        })
}

class LootInfoCommand(lootPoolManager: LootPoolManager) : SubCommand("info") {
    override val command: CommandAPICommand = super.command
        .withPermission("lootpool.info")
        .withArguments(lootKey("key", lootPoolManager))
        .executes(CommandExecutor { sender, args ->
            val key = args.getOrDefaultRaw("key", "")
            assertLootExists(key, sender, lootPoolManager)
            val loot = lootPoolManager.getLootPool(key)!! as KeyedLoot
            sender.placeholderComponent(
                "info-loot",
                "key" replace key,
                "loot-type" replace loot.loot.item.type.name,
                "min-amount" replace loot.loot.minAmount,
                "max-amount" replace loot.loot.maxAmount,
            ).replace("loot-name" replaceComponent loot.loot.item.nameComponent).sendMessage(sender)
        })
}

fun lootKey(nodeName: String, lootPoolManager: LootPoolManager): Argument<String> =
    StringArgument(nodeName)
        .replaceSuggestions(ArgumentSuggestions.stringCollection { _ -> lootPoolManager.getMapOf(KeyedLoot::class.java).keys })

fun assertLootExists(key: String, sender: CommandSender, lootPoolManager: LootPoolManager) {
    if (lootPoolManager.getMapOf(KeyedLoot::class.java).containsKey(key)) return
    throw CommandAPIBukkit.failWithAdventureComponent(
        sender.placeholderComponent("error-loot-not-found", "key" replace key).get()
    )
}

private fun assertLootNotNull(key: String, sender: Player) {
    if (sender.inventory.itemInMainHand.isNotNullOrAir()) return
    throw CommandAPIBukkit.failWithAdventureComponent(
        sender.placeholderComponent("error-empty-loot", "key" replace key).get()
    )
}
