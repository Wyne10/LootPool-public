package me.wyne.lootpool.gui

import me.wyne.lootpool.api.EditableLootPool
import org.bukkit.entity.Player

/**
 * Opens the basic loot pool editor. Kept as a class with these constructors so the commands that
 * open it read the same as they always have.
 *
 * @param source  the pool being edited, or `null` when creating one from scratch
 * @param pending whether [source] is a transformation the opening command has not saved yet, in
 *                which case closing the editor untouched must still write it
 */
class LootPoolGui(
    key: String,
    player: Player,
    source: EditableLootPool? = null,
    pending: Boolean = false
) {

    init {
        val session = EditSession(player)
        val screen = BasicPoolScreen(session, key, source, pending)
        session.commitWith { screen.commit() }
        session.push(screen)
    }

}
