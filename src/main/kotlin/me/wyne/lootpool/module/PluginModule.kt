package me.wyne.lootpool.module

import com.google.inject.AbstractModule
import me.wyne.lootpool.LootPool
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.java.JavaPlugin

class PluginModule(private val plugin: LootPool) : AbstractModule() {
    override fun configure() {
        bind(LootPool::class.java)
            .toInstance(plugin)
        bind(JavaPlugin::class.java)
            .toInstance(plugin)
        bind(FileConfiguration::class.java)
            .toInstance(plugin.getConfig())
    }
}
