package me.wyne.lootpool.command.complex

import dev.jorel.commandapi.CommandAPIBukkit
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.BooleanArgument
import dev.jorel.commandapi.arguments.DoubleArgument
import dev.jorel.commandapi.arguments.GreedyStringArgument
import dev.jorel.commandapi.arguments.IntegerArgument
import dev.jorel.commandapi.arguments.ListArgumentBuilder
import dev.jorel.commandapi.arguments.LongArgument
import dev.jorel.commandapi.arguments.MultiLiteralArgument
import dev.jorel.commandapi.arguments.SafeSuggestions
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException
import dev.jorel.commandapi.executors.CommandArguments
import dev.jorel.commandapi.executors.CommandExecutor
import me.wyne.lootpool.api.LootPoolProvider
import me.wyne.lootpool.api.complex.condition.AllOfCondition
import me.wyne.lootpool.api.complex.condition.AnyOfCondition
import me.wyne.lootpool.api.complex.condition.InvertedCondition
import me.wyne.lootpool.api.complex.condition.LootCondition
import me.wyne.lootpool.api.complex.condition.PermissionCondition
import me.wyne.lootpool.api.complex.condition.RandomChanceCondition
import me.wyne.lootpool.api.complex.condition.TimeCondition
import me.wyne.lootpool.api.complex.condition.WeatherCondition
import me.wyne.lootpool.api.complex.condition.WorldCondition
import me.wyne.lootpool.command.complexPoolKey
import me.wyne.lootpool.command.elementPath
import me.wyne.lootpool.command.failInvalidIndex
import me.wyne.lootpool.command.helpCommand
import me.wyne.lootpool.command.modified
import me.wyne.lootpool.command.ownerFor
import me.wyne.lootpool.condition.PlaceholderCondition
import me.wyne.lootpool.core.ComplexOwner
import me.wyne.wutils.i18n.kotlin.placeholderComponent
import me.wyne.wutils.i18n.kotlin.replace
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender

fun conditionComplexCommand(provider: LootPoolProvider): CommandAPICommand {
    val subcommands = listOf(
        addConditionCommand(provider),
        removeConditionCommand(provider),
        moveConditionCommand(provider),
        invertConditionCommand(provider),
        setConditionCommand(provider)
    )
    return CommandAPICommand("condition")
        .withShortDescription("Manage conditions.")
        .apply { subcommands.forEach { withSubcommand(it) } }
        .withSubcommand(helpCommand("lootpool complex condition", subcommands))
}

