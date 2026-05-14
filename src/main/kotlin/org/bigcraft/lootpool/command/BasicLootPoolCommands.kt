package org.bigcraft.lootpool.command

import dev.jorel.commandapi.CommandAPIBukkit
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.GreedyStringArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.arguments.TextArgument
import dev.jorel.commandapi.executors.CommandExecutor
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import me.wyne.wutils.common.operation.Operations
import me.wyne.wutils.i18n.kotlin.placeholderComponent
import me.wyne.wutils.i18n.kotlin.replace
import org.bigcraft.lootpool.api.BasicLootPool
import org.bigcraft.lootpool.api.Loot
import org.bigcraft.lootpool.api.LootPool
import org.bigcraft.lootpool.api.LootPoolProvider
import org.bigcraft.lootpool.gui.LootPoolGui
import org.bukkit.command.CommandSender

class CreateBasicCommand(lootPoolProvider: LootPoolProvider) : SubCommand("create") {
    override val command: CommandAPICommand = super.command
        .withPermission("lootpool.create")
        .withArguments(StringArgument("key"))
        .executesPlayer(PlayerCommandExecutor { sender, args ->
            val key = args.getByClass("key", String::class.java)!!
            assertBasicLootPoolNotExists(key, sender, lootPoolProvider.lootPoolMap)
            LootPoolGui(key, sender)
        })
}

class ModifyBasicCommand(lootPoolProvider: LootPoolProvider) : SubCommand("modify") {
    override val command: CommandAPICommand = super.command
        .withPermission("lootpool.modify")
        .withArguments(lootPoolKey("key") { lootPoolProvider.getMapOf(BasicLootPool::class.java) })
        .executesPlayer(PlayerCommandExecutor { sender, args ->
            val key = args.getByClass("key", String::class.java)!!
            assertLootPoolExists(key, sender, lootPoolProvider.getMapOf(BasicLootPool::class.java))
            val lootPool = lootPoolProvider.getLootPool(key)
            LootPoolGui(key, sender, lootPool!!)
        })
}

class CloneBasicCommand(lootPoolProvider: LootPoolProvider) : SubCommand("clone") {
    override val command: CommandAPICommand = super.command
        .withPermission("lootpool.clone")
        .withArguments(lootPoolKey("key") { lootPoolProvider.getMapOf(BasicLootPool::class.java) })
        .withArguments(StringArgument("newKey"))
        .executesPlayer(PlayerCommandExecutor { sender, args ->
            val key = args.getByClass("key", String::class.java)!!
            assertLootPoolExists(key, sender, lootPoolProvider.getMapOf(BasicLootPool::class.java))
            val lootPool = lootPoolProvider.getLootPool(key)!!
            val newKey = args.getByClass("newKey", String::class.java)!!
            assertBasicLootPoolNotExists(newKey, sender, lootPoolProvider.lootPoolMap)
            LootPoolGui(newKey, sender, lootPool)
        })
}

class MergeBasicCommand(lootPoolProvider: LootPoolProvider) : SubCommand("merge") {
    override val command: CommandAPICommand = super.command
        .withPermission("lootpool.create")
        .withArguments(lootPoolKey("destination") { lootPoolProvider.getMapOf(BasicLootPool::class.java) })
        .withArguments(lootPoolKey("source") { lootPoolProvider.getMapOf(LootPool::class.java) })
        .executesPlayer(PlayerCommandExecutor { sender, args ->
            val destinationKey = args.getByClass("destination", String::class.java)!!
            assertLootPoolExists(destinationKey, sender, lootPoolProvider.getMapOf(BasicLootPool::class.java))
            val sourceKey = args.getByClass("source", String::class.java)!!
            assertLootPoolExists(sourceKey, sender, lootPoolProvider.getMapOf(LootPool::class.java))
            val destination = lootPoolProvider.getLootPool(destinationKey)!!
            val source = lootPoolProvider.getLootPool(sourceKey)!!
            val mergedLootList = destination.lootList + source.lootList
            val newLootPool = BasicLootPool(destinationKey, mergedLootList)
            LootPoolGui(destinationKey, sender, newLootPool)
        })
}

class WeightBasicCommand(lootPoolProvider: LootPoolProvider) : SubCommand("weight") {
    override val command: CommandAPICommand = super.command
        .withPermission("lootpool.modify")
        .withArguments(lootPoolKey("key") { lootPoolProvider.getMapOf(BasicLootPool::class.java) })
        .withArguments(GreedyStringArgument("operation"))
        .executesPlayer(PlayerCommandExecutor { sender, args ->
            val key = args.getByClass("key", String::class.java)!!
            assertLootPoolExists(key, sender, lootPoolProvider.getMapOf(BasicLootPool::class.java))
            val lootPool = lootPoolProvider.getLootPool(key)!!
            val operationString = args.getByClass("operation", String::class.java)!!
            val operation = Operations.getIntOperation(operationString)
            val modifiedLootList = lootPool.getLootList()
                .map { Loot(it.item, operation.evaluate(it.weight), it.minAmount, it.maxAmount) }
            val newLootPool = BasicLootPool(key, modifiedLootList)
            LootPoolGui(key, sender, newLootPool)
        })
}

private fun assertBasicLootPoolNotExists(key: String, sender: CommandSender, lootPoolMap: Map<String, LootPool>) {
    if (lootPoolMap.contains(key)) {
        if (sender.hasPermission("lootpool.modify") && lootPoolMap[key] is BasicLootPool)
            throw CommandAPIBukkit.failWithAdventureComponent(
                sender.placeholderComponent("info-lootpool-already-exists", "key" replace key).get()
            )
        else
            throw CommandAPIBukkit.failWithAdventureComponent(
                sender.placeholderComponent("error-lootpool-already-exists", "key" replace key).get()
            )
    }
}
