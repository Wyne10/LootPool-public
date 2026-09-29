package me.wyne.lootpool.gui.complex

import me.wyne.lootpool.api.Loot
import me.wyne.lootpool.api.LootPool
import me.wyne.lootpool.api.LootPoolProvider
import me.wyne.lootpool.api.complex.ComplexLootPool
import me.wyne.lootpool.api.complex.EmptyEntry
import me.wyne.lootpool.api.complex.EnchantmentPoolProvider
import me.wyne.lootpool.api.complex.ItemEntry
import me.wyne.lootpool.api.complex.LootEntry
import me.wyne.lootpool.api.complex.LootRoll
import me.wyne.lootpool.api.complex.PoolEntry
import me.wyne.lootpool.core.ComplexPoolEditor
import me.wyne.lootpool.core.withWeight
import me.wyne.lootpool.gui.ButtonScreen
import me.wyne.lootpool.gui.EditSession
import me.wyne.lootpool.gui.renderLoot
import me.wyne.lootpool.gui.wrapAmount
import me.wyne.wutils.common.kotlin.item.isNotNullOrAir
import me.wyne.wutils.common.kotlin.item.isNullOrAir
import me.wyne.wutils.i18n.kotlin.placeholderComponent
import me.wyne.wutils.i18n.kotlin.replace
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack

/** Everything the complex editor's screens need beyond the session they run in. */
class ComplexGuiContext(
    val editor: ComplexPoolEditor,
    val lootPools: LootPoolProvider,
    val enchantmentPools: EnchantmentPoolProvider
)

/** Top level of the complex editor: the pool's rolls, in the order they are produced. */
class ComplexPoolScreen(
    session: EditSession,
    private val ctx: ComplexGuiContext
) : ButtonScreen(session, ROWS, Component.text(ctx.editor.pool.key())) {

    override val contentCount: Int get() = ctx.editor.rolls.size
    override val isRoot: Boolean get() = true

    override fun renderContent() {
        ctx.editor.rolls.items.drop(page * contentSize)
            .take(contentSize)
            .forEachIndexed { slot, roll ->
                inventory.setItem(slot, renderRoll(index(slot), roll))
            }
    }

    private fun renderRoll(index: Int, roll: LootRoll): ItemStack =
        item(
            Material.CHEST, "gui-roll",
            "index" replace index,
            "min-rolls" replace roll.minRolls(),
            "max-rolls" replace roll.maxRolls()
        ).lore(
            "gui-roll-lore",
            "entries" replace roll.entries().size,
            "modifiers" replace roll.modifiers().size,
            "conditions" replace roll.conditions().size,
            "min-rolls" replace roll.minRolls(),
            "max-rolls" replace roll.maxRolls()
        )

    override fun renderControls() {
        super.renderControls()
        button(
            controlRow + 2,
            item(Material.REDSTONE_TORCH, "gui-pool-conditions", "count" replace ctx.editor.conditions.size).lore("gui-open-lore")
        ) {
            session.push(ConditionListScreen(session, ctx, ctx.editor.pool.key(), ctx.editor.conditions))
        }
        button(
            controlRow + 3,
            item(Material.BREWING_STAND, "gui-pool-modifiers", "count" replace ctx.editor.modifiers.size).lore("gui-open-lore")
        ) {
            session.push(ModifierListScreen(session, ctx, ctx.editor.pool.key(), ctx.editor.modifiers))
        }
        button(controlRow + 6, item(Material.LIME_DYE, "gui-add-roll").lore("gui-add-roll-lore")) {
            ctx.editor.rolls.add(LootRoll())
            session.markDirty()
            session.refresh()
        }
    }

    override fun onContentClick(event: InventoryClickEvent, index: Int) {
        event.isCancelled = true
        val rolls = ctx.editor.rolls
        val roll = rolls.getOrNull(index) ?: return

        if (event.click == ClickType.SWAP_OFFHAND) {
            session.push(RollScreen(session, ctx, index))
            return
        }

        when {
            event.action == InventoryAction.DROP_ONE_SLOT || event.action == InventoryAction.DROP_ALL_SLOT ->
                rolls.removeAt(index)
            event.isShiftClick && event.isLeftClick -> rolls.move(index, -1)
            event.isShiftClick && event.isRightClick -> rolls.move(index, 1)
            event.click == ClickType.NUMBER_KEY && event.hotbarButton == 0 -> rolls[index] = roll.withRolls(roll.minRolls(), roll.maxRolls() - session.step)
            event.click == ClickType.NUMBER_KEY && event.hotbarButton == 1 -> rolls[index] = roll.withRolls(roll.minRolls(), roll.maxRolls() + session.step)
            event.isLeftClick -> rolls[index] = roll.withRolls(roll.minRolls() - session.step, roll.maxRolls())
            event.isRightClick -> rolls[index] = roll.withRolls(roll.minRolls() + session.step, roll.maxRolls())
            else -> return
        }
        session.markDirty()
        session.refresh()
    }

    companion object {
        private const val ROWS = 6
    }
}

