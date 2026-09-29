package me.wyne.lootpool.gui

import me.wyne.wutils.i18n.kotlin.placeholderComponent
import me.wyne.wutils.i18n.kotlin.replace
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.InventoryClickEvent
import kotlin.math.roundToInt

/**
 * One editable slot on a [FieldScreen]. The lambdas read and write straight through to the pool's
 * immutable records, so a screen never holds a stale copy of what it is editing.
 */
class Field(
    val slot: Int,
    val material: () -> Material,
    val nameKey: String,
    val loreKey: String?,
    val value: () -> String,
    val mutates: Boolean = true,
    val onClick: (InventoryClickEvent, EditSession) -> Unit
)

/** A fixed-layout screen built from [Field]s rather than a paged list. */
open class FieldScreen(
    session: EditSession,
    rows: Int,
    title: Component,
    private val fields: () -> List<Field>
) : ButtonScreen(session, rows, title) {

    override fun renderContent() {
        fields().forEach { field ->
            val item = item(field.material(), field.nameKey, "value" replace field.value())
                .let { if (field.loreKey != null) it.lore(field.loreKey, "value" replace field.value()) else it }
            button(field.slot, item) { event ->
                field.onClick(event, session)
                if (field.mutates) {
                    session.markDirty()
                    session.refresh()
                }
            }
        }
    }
}

fun intField(
    slot: Int,
    material: Material,
    nameKey: String,
    min: Int = 0,
    get: () -> Int,
    set: (Int) -> Unit
) = Field(slot, { material }, nameKey, "gui-field-number-lore", { get().toString() }) { event, session ->
    when {
        event.isLeftClick -> set((get() - session.step).coerceAtLeast(min))
        event.isRightClick -> set(get() + session.step)
    }
}

/** A 0..1 fraction, nudged in whole percent per step and displayed as a percentage. */
fun fractionField(
    slot: Int,
    material: Material,
    nameKey: String,
    get: () -> Double,
    set: (Double) -> Unit
) = Field(slot, { material }, nameKey, "gui-field-number-lore", { "${(get() * 100).roundToInt()}%" }) { event, session ->
    val step = session.step * 0.01
    when {
        event.isLeftClick -> set((get() - step).coerceIn(0.0, 1.0))
        event.isRightClick -> set((get() + step).coerceIn(0.0, 1.0))
    }
}

fun doubleField(
    slot: Int,
    material: Material,
    nameKey: String,
    scale: Double = 0.1,
    get: () -> Double,
    set: (Double) -> Unit
) = Field(slot, { material }, nameKey, "gui-field-number-lore", { String.format("%.2f", get()) }) { event, session ->
    val step = session.step * scale
    when {
        event.isLeftClick -> set(get() - step)
        event.isRightClick -> set(get() + step)
    }
}

fun boolField(
    slot: Int,
    nameKey: String,
    get: () -> Boolean,
    set: (Boolean) -> Unit
) = Field(
    slot,
    { if (get()) Material.LIME_DYE else Material.GRAY_DYE },
    nameKey,
    "gui-field-toggle-lore",
    { get().toString() }
) { _, _ -> set(!get()) }

/** A yes / no / "don't care" toggle, for the nullable checks on weather conditions. */
fun tristateField(
    slot: Int,
    nameKey: String,
    get: () -> Boolean?,
    set: (Boolean?) -> Unit
) = Field(
    slot,
    {
        when (get()) {
            true -> Material.LIME_DYE
            false -> Material.RED_DYE
            null -> Material.GRAY_DYE
        }
    },
    nameKey,
    "gui-field-toggle-lore",
    { get()?.toString() ?: "any" }
) { _, _ ->
    set(
        when (get()) {
            null -> true
            true -> false
            false -> null
        }
    )
}

fun <T> cycleField(
    slot: Int,
    material: Material,
    nameKey: String,
    values: List<T>,
    get: () -> T,
    set: (T) -> Unit,
    label: (T) -> String
) = Field(slot, { material }, nameKey, "gui-field-cycle-lore", { label(get()) }) { _, _ ->
    if (values.isNotEmpty()) {
        val next = (values.indexOf(get()) + 1).mod(values.size)
        set(values[next])
    }
}

/** A read-only field; clicking sends the command that changes it, ready to run. */
fun textField(
    slot: Int,
    material: Material,
    nameKey: String,
    get: () -> String,
    command: () -> String
) = Field(slot, { material }, nameKey, "gui-field-text-lore", get, mutates = false) { _, session ->
    session.player
        .placeholderComponent("gui-hint-command", "command" replace command())
        .sendMessage(session.player)
}

/** A button that navigates or acts rather than holding a value. */
fun actionField(
    slot: Int,
    material: Material,
    nameKey: String,
    value: () -> String = { "" },
    loreKey: String? = null,
    action: (EditSession) -> Unit
) = Field(slot, { material }, nameKey, loreKey, value, mutates = false) { _, session -> action(session) }
