package org.bigcraft.lootpool

import com.google.inject.Inject
import com.google.inject.Singleton
import org.bigcraft.lootpool.api.LootPoolApi
import org.bigcraft.lootpool.api.LootPoolProvider
import org.bigcraft.lootpool.core.LootPoolManager
import org.bukkit.plugin.ServicePriority

@Singleton
class LootPoolApi @Inject constructor(plugin: LootPool, lootPoolManager: LootPoolManager) {
    init {
        plugin.server.servicesManager.register(LootPoolProvider::class.java, lootPoolManager, plugin, ServicePriority.Normal)
        LootPoolApi.setProvider(lootPoolManager)
    }
}