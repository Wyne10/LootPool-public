package me.wyne.lootpool.command

import dev.jorel.commandapi.CommandAPIBukkit
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.IntegerArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import me.wyne.wutils.common.kotlin.item.isNotNullOrAir
import me.wyne.wutils.i18n.kotlin.placeholderComponent
import me.wyne.wutils.i18n.kotlin.replace
import me.wyne.lootpool.api.KeyedLoot
import me.wyne.lootpool.api.Loot
import me.wyne.lootpool.api.LootPool
import me.wyne.lootpool.api.LootPoolProvider
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CreateLootCommand(lootPoolProvider: LootPoolProvider) : SubCommand("create") {
    override val command: CommandAPICommand = super.command
        .withShortDescription("Create a keyed loot.")
        .withFullDescription(
            """
                Create a new keyed loot from the item held in your main hand.
                A keyed loot is a single loot entry stored under its own key.
                "minAmount" and "maxAmount" set the item's amount range (1-64).
                "minAmount" defaults to the held item's amount, "maxAmount" defaults to the
                held item's amount or "minAmount", whichever is greater.
                "weight" sets the loot weight used when this loot is rolled from a pool (defaults to 1).
            """.trimIndent()
        )
        .withPermission("lootpool.create")
        .withArguments(StringArgument("key"))
        .withOptionalArguments(IntegerArgument("minAmount", 1, 64))
        .withOptionalArguments(IntegerArgument("maxAmount", 1, 64))
        .withOptionalArguments(IntegerArgument("weight", 0))
        .executesPlayer(PlayerCommandExecutor { sender, args ->
            val key = args.getByClass("key", String::class.java)!!
            assertLootNotNull(key, sender)
            assertLootNotExists(key, sender, lootPoolProvider.lootPoolMap)
            val heldAmount = sender.inventory.itemInMainHand.amount
            val minAmount = args.getByClassOrDefault("minAmount", Int::class.java, heldAmount)
            val maxAmount = args.getByClassOrDefault("maxAmount", Int::class.java, maxOf(heldAmount, minAmount))
            val weight = args.getByClassOrDefault("weight", Int::class.java, Loot.EMPTY.weight)
            lootPoolProvider.writeLootPool(KeyedLoot(key, Loot(sender.inventory.itemInMainHand.clone(), weight,
                        minAmount.coerceAtMost(maxAmount), maxAmount.coerceAtLeast(minAmount))))
            sender.placeholderComponent("success-lootpool-create", "key" replace key).sendMessage(sender)
        })
}

class ModifyLootCommand(lootPoolProvider: LootPoolProvider) : SubCommand("modify") {
    override val command: CommandAPICommand = super.command
        .withShortDescription("Modify a keyed loot.")
        .withFullDescription(
            """
                Modify an existing keyed loot, replacing its item with the one held in your main hand.
                "minAmount" and "maxAmount" set the item's amount range.
                "weight" sets the loot weight used when this loot is rolled from a pool.
                All parameters default to previous loot values.
            """.trimIndent()
        )
        .withPermission("lootpool.modify")
        .withArguments(lootPoolKey("key") { lootPoolProvider.getMapOf(KeyedLoot::class.java) })
        .withOptionalArguments(IntegerArgument("minAmount", 1, 64))
        .withOptionalArguments(IntegerArgument("maxAmount", 1, 64))
        .withOptionalArguments(IntegerArgument("weight", 0))
        .executesPlayer(PlayerCommandExecutor { sender, args ->
            val key = args.getByClass("key", String::class.java)!!
            assertLootPoolExists(key, sender, lootPoolProvider.getMapOf(KeyedLoot::class.java))
            assertLootNotNull(key, sender)
            val loot = lootPoolProvider.getLootPool(key)!! as KeyedLoot
            val minAmount = args.getByClassOrDefault("minAmount", Int::class.java, loot.loot.minAmount)
            val maxAmount = args.getByClassOrDefault("maxAmount", Int::class.java, loot.loot.maxAmount)
            val weight = args.getByClassOrDefault("weight", Int::class.java, loot.loot.weight)
            lootPoolProvider.writeLootPool(KeyedLoot(key, Loot(sender.inventory.itemInMainHand.clone(), weight,
                        minAmount.coerceAtMost(maxAmount), maxAmount.coerceAtLeast(minAmount))))
            sender.placeholderComponent("success-lootpool-create", "key" replace key).sendMessage(sender)
        })
}

private fun assertLootNotExists(key: String, sender: CommandSender, lootPoolMap: Map<String, LootPool>) {
    assertValidKey(key, sender)
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
