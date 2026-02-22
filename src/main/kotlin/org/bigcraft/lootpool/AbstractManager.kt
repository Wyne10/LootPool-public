package org.bigcraft.lootpool

import me.wyne.wutils.common.loadable.Loadable
import me.wyne.wutils.common.loadable.Loader
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

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
            LootPool.logger.debug("Loading key '{}' from '{}'", key, sectionKey)
            runCatching {
                loadedMap[key] = valueLoader.fromConfig(key, section)
            }.onFailure { LootPool.logger.error("Failed loading '{}' from '{}'", key, sectionKey, it) }
        }
    }

    protected fun loadFiles(directory: File) {
        if (!directory.exists())
            directory.mkdirs()
        directory.listFiles()
            ?.forEach { file ->
                val key = file.nameWithoutExtension
                LootPool.logger.debug("Loading key '{}' from '{}'", key, directory.name)
                runCatching {
                    loadedMap[key] = valueLoader.fromConfig(key, YamlConfiguration.loadConfiguration(file))
                }.onFailure { LootPool.logger.error("Failed loading '{}' from '{}'", key, directory.name, it) }
            }
    }

}