package org.bigcraft.lootpool.core

import org.bigcraft.lootpool.ConfigurableFactory
import org.bigcraft.lootpool.api.KeyedLoot
import org.bukkit.configuration.ConfigurationSection

object LootFactory : ConfigurableFactory<KeyedLoot> {
    override fun fromConfig(key: String, config: ConfigurationSection): KeyedLoot =
        config.get(key) as KeyedLoot
}