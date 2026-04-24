package org.bigcraft.lootpool.core

import com.google.inject.Inject
import com.google.inject.Singleton
import org.bigcraft.lootpool.AbstractManager
import org.bigcraft.lootpool.ConfigurableFactory
import org.bigcraft.lootpool.ReloadConfig
import org.bigcraft.lootpool.api.BasicLootPool
import org.bigcraft.lootpool.api.CompositeLootPool
import org.bigcraft.lootpool.api.KeyedLoot
import org.bigcraft.lootpool.api.Loot
import org.bigcraft.lootpool.api.LootPool
import org.bigcraft.lootpool.api.LootPoolProvider
import org.bigcraft.lootpool.api.SnapshotLootPool
import org.bigcraft.lootpool.command.LootPoolCommand
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
        ConfigurationSerialization.registerClass(KeyedLoot::class.java)
        ConfigurationSerialization.registerClass(BasicLootPool::class.java)
        ConfigurationSerialization.registerClass(CompositeLootPool::class.java)
        ConfigurationSerialization.registerClass(SnapshotLootPool::class.java)
    }

    override fun getLootPoolMap(): Map<String, LootPool> =
        loadedMap.toMap()

    override fun getMapOf(clazz: Class<out LootPool?>): Map<String, LootPool> =
        loadedMap.filter { clazz.isAssignableFrom(it.value.javaClass) }.toMap()

    override fun getLootPool(key: String) =
        loadedMap[key]

    override fun removeLootPool(key: String): LootPool? {
        val lootPool = loadedMap.remove(key)
        val file = File(lootPoolDirectory, "$key.yml")
        if (file.exists())
            file.delete()
        return lootPool
    }

    override fun addLootPool(lootPool: LootPool) {
        loadedMap[lootPool.key.key] = lootPool
    }

    override fun writeLootPool(lootPool: LootPool) {
        addLootPool(lootPool)
        val file = File(lootPoolDirectory, "${lootPool.key.key}.yml")
        val config = YamlConfiguration.loadConfiguration(file)
        config.set(lootPool.key.key, lootPool)
        config.save(file)
    }

    override fun reload() {
        ReloadConfig.run(plugin)
        load(plugin.config)
    }

    override fun load(config: ConfigurationSection) {
        migrateLoot()
        lootPoolDirectory.listFiles()
            ?.forEach { migrateLootPool(it) }
        super.load(config)
        loadFiles(lootPoolDirectory)
    }

    private fun migrateLoot() {
        val oldDirectory = File(plugin.dataFolder, "loot/")
        if (oldDirectory.exists() && oldDirectory.isDirectory) {
            lootPoolDirectory.mkdirs()
            oldDirectory.listFiles()?.forEach {
                var target = File(lootPoolDirectory, "${it.nameWithoutExtension}-loot.${it.extension}")
                var counter = 1
                while (target.exists()) {
                    val nameWithoutExt = it.nameWithoutExtension
                    val ext = it.extension
                    target = File(lootPoolDirectory, "${nameWithoutExt}_$counter.$ext")
                    counter++
                }
                it.renameTo(target)
                val content = target.readText()
                val lines = content.lines().toMutableList()
                if (lines.isNotEmpty()) {
                    lines[0] = "${target.nameWithoutExtension}:"
                }
                val migrated = lines.joinToString("\n")
                    .replace("key: ${it.nameWithoutExtension}", "key: ${target.nameWithoutExtension}")
                target.writeText(migrated)
            }
            oldDirectory.delete()
        }
    }

    private fun migrateLootPool(file: File) {
        val content = file.readText()
        val migrated = content.replace(
            Regex("""(?m)^(\s*)==:\s*org\.bigcraft\.lootpool\.api\.LootPool\s*$"""),
            "$1==: org.bigcraft.lootpool.api.BasicLootPool"
        )
        if (migrated != content) {
            file.writeText(migrated)
        }
    }

    companion object {
        lateinit var instance: LootPoolManager
            private set
    }

}