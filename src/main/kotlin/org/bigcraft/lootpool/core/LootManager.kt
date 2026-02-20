package org.bigcraft.lootpool.core

import com.google.inject.Inject
import com.google.inject.Singleton
import org.bigcraft.lootpool.AbstractManager
import org.bigcraft.lootpool.ConfigurableFactory
import org.bigcraft.lootpool.ReloadConfig
import org.bigcraft.lootpool.api.KeyedLoot
import org.bigcraft.lootpool.api.LootProvider
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.configuration.serialization.ConfigurationSerialization
import java.io.File

@Singleton
class LootManager @Inject constructor(private val plugin: org.bigcraft.lootpool.LootPool) : AbstractManager<KeyedLoot>(), LootProvider {

    override val sectionKey: String = "loot"
    override val valueLoader: ConfigurableFactory<KeyedLoot> = LootFactory

    private val lootDirectory = File(plugin.dataFolder, "$sectionKey/")

    init {
        ConfigurationSerialization.registerClass(KeyedLoot::class.java)
    }

    override fun getLoot(key: String) = loadedMap[key]

    override fun removeLoot(key: String): KeyedLoot? {
        val lootPool = loadedMap.remove(key)
        val file = File(lootDirectory, "$key.yml")
        if (file.exists())
            file.delete()
        return lootPool
    }

    override fun addLoot(loot: KeyedLoot) {
        loadedMap[loot.key()] = loot
    }

    override fun writeLoot(loot: KeyedLoot) {
        loadedMap[loot.key()] = loot
        val file = File(lootDirectory, "${loot.key()}.yml")
        val config = YamlConfiguration.loadConfiguration(file)
        config.set(loot.key(), loot)
        config.save(file)
    }

    override fun reload() {
        ReloadConfig.run(plugin)
        load(plugin.config)
    }

    override fun load(config: ConfigurationSection) {
        super.load(config)
        loadFiles(lootDirectory)
    }

}