package me.wyne.lootpool.command.complex

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.IntegerArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.CommandExecutor
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import me.wyne.lootpool.api.LootPool
import me.wyne.lootpool.api.LootPoolProvider
import me.wyne.lootpool.api.complex.ComplexLootPool
import me.wyne.lootpool.api.complex.EnchantmentPoolProvider
import me.wyne.lootpool.api.complex.ItemEntry
import me.wyne.lootpool.api.complex.LootRoll
import me.wyne.lootpool.command.SubCommand
import me.wyne.lootpool.command.assertLootPoolExists
import me.wyne.lootpool.command.assertLootPoolNotExists
import me.wyne.lootpool.command.complexPoolKey
import me.wyne.lootpool.command.lootPoolKey
import me.wyne.lootpool.gui.complex.openComplexPoolGui
import me.wyne.wutils.i18n.kotlin.placeholderComponent
import me.wyne.wutils.i18n.kotlin.replace

class CreateComplexCommand(provider: LootPoolProvider) : SubCommand("create") {
    override val command: CommandAPICommand = super.command
        .withShortDescription("Create a complex loot pool.")
        .withFullDescription(
            """
                Create a new, empty complex loot pool under a unique key.
                A complex loot pool is shaped like a vanilla loot table: an ordered list of rolls,
                each with its own roll count and weighted entries, plus item modifiers that
                transform what is produced and conditions that gate it.
                Add rolls with "/lootpool complex roll add", or bootstrap one from an existing pool
                with "/lootpool complex convert".
            """.trimIndent()
        )
        .withPermission("lootpool.create")
        .withArguments(StringArgument("key"))
        .executes(CommandExecutor { sender, args ->
            val key = args.getByClass("key", String::class.java)!!
            assertLootPoolNotExists(key, sender, provider.getMapOf(LootPool::class.java))
            provider.writeLootPool(ComplexLootPool(key))
            sender.placeholderComponent("success-lootpool-create", "key" replace key).sendMessage(sender)
        })
}

class ConvertComplexCommand(provider: LootPoolProvider) : SubCommand("convert") {
    override val command: CommandAPICommand = super.command
        .withShortDescription("Convert any pool into a complex one.")
        .withFullDescription(
            """
                Create a complex loot pool from an existing pool of any type.
                Every entry of the source pool becomes an item entry in a single roll, keeping its
                weight and amount range, so the new pool starts out behaving like the old one.
                Optionally provide a roll range for that roll; it defaults to a single roll.
                The source pool is left untouched.
            """.trimIndent()
        )
        .withPermission("lootpool.create")
        .withArguments(lootPoolKey("source") { provider.getMapOf(LootPool::class.java) })
        .withArguments(StringArgument("key"))
        .withOptionalArguments(IntegerArgument("minRolls", 0), IntegerArgument("maxRolls", 0))
        .executes(CommandExecutor { sender, args ->
            val source = args.getByClass("source", String::class.java)!!
            assertLootPoolExists(source, sender, provider.getMapOf(LootPool::class.java))
            val key = args.getByClass("key", String::class.java)!!
            assertLootPoolNotExists(key, sender, provider.getMapOf(LootPool::class.java))
            val minRolls = args.getByClassOrDefault("minRolls", Int::class.java, 1)
            val maxRolls = args.getByClassOrDefault("maxRolls", Int::class.java, minRolls)

            val entries = provider.getLootPool(source)!!.lootList.map { ItemEntry(it) }
            val roll = LootRoll(minRolls, maxRolls.coerceAtLeast(minRolls), entries)
            provider.writeLootPool(ComplexLootPool(key, listOf(roll), emptyList(), emptyList()))
            sender.placeholderComponent("success-lootpool-create", "key" replace key).sendMessage(sender)
        })
}

class EditComplexCommand(
    provider: LootPoolProvider,
    enchantmentProvider: EnchantmentPoolProvider
) : SubCommand("edit") {
    override val command: CommandAPICommand = super.command
        .withShortDescription("Edit a complex loot pool in a GUI.")
        .withFullDescription(
            """
                Open a complex loot pool in an editor.
                The first screen lists the pool's rolls; press F on one to open its entries, and
                again on an entry to reach its own modifiers and conditions. The buttons along the
                bottom row page through the list, go back, change the step that clicks nudge
                numbers by, and open the pool's own modifiers and conditions.
                Everything is kept in memory and written once when the editor is closed.
            """.trimIndent()
        )
        .withPermission("lootpool.modify")
        .withArguments(complexPoolKey("key", provider))
        .executesPlayer(PlayerCommandExecutor { sender, args ->
            val key = args.getByClass("key", String::class.java)!!
            assertLootPoolExists(key, sender, provider.getMapOf(ComplexLootPool::class.java))
            val pool = provider.getLootPool(key) as ComplexLootPool
            openComplexPoolGui(sender, pool, provider, enchantmentProvider)
        })
}
