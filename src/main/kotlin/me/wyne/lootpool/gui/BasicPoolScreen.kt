package me.wyne.lootpool.gui

import me.wyne.lootpool.api.BasicLootPool
import me.wyne.lootpool.api.EditableLootPool
import me.wyne.lootpool.api.Loot
import me.wyne.lootpool.core.LootPoolManager
import me.wyne.wutils.common.kotlin.item.isNotNullOrAir
import me.wyne.wutils.common.kotlin.item.isNullOrAir
import me.wyne.wutils.i18n.kotlin.placeholderComponent
import me.wyne.wutils.i18n.kotlin.replace
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack
import java.util.LinkedList

/**
 * The basic loot pool editor: every content slot is one loot entry, edited in place by click
 * gestures. The "nothing" entry - the pool's chance of dropping no item at all - lives on the
 * control row rather than in slot 0, so content slots need no special cases.
 */
class BasicPoolScreen(
    session: EditSession,
    private val key: String,
    private val source: EditableLootPool?,
    private val pending: Boolean
) : ButtonScreen(session, ROWS, Component.text(key)) {

    private val drafts = mutableListOf<MutableLoot>()
    private var nothingWeight = 0
    private var nothingMin = 1
    private var nothingMax = 1

    override val contentCount: Int get() = drafts.size
    override val isRoot: Boolean get() = true

    init {
        source?.lootList
            ?.filter { it.item.isNotNullOrAir() }
            ?.forEach { drafts.add(it.asMutable()) }
        source?.lootList
            ?.firstOrNull { it.item.type == Material.AIR }
            ?.let {
                nothingWeight = it.weight
                nothingMin = it.minAmount
                nothingMax = it.maxAmount
            }
    }

    private val totalWeight: Double
        get() = drafts.sumOf { it.weight.toDouble() } + nothingWeight

    private fun percentage(weight: Int): Double {
        val total = totalWeight
        return if (total == 0.0) 0.0 else (weight / total) * 100
    }

    /** The "no drop" entry, named with its share the same way real loot entries are. */
    private fun nothingItem(): ItemStack {
        val stack = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        stack.editMeta { meta ->
            meta.displayName(
                Component.empty().decoration(TextDecoration.ITALIC, false)
                    .append(player.placeholderComponent("gui-nothing-item").get())
                    .append(
                        Component.space()
                            .decorations(TextDecoration.entries.associateWith { TextDecoration.State.FALSE })
                            .append(Component.text("(${String.format("%.2f", percentage(nothingWeight))})").color(NamedTextColor.AQUA))
                    )
            )
        }
        return stack
    }

    override fun renderContent() {
        drafts.drop(page * contentSize)
            .take(contentSize)
            .forEachIndexed { slot, loot ->
                inventory.setItem(slot, loot.render(percentage(loot.weight), player))
            }
    }

    override val previousPageSlot: Int get() = 0
    override val nextPageSlot: Int get() = 1
    override val stepSlot: Int get() = 7

    override fun renderControls() {
        super.renderControls()
        button(
            controlRow + 8,
            nothingItem()
                .lore("gui-loot", "weight" replace nothingWeight, "min-amount" replace nothingMin, "max-amount" replace nothingMax)
        ) { event ->
            when {
                event.click == ClickType.NUMBER_KEY && event.hotbarButton == 0 -> nothingMax = wrapAmount(nothingMax - session.step, 64)
                event.click == ClickType.NUMBER_KEY && event.hotbarButton == 1 -> nothingMax = wrapAmount(nothingMax + session.step, 64)
                event.isShiftClick && event.isLeftClick -> nothingMin = wrapAmount(nothingMin - session.step, 64)
                event.isShiftClick && event.isRightClick -> nothingMin = wrapAmount(nothingMin + session.step, 64)
                event.isLeftClick -> nothingWeight = (nothingWeight - session.step).coerceAtLeast(0)
                event.isRightClick -> nothingWeight += session.step
                else -> return@button
            }
            nothingMin = nothingMin.coerceAtMost(nothingMax)
            session.markDirty()
            session.refresh()
        }
    }

    override fun onContentClick(event: InventoryClickEvent, index: Int) {
        event.isCancelled = true

        if (event.currentItem.isNullOrAir()) {
            if (event.cursor.isNotNullOrAir()) {
                drafts.add(MutableLoot(event.cursor!!.clone()))
                session.markDirty()
                session.refresh()
            }
            return
        }

        val loot = drafts.getOrNull(index) ?: return

        if (event.cursor.isNotNullOrAir()) {
            val previous = loot.item.clone()
            loot.item = event.cursor!!.clone()
            session.markDirty()
            session.later {
                event.view.cursor = previous
                render()
            }
            return
        }

        when {
            event.action == InventoryAction.DROP_ONE_SLOT || event.action == InventoryAction.DROP_ALL_SLOT ->
                drafts.removeAt(index)
            event.click == ClickType.NUMBER_KEY && event.hotbarButton == 0 -> loot.maxAmount -= session.step
            event.click == ClickType.NUMBER_KEY && event.hotbarButton == 1 -> loot.maxAmount += session.step
            event.isShiftClick && event.isLeftClick -> loot.minAmount -= session.step
            event.isShiftClick && event.isRightClick -> loot.minAmount += session.step
            event.isLeftClick -> loot.weight -= session.step
            event.isRightClick -> loot.weight += session.step
            else -> return
        }
        session.markDirty()
        session.refresh()
    }

    override fun onForeignClick(event: InventoryClickEvent) {
        if (event.slotType == InventoryType.SlotType.OUTSIDE) {
            super.onForeignClick(event)
            return
        }
        if (event.clickedInventory != player.inventory) return
        if (!event.isShiftClick || event.currentItem.isNullOrAir()) return
        event.isCancelled = true
        drafts.add(MutableLoot(event.currentItem!!.clone()))
        session.markDirty()
        session.refresh()
    }

    /**
     * Writes only when there is something to write: an edit the player made, or a pool a command
     * already transformed and handed over unsaved (merge, weight, amount, flatten). Opening an
     * existing pool and closing it untouched changes nothing.
     */
    fun commit() {
        if (!session.dirty && !pending) return
        val lootList = LinkedList(drafts.map { it.asImmutable() })
        if (nothingWeight > 0)
            lootList.addFirst(Loot(ItemStack(Material.AIR), nothingWeight, nothingMin, nothingMax))
        LootPoolManager.instance.writeLootPool(source?.withLoot(key, lootList) ?: BasicLootPool(key, lootList))
        val message = if (source == null) "success-lootpool-create" else "success-lootpool-modify"
        player.placeholderComponent(message, "key" replace key).sendMessage(player)
    }

    companion object {
        private const val ROWS = 6
    }
}
