package me.wyne.lootpool.gui

import me.wyne.lootpool.LootPool
import me.wyne.wutils.common.event.ListenerRegistry
import me.wyne.wutils.i18n.kotlin.placeholderComponent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerQuitEvent

/**
 * One open editor: a stack of [Screen]s the player navigates, and the single Bukkit listener that
 * drives them.
 */
class EditSession(val player: Player) : Listener {

    private val listenerRegistry = ListenerRegistry(LootPool.instance)
    private val stack = ArrayDeque<Screen>()
    private var onCommit: (() -> Unit)? = null

    /**
     * Set while an inventory swap is in flight. `openInventory` fires the outgoing inventory's
     * [InventoryCloseEvent] synchronously, and without this the editor would commit and tear itself
     * down the first time the player opened a child screen.
     */
    private var navigating = false
    private var closed = false

    /** Set when the editor is being closed deliberately without keeping the edits. */
    private var discarding = false

    var dirty = false
        private set

    var step = 1
        private set

    val current: Screen?
        get() = stack.lastOrNull()

    init {
        listenerRegistry.register(this)
    }

    /** Runs when the editor is closed for good, whether by the close button, escape or quitting. */
    fun commitWith(action: () -> Unit) {
        onCommit = action
    }

    fun markDirty() {
        dirty = true
    }

    fun cycleStep() {
        step = when (step) {
            1 -> 10
            10 -> 100
            else -> 1
        }
    }

    fun push(screen: Screen) {
        stack.addLast(screen)
        run { open(screen) }
    }

    fun pop() {
        if (stack.size <= 1) {
            close()
            return
        }
        stack.removeLast()
        run { open(stack.last()) }
    }

    fun refresh() {
        run { current?.render() }
    }

    /** Defers an action to the next tick, for anything that must not run inside a click handler. */
    fun later(action: () -> Unit) {
        run { action() }
    }

    /**
     * Closes the editor. With [save] unset the edits are thrown away instead of committed; closing
     * any other way, escape included, still saves.
     */
    fun close(save: Boolean = true) {
        discarding = !save
        run { player.closeInventory() }
    }

    private fun open(screen: Screen) {
        navigating = true
        screen.render()
        player.openInventory(screen.inventory)
        navigating = false
    }

    @EventHandler(ignoreCancelled = true)
    private fun onClick(event: InventoryClickEvent) {
        val screen = current ?: return
        if (event.inventory != screen.inventory) return
        // Clicking off the window is action NOTHING, which the filter below would swallow - and
        // that is exactly the click that turns the page.
        if (event.slotType != InventoryType.SlotType.OUTSIDE &&
            (event.action in CANCELLED_ACTIONS || event.click in CANCELLED_CLICKS)) {
            event.isCancelled = true
            return
        }
        screen.onClick(event)
    }

    @EventHandler(ignoreCancelled = true)
    private fun onDrag(event: InventoryDragEvent) {
        val screen = current ?: return
        if (event.inventory != screen.inventory) return
        event.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    private fun onClose(event: InventoryCloseEvent) {
        if (navigating || closed) return
        val screen = current ?: return
        if (event.inventory != screen.inventory) return
        terminate()
    }

    @EventHandler(ignoreCancelled = true)
    private fun onQuit(event: PlayerQuitEvent) {
        if (event.player != player) return
        terminate()
    }

    private fun terminate() {
        if (closed) return
        closed = true
        stack.clear()
        listenerRegistry.close()
        if (discarding) {
            if (dirty)
                player.placeholderComponent("gui-discarded").sendMessage(player)
        } else {
            onCommit?.invoke()
        }
        onCommit = null
    }

    /**
     * Defers to the next tick. Opening an inventory straight out of a click handler desyncs the
     * client's cursor.
     */
    private fun run(runnable: Runnable) {
        Bukkit.getScheduler().runTask(LootPool.instance, runnable)
    }

    companion object {
        private val CANCELLED_ACTIONS = setOf(
            InventoryAction.COLLECT_TO_CURSOR,
            InventoryAction.CLONE_STACK,
            InventoryAction.UNKNOWN,
            InventoryAction.NOTHING,
        )

        private val CANCELLED_CLICKS = setOf(
            ClickType.DOUBLE_CLICK,
            ClickType.UNKNOWN,
        )
    }
}
