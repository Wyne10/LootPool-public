package org.bigcraft.lootpool.command

import dev.jorel.commandapi.CommandAPIBukkit
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import me.wyne.wutils.i18n.kotlin.placeholderComponent
import me.wyne.wutils.i18n.kotlin.replace
import org.bigcraft.lootpool.api.BasicLootPool
import org.bigcraft.lootpool.api.LootPool
import org.bigcraft.lootpool.api.LootPoolProvider
import org.bigcraft.lootpool.gui.LootPoolGui
import org.bukkit.command.CommandSender

class CreateBasicCommand(lootPoolProvider: LootPoolProvider) : SubCommand("create") {
    override val command: CommandAPICommand = super.command
        .withPermission("lootpool.create")
        .withArguments(StringArgument("key"))
        .executesPlayer(PlayerCommandExecutor { sender, args ->
            val key = args.getOrDefaultRaw("key", "")
            assertBasicLootPoolNotExists(key, sender, lootPoolProvider.lootPoolMap)
            LootPoolGui(key, sender)
        })
}

class ModifyBasicCommand(lootPoolProvider: LootPoolProvider) : SubCommand("modify") {
    override val command: CommandAPICommand = super.command
        .withPermission("lootpool.modify")
        .withArguments(lootPoolKey("key") { lootPoolProvider.getMapOf(BasicLootPool::class.java) })
        .executesPlayer(PlayerCommandExecutor { sender, args ->
            val key = args.getOrDefaultRaw("key", "")
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
            val key = args.getOrDefaultRaw("key", "")
            val lootPool = lootPoolProvider.getLootPool(key)!!
            assertLootPoolExists(key, sender, lootPoolProvider.getMapOf(BasicLootPool::class.java))
            val newKey = args.getOrDefaultRaw("newKey", "")
            assertBasicLootPoolNotExists(newKey, sender, lootPoolProvider.lootPoolMap)
            LootPoolGui(newKey, sender, lootPool)
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
