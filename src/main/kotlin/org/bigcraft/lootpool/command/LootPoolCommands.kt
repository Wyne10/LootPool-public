@file:Suppress("UNCHECKED_CAST")

package org.bigcraft.lootpool.command

import dev.jorel.commandapi.CommandAPIBukkit
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.IntegerArgument
import dev.jorel.commandapi.arguments.ListArgumentBuilder
import dev.jorel.commandapi.arguments.MapArgumentBuilder
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.CommandExecutor
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import me.wyne.wutils.i18n.kotlin.placeholderComponent
import me.wyne.wutils.i18n.kotlin.reduce
import me.wyne.wutils.i18n.kotlin.replace
import me.wyne.wutils.i18n.kotlin.replaceComponent
import net.kyori.adventure.text.Component
import org.bigcraft.lootpool.api.CompositeLootPool
import org.bigcraft.lootpool.api.LootPool
import org.bigcraft.lootpool.api.LootPoolProvider
import org.bigcraft.lootpool.api.MultiLootPool
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender

class InfoCommand<T : LootPool>(lootPoolProvider: LootPoolProvider, lootPoolType: Class<T> = LootPool::class.java as Class<T>) : SubCommand("info") {
    override val command: CommandAPICommand = super.command
        .withPermission("lootpool.info")
        .withArguments(lootPoolKey("key") { lootPoolProvider.getMapOf(lootPoolType) })
        .executes(CommandExecutor { sender, args ->
            val key = args.getByClass("key", String::class.java)!!
            assertLootPoolExists(key, sender, lootPoolProvider.getMapOf(lootPoolType))
            val lootPool = lootPoolProvider.getLootPool(key)!!
            if (lootPool is CompositeLootPool)
                return@CommandExecutor displayCompositeInfo(sender, lootPool)
            val weightSorted = lootPool.lootList.sortedByDescending { it.weight }
            val totalWeight = weightSorted.sumOf { it.weight.toDouble() }
            val percentage = weightSorted.map { (it.weight / totalWeight) * 100 }
            val lootList = weightSorted
                .mapIndexed { index, loot ->
                    sender.placeholderComponent(
                        "info-lootpool-loot",
                        "loot-type" replace loot.item.type.name,
                        "weight" replace loot.weight,
                        "min-amount" replace loot.minAmount,
                        "max-amount" replace loot.maxAmount,
                        "percentage" replace String.format("%.2f", percentage[index])
                    ).replace("loot-name" replaceComponent loot.item.nameComponent)
                }.reduce() ?: Component.empty()
            sender.placeholderComponent("info-lootpool", "key" replace key, "type" replace lootPool.javaClass.simpleName)
                .replace("loot-list" replaceComponent lootList)
                .sendMessage(sender)
        })
}

class RemoveCommand<T : LootPool>(lootPoolProvider: LootPoolProvider, lootPoolType: Class<T> = LootPool::class.java as Class<T>) : SubCommand("remove") {
    override val command: CommandAPICommand = super.command
        .withPermission("lootpool.remove")
        .withArguments(lootPoolKey("key") { lootPoolProvider.getMapOf(lootPoolType) })
        .executes(CommandExecutor { sender, args ->
            val key = args.getByClass("key", String::class.java)!!
            assertLootPoolExists(key, sender, lootPoolProvider.getMapOf(lootPoolType))
            lootPoolProvider.removeLootPool(key)
            sender.placeholderComponent("success-lootpool-remove", "key" replace key).sendMessage(sender)
        })
}

class ComposeCommand<T : LootPool>(lootPoolProvider: LootPoolProvider, lootPoolType: Class<T> = LootPool::class.java as Class<T>) : SubCommand("compose") {
    override val command: CommandAPICommand = super.command
        .withPermission("lootpool.create")
        .withArguments(StringArgument("key"))
        .withArguments(
            MapArgumentBuilder<String, Int>("pools")
                .withKeyMapper { s -> s }
                .withValueMapper { s -> s.toInt() }
                .withKeyList { lootPoolProvider.getMapOf(lootPoolType).keys.toList() }
                .withoutValueList(true)
                .build())
        .executes(CommandExecutor { sender, args ->
            val key = args.getByClass("key", String::class.java)!!
            assertLootPoolNotExists(key, sender, lootPoolProvider.getMapOf(lootPoolType))
            val pools = args.getByClassOrDefault("pools", Map::class.java, emptyMap<String, Int>()) as Map<String, Int>
            lootPoolProvider.writeLootPool(CompositeLootPool(key, pools.toMap()))
            sender.placeholderComponent("success-lootpool-create", "key" replace key).sendMessage(sender)
        })
}

class IncludeCommand<T : LootPool>(lootPoolProvider: LootPoolProvider, lootPoolType: Class<T> = LootPool::class.java as Class<T>) : SubCommand("include") {
    override val command: CommandAPICommand = super.command
        .withPermission("lootpool.create")
        .withArguments(StringArgument("key"))
        .withArguments(
            ListArgumentBuilder<String>("pools")
                .withList { _ -> lootPoolProvider.getMapOf(lootPoolType).keys }
                .withStringMapper()
                .buildGreedy())
        .executes(CommandExecutor { sender, args ->
            val key = args.getByClass("key", String::class.java)!!
            assertLootPoolNotExists(key, sender, lootPoolProvider.getMapOf(lootPoolType))
            val pools = args.getByClassOrDefault("pools", List::class.java, emptyList<String>()) as List<String>
            lootPoolProvider.writeLootPool(MultiLootPool(key, pools.toSet()))
            sender.placeholderComponent("success-lootpool-create", "key" replace key).sendMessage(sender)
        })
}

class PreviewCommand<T : LootPool>(lootPoolProvider: LootPoolProvider, lootPoolType: Class<T> = LootPool::class.java as Class<T>) : SubCommand("preview") {
    override val command: CommandAPICommand = super.command
        .withPermission("lootpool.info")
        .withArguments(lootPoolKey("key") { lootPoolProvider.getMapOf(lootPoolType) })
        .withOptionalArguments(IntegerArgument("size", 1, 6))
        .executesPlayer(PlayerCommandExecutor { sender, args ->
            val key = args.getByClass("key", String::class.java)!!
            assertLootPoolExists(key, sender, lootPoolProvider.getMapOf(lootPoolType))
            val lootPool = lootPoolProvider.getLootPool(key)!!
            val size = args.getByClassOrDefault("size", Int::class.java, 3)
            val inventory = Bukkit.createInventory(sender, size * 9, Component.text(lootPool.key.key))
            lootPool.populate(inventory)
            sender.openInventory(inventory)
        })
}

fun lootPoolKey(nodeName: String, lootPoolMap: () -> Map<String, LootPool>): Argument<String> =
    StringArgument(nodeName)
        .replaceSuggestions(ArgumentSuggestions.stringCollection { _ -> lootPoolMap().keys })

fun assertLootPoolExists(key: String, sender: CommandSender, lootPoolMap: Map<String, LootPool>) {
    if (lootPoolMap.containsKey(key)) return
    throw CommandAPIBukkit.failWithAdventureComponent(
        sender.placeholderComponent("error-lootpool-not-found", "key" replace key).get()
    )
}

fun assertLootPoolNotExists(key: String, sender: CommandSender, lootPoolMap: Map<String, LootPool>) {
    if (!lootPoolMap.containsKey(key)) return
    throw CommandAPIBukkit.failWithAdventureComponent(
        sender.placeholderComponent("error-lootpool-already-exists", "key" replace key).get()
    )
}