package me.wyne.lootpool.command.complex

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.IntegerArgument
import dev.jorel.commandapi.arguments.SafeSuggestions
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException
import dev.jorel.commandapi.executors.CommandExecutor
import me.wyne.lootpool.api.LootPoolProvider
import me.wyne.lootpool.api.complex.LootRoll
import me.wyne.lootpool.command.complexPoolKey
import me.wyne.lootpool.command.editorFor
import me.wyne.lootpool.command.helpCommand
import me.wyne.lootpool.command.modified
import me.wyne.lootpool.command.rollIndex

fun rollComplexCommand(provider: LootPoolProvider): CommandAPICommand {
    val subcommands = listOf(
        addRollCommand(provider),
        setRollCommand(provider),
        removeRollCommand(provider),
        moveRollCommand(provider)
    )
    return CommandAPICommand("roll")
        .withShortDescription("Manage a complex loot pool's rolls.")
        .apply { subcommands.forEach { withSubcommand(it) } }
        .withSubcommand(helpCommand("lootpool complex roll", subcommands))
}

private fun addRollCommand(provider: LootPoolProvider) = CommandAPICommand("add")
    .withShortDescription("Append a roll.")
    .withFullDescription(
        """
            Append a new, empty roll to a complex loot pool.
            A roll draws a random number of times within its roll range, picking one entry by
            weight each time, so several rolls let you state a drop's composition directly -
            "3 to 5 commons, then exactly 1 tool" is two rolls.
            Optionally provide a minimum and maximum roll count; omit the maximum for a fixed
            count, or omit both for a single roll.
        """.trimIndent()
    )
    .withPermission("lootpool.modify")
    .withArguments(complexPoolKey("key", provider))
    .withOptionalArguments(IntegerArgument("minRolls", 0), IntegerArgument("maxRolls", 0))
    .executes(CommandExecutor { sender, args ->
        val editor = editorFor(args, sender, provider)
        val minRolls = args.getByClassOrDefault("minRolls", Int::class.java, 1)
        val maxRolls = args.getByClassOrDefault("maxRolls", Int::class.java, minRolls)
        editor.rolls.add(LootRoll(minRolls, maxRolls.coerceAtLeast(minRolls), emptyList()))
        modified(sender, args)
    })

private fun setRollCommand(provider: LootPoolProvider) = CommandAPICommand("set")
    .withShortDescription("Change a roll's roll count.")
    .withFullDescription(
        """
            Change how many times a roll draws.
            Omit the maximum to use a fixed count.
            Roll indices are shown by "/lootpool info".
        """.trimIndent()
    )
    .withPermission("lootpool.modify")
    .withArguments(complexPoolKey("key", provider))
    .withArguments(rollIndexArgument("roll", provider))
    .withArguments(IntegerArgument("minRolls", 0))
    .withOptionalArguments(IntegerArgument("maxRolls", 0))
    .executes(CommandExecutor { sender, args ->
        val editor = editorFor(args, sender, provider)
        val index = rollIndex(editor, args, sender)
        val minRolls = args.getByClass("minRolls", Int::class.java)!!
        val maxRolls = args.getByClassOrDefault("maxRolls", Int::class.java, minRolls)
        val roll = editor.rolls[index]
        editor.rolls[index] = LootRoll(
            minRolls, maxRolls.coerceAtLeast(minRolls), roll.entries(), roll.modifiers(), roll.conditions()
        )
        modified(sender, args)
    })

private fun removeRollCommand(provider: LootPoolProvider) = CommandAPICommand("remove")
    .withShortDescription("Remove a roll.")
    .withFullDescription(
        """
            Remove a roll, and everything in it, from a complex loot pool.
            Roll indices shift once a roll is removed; "/lootpool info" shows the current ones.
        """.trimIndent()
    )
    .withPermission("lootpool.modify")
    .withArguments(complexPoolKey("key", provider))
    .withArguments(rollIndexArgument("roll", provider))
    .executes(CommandExecutor { sender, args ->
        val editor = editorFor(args, sender, provider)
        editor.rolls.removeAt(rollIndex(editor, args, sender))
        modified(sender, args)
    })

private fun moveRollCommand(provider: LootPoolProvider) = CommandAPICommand("move")
    .withShortDescription("Reorder a roll.")
    .withFullDescription(
        """
            Move a roll up or down the pool's roll order by the given offset.
            Rolls are evaluated in order, which is the order their items are produced in.
            A negative offset moves the roll earlier, a positive one later.
        """.trimIndent()
    )
    .withPermission("lootpool.modify")
    .withArguments(complexPoolKey("key", provider))
    .withArguments(rollIndexArgument("roll", provider))
    .withArguments(IntegerArgument("delta"))
    .executes(CommandExecutor { sender, args ->
        val editor = editorFor(args, sender, provider)
        editor.rolls.move(rollIndex(editor, args, sender), args.getByClass("delta", Int::class.java)!!)
        modified(sender, args)
    })

fun rollIndexArgument(nodeName: String, provider: LootPoolProvider): Argument<Int> =
    IntegerArgument(nodeName, 0)
        .replaceSafeSuggestions(SafeSuggestions.suggestCollection { info ->
            try {
                val editor = editorFor(info.previousArgs, info.sender, provider)
                List(editor.rolls.size) { index -> index }
            } catch (_: WrapperCommandSyntaxException) {
                emptyList()
            }
        })
