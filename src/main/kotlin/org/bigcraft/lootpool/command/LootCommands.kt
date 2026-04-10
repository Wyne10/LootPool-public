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
import org.bigcraft.lootpool.core.LootManager
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CreateLootCommand(lootManager: LootManager) : SubCommand("create") {
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
            if (lootManager.mapKeys.contains(key)) {
                if (sender.hasPermission("lootpool.modify"))
                    sender.placeholderComponent("info-loot-already-exists", "key" replace key).sendMessage(sender)
                else
                    sender.placeholderComponent("error-loot-already-exists", "key" replace key).sendMessage(sender)
                return@PlayerCommandExecutor
            }
            lootManager.writeLoot(KeyedLoot(key, Loot(sender.inventory.itemInMainHand.clone().apply { amount = 1 }, weight,
                        minAmount.coerceAtMost(maxAmount), maxAmount.coerceAtLeast(minAmount))))
            sender.placeholderComponent("success-loot-create", "key" replace key).sendMessage(sender)
        })
}

class ModifyLootCommand(lootManager: LootManager) : SubCommand("modify") {
    override val command: CommandAPICommand = super.command
        .withPermission("lootpool.modify")
        .withArguments(lootKey("key", lootManager))
        .withOptionalArguments(IntegerArgument("minAmount", 1, 64))
        .withOptionalArguments(IntegerArgument("maxAmount", 1, 64))
        .withOptionalArguments(IntegerArgument("weight", 0))
        .executesPlayer(PlayerCommandExecutor { sender, args ->
            val key = args.getOrDefaultRaw("key", "")
            assertLootExists(key, sender, lootManager)
            assertLootNotNull(key, sender)
            val loot = lootManager.getLoot(key)!!
            val minAmount = args.getByClassOrDefault("minAmount", Int::class.java, loot.loot.minAmount)
            val maxAmount = args.getByClassOrDefault("maxAmount", Int::class.java, loot.loot.maxAmount)
            val weight = args.getByClassOrDefault("weight", Int::class.java, 1)
            lootManager.writeLoot(KeyedLoot(key, Loot(sender.inventory.itemInMainHand.clone().apply { amount = 1 }, weight,
                        minAmount.coerceAtMost(maxAmount), maxAmount.coerceAtLeast(minAmount))))
            sender.placeholderComponent("success-loot-create", "key" replace key).sendMessage(sender)
        })
}

class RemoveLootCommand(lootManager: LootManager) : SubCommand("remove") {
    override val command: CommandAPICommand = super.command
        .withPermission("lootpool.remove")
        .withArguments(lootKey("key", lootManager))
        .executes(CommandExecutor { sender, args ->
            val key = args.getOrDefaultRaw("key", "")
            assertLootExists(key, sender, lootManager)
            lootManager.removeLoot(key)
            sender.placeholderComponent("success-loot-remove", "key" replace key).sendMessage(sender)
        })
}

class LootInfoCommand(lootManager: LootManager) : SubCommand("info") {
    override val command: CommandAPICommand = super.command
        .withPermission("lootpool.info")
        .withArguments(lootKey("key", lootManager))
        .executes(CommandExecutor { sender, args ->
            val key = args.getOrDefaultRaw("key", "")
            assertLootExists(key, sender, lootManager)
            val loot = lootManager.getLoot(key)!!
            sender.placeholderComponent(
                "info-loot",
                "key" replace key,
                "loot-type" replace loot.loot.item.type.name,
                "min-amount" replace loot.loot.minAmount,
                "max-amount" replace loot.loot.maxAmount,
            ).replace("loot-name" replaceComponent loot.loot.item.nameComponent).sendMessage(sender)
        })
}

fun lootKey(nodeName: String, lootManager: LootManager): Argument<String> =
    StringArgument(nodeName)
        .replaceSuggestions(ArgumentSuggestions.stringCollection { _ -> lootManager.mapKeys })

fun assertLootExists(key: String, sender: CommandSender, lootManager: LootManager) {
    if (lootManager.mapKeys.contains(key)) return
    throw CommandAPIBukkit.failWithAdventureComponent(
        sender.placeholderComponent("error-lootpool-not-found", "key" replace key).get()
    )
}

fun assertLootNotNull(key: String, sender: Player) {
    if (sender.inventory.itemInMainHand.isNotNullOrAir()) return
    throw CommandAPIBukkit.failWithAdventureComponent(
        sender.placeholderComponent("error-empty-loot", "key" replace key).get()
    )
}