/** One roll's entries. Items dropped in become item entries, as in the basic editor. */
class RollScreen(
    session: EditSession,
    private val ctx: ComplexGuiContext,
    private val rollIndex: Int
) : ButtonScreen(session, ROWS, Component.text("${ctx.editor.pool.key()} / roll $rollIndex")) {

    private val entries get() = ctx.editor.entries(rollIndex)

    override val contentCount: Int get() = entries.size

    private val totalWeight: Double
        get() = entries.items.sumOf { it.weight().toDouble() }

    private fun percentage(weight: Int): Double {
        val total = totalWeight
        return if (total == 0.0) 0.0 else (weight / total) * 100
    }

    override fun renderContent() {
        entries.items.drop(page * contentSize)
            .take(contentSize)
            .forEachIndexed { slot, entry ->
                inventory.setItem(slot, renderEntry(entry))
            }
    }

    private fun renderEntry(entry: LootEntry): ItemStack = when (entry) {
        is ItemEntry -> renderLoot(
            entry.loot().item(), entry.loot().maxAmount(), entry.weight(),
            entry.loot().minAmount(), entry.loot().maxAmount(), percentage(entry.weight()), player
        )
        is PoolEntry -> withShare(item(Material.CHEST, "gui-entry-pool", "pool" replace entry.pool()), percentage(entry.weight()))
            .lore("gui-entry-lore", "weight" replace entry.weight())
        is EmptyEntry -> withShare(item(Material.GRAY_STAINED_GLASS_PANE, "gui-nothing-item"), percentage(entry.weight()))
            .lore("gui-entry-lore", "weight" replace entry.weight())
        else -> item(Material.BARRIER, "gui-entry-unknown")
    }

    /** Appends the entry's drop share to its name, the way loot entries already show theirs. */
    private fun withShare(stack: ItemStack, percentage: Double): ItemStack {
        stack.editMeta { meta ->
            val name = meta.displayName() ?: Component.empty()
            meta.displayName(
                Component.empty().decoration(TextDecoration.ITALIC, false)
                    .append(name)
                    .append(
                        Component.space()
                            .decorations(TextDecoration.entries.associateWith { TextDecoration.State.FALSE })
                            .append(Component.text("(${String.format("%.2f", percentage)})").color(NamedTextColor.AQUA))
                    )
            )
        }
        return stack
    }

    override fun renderControls() {
        super.renderControls()
        button(
            controlRow + 2,
            item(Material.REDSTONE_TORCH, "gui-roll-conditions", "count" replace ctx.editor.rollConditions(rollIndex).size).lore("gui-open-lore")
        ) {
            session.push(ConditionListScreen(session, ctx, "roll $rollIndex", ctx.editor.rollConditions(rollIndex)))
        }
        button(
            controlRow + 3,
            item(Material.BREWING_STAND, "gui-roll-modifiers", "count" replace ctx.editor.rollModifiers(rollIndex).size).lore("gui-open-lore")
        ) {
            session.push(ModifierListScreen(session, ctx, "roll $rollIndex", ctx.editor.rollModifiers(rollIndex)))
        }
        button(controlRow + 6, item(Material.LIME_DYE, "gui-add-entry").lore("gui-add-entry-lore")) { event ->
            if (event.isRightClick) {
                session.push(PoolPickerScreen(session, ctx) { key ->
                    entries.add(PoolEntry(key, 1))
                    session.markDirty()
                })
                return@button
            }
            entries.add(EmptyEntry(1))
            session.markDirty()
            session.refresh()
        }
    }

    override fun onContentClick(event: InventoryClickEvent, index: Int) {
        event.isCancelled = true

        if (event.currentItem.isNullOrAir()) {
            if (event.cursor.isNotNullOrAir()) {
                entries.add(ItemEntry(Loot(event.cursor!!.clone(), 1, event.cursor!!.amount, event.cursor!!.amount)))
                session.markDirty()
                session.refresh()
            }
            return
        }

        val entry = entries.getOrNull(index) ?: return

        if (event.click == ClickType.SWAP_OFFHAND) {
            session.push(EntryScreen(session, ctx, rollIndex, index))
            return
        }

        when {
            event.action == InventoryAction.DROP_ONE_SLOT || event.action == InventoryAction.DROP_ALL_SLOT ->
                entries.removeAt(index)
            event.isShiftClick && event.isLeftClick && entry is ItemEntry -> entries[index] = entry.withAmounts(entry.loot().minAmount() - session.step, entry.loot().maxAmount())
            event.isShiftClick && event.isRightClick && entry is ItemEntry -> entries[index] = entry.withAmounts(entry.loot().minAmount() + session.step, entry.loot().maxAmount())
            event.isShiftClick && event.isLeftClick -> entries.move(index, -1)
            event.isShiftClick && event.isRightClick -> entries.move(index, 1)
            event.click == ClickType.NUMBER_KEY && event.hotbarButton == 0 && entry is ItemEntry -> entries[index] = entry.withAmounts(entry.loot().minAmount(), entry.loot().maxAmount() - session.step)
            event.click == ClickType.NUMBER_KEY && event.hotbarButton == 1 && entry is ItemEntry -> entries[index] = entry.withAmounts(entry.loot().minAmount(), entry.loot().maxAmount() + session.step)
            event.isLeftClick -> entries[index] = withWeight(entry, (entry.weight() - session.step).coerceAtLeast(0))
            event.isRightClick -> entries[index] = withWeight(entry, entry.weight() + session.step)
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
        val item = event.currentItem!!.clone()
        entries.add(ItemEntry(Loot(item, 1, item.amount, item.amount)))
        session.markDirty()
        session.refresh()
    }

    companion object {
        private const val ROWS = 6
    }
}

/** A single entry's own modifiers and conditions, plus whatever is type-specific about it. */
class EntryScreen(
    session: EditSession,
    private val ctx: ComplexGuiContext,
    private val rollIndex: Int,
    private val entryIndex: Int
) : ButtonScreen(session, ROWS, Component.text("entry $rollIndex.$entryIndex")) {

    private val entries get() = ctx.editor.entries(rollIndex)

    override fun renderContent() {
        val entry = entries.getOrNull(entryIndex) ?: return

        button(
            11,
            item(
                Material.BREWING_STAND, "gui-entry-modifiers",
                "count" replace (ctx.editor.entryModifiers(rollIndex, entryIndex)?.size ?: 0)
            ).lore("gui-open-lore")
        ) {
            val modifiers = ctx.editor.entryModifiers(rollIndex, entryIndex)
            if (modifiers == null) {
                player.placeholderComponent("gui-entry-no-modifiers").sendMessage(player)
                return@button
            }
            session.push(ModifierListScreen(session, ctx, "entry $rollIndex.$entryIndex", modifiers))
        }

        inventory.setItem(13, describeEntry(entry))

        button(
            15,
            item(
                Material.REDSTONE_TORCH, "gui-entry-conditions",
                "count" replace ctx.editor.entryConditions(rollIndex, entryIndex).size
            ).lore("gui-open-lore")
        ) {
            session.push(ConditionListScreen(session, ctx, "entry $rollIndex.$entryIndex", ctx.editor.entryConditions(rollIndex, entryIndex)))
        }

        if (entry is PoolEntry) {
            button(4, item(Material.CHEST, "gui-entry-repoint", "pool" replace entry.pool()).lore("gui-open-lore")) {
                session.push(PoolPickerScreen(session, ctx) { key ->
                    entries[entryIndex] = PoolEntry(key, entry.weight(), entry.conditions(), entry.modifiers())
                    session.markDirty()
                })
            }
        }
    }

    private fun describeEntry(entry: LootEntry): ItemStack = when (entry) {
        is ItemEntry -> renderLoot(
            entry.loot().item(), entry.loot().maxAmount(), entry.weight(),
            entry.loot().minAmount(), entry.loot().maxAmount(), 0.0, player
        )
        is PoolEntry -> item(Material.CHEST, "gui-entry-pool", "pool" replace entry.pool())
        is EmptyEntry -> item(Material.GRAY_STAINED_GLASS_PANE, "gui-nothing-item")
        else -> item(Material.BARRIER, "gui-entry-unknown")
    }

    companion object {
        private const val ROWS = 3
    }
}

/** Picks one of the registered loot pools, for pool entries. */
class PoolPickerScreen(
    session: EditSession,
    private val ctx: ComplexGuiContext,
    private val onPick: (String) -> Unit
) : ButtonScreen(session, ROWS, Component.text("Select a loot pool")) {

    private val keys: List<String> = ctx.lootPools.lootPoolMap.keys.sorted()

    override val contentCount: Int get() = keys.size

    override fun renderContent() {
        keys.drop(page * contentSize)
            .take(contentSize)
            .forEachIndexed { slot, key ->
                val pool: LootPool? = ctx.lootPools.getLootPool(key)
                inventory.setItem(
                    slot,
                    item(
                        if (pool is ComplexLootPool) Material.ENDER_CHEST else Material.CHEST,
                        "gui-pool-option",
                        "pool" replace key,
                        "type" replace (pool?.javaClass?.simpleName ?: "?")
                    ).lore("gui-select-lore")
                )
            }
    }

    override fun onContentClick(event: InventoryClickEvent, index: Int) {
        event.isCancelled = true
        val key = keys.getOrNull(index) ?: return
        onPick(key)
        session.pop()
    }

    companion object {
        private const val ROWS = 6
    }
}

private fun LootRoll.withRolls(minRolls: Int, maxRolls: Int): LootRoll {
    val min = minRolls.coerceAtLeast(0)
    val max = maxRolls.coerceAtLeast(min)
    return LootRoll(min, max, entries(), modifiers(), conditions())
}

private fun ItemEntry.withAmounts(minAmount: Int, maxAmount: Int): ItemEntry {
    val stackSize = loot().item().maxStackSize
    val min = wrapAmount(minAmount, stackSize)
    val max = wrapAmount(maxAmount, stackSize).coerceAtLeast(min)
    return ItemEntry(Loot(loot().item(), loot().weight(), min, max), conditions(), modifiers())
}

/** Opens the complex loot pool editor, saving once when the player closes it. */
fun openComplexPoolGui(
    player: Player,
    pool: ComplexLootPool,
    lootPools: LootPoolProvider,
    enchantmentPools: EnchantmentPoolProvider
) {
    val session = EditSession(player)
    val editor = ComplexPoolEditor(pool) { }
    val ctx = ComplexGuiContext(editor, lootPools, enchantmentPools)
    session.commitWith {
        if (session.dirty) {
            lootPools.writeLootPool(editor.pool)
            player.placeholderComponent("success-lootpool-modify", "key" replace editor.pool.key()).sendMessage(player)
        }
    }
    session.push(ComplexPoolScreen(session, ctx))
}
