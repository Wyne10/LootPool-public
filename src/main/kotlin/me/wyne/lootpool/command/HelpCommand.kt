package me.wyne.lootpool.command

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.StringTooltip
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.CommandExecutor
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor

fun helpCommand(path: String, subcommands: List<CommandAPICommand>): CommandAPICommand {
    val byName = subcommands.associateBy { it.name }
    return CommandAPICommand("help")
        .withShortDescription("Show command help.")
        .withFullDescription(
            """
                List every subcommand with its description.
                Provide a subcommand name to see its full description.
            """.trimIndent()
        )
        .withOptionalArguments(
            StringArgument("command").replaceSuggestions(
                ArgumentSuggestions.stringsWithTooltips { _ ->
                    subcommands
                        .map { StringTooltip.ofString(it.name, it.shortDescription ?: "") }
                        .toTypedArray()
                }
            )
        )
        .executes(CommandExecutor { sender, args ->
            val name = args.getByClass("command", String::class.java)
            if (name != null) {
                val command = byName[name]
                    ?: return@CommandExecutor sender.sendMessage(
                        Component.text("Unknown command: $name", NamedTextColor.RED)
                    )
                sender.sendMessage(fullHelp(path, command))
                return@CommandExecutor
            }
            sender.sendMessage(Component.text("/$path commands:", NamedTextColor.YELLOW))
            subcommands.forEach { sender.sendMessage(shortHelp(path, it)) }
        })
}

private fun shortHelp(path: String, command: CommandAPICommand): Component {
    val short = command.shortDescription ?: command.fullDescription ?: ""
    val full = command.fullDescription ?: command.shortDescription ?: ""
    return Component.text("/$path ${command.name}", NamedTextColor.GOLD)
        .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
        .append(Component.text(short, NamedTextColor.GRAY))
        .hoverEvent(HoverEvent.showText(Component.text(full)))
        .clickEvent(ClickEvent.runCommand("/$path help ${command.name}"))
}

private fun fullHelp(path: String, command: CommandAPICommand): Component {
    val full = command.fullDescription ?: command.shortDescription ?: "No description."
    return Component.text("/$path ${command.name}", NamedTextColor.GOLD)
        .append(Component.newline())
        .append(Component.text(full, NamedTextColor.GRAY))
}
