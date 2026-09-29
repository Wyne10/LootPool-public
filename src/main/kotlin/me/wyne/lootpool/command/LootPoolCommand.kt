package me.wyne.lootpool.command

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.jorel.commandapi.CommandAPICommand
import me.wyne.lootpool.api.LootPool
import me.wyne.lootpool.api.CloneableLootPool
import me.wyne.lootpool.command.complex.ConvertComplexCommand
import me.wyne.lootpool.command.complex.CreateComplexCommand
import me.wyne.lootpool.command.complex.EditComplexCommand
import me.wyne.lootpool.command.complex.conditionComplexCommand
import me.wyne.lootpool.command.complex.entryComplexCommand
import me.wyne.lootpool.command.complex.modifierComplexCommand
import me.wyne.lootpool.command.complex.rollComplexCommand
import me.wyne.lootpool.core.EnchantmentPoolManager
import me.wyne.lootpool.core.LootPoolManager

@Singleton
class LootPoolCommand @Inject constructor(
    private val plugin: me.wyne.lootpool.LootPool,
    private val lootPoolManager: LootPoolManager,
    private val enchantmentPoolManager: EnchantmentPoolManager
) {

    init {
        registerCommand()
    }

    private fun registerCommand() {
        val poolSubcommands = listOf(
            CreateBasicCommand(lootPoolManager)(),
            ModifyBasicCommand(lootPoolManager)(),
            MergeBasicCommand(lootPoolManager)(),
            WeightBasicCommand(lootPoolManager)(),
            AmountBasicCommand(lootPoolManager)(),
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

        val complexSubcommands = listOf(
            CreateComplexCommand(lootPoolManager)(),
            ConvertComplexCommand(lootPoolManager)(),
            EditComplexCommand(lootPoolManager, enchantmentPoolManager)(),
            rollComplexCommand(lootPoolManager),
            entryComplexCommand(lootPoolManager),
            modifierComplexCommand(lootPoolManager, enchantmentPoolManager),
            conditionComplexCommand(lootPoolManager),
        )
        val complexCommand = CommandAPICommand("complex")
            .withShortDescription("Manage complex loot pools.")
            .apply { complexSubcommands.forEach { withSubcommand(it) } }
            .withSubcommand(helpCommand("lootpool complex", complexSubcommands))

        val enchantSubcommands = listOf(
            CreateEnchantmentPoolCommand(enchantmentPoolManager)(),
            RemoveEnchantmentPoolCommand(enchantmentPoolManager)(),
            CloneEnchantmentPoolCommand(enchantmentPoolManager)(),
            InfoEnchantmentPoolCommand(enchantmentPoolManager)(),
            EditEnchantmentPoolCommand(enchantmentPoolManager)(),
            entryEnchantmentCommand(enchantmentPoolManager),
        )
        val enchantCommand = CommandAPICommand("enchant")
            .withShortDescription("Manage enchantment pools.")
            .apply { enchantSubcommands.forEach { withSubcommand(it) } }
            .withSubcommand(helpCommand("lootpool enchant", enchantSubcommands))

        val rootSubcommands = listOf(
            RemoveCommand<LootPool>(lootPoolManager)(),
            CloneCommand<CloneableLootPool>(lootPoolManager)(),
            ComposeCommand<LootPool>(lootPoolManager)(),
            IncludeCommand<LootPool>(lootPoolManager)(),
            RollCommand<LootPool>(lootPoolManager)(),
            SnapshotCommand(lootPoolManager)(),
            RegisterCommand(lootPoolManager)(),
            InfoCommand<LootPool>(lootPoolManager)(),
            GiveCommand<LootPool>(lootPoolManager)(),
            DropCommand<LootPool>(lootPoolManager)(),
            SpawnCommand<LootPool>(lootPoolManager)(),
            InsertCommand<LootPool>(lootPoolManager)(),
            FillCommand<LootPool>(lootPoolManager)(),
            PopulateCommand<LootPool>(lootPoolManager)(),
            ProjectCommand<LootPool>(lootPoolManager)(),
            PreviewCommand<LootPool>(lootPoolManager)(),
            poolCommand,
            itemCommand,
            complexCommand,
            enchantCommand,
            ReloadCommand(plugin)()
        )

        CommandAPICommand("lootpool")
            .apply { rootSubcommands.forEach { withSubcommand(it) } }
            .withSubcommand(helpCommand("lootpool", rootSubcommands))
            .register(plugin)
    }

}
