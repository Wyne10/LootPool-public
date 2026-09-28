package me.wyne.lootpool

import com.google.inject.Inject
import com.google.inject.Singleton
import me.wyne.lootpool.api.LootPoolApi
import me.wyne.lootpool.api.LootPoolProvider
import me.wyne.lootpool.api.complex.EnchantmentPoolProvider
import me.wyne.lootpool.core.EnchantmentPoolManager
import me.wyne.lootpool.core.LootPoolManager
import me.wyne.wutils.i18n.I18n
import net.kyori.adventure.text.Component
import org.bukkit.plugin.ServicePriority

@Singleton
class LootPoolApi @Inject constructor(
    plugin: LootPool,
    lootPoolManager: LootPoolManager,
    enchantmentPoolManager: EnchantmentPoolManager
) {
    init {
        plugin.server.servicesManager.register(LootPoolProvider::class.java, lootPoolManager, plugin, ServicePriority.Normal)
        plugin.server.servicesManager.register(EnchantmentPoolProvider::class.java, enchantmentPoolManager, plugin, ServicePriority.Normal)
        LootPoolApi.setProvider(lootPoolManager)
        LootPoolApi.setEnchantmentProvider(enchantmentPoolManager)
        LootPoolApi.setTextRenderer { text -> I18n.global?.component()?.fromString(text) ?: Component.text(text) }
    }
}
