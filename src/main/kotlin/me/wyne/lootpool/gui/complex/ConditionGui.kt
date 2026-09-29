package me.wyne.lootpool.gui.complex

import me.wyne.lootpool.api.complex.condition.AllOfCondition
import me.wyne.lootpool.api.complex.condition.AnyOfCondition
import me.wyne.lootpool.api.complex.condition.InvertedCondition
import me.wyne.lootpool.api.complex.condition.LootCondition
import me.wyne.lootpool.api.complex.condition.PermissionCondition
import me.wyne.lootpool.api.complex.condition.RandomChanceCondition
import me.wyne.lootpool.api.complex.condition.TimeCondition
import me.wyne.lootpool.api.complex.condition.WeatherCondition
import me.wyne.lootpool.api.complex.condition.WorldCondition
import me.wyne.lootpool.condition.PlaceholderCondition
import me.wyne.lootpool.core.ListHandle
import me.wyne.lootpool.core.describe
import me.wyne.lootpool.gui.ButtonScreen
import me.wyne.lootpool.gui.EditSession
import me.wyne.lootpool.gui.FieldScreen
import me.wyne.lootpool.gui.actionField
import me.wyne.lootpool.gui.cycleField
import me.wyne.lootpool.gui.fractionField
import me.wyne.lootpool.gui.intField
import me.wyne.lootpool.gui.textField
import me.wyne.lootpool.gui.tristateField
import me.wyne.wutils.i18n.kotlin.replace
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

/** The conditions attached to one owner - the pool, a roll, an entry, a modifier or a group. */
class ConditionListScreen(
    session: EditSession,
    private val ctx: ComplexGuiContext,
    private val ownerLabel: String,
    private val conditions: ListHandle<LootCondition>
) : ButtonScreen(session, ROWS, Component.text("$ownerLabel / conditions")) {

    override val contentCount: Int get() = conditions.size

    override fun renderContent() {
        conditions.items.drop(page * contentSize)
            .take(contentSize)
            .forEachIndexed { slot, condition ->
                inventory.setItem(slot, render(condition))
            }
    }

    private fun render(condition: LootCondition): ItemStack {
        val inner = unwrapCondition(condition)
        val inverted = condition is InvertedCondition
        return item(materialOf(inner), "gui-condition", "description" replace describe(condition))
            .lore(if (inverted) "gui-condition-inverted-lore" else "gui-condition-lore")
    }

    private fun materialOf(condition: LootCondition): Material = when (condition) {
        is RandomChanceCondition -> Material.SUNFLOWER
        is PermissionCondition -> Material.NAME_TAG
        is WorldCondition -> Material.GRASS_BLOCK
        is TimeCondition -> Material.CLOCK
        is WeatherCondition -> Material.WATER_BUCKET
        is AllOfCondition, is AnyOfCondition -> Material.CHEST
        is PlaceholderCondition -> Material.PAPER
        else -> Material.REDSTONE_TORCH
    }

    override fun renderControls() {
        super.renderControls()
        button(controlRow + 3, item(Material.LIME_DYE, "gui-add-condition").lore("gui-add-condition-lore")) {
            session.push(ConditionTypeScreen(session, ownerLabel, conditions))
        }
    }

    override fun onContentClick(event: InventoryClickEvent, index: Int) {
        event.isCancelled = true
        val condition = conditions.getOrNull(index) ?: return
        val inner = unwrapCondition(condition)

        when {
            event.action == InventoryAction.DROP_ONE_SLOT || event.action == InventoryAction.DROP_ALL_SLOT -> {
                conditions.removeAt(index); session.markDirty(); session.refresh()
            }

            event.click == ClickType.SWAP_OFFHAND -> {
                conditions[index] = if (condition is InvertedCondition) condition.condition()
                else InvertedCondition(condition)
                session.markDirty(); session.refresh()
            }
            event.isShiftClick && event.isLeftClick -> {
                conditions.move(index, -1); session.markDirty(); session.refresh()
            }
            event.isShiftClick && event.isRightClick -> {
                conditions.move(index, 1); session.markDirty(); session.refresh()
            }
            inner is AllOfCondition -> session.push(
                ConditionListScreen(
                    session, ctx, "$ownerLabel / all of",
                    conditions.nested(index, { (unwrapCondition(it) as AllOfCondition).conditions() }) { c, list ->
                        rewrapCondition(c, AllOfCondition(list))
                    }
                )
            )
            inner is AnyOfCondition -> session.push(
                ConditionListScreen(
                    session, ctx, "$ownerLabel / any of",
                    conditions.nested(index, { (unwrapCondition(it) as AnyOfCondition).conditions() }) { c, list ->
                        rewrapCondition(c, AnyOfCondition(list))
                    }
                )
            )
            event.isLeftClick -> conditionDetailScreen(session, conditions, index)?.let { session.push(it) }
        }
    }

    companion object {
        private const val ROWS = 6
    }
}

