package org.bigcraft.lootpool

import com.google.inject.*
import me.wyne.wutils.common.plugin.CompositeJavaPlugin
import org.bukkit.configuration.MemoryConfiguration
import org.slf4j.Logger
import java.io.File

class LootPool : CompositeJavaPlugin<LootPool>() {

    internal var injector: Injector by WriteOnce()

    private val logDirectory = File(dataFolder, "log")

    override fun init() {
        instance = this
        addSteps(
            LoadDefaultConfig,
            InitializeLogger(logDirectory),
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

        var log: Logger by WriteOnce()
            internal set

        val EMPTY_CONFIGURATION = MemoryConfiguration()
    }

}

class WriteOnce<T> {
    private var _value: T? = null
    private var isSet = false

    @Suppress("UNCHECKED_CAST")
    operator fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): T {
        if (!isSet) throw IllegalStateException("Property '${property.name}' has not been initialized")
        return _value as T
    }

    operator fun setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: T) {
        if (isSet) throw IllegalStateException("Property '${property.name}' can only be set once")
        _value = value
        isSet = true
    }
}