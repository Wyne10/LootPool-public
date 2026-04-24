package org.bigcraft.lootpool.command

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import me.wyne.wutils.common.kotlin.item.isNullOrAir
import me.wyne.wutils.i18n.kotlin.placeholderComponent
import me.wyne.wutils.i18n.kotlin.replace
import org.bigcraft.lootpool.api.Loot
import org.bigcraft.lootpool.api.LootPoolProvider
import org.bigcraft.lootpool.api.SnapshotLootPool
import java.util.TreeMap

class SnapshotCommand(lootPoolProvider: LootPoolProvider) : SubCommand("snapshot") {
    override val command: CommandAPICommand = super.command
        .withPermission("lootpool.create")
        .withArguments(StringArgument("key"))
        .executesPlayer(PlayerCommandExecutor { sender, args ->
            val key = args.getOrDefaultRaw("key", "")
            assertLootPoolNotExists(key, sender, lootPoolProvider.lootPoolMap)
            val lootPool = TreeMap<Int, Loot>()
            sender.inventory
                .forEachIndexed { slot, item ->
                    if (item.isNullOrAir()) return@forEachIndexed
                    lootPool[slot] = Loot(item, 1, item.amount, item.amount)
                }
            lootPoolProvider.writeLootPool(SnapshotLootPool(key, lootPool))
            sender.placeholderComponent("success-lootpool-create", "key" replace key).sendMessage(sender)
        })
}