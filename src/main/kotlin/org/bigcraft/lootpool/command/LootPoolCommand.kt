package org.bigcraft.lootpool.command

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.jorel.commandapi.CommandAPICommand
import org.bigcraft.lootpool.LootPool
import org.bigcraft.lootpool.core.CommonLootManager
import org.bigcraft.lootpool.core.LootManager
import org.bigcraft.lootpool.core.LootPoolManager

@Singleton
class LootPoolCommand @Inject constructor(
    private val plugin: LootPool,
    private val commonLootManager: CommonLootManager,
    private val lootPoolManager: LootPoolManager,
    private val lootManager: LootManager
) {

    init {
        registerCommand()
    }

    private fun registerCommand() {
        CommandAPICommand("lootpool")
            .withSubcommand(GiveCommand(commonLootManager)())
            .withSubcommand(DropCommand(commonLootManager)())
            .withSubcommand(InsertCommand(commonLootManager)())
            .withSubcommand(FillCommand(commonLootManager)())
            .withSubcommand(
                CommandAPICommand("pool")
                    .withSubcommand(CreateCommand(lootPoolManager)())
                    .withSubcommand(ModifyCommand(lootPoolManager)())
                    .withSubcommand(RemoveCommand(lootPoolManager)())
                    .withSubcommand(InfoCommand(lootPoolManager)())
                    .withSubcommand(CloneCommand(lootPoolManager)())
                    .withSubcommand(GiveCommand(lootPoolManager)())
                    .withSubcommand(DropCommand(lootPoolManager)())
                    .withSubcommand(InsertCommand(lootPoolManager)())
                    .withSubcommand(FillCommand(lootPoolManager)())
            )
            .withSubcommand(
                CommandAPICommand("item")
                    .withSubcommand(CreateLootCommand(lootManager)())
                    .withSubcommand(ModifyLootCommand(lootManager)())
                    .withSubcommand(RemoveLootCommand(lootManager)())
                    .withSubcommand(LootInfoCommand(lootManager)())
                    .withSubcommand(GiveCommand(lootManager)())
                    .withSubcommand(DropCommand(lootManager)())
                    .withSubcommand(InsertCommand(lootManager)())
                    .withSubcommand(FillCommand(lootManager)())
            )
            .withSubcommand(ReloadCommand(plugin)())
            .register(plugin)
    }

}