/** Picks which kind of condition to add. */
class ConditionTypeScreen(
    session: EditSession,
    ownerLabel: String,
    private val conditions: ListHandle<LootCondition>
) : FieldScreen(session, ROWS, Component.text("$ownerLabel / add condition"), { emptyList() }) {

    override fun renderContent() {
        button(10, item(Material.SUNFLOWER, "gui-condition-type-chance").lore("gui-condition-type-chance-lore")) { add(RandomChanceCondition(1.0)) }
        button(11, item(Material.NAME_TAG, "gui-condition-type-permission").lore("gui-condition-type-permission-lore")) { add(PermissionCondition("")) }
        button(12, item(Material.GRASS_BLOCK, "gui-condition-type-world").lore("gui-condition-type-world-lore")) { add(WorldCondition(emptySet())) }
        button(13, item(Material.CLOCK, "gui-condition-type-time").lore("gui-condition-type-time-lore")) { add(TimeCondition(0, 24000)) }
        button(14, item(Material.WATER_BUCKET, "gui-condition-type-weather").lore("gui-condition-type-weather-lore")) { add(WeatherCondition(null, null)) }
        button(15, item(Material.CHEST, "gui-condition-type-allof").lore("gui-condition-type-allof-lore")) { add(AllOfCondition(emptyList())) }
        button(16, item(Material.ENDER_CHEST, "gui-condition-type-anyof").lore("gui-condition-type-anyof-lore")) { add(AnyOfCondition(emptyList())) }
    }

    private fun add(condition: LootCondition) {
        conditions.add(condition)
        session.markDirty()
        session.pop()
    }

    companion object {
        private const val ROWS = 3
    }
}

/** Multi-select over the server's worlds, for world conditions. */
class WorldPickerScreen(
    session: EditSession,
    private val conditions: ListHandle<LootCondition>,
    private val index: Int
) : ButtonScreen(session, ROWS, Component.text("worlds")) {

    private val worlds: List<String> = Bukkit.getWorlds().map { it.name }

    override val contentCount: Int get() = worlds.size

    private fun condition() = unwrapCondition(conditions[index]) as WorldCondition

    override fun renderContent() {
        val selected = condition().worlds()
        worlds.drop(page * contentSize)
            .take(contentSize)
            .forEachIndexed { slot, world ->
                val item = item(
                    if (world in selected) Material.GRASS_BLOCK else Material.DIRT,
                    "gui-world-option", "world" replace world
                ).lore(if (world in selected) "gui-world-selected-lore" else "gui-world-unselected-lore")
                inventory.setItem(slot, item)
            }
    }

    override fun onContentClick(event: InventoryClickEvent, index: Int) {
        event.isCancelled = true
        val world = worlds.getOrNull(index) ?: return
        val selected = condition().worlds().toMutableSet()
        if (!selected.add(world)) selected.remove(world)
        conditions[this.index] = rewrapCondition(conditions[this.index], WorldCondition(selected))
        session.markDirty()
        session.refresh()
    }

    companion object {
        private const val ROWS = 6
    }
}

/** Builds the detail screen for the condition at [index], preserving an inverted wrapper. */
fun conditionDetailScreen(
    session: EditSession,
    conditions: ListHandle<LootCondition>,
    index: Int
): FieldScreen? {
    fun current(): LootCondition = unwrapCondition(conditions[index])
    fun replace(condition: LootCondition) {
        conditions[index] = rewrapCondition(conditions[index], condition)
    }

    val title = Component.text(conditions[index].javaClass.simpleName)

    return when (current()) {
        is RandomChanceCondition -> FieldScreen(session, 3, title) {
            val it = current() as RandomChanceCondition
            listOf(fractionField(13, Material.SUNFLOWER, "gui-field-chance", { it.chance() }) { v -> replace(RandomChanceCondition(v)) })
        }

        is PermissionCondition -> FieldScreen(session, 3, title) {
            val it = current() as PermissionCondition
            listOf(
                textField(13, Material.NAME_TAG, "gui-field-permission", { it.node().ifEmpty { "-" } }) {
                    "/lootpool complex condition set permission "
                }
            )
        }

        is WorldCondition -> FieldScreen(session, 3, title) {
            val it = current() as WorldCondition
            listOf(
                actionField(13, Material.GRASS_BLOCK, "gui-field-worlds", { it.worlds().joinToString(", ").ifEmpty { "-" } }) { s ->
                    s.push(WorldPickerScreen(s, conditions, index))
                }
            )
        }

        is TimeCondition -> FieldScreen(session, 3, title) {
            val it = current() as TimeCondition
            listOf(
                intField(12, Material.CLOCK, "gui-field-min-time", 0, { it.minTime().toInt() }) { v ->
                    replace(TimeCondition(v.coerceIn(0, 24000).toLong(), it.maxTime()))
                },
                intField(14, Material.CLOCK, "gui-field-max-time", 0, { it.maxTime().toInt() }) { v ->
                    replace(TimeCondition(it.minTime(), v.coerceIn(0, 24000).toLong()))
                }
            )
        }

        is WeatherCondition -> FieldScreen(session, 3, title) {
            val it = current() as WeatherCondition
            listOf(
                tristateField(12, "gui-field-raining", { it.raining() }) { v -> replace(WeatherCondition(v, it.thundering())) },
                tristateField(14, "gui-field-thundering", { it.thundering() }) { v -> replace(WeatherCondition(it.raining(), v)) }
            )
        }

        is PlaceholderCondition -> FieldScreen(session, 3, title) {
            val it = current() as PlaceholderCondition
            listOf(
                textField(11, Material.PAPER, "gui-field-placeholder", { it.placeholder.ifEmpty { "-" } }) {
                    "/lootpool complex condition set placeholder "
                },
                cycleField(
                    13, Material.COMPARATOR, "gui-field-placeholder-operator",
                    PlaceholderCondition.Operator.entries.toList(), { it.operator },
                    { v -> replace(PlaceholderCondition(it.placeholder, v, it.value)) },
                    { operator -> operator.argument }
                ),
                textField(15, Material.PAPER, "gui-field-placeholder-value", { it.value.ifEmpty { "-" } }) {
                    "/lootpool complex condition set placeholder "
                }
            )
        }

        else -> null
    }
}

internal fun unwrapCondition(condition: LootCondition): LootCondition =
    if (condition is InvertedCondition) condition.condition() else condition

internal fun rewrapCondition(original: LootCondition, replacement: LootCondition): LootCondition =
    if (original is InvertedCondition) InvertedCondition(replacement) else replacement
