package org.bigcraft.lootpool.command

import dev.jorel.commandapi.CommandAPIBukkit
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.IntegerArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import me.wyne.wutils.common.kotlin.item.isNotNullOrAir
import me.wyne.wutils.i18n.kotlin.placeholderComponent
import me.wyne.wutils.i18n.kotlin.replace
import org.bigcraft.lootpool.api.KeyedLoot
import org.bigcraft.lootpool.api.Loot
import org.bigcraft.lootpool.api.LootPool
import org.bigcraft.lootpool.api.LootPoolProvider
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CreateLootCommand(lootPoolProvider: LootPoolProvider) : SubCommand("create") {
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
            assertLootNotExists(key, sender, lootPoolProvider.lootPoolMap)
            lootPoolProvider.writeLootPool(KeyedLoot(key, Loot(sender.inventory.itemInMainHand.clone().apply { amount = 1 }, weight,
                        minAmount.coerceAtMost(maxAmount), maxAmount.coerceAtLeast(minAmount))))
            sender.placeholderComponent("success-lootpool-create", "key" replace key).sendMessage(sender)
        })
}

class ModifyLootCommand(lootPoolProvider: LootPoolProvider) : SubCommand("modify") {
    override val command: CommandAPICommand = super.command
        .withPermission("lootpool.modify")
        .withArguments(lootPoolKey("key") { lootPoolProvider.getMapOf(KeyedLoot::class.java) })
        .withOptionalArguments(IntegerArgument("minAmount", 1, 64))
        .withOptionalArguments(IntegerArgument("maxAmount", 1, 64))
        .withOptionalArguments(IntegerArgument("weight", 0))
        .executesPlayer(PlayerCommandExecutor { sender, args ->
            val key = args.getOrDefaultRaw("key", "")
            assertLootPoolExists(key, sender, lootPoolProvider.getMapOf(KeyedLoot::class.java))
            assertLootNotNull(key, sender)
            val loot = lootPoolProvider.getLootPool(key)!! as KeyedLoot
            val minAmount = args.getByClassOrDefault("minAmount", Int::class.java, loot.loot.minAmount)
            val maxAmount = args.getByClassOrDefault("maxAmount", Int::class.java, loot.loot.maxAmount)
            val weight = args.getByClassOrDefault("weight", Int::class.java, 1)
            lootPoolProvider.writeLootPool(KeyedLoot(key, Loot(sender.inventory.itemInMainHand.clone().apply { amount = 1 }, weight,
                        minAmount.coerceAtMost(maxAmount), maxAmount.coerceAtLeast(minAmount))))
            sender.placeholderComponent("success-lootpool-create", "key" replace key).sendMessage(sender)
        })
}

private fun assertLootNotExists(key: String, sender: CommandSender, lootPoolMap: Map<String, LootPool>) {
    if (lootPoolMap.contains(key)) {
        if (sender.hasPermission("lootpool.modify") && lootPoolMap[key] is KeyedLoot)
            throw CommandAPIBukkit.failWithAdventureComponent(
                sender.placeholderComponent("info-loot-already-exists", "key" replace key).get()
            )
        else
            throw CommandAPIBukkit.failWithAdventureComponent(
                sender.placeholderComponent("error-lootpool-already-exists", "key" replace key).get()
            )
    }
}

private fun assertLootNotNull(key: String, sender: Player) {
    if (sender.inventory.itemInMainHand.isNotNullOrAir()) return
    throw CommandAPIBukkit.failWithAdventureComponent(
        sender.placeholderComponent("error-empty-loot", "key" replace key).get()
    )
}
