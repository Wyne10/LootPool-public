package me.wyne.lootpool.command.complex

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.Tooltip
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.IntegerArgument
import dev.jorel.commandapi.arguments.SafeSuggestions
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException
import dev.jorel.commandapi.executors.CommandExecutor
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import me.wyne.lootpool.api.Loot
import me.wyne.lootpool.api.LootPool
import me.wyne.lootpool.api.LootPoolProvider
import me.wyne.lootpool.api.complex.EmptyEntry
import me.wyne.lootpool.api.complex.ItemEntry
import me.wyne.lootpool.api.complex.LootEntry
import me.wyne.lootpool.api.complex.PoolEntry
import me.wyne.lootpool.command.assertLootNotEmpty
import me.wyne.lootpool.command.assertLootPoolExists
import me.wyne.lootpool.command.complexPoolKey
import me.wyne.lootpool.command.editorFor
import me.wyne.lootpool.command.entryIndex
import me.wyne.lootpool.command.failInvalidEntryType
import me.wyne.lootpool.command.helpCommand
import me.wyne.lootpool.command.lootPoolKey
import me.wyne.lootpool.command.modified
import me.wyne.lootpool.command.rollIndex
import me.wyne.lootpool.core.describe
import me.wyne.lootpool.core.withWeight

fun entryComplexCommand(provider: LootPoolProvider): CommandAPICommand {
    val subcommands = listOf(
        addEntryCommand(provider),
        removeEntryCommand(provider),
        moveEntryCommand(provider),
        setEntryCommand(provider)
    )
    return CommandAPICommand("entry")
        .withShortDescription("Manage a roll's entries.")
        .apply { subcommands.forEach { withSubcommand(it) } }
        .withSubcommand(helpCommand("lootpool complex entry", subcommands))
}

private fun addEntryCommand(provider: LootPoolProvider): CommandAPICommand {
    val item = CommandAPICommand("item")
        .withShortDescription("Add the held item as an entry.")
        .withFullDescription(
            """
                Add the item in your main hand to a roll as a weighted entry.
                Optionally provide a weight and an inclusive amount range; they default to the
                held stack's amount and a weight of 1.
            """.trimIndent()
        )
        .withPermission("lootpool.modify")
        .withArguments(complexPoolKey("key", provider))
        .withArguments(rollIndexArgument("roll", provider))
        .withOptionalArguments(IntegerArgument("weight", 0), IntegerArgument("minAmount", 1, 64), IntegerArgument("maxAmount", 1, 64))
        .executesPlayer(PlayerCommandExecutor { sender, args ->
            val editor = editorFor(args, sender, provider)
            val index = rollIndex(editor, args, sender)
            val held = sender.inventory.itemInMainHand
            assertLootNotEmpty(held, sender)
            val weight = args.getByClassOrDefault("weight", Int::class.java, Loot.EMPTY.weight())
            val minAmount = args.getByClassOrDefault("minAmount", Int::class.java, held.amount)
            val maxAmount = args.getByClassOrDefault("maxAmount", Int::class.java, minAmount)
            editor.entries(index).add(
                ItemEntry(Loot(held.clone(), weight, minAmount.coerceAtMost(maxAmount), maxAmount.coerceAtLeast(minAmount)))
            )
            modified(sender, args)
        })

    val pool = CommandAPICommand("pool")
        .withShortDescription("Add a reference to another pool.")
        .withFullDescription(
            """
                Add an entry that delegates to another registered loot pool.
                When drawn, the referenced pool produces its own items: another complex pool rolls
                its natural output, a roll pool rolls its own roll count, and any other pool
                produces a single weighted draw.
                Use this to compose pools instead of duplicating their entries.
            """.trimIndent()
        )
        .withPermission("lootpool.modify")
        .withArguments(complexPoolKey("key", provider))
        .withArguments(rollIndexArgument("roll", provider))
        .withArguments(lootPoolKey("pool") { provider.getMapOf(LootPool::class.java) })
        .withOptionalArguments(IntegerArgument("weight", 0))
        .executes(CommandExecutor { sender, args ->
            val editor = editorFor(args, sender, provider)
            val index = rollIndex(editor, args, sender)
            val poolKey = args.getByClass("pool", String::class.java)!!
            assertLootPoolExists(poolKey, sender, provider.getMapOf(LootPool::class.java))
            editor.entries(index).add(PoolEntry(poolKey, args.getByClassOrDefault("weight", Int::class.java, 1)))
            modified(sender, args)
        })

    val empty = CommandAPICommand("empty")
        .withShortDescription("Add a 'nothing' entry.")
        .withFullDescription(
            """
                Add an entry that produces nothing.
                Its weight is what gives a roll a chance of yielding no drop at all: a weight of 3
                among entries totalling 12 makes one draw in four come up empty.
            """.trimIndent()
        )
        .withPermission("lootpool.modify")
        .withArguments(complexPoolKey("key", provider))
        .withArguments(rollIndexArgument("roll", provider))
        .withOptionalArguments(IntegerArgument("weight", 0))
        .executes(CommandExecutor { sender, args ->
            val editor = editorFor(args, sender, provider)
            val index = rollIndex(editor, args, sender)
            editor.entries(index).add(EmptyEntry(args.getByClassOrDefault("weight", Int::class.java, 1)))
            modified(sender, args)
        })

    return CommandAPICommand("add")
        .withShortDescription("Add an entry to a roll.")
        .withFullDescription("Add an item, pool reference or 'nothing' entry to a roll.")
        .withSubcommand(item)
        .withSubcommand(pool)
        .withSubcommand(empty)
}

