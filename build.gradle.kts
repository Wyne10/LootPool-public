import net.minecrell.pluginyml.bukkit.BukkitPluginDescription
import org.codehaus.plexus.util.Os

plugins {
    kotlin("jvm") version "2.2.20"
    alias(libs.plugins.shadow)
    alias(libs.plugins.runPaper)
    alias(libs.plugins.pluginYml)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    compileOnly(libs.paperApi)
    compileOnly(libs.placeholderApi)
    compileOnly(libs.commandApi)
    compileOnly(libs.commandApiKotlin)

    implementation(project(":api"))

    implementation(libs.guice)
    implementation(libs.adventureMini)
    implementation(libs.adventureBukkit)

    implementation(libs.wutilsConfig)
    implementation(libs.wutilsLog)
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
            relocate("net.kyori", "org.bigcraft.lootpool.shadow.net.kyori")
            relocate("me.wyne.wutils", "org.bigcraft.lootpool.shadow.wutils")
        }
    }

    runServer {
        val minecraftVersion: String = if (Os.isFamily(Os.FAMILY_WINDOWS) || isDebug) "1.19.4" else "1.16.5"
        val viaVersion = "5.4.2"
        val commandApiVersion = "9.4.2"
        downloadPlugins {
            url("https://ci.extendedclip.com/view/Plugins/job/PlaceholderAPI/197/artifact/build/libs/PlaceholderAPI-2.11.6.jar")
            url("https://download.luckperms.net/1604/bukkit/loader/LuckPerms-Bukkit-5.5.15.jar")
            github("dmulloy2", "ProtocolLib", "5.4.0", "ProtocolLib.jar")
            github("ViaVersion", "ViaVersion", viaVersion, "ViaVersion-$viaVersion.jar")
            github("ViaVersion", "ViaBackwards", viaVersion, "ViaBackwards-$viaVersion.jar")
            github("CommandAPI", "CommandAPI", commandApiVersion, "CommandAPI-$commandApiVersion.jar")
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
    softDepend = listOf("PlaceholderAPI", "CommandAPI")
    permissions {
        register("lootpool.*") {
            children = listOf("lootpool.reload", "lootpool.create", "lootpool.modify", "lootpool.remove", "lootpool.info")
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
    }
}