package me.wyne.lootpool.core

import me.wyne.wutils.config.configurables.attribute.GenericFactory
import me.wyne.lootpool.api.LootPool
import org.bukkit.configuration.ConfigurationSection

object LootPoolFactory : GenericFactory<LootPool> {
    override fun create(key: String, config: ConfigurationSection): LootPool =
        config.get(key) as LootPool
}