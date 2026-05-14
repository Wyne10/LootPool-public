package org.bigcraft.lootpool.command

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.jorel.commandapi.CommandAPICommand
import org.bigcraft.lootpool.api.LootPool
import org.bigcraft.lootpool.core.LootPoolManager

@Singleton
class LootPoolCommand @Inject constructor(
    private val plugin: org.bigcraft.lootpool.LootPool,
    private val lootPoolManager: LootPoolManager
) {

    init {
        registerCommand()
    }

    private fun registerCommand() {
        CommandAPICommand("lootpool")
            .withSubcommand(RemoveCommand<LootPool>(lootPoolManager)())
            .withSubcommand(ComposeCommand<LootPool>(lootPoolManager)())
            .withSubcommand(SnapshotCommand(lootPoolManager)())
            .withSubcommand(InfoCommand<LootPool>(lootPoolManager)())
            .withSubcommand(GiveCommand<LootPool>(lootPoolManager)())
            .withSubcommand(DropCommand<LootPool>(lootPoolManager)())
            .withSubcommand(InsertCommand<LootPool>(lootPoolManager)())
            .withSubcommand(FillCommand<LootPool>(lootPoolManager)())
            .withSubcommand(PopulateCommand<LootPool>(lootPoolManager)())
            .withSubcommand(ProjectCommand<LootPool>(lootPoolManager)())
            .withSubcommand(PreviewCommand<LootPool>(lootPoolManager)())
            .withSubcommand(
                CommandAPICommand("pool")
                    .withSubcommand(CreateBasicCommand(lootPoolManager)())
                    .withSubcommand(ModifyBasicCommand(lootPoolManager)())
                    .withSubcommand(CloneBasicCommand(lootPoolManager)())
                    .withSubcommand(MergeBasicCommand(lootPoolManager)())
            )
            .withSubcommand(
                CommandAPICommand("item")
                    .withSubcommand(CreateLootCommand(lootPoolManager)())
                    .withSubcommand(ModifyLootCommand(lootPoolManager)())
            )
            .withSubcommand(ReloadCommand(plugin)())
            .register(plugin)
    }

}
