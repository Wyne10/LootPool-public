package me.wyne.lootpool.core

import com.google.inject.Inject
import com.google.inject.Singleton
import me.wyne.lootpool.LoadableProvider
import me.wyne.lootpool.ReloadConfig
import me.wyne.lootpool.api.BasicLootPool
import me.wyne.lootpool.api.CompositeLootPool
import me.wyne.lootpool.api.KeyedLoot
import me.wyne.lootpool.api.Loot
import me.wyne.lootpool.api.LootPool
import me.wyne.lootpool.api.LootPoolProvider
import me.wyne.lootpool.api.MultiLootPool
import me.wyne.lootpool.api.RollLootPool
import me.wyne.lootpool.api.SnapshotLootPool
import me.wyne.lootpool.api.VanillaLootPool
import me.wyne.lootpool.api.complex.condition.AllOfCondition
import me.wyne.lootpool.api.complex.condition.AnyOfCondition
import me.wyne.lootpool.api.complex.modifier.AttributeEntry
import me.wyne.lootpool.api.complex.ComplexLootPool
import me.wyne.lootpool.api.complex.modifier.ConditionalModifier
import me.wyne.lootpool.api.complex.modifier.DamageModifier
import me.wyne.lootpool.api.complex.EmptyEntry
import me.wyne.lootpool.api.complex.modifier.EnchantModifier
import me.wyne.lootpool.api.complex.EnchantmentEntry
import me.wyne.lootpool.api.complex.EnchantmentPool
import me.wyne.lootpool.api.complex.condition.InvertedCondition
import me.wyne.lootpool.api.complex.ItemEntry
import me.wyne.lootpool.api.complex.LootRoll
import me.wyne.lootpool.api.complex.modifier.LoreModifier
import me.wyne.lootpool.api.complex.modifier.NameModifier
import me.wyne.lootpool.api.complex.condition.PermissionCondition
import me.wyne.lootpool.api.complex.PoolEntry
import me.wyne.lootpool.api.complex.condition.RandomChanceCondition
import me.wyne.lootpool.api.complex.modifier.SetAttributesModifier
import me.wyne.lootpool.api.complex.condition.TimeCondition
import me.wyne.lootpool.api.complex.condition.WeatherCondition
import me.wyne.lootpool.api.complex.condition.WorldCondition
import me.wyne.lootpool.condition.PlaceholderCondition
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.configuration.serialization.ConfigurationSerialization
import java.io.File

@Singleton
class LootPoolManager @Inject constructor(private val plugin: me.wyne.lootpool.LootPool) : LoadableProvider<LootPool>(
    "lootpool", LootPoolFactory
), LootPoolProvider {

    init {
        instance = this
        ConfigurationSerialization.registerClass(Loot::class.java)
        ConfigurationSerialization.registerClass(KeyedLoot::class.java)
        ConfigurationSerialization.registerClass(BasicLootPool::class.java)
        ConfigurationSerialization.registerClass(RollLootPool::class.java)
        ConfigurationSerialization.registerClass(CompositeLootPool::class.java)
        ConfigurationSerialization.registerClass(SnapshotLootPool::class.java)
        ConfigurationSerialization.registerClass(MultiLootPool::class.java)
        ConfigurationSerialization.registerClass(VanillaLootPool::class.java)

        ConfigurationSerialization.registerClass(ComplexLootPool::class.java)
        ConfigurationSerialization.registerClass(LootRoll::class.java)

        ConfigurationSerialization.registerClass(ItemEntry::class.java)
        ConfigurationSerialization.registerClass(PoolEntry::class.java)
        ConfigurationSerialization.registerClass(EmptyEntry::class.java)

        ConfigurationSerialization.registerClass(ConditionalModifier::class.java)
        ConfigurationSerialization.registerClass(EnchantModifier::class.java)
        ConfigurationSerialization.registerClass(DamageModifier::class.java)
        ConfigurationSerialization.registerClass(SetAttributesModifier::class.java)
        ConfigurationSerialization.registerClass(AttributeEntry::class.java)
        ConfigurationSerialization.registerClass(NameModifier::class.java)
        ConfigurationSerialization.registerClass(LoreModifier::class.java)

        ConfigurationSerialization.registerClass(RandomChanceCondition::class.java)
        ConfigurationSerialization.registerClass(AllOfCondition::class.java)
        ConfigurationSerialization.registerClass(AnyOfCondition::class.java)
        ConfigurationSerialization.registerClass(InvertedCondition::class.java)
        ConfigurationSerialization.registerClass(PermissionCondition::class.java)
        ConfigurationSerialization.registerClass(WorldCondition::class.java)
        ConfigurationSerialization.registerClass(TimeCondition::class.java)
        ConfigurationSerialization.registerClass(WeatherCondition::class.java)
        ConfigurationSerialization.registerClass(PlaceholderCondition::class.java)

        ConfigurationSerialization.registerClass(EnchantmentPool::class.java)
        ConfigurationSerialization.registerClass(EnchantmentEntry::class.java)
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