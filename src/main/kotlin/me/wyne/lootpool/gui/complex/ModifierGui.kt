package me.wyne.lootpool.gui.complex

import me.wyne.lootpool.api.complex.modifier.AttributeEntry
import me.wyne.lootpool.api.complex.modifier.ConditionalModifier
import me.wyne.lootpool.api.complex.modifier.DamageModifier
import me.wyne.lootpool.api.complex.modifier.EnchantModifier
import me.wyne.lootpool.api.complex.modifier.LootModifier
import me.wyne.lootpool.api.complex.modifier.LoreModifier
import me.wyne.lootpool.api.complex.modifier.NameModifier
import me.wyne.lootpool.api.complex.modifier.SetAttributesModifier
import me.wyne.lootpool.core.ListHandle
import me.wyne.lootpool.core.describe
import me.wyne.lootpool.core.rewrap
import me.wyne.lootpool.core.unwrap
import me.wyne.lootpool.gui.ButtonScreen
import me.wyne.lootpool.gui.EditSession
import me.wyne.lootpool.gui.FieldScreen
import me.wyne.lootpool.gui.actionField
import me.wyne.lootpool.gui.boolField
import me.wyne.lootpool.gui.cycleField
import me.wyne.lootpool.gui.doubleField
import me.wyne.lootpool.gui.fractionField
import me.wyne.lootpool.gui.intField
import me.wyne.lootpool.gui.textField
import me.wyne.wutils.i18n.kotlin.replace
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

/** The item modifiers attached to one owner - the pool, a roll or an entry. */
class ModifierListScreen(
    session: EditSession,
    private val ctx: ComplexGuiContext,
    private val ownerLabel: String,
    private val modifiers: ListHandle<LootModifier>
) : ButtonScreen(session, ROWS, Component.text("$ownerLabel / modifiers")) {

    override val contentCount: Int get() = modifiers.size

    override fun renderContent() {
        modifiers.items.drop(page * contentSize)
            .take(contentSize)
            .forEachIndexed { slot, modifier ->
                inventory.setItem(slot, render(modifier))
            }
    }

    private fun render(modifier: LootModifier): ItemStack {
        val inner = unwrap(modifier)
        val conditional = modifier is ConditionalModifier
        return item(materialOf(inner), "gui-modifier", "description" replace describe(modifier))
            .lore(if (conditional) "gui-modifier-conditional-lore" else "gui-modifier-lore")
    }

    private fun materialOf(modifier: LootModifier): Material = when (modifier) {
        is EnchantModifier -> Material.ENCHANTED_BOOK
        is DamageModifier -> Material.IRON_SWORD
        is SetAttributesModifier -> Material.IRON_CHESTPLATE
        is NameModifier -> Material.NAME_TAG
        is LoreModifier -> Material.WRITABLE_BOOK
        else -> Material.PAPER
    }

    override fun renderControls() {
        super.renderControls()
        button(controlRow + 3, item(Material.LIME_DYE, "gui-add-modifier").lore("gui-add-modifier-lore")) {
            session.push(ModifierTypeScreen(session, ctx, ownerLabel, modifiers))
        }
    }

    override fun onContentClick(event: InventoryClickEvent, index: Int) {
        event.isCancelled = true
        val modifier = modifiers.getOrNull(index) ?: return

        when {
            event.action == InventoryAction.DROP_ONE_SLOT || event.action == InventoryAction.DROP_ALL_SLOT -> {
                modifiers.removeAt(index)
                session.markDirty()
                session.refresh()
            }

            event.click == ClickType.SWAP_OFFHAND -> {
                modifiers[index] = if (modifier is ConditionalModifier) modifier.modifier()
                else ConditionalModifier(modifier, emptyList())
                session.markDirty()
                session.refresh()
            }
            event.isShiftClick && event.isLeftClick -> {
                modifiers.move(index, -1); session.markDirty(); session.refresh()
            }
            event.isShiftClick && event.isRightClick -> {
                modifiers.move(index, 1); session.markDirty(); session.refresh()
            }
            event.isRightClick && modifier is ConditionalModifier -> {
                session.push(
                    ConditionListScreen(
                        session, ctx, "$ownerLabel / modifier $index",
                        modifiers.nested(index, { (it as ConditionalModifier).conditions() }) { m, list ->
                            ConditionalModifier((m as ConditionalModifier).modifier(), list)
                        }
                    )
                )
            }
            event.isLeftClick -> openDetail(index)
        }
    }

    private fun openDetail(index: Int) {
        val screen = modifierDetailScreen(session, ctx, modifiers, index) ?: return
        session.push(screen)
    }

    companion object {
        private const val ROWS = 6
    }
}

