package org.bigcraft.lootpool

import com.google.inject.*
import me.wyne.wutils.common.plugin.CompositeJavaPlugin
import org.bukkit.configuration.MemoryConfiguration
import org.slf4j.Logger

class LootPool : CompositeJavaPlugin<LootPool>() {

    lateinit var injector: Injector
        internal set

    override fun init() {
        instance = this
        addSteps(
            LoadDefaultConfig,
            InitializeLogger,
            InitializeI18n,
            InitializeInjector,
            InitializeConfig,
            InitializeLoader,
            Load,
            Reload
        )
    }

    companion object {
        lateinit var instance: LootPool
            private set
        lateinit var logger: Logger
            internal set
        val EMPTY_CONFIGURATION = MemoryConfiguration()
    }

}