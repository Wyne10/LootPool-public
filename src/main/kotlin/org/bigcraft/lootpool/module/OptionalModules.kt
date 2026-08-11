package org.bigcraft.lootpool.module

import com.google.inject.AbstractModule
import org.bigcraft.lootpool.LootPool
import org.bigcraft.lootpool.command.LootPoolCommand
import org.bigcraft.lootpool.menu.AbstractMenusRegistry

private class OptionalModule(
    private val className: String,
    private val exceptionMessage: String = "$className not found, module ignored",
    private vararg val modules: Class<out Any>
) : AbstractModule() {
    override fun configure() {
        try {
            Class.forName(className)
            modules.forEach { bind(it) }
        } catch (e: ClassNotFoundException) {
            LootPool.logger.warn(exceptionMessage)
        }
    }
}

val CommandModule: AbstractModule = OptionalModule(
    className = "dev.jorel.commandapi.CommandAPI",
    exceptionMessage = "CommandAPI not found, commands are not registered",
    LootPoolCommand::class.java
)

val AbstractMenusModule: AbstractModule = OptionalModule(
    className = "ru.abstractmenus.api.AbstractMenusPlugin",
    exceptionMessage = "AbstractMenus not found, catalog and property are not registered",
    AbstractMenusRegistry::class.java
)