private fun removeEntryCommand(provider: LootPoolProvider) = CommandAPICommand("remove")
    .withShortDescription("Remove an entry from a roll.")
    .withFullDescription(
        """
            Remove an entry from a roll by its index.
            Entry indices shift once an entry is removed; "/lootpool info" shows the current ones.
        """.trimIndent()
    )
    .withPermission("lootpool.modify")
    .withArguments(complexPoolKey("key", provider))
    .withArguments(rollIndexArgument("roll", provider))
    .withArguments(entryIndexArgument<LootEntry>("entry", provider))
    .executes(CommandExecutor { sender, args ->
        val editor = editorFor(args, sender, provider)
        val roll = rollIndex(editor, args, sender)
        editor.entries(roll).removeAt(entryIndex(editor, roll, args, sender))
        modified(sender, args)
    })

private fun moveEntryCommand(provider: LootPoolProvider) = CommandAPICommand("move")
    .withShortDescription("Reorder an entry.")
    .withFullDescription(
        """
            Move an entry up or down its roll's entry order by the given offset.
            Order does not affect the odds - entries are picked by weight - but it does decide how
            they are numbered and listed.
        """.trimIndent()
    )
    .withPermission("lootpool.modify")
    .withArguments(complexPoolKey("key", provider))
    .withArguments(rollIndexArgument("roll", provider))
    .withArguments(entryIndexArgument<LootEntry>("entry", provider))
    .withArguments(IntegerArgument("delta"))
    .executes(CommandExecutor { sender, args ->
        val editor = editorFor(args, sender, provider)
        val roll = rollIndex(editor, args, sender)
        editor.entries(roll).move(entryIndex(editor, roll, args, sender), args.getByClass("delta", Int::class.java)!!)
        modified(sender, args)
    })

