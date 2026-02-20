package org.bigcraft.lootpool.command

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.jorel.commandapi.CommandAPICommand
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
        CommandAPICommand("lootpool")
            .withSubcommand(
                CommandAPICommand("pool")
                    .withSubcommand(CreateCommand(lootPoolManager)())
                    .withSubcommand(ModifyCommand(lootPoolManager)())
                    .withSubcommand(RemoveCommand(lootPoolManager)())
                    .withSubcommand(InfoCommand(lootPoolManager)())
            )
            .withSubcommand(
                CommandAPICommand("item")
                    .withSubcommand(CreateLootCommand(lootManager)())
                    .withSubcommand(ModifyLootCommand(lootManager)())
                    .withSubcommand(RemoveLootCommand(lootManager)())
                    .withSubcommand(LootInfoCommand(lootManager)())
            )
            .withSubcommand(ReloadCommand(plugin)())
            .register(plugin)
    }

}
