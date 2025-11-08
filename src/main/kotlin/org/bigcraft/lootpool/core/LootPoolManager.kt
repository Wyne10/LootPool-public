package org.bigcraft.lootpool.core

import com.google.inject.Inject
import com.google.inject.Singleton
import org.bigcraft.lootpool.AbstractManager
import org.bigcraft.lootpool.ConfigurableFactory
import org.bigcraft.lootpool.api.LootPool
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

@Singleton
class LootPoolManager @Inject constructor(plugin: org.bigcraft.lootpool.LootPool) : AbstractManager<LootPool>() {

    override val sectionKey: String = "lootpool"
    override val valueLoader: ConfigurableFactory<LootPool> = LootPoolFactory

    val lootDirectory = File(plugin.dataFolder, "$sectionKey/")

    override fun load(config: ConfigurationSection) {
        super.load(config)
        loadFiles()
    }

    fun write(key: String, lootPool: LootPool) {
        loadedMap[key] = lootPool
        val file = File(lootDirectory, "$key.yml")
        val config = YamlConfiguration.loadConfiguration(file)
        config.set(key, lootPool)
        config.save(file)
    }

    private fun loadFiles() {
        if (!lootDirectory.exists())
            lootDirectory.mkdirs()
        lootDirectory.listFiles()
            .forEach { file ->
                val key = file.nameWithoutExtension
                loadedMap[key] = valueLoader.fromConfig(key, YamlConfiguration.loadConfiguration(file))
            }
    }

}