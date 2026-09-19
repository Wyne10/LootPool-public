package me.wyne.lootpool.command

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.executors.CommandExecutor
import me.wyne.wutils.i18n.kotlin.placeholderComponent
import net.kyori.adventure.text.Component
import me.wyne.lootpool.LootPool
import org.bukkit.inventory.ItemStack

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
            LootPool.logger.info("Plugin reloaded")
        })
}

val ItemStack.nameComponent: Component
    get() {
        if (itemMeta == null) return Component.translatable(type.translationKey)
        return if (itemMeta.hasDisplayName())
            itemMeta.displayName()!!
        else
            Component.translatable(type.translationKey)
    }
