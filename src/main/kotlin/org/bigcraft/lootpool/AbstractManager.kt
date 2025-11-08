package org.bigcraft.lootpool

import me.wyne.wutils.common.loadable.Loadable
import me.wyne.wutils.common.loadable.Loader
import org.bukkit.configuration.ConfigurationSection

@Suppress("LeakingThis")
abstract class AbstractManager<V> : Loadable {

    protected abstract val sectionKey: String

    protected abstract val valueLoader: ConfigurableFactory<V>
    protected val loadedMap: MutableMap<String, V> = HashMap()

    val mapKeys
        get() = loadedMap.keys

    init {
        Loader.global.registerLoadable(this)
    }

    override fun load(config: ConfigurationSection) {
        loadedMap.clear()
        val section = config.getConfigurationSection(sectionKey) ?: return
        section.getKeys(false).forEach { key ->
            LootPool.log.debug("Loading key '{}' from '{}'", key, section.name)
            loadedMap[key] = valueLoader.fromConfig(key, section)
        }
    }

}