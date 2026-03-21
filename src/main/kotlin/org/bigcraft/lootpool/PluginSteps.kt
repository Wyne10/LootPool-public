package org.bigcraft.lootpool

import com.google.inject.CreationException
import com.google.inject.Guice
import com.google.inject.Stage
import me.wyne.wutils.common.loadable.Loader
import me.wyne.wutils.common.plugin.CompositeStep
import me.wyne.wutils.common.plugin.LevelWrapper
import me.wyne.wutils.common.plugin.LoggerWrapper
import me.wyne.wutils.common.plugin.PluginStep
import me.wyne.wutils.common.plugin.Step
import me.wyne.wutils.common.plugin.StepScope
import me.wyne.wutils.config.Config
import me.wyne.wutils.i18n.I18n
import me.wyne.wutils.i18n.PluginI18nBuilder
import me.wyne.wutils.i18n.language.component.PaperComponentAudiences
import me.wyne.wutils.i18n.language.interpretation.ComponentInterpreters
import me.wyne.wutils.i18n.language.validation.EmptyValidator
import org.bigcraft.lootpool.LootPool.Companion.EMPTY_CONFIGURATION
import org.bigcraft.lootpool.LootPool.Companion.logger
import org.bigcraft.lootpool.module.AbstractMenusModule
import org.bigcraft.lootpool.module.ApiModule
import org.bigcraft.lootpool.module.CommandModule
import org.bigcraft.lootpool.module.LootPoolModule
import org.bigcraft.lootpool.module.PluginModule

@Step(priority = 0, scope = StepScope.ENABLE)
object LoadDefaultConfig : PluginStep<LootPool> {
    override fun run(plugin: LootPool) {
        plugin.saveDefaultConfig()
        plugin.config.setDefaults(EMPTY_CONFIGURATION)
    }
}

@Step(priority = 1, scope = StepScope.ENABLE)
object InitializeLogger : PluginStep<LootPool> {
    override fun run(plugin: LootPool) {
        logger = LoggerWrapper(plugin.slF4JLogger, LevelWrapper.valueOf(plugin.config.getString("logLevel", "INFO")!!))
    }
}

@Step(priority = 3, scope = StepScope.ENABLE)
object InitializeI18n : PluginStep<LootPool> {
    override fun run(plugin: LootPool) {
        I18n.global = PluginI18nBuilder(plugin)
            .setLogger(logger)
            .setComponentAudience(PaperComponentAudiences())
            .setComponentInterpreter(
                ComponentInterpreters.valueOf(
                    plugin.config.getString("serializer", "ENHANCED_LEGACY")!!
                ).get(EmptyValidator())
            )
            .setUsePlayerLanguage(plugin.config.getBoolean("usePlayerLanguage", true))
            .loadLanguage("lang/ru.yml")
            .loadLanguage("lang/en.yml")
            .build()
    }
}

@Step(priority = 4, scope = StepScope.ENABLE)
object InitializeInjector : PluginStep<LootPool> {
    override fun run(plugin: LootPool) {
        try {
            LootPool.instance.injector = Guice.createInjector(
                Stage.PRODUCTION,
                PluginModule(plugin),
                LootPoolModule,
                ApiModule,
                CommandModule,
                AbstractMenusModule
            )
        } catch (e: CreationException) {
            logger.error("Guice injector creation exception", e)
        }
    }
}

@Step(priority = 5, scope = StepScope.ENABLE)
object InitializeConfig : CompositeStep<LootPool>(ReloadConfig) {
    override fun before(plugin: LootPool) {
        Config.global.apply {
            logger = LootPool.logger
            setConfigGenerator(plugin, "config.yml")
            generateConfig()
        }
    }
}

@Step(priority = 6, scope = StepScope.ENABLE)
object InitializeLoader : PluginStep<LootPool> {
    override fun run(plugin: LootPool) {
        Loader.global.registerConfig(Loader.DEFAULT_PATH, plugin.config)
    }
}

@Step(priority = 7, scope = StepScope.ENABLE)
object Load : PluginStep<LootPool> {
    override fun run(plugin: LootPool) {
        Loader.global.load(plugin)
    }
}

@Step(scope = StepScope.RELOAD)
object Reload : CompositeStep<LootPool>(ReloadConfig, InitializeLogger, InitializeLoader, InitializeI18n, Load)

object ReloadConfig : PluginStep<LootPool> {
    override fun run(plugin: LootPool) {
        plugin.reloadConfig()
        plugin.config.setDefaults(EMPTY_CONFIGURATION)
        Config.global.reloadConfig(plugin.config)
    }
}