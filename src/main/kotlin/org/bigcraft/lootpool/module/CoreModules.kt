package org.bigcraft.lootpool.module

import com.google.inject.AbstractModule
import org.bigcraft.lootpool.LootPoolApi
import org.bigcraft.lootpool.core.LootPoolManager

private class CoreModule(private vararg val modules: Class<out Any>) : AbstractModule() {
    override fun configure() {
        modules.forEach { bind(it) }
    }
}

val LootPoolModule: AbstractModule = CoreModule(LootPoolManager::class.java)

val ApiModule: AbstractModule = CoreModule(LootPoolApi::class.java)