private fun setEntryCommand(provider: LootPoolProvider): CommandAPICommand {
    val item = CommandAPICommand("item")
        .withShortDescription("Replace an item entry's item.")
        .withFullDescription(
            """
                Replace an item entry's item with the one in your main hand, keeping its weight and
                amount range, as well as any modifiers and conditions attached to it.
            """.trimIndent()
        )
        .withPermission("lootpool.modify")
        .withArguments(complexPoolKey("key", provider))
        .withArguments(rollIndexArgument("roll", provider))
        .withArguments(entryIndexArgument<ItemEntry>("entry", provider))
        .executesPlayer(PlayerCommandExecutor { sender, args ->
            val editor = editorFor(args, sender, provider)
            val roll = rollIndex(editor, args, sender)
            val index = entryIndex(editor, roll, args, sender)
            val entries = editor.entries(roll)
            val entry = entries[index] as? ItemEntry ?: failInvalidEntryType(sender, args)
            val held = sender.inventory.itemInMainHand
            assertLootNotEmpty(held, sender)
            entries[index] = ItemEntry(
                Loot(held.clone(), entry.loot().weight(), entry.loot().minAmount(), entry.loot().maxAmount()),
                entry.conditions(), entry.modifiers()
            )
            modified(sender, args)
        })

    val pool = CommandAPICommand("pool")
        .withShortDescription("Repoint a pool entry.")
        .withFullDescription(
            """
                Change which loot pool a pool entry delegates to, keeping its weight, modifiers and
                conditions.
            """.trimIndent()
        )
        .withPermission("lootpool.modify")
        .withArguments(complexPoolKey("key", provider))
        .withArguments(rollIndexArgument("roll", provider))
        .withArguments(entryIndexArgument<PoolEntry>("entry", provider))
        .withArguments(lootPoolKey("pool") { provider.getMapOf(LootPool::class.java) })
        .executes(CommandExecutor { sender, args ->
            val editor = editorFor(args, sender, provider)
            val roll = rollIndex(editor, args, sender)
            val index = entryIndex(editor, roll, args, sender)
            val entries = editor.entries(roll)
            val entry = entries[index] as? PoolEntry ?: failInvalidEntryType(sender, args)
            val poolKey = args.getByClass("pool", String::class.java)!!
            assertLootPoolExists(poolKey, sender, provider.getMapOf(LootPool::class.java))
            entries[index] = PoolEntry(poolKey, entry.weight(), entry.conditions(), entry.modifiers())
            modified(sender, args)
        })

    val weight = CommandAPICommand("weight")
        .withShortDescription("Change an entry's weight.")
        .withFullDescription(
            """
                Set an entry's selection weight within its roll.
                An entry's chance of being drawn is its weight divided by the total weight of every
                eligible entry in the same roll; a weight of 0 is never drawn.
            """.trimIndent()
        )
        .withPermission("lootpool.modify")
        .withArguments(complexPoolKey("key", provider))
        .withArguments(rollIndexArgument("roll", provider))
        .withArguments(entryIndexArgument<LootEntry>("entry", provider))
        .withArguments(IntegerArgument("weight", 0))
        .executes(CommandExecutor { sender, args ->
            val editor = editorFor(args, sender, provider)
            val roll = rollIndex(editor, args, sender)
            val index = entryIndex(editor, roll, args, sender)
            val entries = editor.entries(roll)
            entries[index] = withWeight(entries[index], args.getByClass("weight", Int::class.java)!!)
            modified(sender, args)
        })

    val amount = CommandAPICommand("amount")
        .withShortDescription("Change an item entry's amount range.")
        .withFullDescription(
            """
                Set the inclusive stack-size range an item entry rolls its amount from.
                Omit the maximum to use a fixed amount.
            """.trimIndent()
        )
        .withPermission("lootpool.modify")
        .withArguments(complexPoolKey("key", provider))
        .withArguments(rollIndexArgument("roll", provider))
        .withArguments(entryIndexArgument<ItemEntry>("entry", provider))
        .withArguments(IntegerArgument("minAmount", 1, 64))
        .withOptionalArguments(IntegerArgument("maxAmount", 1, 64))
        .executes(CommandExecutor { sender, args ->
            val editor = editorFor(args, sender, provider)
            val roll = rollIndex(editor, args, sender)
            val index = entryIndex(editor, roll, args, sender)
            val entries = editor.entries(roll)
            val entry = entries[index] as? ItemEntry ?: failInvalidEntryType(sender, args)
            val minAmount = args.getByClass("minAmount", Int::class.java)!!
            val maxAmount = args.getByClassOrDefault("maxAmount", Int::class.java, minAmount)
            entries[index] = ItemEntry(
                Loot(entry.loot().item(), entry.loot().weight(), minAmount.coerceAtMost(maxAmount), maxAmount.coerceAtLeast(minAmount)),
                entry.conditions(), entry.modifiers()
            )
            modified(sender, args)
        })

    return CommandAPICommand("set")
        .withShortDescription("Change an entry in a roll.")
        .withFullDescription(
            """
                Change one property of an entry already in a roll, leaving everything else about it
                alone - an entry keeps its place in the roll, and its modifiers and conditions.
                "weight" applies to any entry; "item" and "amount" need an item entry, and "pool"
                needs a pool entry.
            """.trimIndent()
        )
        .withSubcommand(item)
        .withSubcommand(pool)
        .withSubcommand(weight)
        .withSubcommand(amount)
}

private inline fun <reified T : LootEntry> entryIndexArgument(nodeName: String, provider: LootPoolProvider): Argument<Int> =
    IntegerArgument(nodeName, 0)
        .replaceSafeSuggestions(SafeSuggestions.tooltipCollection { info ->
            try {
                val editor = editorFor(info.previousArgs, info.sender, provider)
                val roll = rollIndex(editor, info.previousArgs, info.sender)
                val entries = editor.entries(roll)
                entries.items
                    .mapIndexedNotNull { index, entry -> if (entry is T) Tooltip.ofString(index, describe(entry)) else null }
            } catch (_: WrapperCommandSyntaxException) {
                emptyList()
            }
        })