private fun addConditionCommand(provider: LootPoolProvider): CommandAPICommand {
    val chance = CommandAPICommand("chance")
        .withShortDescription("Add a random-chance condition.")
        .withFullDescription(
            """
                Pass with a fixed probability between 0.0 and 1.0.
                This is the one condition that behaves the same everywhere, since it needs nothing
                from the situation the loot is rolled in.
            """.trimIndent()
        )
        .withPermission("lootpool.modify")
        .withArguments(complexPoolKey("key", provider))
        .withArguments(elementPath("path", provider))
        .withArguments(DoubleArgument("chance", 0.0, 1.0))
        .executes(CommandExecutor { sender, args ->
            ownerFor(args, sender, provider).conditions.add(RandomChanceCondition(args.getByClass("chance", Double::class.java)!!))
            modified(sender, args)
        })

    val permission = CommandAPICommand("permission")
        .withShortDescription("Add a permission condition.")
        .withFullDescription(
            """
                Pass when the player the loot is being generated for holds a permission node.
                This fails whenever there is no such player, which includes every command and API
                path that does not name one, so attach it only to loot always rolled for a player.
            """.trimIndent()
        )
        .withPermission("lootpool.modify")
        .withArguments(complexPoolKey("key", provider))
        .withArguments(elementPath("path", provider))
        .withArguments(GreedyStringArgument("permission"))
        .executes(CommandExecutor { sender, args ->
            ownerFor(args, sender, provider).conditions.add(PermissionCondition(args.getByClass("permission", String::class.java)!!))
            modified(sender, args)
        })

    @Suppress("UNCHECKED_CAST")
    val world = CommandAPICommand("world")
        .withShortDescription("Add a world condition.")
        .withFullDescription(
            """
                Pass when the loot is being generated in one of the named worlds.
                Fails when the situation carries no location and no player.
            """.trimIndent()
        )
        .withPermission("lootpool.modify")
        .withArguments(complexPoolKey("key", provider))
        .withArguments(elementPath("path", provider))
        .withArguments(
            ListArgumentBuilder<String>("worlds")
                .withList { _ -> Bukkit.getWorlds().map { it.name } }
                .withStringMapper()
                .buildGreedy()
        )
        .executes(CommandExecutor { sender, args ->
            val worlds = (args.getByClass("worlds", List::class.java) as List<String>).toSet()
            ownerFor(args, sender, provider).conditions.add(WorldCondition(worlds))
            modified(sender, args)
        })

    val time = CommandAPICommand("time")
        .withShortDescription("Add a time-of-day condition.")
        .withFullDescription(
            """
                Pass when the world's time of day falls within an inclusive tick range.
                A day is 24000 ticks: 0 is sunrise, 6000 noon, 13000 nightfall.
                Ranges wrap, so 22000 to 2000 matches the hours around midnight.
            """.trimIndent()
        )
        .withPermission("lootpool.modify")
        .withArguments(complexPoolKey("key", provider))
        .withArguments(elementPath("path", provider))
        .withArguments(LongArgument("minTime", 0, 24000))
        .withArguments(LongArgument("maxTime", 0, 24000))
        .executes(CommandExecutor { sender, args ->
            ownerFor(args, sender, provider).conditions.add(
                TimeCondition(args.getByClass("minTime", Long::class.java)!!, args.getByClass("maxTime", Long::class.java)!!)
            )
            modified(sender, args)
        })

    val weather = CommandAPICommand("weather")
        .withShortDescription("Add a weather condition.")
        .withFullDescription(
            """
                Pass when the world's weather matches.
                Either check can be left out, meaning "don't care"; leaving both out passes for any
                weather.
            """.trimIndent()
        )
        .withPermission("lootpool.modify")
        .withArguments(complexPoolKey("key", provider))
        .withArguments(elementPath("path", provider))
        .withOptionalArguments(BooleanArgument("raining"), BooleanArgument("thundering"))
        .executes(CommandExecutor { sender, args ->
            ownerFor(args, sender, provider).conditions.add(
                WeatherCondition(
                    args.getByClass("raining", Boolean::class.java),
                    args.getByClass("thundering", Boolean::class.java)
                )
            )
            modified(sender, args)
        })

    val allOf = CommandAPICommand("allof")
        .withShortDescription("Add an 'all of' group.")
        .withFullDescription(
            """
                Add an empty condition group that passes only when every condition inside it
                passes.
                Add conditions to it using its own path, which "/lootpool info" shows - for a group
                at index 0 of roll 1 that is "1.c0".
            """.trimIndent()
        )
        .withPermission("lootpool.modify")
        .withArguments(complexPoolKey("key", provider))
        .withArguments(elementPath("path", provider))
        .executes(CommandExecutor { sender, args ->
            ownerFor(args, sender, provider).conditions.add(AllOfCondition(emptyList()))
            modified(sender, args)
        })

    val anyOf = CommandAPICommand("anyof")
        .withShortDescription("Add an 'any of' group.")
        .withFullDescription(
            """
                Add an empty condition group that passes as soon as any condition inside it passes.
                An empty group fails, since there is nothing that could pass.
                Add conditions to it using its own path, which "/lootpool info" shows.
            """.trimIndent()
        )
        .withPermission("lootpool.modify")
        .withArguments(complexPoolKey("key", provider))
        .withArguments(elementPath("path", provider))
        .executes(CommandExecutor { sender, args ->
            ownerFor(args, sender, provider).conditions.add(AnyOfCondition(emptyList()))
            modified(sender, args)
        })

    val placeholder = CommandAPICommand("placeholder")
        .withShortDescription("Add a PlaceholderAPI condition.")
        .withFullDescription(
            """
                Compare a PlaceholderAPI expression against a value for the player the loot is
                being generated for.
                Both sides are resolved as placeholders, so either may contain them.
                The comparison operators "==", "!=" and "contains" work on text; ">", ">=", "<"
                and "<=" require both sides to resolve to numbers.
                Fails when there is no player, and when PlaceholderAPI is not installed.
            """.trimIndent()
        )
        .withPermission("lootpool.modify")
        .withArguments(complexPoolKey("key", provider))
        .withArguments(elementPath("path", provider))
        .withArguments(StringArgument("placeholder"))
        .withArguments(MultiLiteralArgument("operator", *PlaceholderCondition.Operator.entries.map { it.argument }.toTypedArray()))
        .withArguments(GreedyStringArgument("value"))
        .executes(CommandExecutor { sender, args ->
            val operator = PlaceholderCondition.Operator.of(args.getByClass("operator", String::class.java)!!)
                ?: PlaceholderCondition.Operator.EQUALS
            ownerFor(args, sender, provider).conditions.add(
                PlaceholderCondition(
                    args.getByClass("placeholder", String::class.java)!!,
                    operator,
                    args.getByClass("value", String::class.java)!!
                )
            )
            modified(sender, args)
        })

    return CommandAPICommand("add")
        .withShortDescription("Add a condition.")
        .withFullDescription("Attach a condition to a pool, a roll, an entry, a modifier or a condition group.")
        .withSubcommand(chance)
        .withSubcommand(permission)
        .withSubcommand(world)
        .withSubcommand(time)
        .withSubcommand(weather)
        .withSubcommand(allOf)
        .withSubcommand(anyOf)
        .withSubcommand(placeholder)
}

