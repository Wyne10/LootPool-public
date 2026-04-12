package org.bigcraft.lootpool.command

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.jorel.commandapi.CommandAPICommand
import org.bigcraft.lootpool.api.BasicLootPool
import org.bigcraft.lootpool.api.KeyedLoot
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
            .withSubcommand(InfoCommand<LootPool>(lootPoolManager)())
            .withSubcommand(GiveCommand<LootPool>(lootPoolManager)())
            .withSubcommand(DropCommand<LootPool>(lootPoolManager)())
            .withSubcommand(InsertCommand<LootPool>(lootPoolManager)())
            .withSubcommand(FillCommand<LootPool>(lootPoolManager)())
            .withSubcommand(ProjectCommand<LootPool>(lootPoolManager)())
            .withSubcommand(
                CommandAPICommand("pool")
                    .withSubcommand(CreateBasicCommand(lootPoolManager)())
                    .withSubcommand(ModifyBasicCommand(lootPoolManager)())
                    .withSubcommand(CloneBasicCommand(lootPoolManager)())
                    .withSubcommand(RemoveCommand(lootPoolManager, BasicLootPool::class.java)())
                    .withSubcommand(ComposeCommand(lootPoolManager, BasicLootPool::class.java)())
                    .withSubcommand(InfoCommand(lootPoolManager, BasicLootPool::class.java)())
                    .withSubcommand(GiveCommand(lootPoolManager, BasicLootPool::class.java)())
                    .withSubcommand(DropCommand(lootPoolManager, BasicLootPool::class.java)())
                    .withSubcommand(InsertCommand(lootPoolManager, BasicLootPool::class.java)())
                    .withSubcommand(FillCommand(lootPoolManager, BasicLootPool::class.java)())
                    .withSubcommand(ProjectCommand(lootPoolManager, BasicLootPool::class.java)())
            )
            .withSubcommand(
                CommandAPICommand("item")
                    .withSubcommand(CreateLootCommand(lootPoolManager)())
                    .withSubcommand(ModifyLootCommand(lootPoolManager)())
                    .withSubcommand(RemoveCommand(lootPoolManager, KeyedLoot::class.java)())
                    .withSubcommand(ComposeCommand(lootPoolManager, KeyedLoot::class.java)())
                    .withSubcommand(InfoCommand(lootPoolManager, KeyedLoot::class.java)())
                    .withSubcommand(GiveCommand(lootPoolManager, KeyedLoot::class.java)())
                    .withSubcommand(DropCommand(lootPoolManager, KeyedLoot::class.java)())
                    .withSubcommand(InsertCommand(lootPoolManager, KeyedLoot::class.java)())
                    .withSubcommand(FillCommand(lootPoolManager, KeyedLoot::class.java)())
                    .withSubcommand(ProjectCommand(lootPoolManager, BasicLootPool::class.java)())
            )
            .withSubcommand(ReloadCommand(plugin)())
            .register(plugin)
    }

}
