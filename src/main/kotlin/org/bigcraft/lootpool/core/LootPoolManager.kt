package org.bigcraft.lootpool.core

import com.google.inject.Inject
import com.google.inject.Singleton
import org.bigcraft.lootpool.AbstractManager
import org.bigcraft.lootpool.ConfigurableFactory
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

    private val lootPoolDirectory = File(plugin.dataFolder, "$sectionKey/")

    init {
        instance = this
        ConfigurationSerialization.registerClass(Loot::class.java)
        ConfigurationSerialization.registerClass(LootPool::class.java)
    }

    override fun getLootKeys(): Set<String> =
        mapKeys.toSet()

    override fun getLootList(key: String): List<Loot>? =
        getLootPool(key)?.lootPool

    override fun getLootPoolMap(): Map<String, LootPool> =
        loadedMap.toMap()

    override fun getLootPool(key: String) = loadedMap[key]

    override fun removeLootPool(key: String): LootPool? {
        val lootPool = loadedMap.remove(key)
        val file = File(lootPoolDirectory, "$key.yml")
        if (file.exists())
            file.delete()
        return lootPool
    }

    override fun addLootPool(lootPool: LootPool) {
        loadedMap[lootPool.key()] = lootPool
    }

    override fun writeLootPool(lootPool: LootPool) {
        loadedMap[lootPool.key()] = lootPool
        val file = File(lootPoolDirectory, "${lootPool.key()}.yml")
        val config = YamlConfiguration.loadConfiguration(file)
        config.set(lootPool.key(), lootPool)
        config.save(file)
    }

    override fun reload() {
        ReloadConfig.run(plugin)
        load(plugin.config)
    }

    override fun load(config: ConfigurationSection) {
        super.load(config)
        loadFiles(lootPoolDirectory)
    }

    companion object {
        lateinit var instance: LootPoolManager
            private set
    }

}