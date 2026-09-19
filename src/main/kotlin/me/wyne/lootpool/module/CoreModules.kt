package me.wyne.lootpool.module

import com.google.inject.AbstractModule
import me.wyne.lootpool.LootPoolApi
import me.wyne.lootpool.core.LootPoolManager

private class CoreModule(private vararg val modules: Class<out Any>) : AbstractModule() {
    override fun configure() {
        modules.forEach { bind(it) }
    }
}

val LootPoolModule: AbstractModule = CoreModule(LootPoolManager::class.java)

val ApiModule: AbstractModule = CoreModule(LootPoolApi::class.java)

