package me.wyne.lootpool

import com.google.inject.Inject
import com.google.inject.Singleton
import me.wyne.lootpool.api.LootPoolApi
import me.wyne.lootpool.api.LootPoolProvider
import me.wyne.lootpool.core.LootPoolManager
import org.bukkit.plugin.ServicePriority

@Singleton
class LootPoolApi @Inject constructor(plugin: LootPool, lootPoolManager: LootPoolManager) {
    init {
        plugin.server.servicesManager.register(LootPoolProvider::class.java, lootPoolManager, plugin, ServicePriority.Normal)
        LootPoolApi.setProvider(lootPoolManager)
    }
}