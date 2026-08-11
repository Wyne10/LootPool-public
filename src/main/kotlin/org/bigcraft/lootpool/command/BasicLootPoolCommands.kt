package org.bigcraft.lootpool.command

import dev.jorel.commandapi.CommandAPIBukkit
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.GreedyStringArgument
import dev.jorel.commandapi.arguments.MapArgumentBuilder
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import me.wyne.wutils.common.operation.IntOperation
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
        .withShortDescription("Create a basic loot.")
        .withFullDescription(
            """
                Create a new basic loot pool.
                Basic loot pool is modified using GUI
                setting each individual item weight, minimum and maximum amounts.
            """.trimIndent()
        )
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
        .withShortDescription("Modify a basic loot pool.")
        .withFullDescription(
            """
                Modify an existing basic loot pool.
                Opens up a GUI, changes are saved upon GUI closing.
            """.trimIndent()
        )
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
        .withShortDescription("Clone a basic loot pool.")
        .withFullDescription(
            """
                Clone an existing basic loot pool
                to an identical loot pool with a new key.
            """.trimIndent()
        )
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
        .withShortDescription("Merge two loot pools.")
        .withFullDescription(
            """
                Merge two existing loot pools.
                Destination loot pool must be a basic loot pool
                and source pool can be any other pool.
                This effectively constructs a new basic loot pool with the same key as destination pool
                Original destination pool is destroyed and source loot pool loses any
                special characteristics preserving only a loot list (Original source loot pool is preserved).
            """.trimIndent()
        )
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
        .withShortDescription("Modify basic loot pool item weights.")
        .withFullDescription(
            """
                Applies provided operation to all item weights in a loot pool.
                Operation examples: "+5", "-1", "*1000", "=10", "**2", "/3"
                If no operation symbol is provided, the "set" operation will be performed.
            """.trimIndent()
        )
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

class FlattenBasicCommand(lootPoolProvider: LootPoolProvider) : SubCommand("flatten") {
    override val command: CommandAPICommand = super.command
        .withShortDescription("Flatten multiple loot pools.")
        .withFullDescription(
            """
                Flatten multiple loot pools into a single new basic loot pool.
                Combines the merge and weight commands: for each provided pool
                an operation is applied to all of its item weights before merging.
                Provide a new unique key followed by a map of pools and their weight operations,
                e.g. "pool1:*2 pool2:+5 pool3:=10".
                Source pools can be of any type but only their loot lists are preserved,
                losing any special characteristics. Original source pools are not modified.
            """.trimIndent()
        )
        .withPermission("lootpool.create")
        .withArguments(StringArgument("key"))
        .withArguments(
            MapArgumentBuilder<String, IntOperation>("pools")
                .withKeyMapper { s -> s }
                .withValueMapper { s -> Operations.getIntOperation(s) }
                .withKeyList { lootPoolProvider.getMapOf(LootPool::class.java).keys.toList() }
                .withoutValueList(true)
                .build())
        .executesPlayer(PlayerCommandExecutor { sender, args ->
            val key = args.getByClass("key", String::class.java)!!
            assertLootPoolNotExists(key, sender, lootPoolProvider.getMapOf(LootPool::class.java))
            val pools = args.getByClassOrDefault("pools", Map::class.java, emptyMap<String, IntOperation>()) as Map<String, IntOperation>
            val flattenedLootList = pools
                .mapKeys { lootPoolProvider.getLootPool(it.key)!! }
                .flatMap { entry -> entry.key.lootList.map { Loot(it.item, entry.value.evaluate(it.weight), it.minAmount, it.maxAmount) } }
            val newLootPool = BasicLootPool(key, flattenedLootList)
            LootPoolGui(key, sender, newLootPool)
            sender.placeholderComponent("success-lootpool-create", "key" replace key).sendMessage(sender)
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
