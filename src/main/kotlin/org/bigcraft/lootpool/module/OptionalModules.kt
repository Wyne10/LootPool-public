package org.bigcraft.lootpool.module

import com.google.inject.AbstractModule
import org.bigcraft.lootpool.LootPool
import org.bigcraft.lootpool.command.LootPoolCommand

//region Implementations

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
            LootPool.log.warn(exceptionMessage)
        }
    }
}

private class OptionalAbstractModule(
    private val className: String,
    private val exceptionMessage: String = "$className not found, module ignored",
    private vararg val modules: AbstractModule
) : AbstractModule() {
    override fun configure() {
        try {
            Class.forName(className)
            modules.forEach { install(it) }
        } catch (e: ClassNotFoundException) {
            LootPool.log.warn(exceptionMessage)
        }
    }
}

//endregion

val CommandModule: AbstractModule = OptionalModule(
    className = "dev.jorel.commandapi.CommandAPI",
    exceptionMessage = "CommandAPI not found, commands are not registered",
    LootPoolCommand::class.java
)
