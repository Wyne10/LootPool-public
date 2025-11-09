package org.bigcraft.lootpool.core

import com.google.inject.Inject
import com.google.inject.Singleton
import org.bigcraft.lootpool.AbstractManager
import org.bigcraft.lootpool.ConfigurableFactory
import org.bigcraft.lootpool.Load
import org.bigcraft.lootpool.ReloadConfig
import org.bigcraft.lootpool.api.Loot
import org.bigcraft.lootpool.api.LootPool
import org.bigcraft.lootpool.api.LootPoolProvider
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.configuration.serialization.ConfigurationSerialization
import java.io.File

@Singleton
class LootPoolManager @Inject constructor(private val plugin: org.bigcraft.lootpool.LootPool) : AbstractManager<LootPool>(), LootPoolProvider {

    override val sectionKey: String = "lootpool"
    override val valueLoader: ConfigurableFactory<LootPool> = LootPoolFactory

    val lootDirectory = File(plugin.dataFolder, "$sectionKey/")

    init {
        instance = this
        ConfigurationSerialization.registerClass(Loot::class.java)
        ConfigurationSerialization.registerClass(LootPool::class.java)
    }

    override fun getLootPool(key: String) = loadedMap[key]

    override fun removeLootPool(key: String): LootPool? {
        val lootPool = loadedMap.remove(key)
        val file = File(lootDirectory, "$key.yml")
        if (file.exists())
            file.delete()
        return lootPool
    }

    override fun addLootPool(key: String, lootPool: LootPool) {
        loadedMap[key] = lootPool
    }

    override fun writeLootPool(key: String, lootPool: LootPool) {
        loadedMap[key] = lootPool
        val file = File(lootDirectory, "$key.yml")
        val config = YamlConfiguration.loadConfiguration(file)
        config.set(key, lootPool)
        config.save(file)
    }

    override fun reload() {
        ReloadConfig.run(plugin)
        Load.run(plugin)
    }

    override fun load(config: ConfigurationSection) {
        super.load(config)
        loadFiles()
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

    companion object {
        lateinit var instance: LootPoolManager
            private set
    }

}