/** Picks which kind of modifier to add. */
class ModifierTypeScreen(
    session: EditSession,
    private val ctx: ComplexGuiContext,
    ownerLabel: String,
    private val modifiers: ListHandle<LootModifier>
) : FieldScreen(session, ROWS, Component.text("$ownerLabel / add modifier"), { emptyList() }) {

    override fun renderContent() {
        button(11, item(Material.ENCHANTED_BOOK, "gui-modifier-type-enchant").lore("gui-modifier-type-enchant-lore")) {
            session.push(EnchantmentPoolPickerScreen(session, ctx) { key ->
                modifiers.add(EnchantModifier(key, 1, 1, true))
                session.markDirty()
            })
        }
        button(12, item(Material.IRON_SWORD, "gui-modifier-type-damage").lore("gui-modifier-type-damage-lore")) { add(DamageModifier(0.0, 0.0)) }
        button(13, item(Material.IRON_CHESTPLATE, "gui-modifier-type-attributes").lore("gui-modifier-type-attributes-lore")) { add(SetAttributesModifier(emptyList(), false)) }
        button(14, item(Material.NAME_TAG, "gui-modifier-type-name").lore("gui-modifier-type-name-lore")) { add(NameModifier("")) }
        button(15, item(Material.WRITABLE_BOOK, "gui-modifier-type-lore").lore("gui-modifier-type-lore-lore")) { add(LoreModifier(emptyList(), LoreModifier.Mode.APPEND)) }
    }

    private fun add(modifier: LootModifier) {
        modifiers.add(modifier)
        session.markDirty()
        session.pop()
    }

    companion object {
        private const val ROWS = 3
    }
}

