package org.bigcraft.lootpool.core

import me.wyne.wutils.config.configurables.attribute.GenericFactory
import org.bigcraft.lootpool.api.LootPool
import org.bukkit.configuration.ConfigurationSection

object LootPoolFactory : GenericFactory<LootPool> {
    override fun create(key: String, config: ConfigurationSection): LootPool =
        config.get(key) as LootPool
}