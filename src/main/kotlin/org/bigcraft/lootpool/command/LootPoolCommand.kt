package org.bigcraft.lootpool.command

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.jorel.commandapi.kotlindsl.commandAPICommand
import org.bigcraft.lootpool.LootPool
import org.bigcraft.lootpool.core.LootManager
import org.bigcraft.lootpool.core.LootPoolManager

@Singleton
class LootPoolCommand @Inject constructor(
    private val plugin: LootPool,
    private val lootPoolManager: LootPoolManager,
    private val lootManager: LootManager
) {

    init {
        registerCommand()
    }

    private fun registerCommand() {
        commandAPICommand("lootpool") {
            withSubcommand(ReloadCommand(plugin)())
            withSubcommand(CreateCommand(lootPoolManager)())
            withSubcommand(ModifyCommand(lootPoolManager)())
            withSubcommand(RemoveCommand(lootPoolManager)())
            withSubcommand(InfoCommand(lootPoolManager)())
            withSubcommand(CreateLootCommand(lootManager)())
            withSubcommand(ModifyLootCommand(lootManager)())
            withSubcommand(RemoveLootCommand(lootManager)())
            withSubcommand(LootInfoCommand(lootManager)())
        }
    }

}
