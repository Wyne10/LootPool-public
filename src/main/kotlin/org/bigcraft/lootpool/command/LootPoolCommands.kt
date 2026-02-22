package org.bigcraft.lootpool.command

import dev.jorel.commandapi.CommandAPIBukkit
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.CommandExecutor
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import me.wyne.wutils.i18n.I18n
import me.wyne.wutils.i18n.kotlin.placeholderComponent
import me.wyne.wutils.i18n.kotlin.reduce
import me.wyne.wutils.i18n.kotlin.replace
import me.wyne.wutils.i18n.kotlin.replaceComponent
import net.kyori.adventure.text.Component
import org.bigcraft.lootpool.core.LootPoolManager
import org.bigcraft.lootpool.gui.LootPoolGui
import org.bukkit.command.CommandSender

class CreateCommand(lootPoolManager: LootPoolManager) : SubCommand("create") {
    override val command: CommandAPICommand = super.command
        .withPermission("lootpool.create")
        .withArguments(StringArgument("key"))
        .executesPlayer(PlayerCommandExecutor { sender, args ->
            val key = args.getOrDefaultRaw("key", "")
            if (lootPoolManager.mapKeys.contains(key)) {
                if (sender.hasPermission("lootpool.modify"))
                    sender.placeholderComponent("info-lootpool-already-exists", "key" replace key).sendMessage(sender)
                else
                    sender.placeholderComponent("error-lootpool-already-exists", "key" replace key).sendMessage(sender)
                return@PlayerCommandExecutor
            }
            LootPoolGui(key, sender)
        })
}

class ModifyCommand(lootPoolManager: LootPoolManager) : SubCommand("modify") {
    override val command: CommandAPICommand = super.command
        .withPermission("lootpool.modify")
        .withArguments(lootPoolKey("key", lootPoolManager))
        .executesPlayer(PlayerCommandExecutor { sender, args ->
            val key = args.getOrDefaultRaw("key", "")
            assertLootPoolExists(key, sender, lootPoolManager)
            val lootPool = lootPoolManager.getLootPool(key)
            LootPoolGui(key, sender, lootPool!!)
        })
}

class RemoveCommand(lootPoolManager: LootPoolManager) : SubCommand("remove") {
    override val command: CommandAPICommand = super.command
        .withPermission("lootpool.remove")
        .withArguments(lootPoolKey("key", lootPoolManager))
        .executes(CommandExecutor { sender, args ->
            val key = args.getOrDefaultRaw("key", "")
            assertLootPoolExists(key, sender, lootPoolManager)
            lootPoolManager.removeLootPool(key)
            sender.placeholderComponent("success-lootpool-remove", "key" replace key).sendMessage(sender)
        })
}

class InfoCommand(lootPoolManager: LootPoolManager) : SubCommand("info") {
    override val command: CommandAPICommand = super.command
        .withPermission("lootpool.info")
        .withArguments(lootPoolKey("key", lootPoolManager))
        .executes(CommandExecutor { sender, args ->
            val key = args.getOrDefaultRaw("key", "")
            assertLootPoolExists(key, sender, lootPoolManager)
            val lootPool = lootPoolManager.getLootPool(key)!!
            val weightSorted = lootPool.lootPool.sortedByDescending { it.weight }
            val totalWeight = weightSorted.sumOf { it.weight.toDouble() }
            val percentage = weightSorted.map { (it.weight / totalWeight) * 100 }
            val lootList = weightSorted
                .mapIndexed { index, loot ->
                    sender.placeholderComponent(
                        "info-lootpool-loot",
                        "loot-name" replace I18n.global.component().toString(loot.item.nameComponent),
                        "weight" replace loot.weight,
                        "min-amount" replace loot.minAmount,
                        "max-amount" replace loot.maxAmount,
                        "percentage" replace String.format("%.2f", percentage[index])
                    )
                }.reduce() ?: Component.empty()
            sender.placeholderComponent("info-lootpool", "key" replace key)
                .replace("loot-list" replaceComponent lootList)
                .sendMessage(sender)
        })
}

class CloneCommand(lootPoolManager: LootPoolManager) : SubCommand("clone") {
    override val command: CommandAPICommand = super.command
        .withPermission("lootpool.clone")
        .withArguments(lootPoolKey("key", lootPoolManager))
        .withArguments(StringArgument("newKey"))
        .executesPlayer(PlayerCommandExecutor { sender, args ->
            val key = args.getOrDefaultRaw("key", "")
            val lootPool = lootPoolManager.getLootPool(key)!!
            assertLootPoolExists(key, sender, lootPoolManager)
            val newKey = args.getOrDefaultRaw("newKey", "")
            if (lootPoolManager.mapKeys.contains(newKey)) {
                if (sender.hasPermission("lootpool.modify"))
                    sender.placeholderComponent("info-lootpool-already-exists", "key" replace newKey).sendMessage(sender)
                else
                    sender.placeholderComponent("error-lootpool-already-exists", "key" replace newKey).sendMessage(sender)
                return@PlayerCommandExecutor
            }
            LootPoolGui(newKey, sender, lootPool)
        })
}

fun lootPoolKey(nodeName: String, lootPoolManager: LootPoolManager): Argument<String> =
    StringArgument(nodeName)
        .replaceSuggestions(ArgumentSuggestions.stringCollection { _ -> lootPoolManager.mapKeys })

fun assertLootPoolExists(key: String, sender: CommandSender, lootPoolManager: LootPoolManager) {
    if (lootPoolManager.mapKeys.contains(key)) return
    throw CommandAPIBukkit.failWithBaseComponents(
        *sender.placeholderComponent("error-lootpool-not-found", "key" replace key).bungee()
    )
}