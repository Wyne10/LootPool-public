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
        val poolSubcommands = listOf(
            CreateBasicCommand(lootPoolManager)(),
            ModifyBasicCommand(lootPoolManager)(),
            CloneBasicCommand(lootPoolManager)(),
            MergeBasicCommand(lootPoolManager)(),
            WeightBasicCommand(lootPoolManager)(),
            AmountBasicCommand(lootPoolManager)(),
            RollBasicCommand(lootPoolManager)(),
            FlattenBasicCommand(lootPoolManager)(),
        )
        val poolCommand = CommandAPICommand("pool")
            .withShortDescription("Manage basic loot pools.")
            .apply { poolSubcommands.forEach { withSubcommand(it) } }
            .withSubcommand(helpCommand("lootpool pool", poolSubcommands))

        val itemSubcommands = listOf(
            CreateLootCommand(lootPoolManager)(),
            ModifyLootCommand(lootPoolManager)(),
        )
        val itemCommand = CommandAPICommand("item")
            .withShortDescription("Manage keyed loot items.")
            .apply { itemSubcommands.forEach { withSubcommand(it) } }
            .withSubcommand(helpCommand("lootpool item", itemSubcommands))

        val rootSubcommands = listOf(
            RemoveCommand<LootPool>(lootPoolManager)(),
            ComposeCommand<LootPool>(lootPoolManager)(),
            IncludeCommand<LootPool>(lootPoolManager)(),
            SnapshotCommand(lootPoolManager)(),
            RegisterCommand(lootPoolManager)(),
            InfoCommand<LootPool>(lootPoolManager)(),
            GiveCommand<LootPool>(lootPoolManager)(),
            DropCommand<LootPool>(lootPoolManager)(),
            InsertCommand<LootPool>(lootPoolManager)(),
            FillCommand<LootPool>(lootPoolManager)(),
            PopulateCommand<LootPool>(lootPoolManager)(),
            ProjectCommand<LootPool>(lootPoolManager)(),
            PreviewCommand<LootPool>(lootPoolManager)(),
            poolCommand,
            itemCommand,
            ReloadCommand(plugin)(),
        )

        CommandAPICommand("lootpool")
            .apply { rootSubcommands.forEach { withSubcommand(it) } }
            .withSubcommand(helpCommand("lootpool", rootSubcommands))
            .register(plugin)
    }

}
