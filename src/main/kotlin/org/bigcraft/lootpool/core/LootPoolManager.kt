package org.bigcraft.lootpool.core

import com.google.inject.Inject
import com.google.inject.Singleton
import org.bigcraft.lootpool.LoadableProvider
import org.bigcraft.lootpool.ReloadConfig
import org.bigcraft.lootpool.api.BasicLootPool
import org.bigcraft.lootpool.api.CompositeLootPool
import org.bigcraft.lootpool.api.KeyedLoot
import org.bigcraft.lootpool.api.Loot
import org.bigcraft.lootpool.api.LootPool
import org.bigcraft.lootpool.api.LootPoolProvider
import org.bigcraft.lootpool.api.MultiLootPool
import org.bigcraft.lootpool.api.SnapshotLootPool
import org.bigcraft.lootpool.api.VanillaLootPool
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.configuration.serialization.ConfigurationSerialization
import java.io.File

@Singleton
class LootPoolManager @Inject constructor(private val plugin: org.bigcraft.lootpool.LootPool) : LoadableProvider<LootPool>(
        "lootpool", LootPoolFactory
    ), LootPoolProvider {

    init {
        instance = this
        ConfigurationSerialization.registerClass(Loot::class.java)
        ConfigurationSerialization.registerClass(KeyedLoot::class.java)
        ConfigurationSerialization.registerClass(BasicLootPool::class.java)
        ConfigurationSerialization.registerClass(CompositeLootPool::class.java)
        ConfigurationSerialization.registerClass(SnapshotLootPool::class.java)
        ConfigurationSerialization.registerClass(MultiLootPool::class.java)
        ConfigurationSerialization.registerClass(VanillaLootPool::class.java)
    }

    override fun getLootPoolMap(): Map<String, LootPool> =
        loadedMap.toMap()

    override fun getMapOf(clazz: Class<out LootPool?>): Map<String, LootPool> =
        loadedMap.filter { clazz.isAssignableFrom(it.value.javaClass) }.toMap()

    override fun getLootPool(key: String) =
        loadedMap[key]

    override fun removeLootPool(key: String): LootPool? {
        val lootPool = loadedMap.remove(key)
        val file = File(directory, "$key.yml")
        if (file.exists())
            file.delete()
        return lootPool
    }

    override fun addLootPool(lootPool: LootPool) {
        loadedMap[lootPool.key.key] = lootPool
    }

    override fun writeLootPool(lootPool: LootPool) {
        addLootPool(lootPool)
        val file = File(directory, "${lootPool.key.key}.yml")
        val config = YamlConfiguration.loadConfiguration(file)
        config.set(lootPool.key.key, lootPool)
        config.save(file)
    }

    override fun reload() {
        ReloadConfig.run(plugin)
        load(plugin.config)
    }

    override fun load(config: ConfigurationSection) {
        super.load(config)
        loadFiles(directory)
    }

    companion object {
        lateinit var instance: LootPoolManager
            private set
    }

}