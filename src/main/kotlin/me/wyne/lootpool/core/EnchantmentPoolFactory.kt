package me.wyne.lootpool.core

import me.wyne.lootpool.api.complex.EnchantmentPool
import me.wyne.wutils.config.configurables.attribute.GenericFactory
import org.bukkit.configuration.ConfigurationSection

object EnchantmentPoolFactory : GenericFactory<EnchantmentPool> {
    override fun create(key: String, config: ConfigurationSection): EnchantmentPool =
        config.get(key) as EnchantmentPool
}
