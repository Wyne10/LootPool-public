package org.bigcraft.lootpool.command

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.jorel.commandapi.kotlindsl.commandAPICommand
import org.bigcraft.lootpool.LootPool

@Singleton
class LootPoolCommand @Inject constructor(private val plugin: LootPool) {

    init {
        registerCommand()
    }

    private fun registerCommand() {
        commandAPICommand("lootpool") {
            withSubcommand(ReloadCommand(plugin)())
            withSubcommand(CreateCommand())
        }
    }

}
