package me.wyne.lootpool.core

import com.google.inject.Singleton
import me.wyne.lootpool.LoadableProvider
import me.wyne.lootpool.api.complex.EnchantmentPool
import me.wyne.lootpool.api.complex.EnchantmentPoolProvider
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

@Singleton
class EnchantmentPoolManager : LoadableProvider<EnchantmentPool>(
    "enchantment", EnchantmentPoolFactory
), EnchantmentPoolProvider {

    init {
        instance = this
    }

    override fun getEnchantmentPoolMap(): Map<String, EnchantmentPool> =
        loadedMap.toMap()

    override fun getEnchantmentPool(key: String) =
        loadedMap[key]

    override fun removeEnchantmentPool(key: String): EnchantmentPool? {
        val enchantmentPool = loadedMap.remove(key)
        val file = File(directory, "$key.yml")
        if (file.exists())
            file.delete()
        return enchantmentPool
    }

    override fun addEnchantmentPool(enchantmentPool: EnchantmentPool) {
        loadedMap[enchantmentPool.key] = enchantmentPool
    }

    override fun writeEnchantmentPool(enchantmentPool: EnchantmentPool) {
        addEnchantmentPool(enchantmentPool)
        val file = File(directory, "${enchantmentPool.key}.yml")
        val config = YamlConfiguration.loadConfiguration(file)
        config.set(enchantmentPool.key, enchantmentPool)
        config.save(file)
    }

    override fun load(config: ConfigurationSection) {
        super.load(config)
        loadFiles(directory)
    }

    companion object {
        lateinit var instance: EnchantmentPoolManager
            private set
    }

}
