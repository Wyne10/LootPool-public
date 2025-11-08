package org.bigcraft.lootpool.command

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.CommandExecutor
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import dev.jorel.commandapi.kotlindsl.getValue
import me.wyne.wutils.i18n.kotlin.placeholderComponent
import org.bigcraft.lootpool.LootPool
import org.bigcraft.lootpool.gui.LootPoolGui

abstract class SubCommand(argument: String) {
    open val command = CommandAPICommand(argument)
    operator fun invoke() = command
}

class ReloadCommand(plugin: LootPool) : SubCommand("reload") {
    override val command: CommandAPICommand = super.command
        .withPermission("lootpool.reload")
        .executes(CommandExecutor { sender, _ ->
            plugin.reload()
            sender.placeholderComponent("success-plugin-reload").sendMessage(sender)
            LootPool.log.info("Plugin reloaded")
        })
}

object CreateCommand : SubCommand("create") {
    override val command: CommandAPICommand = super.command
        .withPermission("lootpool.create")
        .withArguments(StringArgument("key"))
        .executesPlayer(PlayerCommandExecutor { sender, args ->
            val key: String by args
            LootPoolGui(key, sender)
        })
}