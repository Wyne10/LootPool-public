package org.bigcraft.lootpool

import com.google.inject.Inject
import com.google.inject.Singleton
import org.bigcraft.lootpool.api.LootPoolApi
import org.bigcraft.lootpool.api.LootPoolProvider
import org.bigcraft.lootpool.api.LootProvider
import org.bigcraft.lootpool.core.LootManager
import org.bigcraft.lootpool.core.LootPoolManager
import org.bukkit.plugin.ServicePriority

@Singleton
class LootPoolApi @Inject constructor(plugin: LootPool, lootPoolManager: LootPoolManager, lootManager: LootManager) {
    init {
        plugin.server.servicesManager.register(LootPoolProvider::class.java, lootPoolManager, plugin, ServicePriority.Normal)
        plugin.server.servicesManager.register(LootProvider::class.java, lootManager, plugin, ServicePriority.Normal)
        LootPoolApi.setProvider(lootPoolManager)
        LootPoolApi.setLootProvider(lootManager)
    }
}