/** Picks one of the registered enchantment pools. */
class EnchantmentPoolPickerScreen(
    session: EditSession,
    private val ctx: ComplexGuiContext,
    private val onPick: (String) -> Unit
) : ButtonScreen(session, ROWS, Component.text("Select an enchantment pool")) {

    private val keys: List<String> = ctx.enchantmentPools.enchantmentPoolMap.keys.sorted()

    override val contentCount: Int get() = keys.size

    override fun renderContent() {
        keys.drop(page * contentSize)
            .take(contentSize)
            .forEachIndexed { slot, key ->
                val size = ctx.enchantmentPools.getEnchantmentPool(key)?.entries()?.size ?: 0
                inventory.setItem(
                    slot,
                    item(Material.ENCHANTED_BOOK, "gui-enchantment-pool-option", "pool" replace key)
                        .lore("gui-enchantment-pool-option-lore", "entries" replace size)
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

/**
 * Builds the detail screen for whatever kind of modifier sits at [index], editing through the
 * handle so a conditional wrapper is preserved.
 */
fun modifierDetailScreen(
    session: EditSession,
    ctx: ComplexGuiContext,
    modifiers: ListHandle<LootModifier>,
    index: Int
): FieldScreen? {
    fun current(): LootModifier = unwrap(modifiers[index])
    fun replace(modifier: LootModifier) {
        modifiers[index] = rewrap(modifiers[index], modifier)
    }

    val title = Component.text(describe(unwrap(modifiers[index])))

    return when (current()) {
        is EnchantModifier -> FieldScreen(session, 3, title) {
            val it = current() as EnchantModifier
            listOf(
                actionField(10, Material.ENCHANTED_BOOK, "gui-field-enchant-pool", { it.pool() }) { s ->
                    s.push(EnchantmentPoolPickerScreen(s, ctx) { key ->
                        val now = current() as EnchantModifier
                        replace(EnchantModifier(key, now.minEnchants(), now.maxEnchants(), now.onlyCompatible()))
                        s.markDirty()
                    })
                },
                intField(12, Material.PAPER, "gui-field-min-enchants", 0, { it.minEnchants() }) { v ->
                    replace(EnchantModifier(it.pool(), v, maxOf(v, it.maxEnchants()), it.onlyCompatible()))
                },
                intField(14, Material.PAPER, "gui-field-max-enchants", 0, { it.maxEnchants() }) { v ->
                    replace(EnchantModifier(it.pool(), minOf(v, it.minEnchants()), v, it.onlyCompatible()))
                },
                boolField(16, "gui-field-only-compatible", { it.onlyCompatible() }) { v ->
                    replace(EnchantModifier(it.pool(), it.minEnchants(), it.maxEnchants(), v))
                }
            )
        }

        is DamageModifier -> FieldScreen(session, 3, title) {
            val it = current() as DamageModifier
            listOf(
                fractionField(12, Material.IRON_SWORD, "gui-field-min-fraction", { it.minFraction() }) { v ->
                    replace(DamageModifier(v, maxOf(v, it.maxFraction())))
                },
                fractionField(14, Material.IRON_SWORD, "gui-field-max-fraction", { it.maxFraction() }) { v ->
                    replace(DamageModifier(minOf(v, it.minFraction()), v))
                }
            )
        }

        is NameModifier -> FieldScreen(session, 3, title) {
            val it = current() as NameModifier
            listOf(
                textField(13, Material.NAME_TAG, "gui-field-name-text", { it.text().ifEmpty { "-" } }) {
                    "/lootpool complex modifier set name "
                }
            )
        }

        is LoreModifier -> FieldScreen(session, 3, title) {
            val it = current() as LoreModifier
            listOf(
                cycleField(
                    11, Material.WRITABLE_BOOK, "gui-field-lore-mode",
                    LoreModifier.Mode.entries.toList(), { it.mode() },
                    { v -> replace(LoreModifier(it.lines(), v)) }, { mode -> mode.name.lowercase() }
                ),
                textField(15, Material.WRITTEN_BOOK, "gui-field-lore-lines", { it.lines().size.toString() }) {
                    "/lootpool complex modifier set lore "
                }
            )
        }

        is SetAttributesModifier -> FieldScreen(session, 3, title) {
            val it = current() as SetAttributesModifier
            listOf(
                boolField(11, "gui-field-attributes-replace", { it.replace() }) { v ->
                    replace(SetAttributesModifier(it.entries(), v))
                },
                actionField(15, Material.IRON_CHESTPLATE, "gui-field-attributes-entries", { it.entries().size.toString() }) { s ->
                    s.push(AttributeListScreen(s, modifiers, index))
                }
            )
        }

        else -> null
    }
}

/** The attribute modifications inside one attributes modifier. */
class AttributeListScreen(
    session: EditSession,
    private val modifiers: ListHandle<LootModifier>,
    private val modifierIndex: Int
) : ButtonScreen(session, ROWS, Component.text("attributes")) {

    private fun modifier() = unwrap(modifiers[modifierIndex]) as SetAttributesModifier

    private fun replaceEntries(entries: List<AttributeEntry>) {
        modifiers[modifierIndex] = rewrap(modifiers[modifierIndex], SetAttributesModifier(entries, modifier().replace()))
    }

    override val contentCount: Int get() = modifier().entries().size

    override fun renderContent() {
        modifier().entries().drop(page * contentSize)
            .take(contentSize)
            .forEachIndexed { slot, entry ->
                inventory.setItem(
                    slot,
                    item(Material.IRON_CHESTPLATE, "gui-attribute-entry", "attribute" replace entry.attribute().name.lowercase())
                        .lore(
                            "gui-attribute-entry-lore",
                            "operation" replace entry.operation().name.lowercase(),
                            "min" replace String.format("%.2f", entry.minAmount()),
                            "max" replace String.format("%.2f", entry.maxAmount()),
                            "slot" replace (entry.slot()?.name?.lowercase() ?: "any")
                        )
                )
            }
    }

    override fun renderControls() {
        super.renderControls()
        button(controlRow + 3, item(Material.LIME_DYE, "gui-add-attribute").lore("gui-add-attribute-lore")) {
            replaceEntries(
                modifier().entries() + AttributeEntry(
                    Attribute.GENERIC_ATTACK_DAMAGE, AttributeModifier.Operation.ADD_NUMBER, 1.0, 1.0, null
                )
            )
            session.markDirty()
            session.refresh()
        }
    }

    override fun onContentClick(event: InventoryClickEvent, index: Int) {
        event.isCancelled = true
        val entries = modifier().entries()
        if (index !in entries.indices) return

        if (event.action == InventoryAction.DROP_ONE_SLOT || event.action == InventoryAction.DROP_ALL_SLOT) {
            replaceEntries(entries.toMutableList().also { it.removeAt(index) })
            session.markDirty()
            session.refresh()
            return
        }
        if (event.isLeftClick)
            session.push(attributeEntryScreen(session, modifiers, modifierIndex, index))
    }

    companion object {
        private const val ROWS = 6
    }
}

private fun attributeEntryScreen(
    session: EditSession,
    modifiers: ListHandle<LootModifier>,
    modifierIndex: Int,
    entryIndex: Int
): FieldScreen {
    fun modifier() = unwrap(modifiers[modifierIndex]) as SetAttributesModifier
    fun entry() = modifier().entries()[entryIndex]
    fun replace(entry: AttributeEntry) {
        val entries = modifier().entries().toMutableList().also { it[entryIndex] = entry }
        modifiers[modifierIndex] = rewrap(modifiers[modifierIndex], SetAttributesModifier(entries, modifier().replace()))
    }

    return FieldScreen(session, 3, Component.text("attribute")) {
        val it = entry()
        listOf(
            cycleField(
                10, Material.IRON_CHESTPLATE, "gui-field-attribute",
                Attribute.entries, { it.attribute() },
                { v -> replace(AttributeEntry(v, it.operation(), it.minAmount(), it.maxAmount(), it.slot())) },
                { attribute -> attribute.name.lowercase() }
            ),
            cycleField(
                11, Material.COMPARATOR, "gui-field-attribute-operation",
                AttributeModifier.Operation.entries, { it.operation() },
                { v -> replace(AttributeEntry(it.attribute(), v, it.minAmount(), it.maxAmount(), it.slot())) },
                { operation -> operation.name.lowercase() }
            ),
            doubleField(13, Material.PAPER, "gui-field-min-amount", 0.1, { it.minAmount() }) { v ->
                replace(AttributeEntry(it.attribute(), it.operation(), v, maxOf(v, it.maxAmount()), it.slot()))
            },
            doubleField(15, Material.PAPER, "gui-field-max-amount", 0.1, { it.maxAmount() }) { v ->
                replace(AttributeEntry(it.attribute(), it.operation(), minOf(v, it.minAmount()), v, it.slot()))
            },
            cycleField(
                16, Material.ARMOR_STAND, "gui-field-attribute-slot",
                listOf(null) + EquipmentSlot.entries, { it.slot() },
                { v -> replace(AttributeEntry(it.attribute(), it.operation(), it.minAmount(), it.maxAmount(), v)) },
                { slot -> slot?.name?.lowercase() ?: "any" }
            )
        )
    }
}
