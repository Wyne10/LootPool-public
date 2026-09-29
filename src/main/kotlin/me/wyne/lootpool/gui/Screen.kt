package me.wyne.lootpool.gui

import me.wyne.wutils.i18n.kotlin.placeholderComponent
import me.wyne.wutils.i18n.kotlin.placeholderComponents
import me.wyne.wutils.i18n.kotlin.replace
import me.wyne.wutils.i18n.language.replacement.TextReplacement
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

interface Screen {
    val inventory: Inventory
    fun render()
    fun onClick(event: InventoryClickEvent)
}

class Button(val item: ItemStack, val onClick: (InventoryClickEvent) -> Unit)

abstract class ButtonScreen(
    protected val session: EditSession,
    rows: Int,
    title: Component
) : Screen {

    protected val player: Player = session.player
    override val inventory: Inventory = Bukkit.createInventory(player, rows * 9, title)

    protected val controlRow: Int = (rows - 1) * 9
    protected val contentSize: Int = controlRow

    private val buttons = mutableMapOf<Int, Button>()
    protected var page = 0

    protected open val contentCount: Int get() = 0
    protected open val isRoot: Boolean get() = false

    protected fun button(slot: Int, item: ItemStack, onClick: (InventoryClickEvent) -> Unit) {
        buttons[slot] = Button(item, onClick)
    }

    protected fun index(slot: Int) = page * contentSize + slot

    protected val pageCount: Int
        get() = maxOf(1, (contentCount + contentSize - 1) / contentSize)

    final override fun render() {
        buttons.clear()
        for (slot in 0 until inventory.size)
            inventory.setItem(slot, null)
        page = page.coerceIn(0, pageCount - 1)
        renderControls()
        renderContent()
        buttons.forEach { (slot, button) -> inventory.setItem(slot, button.item) }
    }

    final override fun onClick(event: InventoryClickEvent) {
        if (event.clickedInventory != inventory) {
            onForeignClick(event)
            return
        }
        val button = buttons[event.slot]
        if (button != null) {
            event.isCancelled = true
            button.onClick(event)
            return
        }
        if (event.slot >= contentSize) {
            event.isCancelled = true
            return
        }
        onContentClick(event, index(event.slot))
    }

    protected abstract fun renderContent()

    protected open fun onContentClick(event: InventoryClickEvent, index: Int) {
        event.isCancelled = true
    }

    /** Clicks landing outside this inventory: in the player's own inventory, or off-window entirely. */
    protected open fun onForeignClick(event: InventoryClickEvent) {
        if (event.slotType != InventoryType.SlotType.OUTSIDE) return
        when (event.click) {
            ClickType.LEFT -> turnPage(-1)
            ClickType.RIGHT -> turnPage(1)
            else -> return
        }
    }

    protected fun turnPage(delta: Int) {
        val next = (page + delta).coerceIn(0, pageCount - 1)
        if (next == page) return
        page = next
        session.refresh()
    }

    /** Where the control row's fixed buttons sit; screens move them out of each other's way. */
    protected open val previousPageSlot: Int get() = 0
    protected open val nextPageSlot: Int get() = 1
    protected open val pageIndicatorSlot: Int? get() = null
    protected open val stepSlot: Int get() = 5

    protected open fun renderControls() {
        if (pageCount > 1) {
            val pageNumber = "page" replace (page + 1)
            val pageTotal = "pages" replace pageCount
            button(controlRow + previousPageSlot, item(Material.ARROW, "gui-page-previous").lore("gui-page-lore", pageNumber, pageTotal)) { turnPage(-1) }
            button(controlRow + nextPageSlot, item(Material.ARROW, "gui-page-next").lore("gui-page-lore", pageNumber, pageTotal)) { turnPage(1) }
            pageIndicatorSlot?.let { slot ->
                button(controlRow + slot, item(Material.PAPER, "gui-page", pageNumber, pageTotal)) { }
            }
        }
        if (isRoot)
            button(controlRow + 4, item(Material.BARRIER, "gui-close").lore("gui-close-lore")) { event ->
                session.close(save = !event.isRightClick)
            }
        else
            button(controlRow + 4, item(Material.OAK_DOOR, "gui-back").lore("gui-back-lore")) { session.pop() }
        button(controlRow + stepSlot, item(Material.COMPARATOR, "gui-step", "step" replace session.step).lore("gui-step-lore")) {
            session.cycleStep()
            session.refresh()
        }
    }

    protected fun item(material: Material, nameKey: String, vararg replacements: TextReplacement): ItemStack =
        ItemStack(material).also { stack ->
            stack.editMeta { meta ->
                meta.displayName(
                    player.placeholderComponent(nameKey, *replacements).get()
                        .decoration(TextDecoration.ITALIC, false)
                )
            }
        }

    /** Appends the lines of a list-valued language key as lore. */
    protected fun ItemStack.lore(loreKey: String, vararg replacements: TextReplacement): ItemStack = also { stack ->
        val lines = player.placeholderComponents(loreKey, *replacements).map { it.get() }
        if (lines.isEmpty()) return@also
        stack.editMeta { meta ->
            val lore = (meta.lore() ?: emptyList()).toMutableList()
            lines.forEach { lore.add(it.decoration(TextDecoration.ITALIC, false)) }
            meta.lore(lore)
        }
    }

    /** Appends already-built components as lore. */
    protected fun ItemStack.lore(vararg lines: Component): ItemStack = also { stack ->
        if (lines.isEmpty()) return@also
        stack.editMeta { meta ->
            val lore = (meta.lore() ?: emptyList()).toMutableList()
            lines.forEach { lore.add(it.decoration(TextDecoration.ITALIC, false)) }
            meta.lore(lore)
        }
    }
}
