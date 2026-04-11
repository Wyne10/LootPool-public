package org.bigcraft.lootpool.command

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.jorel.commandapi.CommandAPICommand
import org.bigcraft.lootpool.LootPool
import org.bigcraft.lootpool.core.LootPoolManager

@Singleton
class LootPoolCommand @Inject constructor(
    private val plugin: LootPool,
    private val lootPoolManager: LootPoolManager
) {

    init {
        registerCommand()
    }

    private fun registerCommand() {
        CommandAPICommand("lootpool")
            .withSubcommand(GiveCommand(lootPoolManager)())
            .withSubcommand(DropCommand(lootPoolManager)())
            .withSubcommand(InsertCommand(lootPoolManager)())
            .withSubcommand(FillCommand(lootPoolManager)())
            .withSubcommand(ProjectCommand(lootPoolManager)())
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
                    .withSubcommand(ProjectCommand(lootPoolManager)())
            )
            .withSubcommand(
                CommandAPICommand("item")
                    .withSubcommand(CreateLootCommand(lootPoolManager)())
                    .withSubcommand(ModifyLootCommand(lootPoolManager)())
                    .withSubcommand(RemoveLootCommand(lootPoolManager)())
                    .withSubcommand(LootInfoCommand(lootPoolManager)())
                    .withSubcommand(GiveCommand(lootPoolManager)())
                    .withSubcommand(DropCommand(lootPoolManager)())
                    .withSubcommand(InsertCommand(lootPoolManager)())
                    .withSubcommand(FillCommand(lootPoolManager)())
                    .withSubcommand(ProjectCommand(lootPoolManager)())
            )
            .withSubcommand(ReloadCommand(plugin)())
            .register(plugin)
    }

}