private fun removeConditionCommand(provider: LootPoolProvider) = CommandAPICommand("remove")
    .withShortDescription("Remove a condition.")
    .withFullDescription(
        """
            Remove a condition from the list the path names, by its index.
            Indices shift once a condition is removed; "/lootpool info" shows the current ones.
        """.trimIndent()
    )
    .withPermission("lootpool.modify")
    .withArguments(complexPoolKey("key", provider))
    .withArguments(elementPath("path", provider))
    .withArguments(conditionIndexArgument<LootCondition>("condition", provider))
    .executes(CommandExecutor { sender, args ->
        val owner = ownerFor(args, sender, provider)
        owner.conditions.removeAt(conditionIndex(owner, args, sender))
        modified(sender, args)
    })

private fun moveConditionCommand(provider: LootPoolProvider) = CommandAPICommand("move")
    .withShortDescription("Reorder a condition.")
    .withFullDescription(
        """
            Move a condition up or down its list by the given offset.
            Conditions in a list must all pass, so order only affects which one is checked first
            and how they are numbered.
        """.trimIndent()
    )
    .withPermission("lootpool.modify")
    .withArguments(complexPoolKey("key", provider))
    .withArguments(elementPath("path", provider))
    .withArguments(conditionIndexArgument<LootCondition>("condition", provider))
    .withArguments(IntegerArgument("delta"))
    .executes(CommandExecutor { sender, args ->
        val owner = ownerFor(args, sender, provider)
        owner.conditions.move(conditionIndex(owner, args, sender), args.getByClass("delta", Int::class.java)!!)
        modified(sender, args)
    })

private fun invertConditionCommand(provider: LootPoolProvider) = CommandAPICommand("invert")
    .withShortDescription("Negate a condition, or un-negate it.")
    .withFullDescription(
        """
            Wrap a condition so that it passes exactly when it would otherwise fail, or unwrap one
            that already is negated.
        """.trimIndent()
    )
    .withPermission("lootpool.modify")
    .withArguments(complexPoolKey("key", provider))
    .withArguments(elementPath("path", provider))
    .withArguments(conditionIndexArgument<LootCondition>("condition", provider))
    .withArguments(BooleanArgument("inverted"))
    .executes(CommandExecutor { sender, args ->
        val owner = ownerFor(args, sender, provider)
        val index = conditionIndex(owner, args, sender)
        val condition = owner.conditions[index]
        owner.conditions[index] = if (args.getByClass("inverted", Boolean::class.java)!!) {
            condition as? InvertedCondition ?: InvertedCondition(condition)
        } else {
            if (condition is InvertedCondition) condition.condition() else condition
        }
        modified(sender, args)
    })

