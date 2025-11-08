package org.bigcraft.lootpool.core

import org.bigcraft.lootpool.ConfigurableFactory
import org.bigcraft.lootpool.api.LootPool
import org.bukkit.configuration.ConfigurationSection

object LootPoolFactory : ConfigurableFactory<LootPool> {
    override fun fromConfig(key: String, config: ConfigurationSection): LootPool =
        config.get(key) as LootPool
}