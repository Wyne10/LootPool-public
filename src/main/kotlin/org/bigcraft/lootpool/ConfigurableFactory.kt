package org.bigcraft.lootpool

import org.bukkit.configuration.ConfigurationSection

interface ConfigurableFactory<out T> {
    fun fromConfig(key: String, config: ConfigurationSection): T
}