private fun setConditionCommand(provider: LootPoolProvider): CommandAPICommand {
    val chance = CommandAPICommand("chance")
        .withShortDescription("Change a random-chance condition.")
        .withFullDescription("Replace an existing random-chance condition's probability.")
        .withPermission("lootpool.modify")
        .withArguments(complexPoolKey("key", provider))
        .withArguments(elementPath("path", provider))
        .withArguments(conditionIndexArgument<RandomChanceCondition>("condition", provider))
        .withArguments(DoubleArgument("chance", 0.0, 1.0))
        .executes(CommandExecutor { sender, args ->
            setCondition<RandomChanceCondition>(args, sender, provider) {
                RandomChanceCondition(args.getByClass("chance", Double::class.java)!!)
            }
            modified(sender, args)
        })

    val permission = CommandAPICommand("permission")
        .withShortDescription("Change a permission condition.")
        .withFullDescription("Replace the permission node an existing permission condition checks for.")
        .withPermission("lootpool.modify")
        .withArguments(complexPoolKey("key", provider))
        .withArguments(elementPath("path", provider))
        .withArguments(conditionIndexArgument<PermissionCondition>("condition", provider))
        .withArguments(GreedyStringArgument("permission"))
        .executes(CommandExecutor { sender, args ->
            setCondition<PermissionCondition>(args, sender, provider) {
                PermissionCondition(args.getByClass("permission", String::class.java)!!)
            }
            modified(sender, args)
        })

    @Suppress("UNCHECKED_CAST")
    val world = CommandAPICommand("world")
        .withShortDescription("Change a world condition.")
        .withFullDescription(
            """
                Replace the worlds an existing world condition accepts.
                The list given here replaces the old one outright rather than adding to it.
            """.trimIndent()
        )
        .withPermission("lootpool.modify")
        .withArguments(complexPoolKey("key", provider))
        .withArguments(elementPath("path", provider))
        .withArguments(conditionIndexArgument<WorldCondition>("condition", provider))
        .withArguments(
            ListArgumentBuilder<String>("worlds")
                .withList { _ -> Bukkit.getWorlds().map { it.name } }
                .withStringMapper()
                .buildGreedy()
        )
        .executes(CommandExecutor { sender, args ->
            setCondition<WorldCondition>(args, sender, provider) {
                WorldCondition((args.getByClass("worlds", List::class.java) as List<String>).toSet())
            }
            modified(sender, args)
        })

    val time = CommandAPICommand("time")
        .withShortDescription("Change a time-of-day condition.")
        .withFullDescription(
            """
                Replace the tick range an existing time condition accepts.
                A day is 24000 ticks: 0 is sunrise, 6000 noon, 13000 nightfall.
                Ranges wrap, so 22000 to 2000 matches the hours around midnight.
            """.trimIndent()
        )
        .withPermission("lootpool.modify")
        .withArguments(complexPoolKey("key", provider))
        .withArguments(elementPath("path", provider))
        .withArguments(conditionIndexArgument<TimeCondition>("condition", provider))
        .withArguments(LongArgument("minTime", 0, 24000))
        .withArguments(LongArgument("maxTime", 0, 24000))
        .executes(CommandExecutor { sender, args ->
            setCondition<TimeCondition>(args, sender, provider) {
                TimeCondition(
                    args.getByClass("minTime", Long::class.java)!!,
                    args.getByClass("maxTime", Long::class.java)!!
                )
            }
            modified(sender, args)
        })

    val weather = CommandAPICommand("weather")
        .withShortDescription("Change a weather condition.")
        .withFullDescription(
            """
                Replace the checks an existing weather condition makes.
                Either check can be left out, meaning "don't care"; leaving both out passes for any
                weather, so omitting one clears it rather than keeping its old value.
            """.trimIndent()
        )
        .withPermission("lootpool.modify")
        .withArguments(complexPoolKey("key", provider))
        .withArguments(elementPath("path", provider))
        .withArguments(conditionIndexArgument<WeatherCondition>("condition", provider))
        .withOptionalArguments(BooleanArgument("raining"), BooleanArgument("thundering"))
        .executes(CommandExecutor { sender, args ->
            setCondition<WeatherCondition>(args, sender, provider) {
                WeatherCondition(
                    args.getByClass("raining", Boolean::class.java),
                    args.getByClass("thundering", Boolean::class.java)
                )
            }
            modified(sender, args)
        })

    val placeholder = CommandAPICommand("placeholder")
        .withShortDescription("Change a PlaceholderAPI condition.")
        .withFullDescription(
            """
                Replace the expression, operator and value an existing placeholder condition
                compares.
                The comparison operators "==", "!=" and "contains" work on text; ">", ">=", "<"
                and "<=" require both sides to resolve to numbers.
            """.trimIndent()
        )
        .withPermission("lootpool.modify")
        .withArguments(complexPoolKey("key", provider))
        .withArguments(elementPath("path", provider))
        .withArguments(conditionIndexArgument<PlaceholderCondition>("condition", provider))
        .withArguments(StringArgument("placeholder"))
        .withArguments(MultiLiteralArgument("operator", *PlaceholderCondition.Operator.entries.map { it.argument }.toTypedArray()))
        .withArguments(GreedyStringArgument("value"))
        .executes(CommandExecutor { sender, args ->
            setCondition<PlaceholderCondition>(args, sender, provider) {
                PlaceholderCondition(
                    args.getByClass("placeholder", String::class.java)!!,
                    PlaceholderCondition.Operator.of(args.getByClass("operator", String::class.java)!!)
                        ?: PlaceholderCondition.Operator.EQUALS,
                    args.getByClass("value", String::class.java)!!
                )
            }
            modified(sender, args)
        })

    return CommandAPICommand("set")
        .withShortDescription("Change an existing condition.")
        .withFullDescription(
            """
                Replace the settings of a condition already attached to a pool, a roll, an entry, a
                modifier or a condition group.
                The condition keeps its place in the list, and an inverted condition stays inverted.
                Group conditions have nothing to set - add to them by their own path instead, and
                use "/lootpool complex condition invert" to negate one.
            """.trimIndent()
        )
        .withSubcommand(chance)
        .withSubcommand(permission)
        .withSubcommand(world)
        .withSubcommand(time)
        .withSubcommand(weather)
        .withSubcommand(placeholder)
}

