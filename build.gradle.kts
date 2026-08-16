import net.minecrell.pluginyml.bukkit.BukkitPluginDescription
import org.codehaus.plexus.util.Os

plugins {
    kotlin("jvm") version "2.4.10"
    alias(libs.plugins.shadow)
    alias(libs.plugins.runPaper)
    alias(libs.plugins.pluginYml)
}

kotlin {
    jvmToolchain(16)
}

dependencies {
    compileOnly(libs.paperApi)
    compileOnly(libs.placeholderApi)
    compileOnly(libs.commandApi)
    compileOnly(libs.abstractMenus)

    implementation(project(":api"))

    implementation(libs.guice)
    implementation(libs.enhancedLegacy)

    implementation(libs.wutilsConfig)
    implementation(libs.wutilsConfigurables)
    implementation(libs.wutilsI18nKotlin)
    implementation(libs.wutilsCommonKotlin)
}

tasks {
    val isDebug = findProperty("debug")?.toString()?.toBoolean() ?: false

    shadowJar {
        archiveBaseName.set(findProperty("name").toString())
        archiveClassifier.set("")
        minimize()
        if (!isDebug) {
            relocate("com.google.inject", "org.bigcraft.lootpool.shadow.google.guice")
            relocate("com.google.common", "org.bigcraft.lootpool.shadow.google.common")
            relocate("me.wyne.wutils", "org.bigcraft.lootpool.shadow.wutils")
            relocate("dev.vankka", "org.bigcraft.lootpool.shadow.dev.vankka")
        }
    }

    runServer {
        val minecraftVersion: String = if (Os.isFamily(Os.FAMILY_WINDOWS) || isDebug) "1.19.4" else "1.16.5"
        val viaVersion = "5.11.0"
        val commandApiVersion = "9.4.3"
        downloadPlugins {
            url("https://download.luckperms.net/1652/bukkit/loader/LuckPerms-Bukkit-5.5.65.jar")
            github("PlaceholderAPI", "PlaceholderAPI", "2.12.2", "PlaceholderAPI-2.12.2.jar")
            github("dmulloy2", "ProtocolLib", "5.4.0", "ProtocolLib.jar")
            github("ViaVersion", "ViaVersion", viaVersion, "ViaVersion-$viaVersion.jar")
            github("ViaVersion", "ViaBackwards", viaVersion, "ViaBackwards-$viaVersion.jar")
            github("NeverMined-Entertainment", "CommandAPI", commandApiVersion, "CommandAPI-$commandApiVersion.jar")
        }
        minecraftVersion(minecraftVersion)
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
    }
}

tasks.withType(xyz.jpenilla.runtask.task.AbstractRun::class) {
    javaLauncher = javaToolchains.launcherFor {
        vendor = JvmVendorSpec.JETBRAINS
        languageVersion = JavaLanguageVersion.of(21)
    }
    jvmArgs("-XX:+AllowEnhancedClassRedefinition", "-DPaper.IgnoreJavaVersion=true")
}

bukkit {
    name = findProperty("name").toString()
    version = getVersion().toString()
    website = findProperty("website").toString()
    author = findProperty("author").toString()
    main = "org.bigcraft.lootpool.LootPool"
    apiVersion = "1.16"
    softDepend = listOf("PlaceholderAPI", "CommandAPI", "AbstractMenus", "CustomEnchants")
    permissions {
        register("lootpool.*") {
            children = listOf("lootpool.reload", "lootpool.create", "lootpool.modify", "lootpool.remove", "lootpool.info",
                "lootpool.give", "lootpool.drop", "lootpool.insert", "lootpool.populate", "lootpool.spawn", "lootpool.fill")
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("lootpool.reload") {
            description = "Allows to reload plugin"
        }
        register("lootpool.create") {
            description = "Allows to create loot pools"
        }
        register("lootpool.modify") {
            description = "Allows to modify existing loot pools"
        }
        register("lootpool.remove") {
            description = "Allows to remove existing loot pools"
        }
        register("lootpool.info") {
            description = "Allows to check info about existing loot pools"
        }

        register("lootpool.give") {
            description = "Allows to give loot pools to players"
        }
        register("lootpool.drop") {
            description = "Allows to drop loot pools on ground"
        }
        register("lootpool.insert") {
            description = "Allows to insert loot pools in container"
        }
        register("lootpool.populate") {
            description = "Allows to populate player inventories with loot pools"
        }
        register("lootpool.spawn") {
            description = "Allows to spawn loot pools on ground"
        }
        register("lootpool.fill") {
            description = "Allows to fill containers with loot pools"
        }
        register("lootpool.project") {
            description = "Allows to project loot pools to players"
        }
    }
}