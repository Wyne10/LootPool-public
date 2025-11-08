package org.bigcraft.lootpool.module

import com.google.inject.AbstractModule
import org.bigcraft.lootpool.LootPool

//region Implementations

private class CoreModule(private vararg val modules: Class<out Any>) : AbstractModule() {
    override fun configure() {
        modules.forEach { bind(it) }
    }
}

private class CoreAbstractModule(private vararg val modules: AbstractModule) : AbstractModule() {
    override fun configure() {
        modules.forEach { install(it) }
    }
}

private class CoreSwappableModule(
    private val className: String,
    private val exceptionMessage: String = "$className not found, module ignored",
    private val modules: Map<Class<out Any>, Class<out Any>>
) : AbstractModule() {
    override fun configure() {
        try {
            Class.forName(className)
            modules.keys.forEach { bind(it) }
        } catch (e: ClassNotFoundException) {
            LootPool.log.warn(exceptionMessage)
            modules.values.forEach { bind(it) }
        }
    }
}

private class CoreAbstractSwappableModule(
    private val className: String,
    private val exceptionMessage: String = "$className not found, module ignored",
    private val modules: Map<AbstractModule, AbstractModule>
) : AbstractModule() {
    override fun configure() {
        try {
            Class.forName(className)
            modules.keys.forEach { install(it) }
        } catch (e: ClassNotFoundException) {
            LootPool.log.warn(exceptionMessage)
            modules.values.forEach { install(it) }
        }
    }
}

//endregion