private inline fun <reified T : LootCondition> setCondition(
    args: CommandArguments,
    sender: CommandSender,
    provider: LootPoolProvider,
    replacement: () -> LootCondition
) {
    val owner = ownerFor(args, sender, provider)
    val index = conditionIndex(owner, args, sender)
    val existing = owner.conditions[index]
    val inverted = existing is InvertedCondition
    if ((if (existing is InvertedCondition) existing.condition() else existing) !is T)
        failWrongConditionType(sender, args)
    owner.conditions[index] = replacement().let { if (inverted) InvertedCondition(it) else it }
}

private inline fun <reified T : LootCondition> conditionIndexArgument(nodeName: String, provider: LootPoolProvider): Argument<Int> =
    IntegerArgument(nodeName, 0)
        .replaceSafeSuggestions(SafeSuggestions.suggestCollection { info ->
            try {
                val owner = ownerFor(info.previousArgs, info.sender, provider)
                owner.conditions.items
                    .mapIndexedNotNull { index, condition -> if (condition is T) index else null }
            } catch (_: WrapperCommandSyntaxException) {
                emptyList()
            }
        })

private fun failWrongConditionType(sender: CommandSender, args: CommandArguments): Nothing =
    throw CommandAPIBukkit.failWithAdventureComponent(
        sender.placeholderComponent(
            "error-wrong-condition-type",
            "path" replace args.getByClassOrDefault("path", String::class.java, ""),
            "condition" replace args.getByClassOrDefault("condition", Int::class.java, 0)
        ).get()
    )

private fun conditionIndex(owner: ComplexOwner, args: CommandArguments, sender: CommandSender): Int {
    val index = args.getByClass("condition", Int::class.java)!!
    if (index in owner.conditions.indices) return index
    failInvalidIndex(sender, owner.path, index)
}
