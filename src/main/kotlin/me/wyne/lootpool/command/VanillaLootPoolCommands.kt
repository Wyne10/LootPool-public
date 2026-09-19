package me.wyne.lootpool.command

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.LootTableArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.CommandExecutor
import me.wyne.wutils.i18n.kotlin.placeholderComponent
import me.wyne.wutils.i18n.kotlin.replace
import me.wyne.lootpool.api.LootPoolProvider
import me.wyne.lootpool.api.VanillaLootPool
import org.bukkit.loot.LootTable

class RegisterCommand(lootPoolProvider: LootPoolProvider) : SubCommand("register") {
    override val command: CommandAPICommand = super.command
        .withShortDescription("Register a vanilla loot table as a loot pool.")
        .withFullDescription(
            """
                Register an existing vanilla loot table as a new loot pool.
                Provide a new unique key followed by the loot table path,
                e.g. "minecraft:chests/simple_dungeon".
                The pool wraps the vanilla table and rolls it whenever loot is generated.
            """.trimIndent()
        )
        .withPermission("lootpool.create")
        .withArguments(StringArgument("key"))
        .withArguments(LootTableArgument("path"))
        .executes(CommandExecutor { sender, args ->
            val key = args.getByClass("key", String::class.java)!!
            assertLootPoolNotExists(key, sender, lootPoolProvider.lootPoolMap)
            val lootTable = args.getByClass("path", LootTable::class.java)!!
            lootPoolProvider.writeLootPool(VanillaLootPool(key, lootTable.key))
            sender.placeholderComponent("success-lootpool-create", "key" replace key).sendMessage(sender)
        })
}