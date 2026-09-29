package me.wyne.lootpool.gui

import me.wyne.lootpool.api.Loot
import me.wyne.lootpool.command.nameComponent
import me.wyne.wutils.i18n.kotlin.placeholderComponents
import me.wyne.wutils.i18n.kotlin.replace
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.EnchantmentStorageMeta

/**
 * The basic-pool editor's working copy of a [Loot] entry. Kept mutable because every click nudges
 * one of its numbers, and converted back with [asImmutable] when the editor commits.
 */
internal data class MutableLoot(var item: ItemStack, private var _weight: Int, private var _minAmount: Int, private var _maxAmount: Int) {

    constructor(item: ItemStack) : this(item, 1, item.amount, item.amount)

    var weight: Int
        get() = _weight
        set(value) { _weight = value.coerceAtLeast(0) }

    var minAmount: Int
        get() = _minAmount
        set(value) {
            val set = wrapAmount(value, item.maxStackSize)
            if (set > maxAmount)
                maxAmount = set.coerceAtMost(item.maxStackSize)
            _minAmount = set.coerceIn(1, _maxAmount)
        }

    var maxAmount: Int
        get() = _maxAmount
        set(value) {
            val set = wrapAmount(value, item.maxStackSize)
            if (set < minAmount)
                minAmount = set.coerceAtLeast(1)
            _maxAmount = set.coerceIn(_minAmount, item.maxStackSize)
        }

    fun render(percentage: Double, player: Player): ItemStack =
        renderLoot(item, maxAmount, weight, minAmount, maxAmount, percentage, player)
}

/** Amount nudging wraps around rather than clamping, matching the original editor's feel. */
internal fun wrapAmount(value: Int, maxStackSize: Int): Int =
    if (value < 1) 64 else if (value > maxStackSize) 1 else value

internal fun Loot.asMutable() =
    MutableLoot(this.item.clone(), this.weight, this.minAmount, this.maxAmount)

internal fun MutableLoot.asImmutable() =
    Loot(this.item.clone(), this.weight, this.minAmount, this.maxAmount)

/**
 * Renders a loot entry as the item the editor shows: the item itself, named with its drop chance,
 * its non-vanilla enchantments listed, and the weight/amount controls spelled out in lore.
 */
internal fun renderLoot(
    item: ItemStack,
    displayAmount: Int,
    weight: Int,
    minAmount: Int,
    maxAmount: Int,
    percentage: Double,
    player: Player
): ItemStack {
    val render = item.clone()
    render.amount = displayAmount.coerceIn(1, item.maxStackSize)
    render.editMeta { meta ->
        meta.displayName(
            Component.empty().decoration(TextDecoration.ITALIC, false).append(
                render.nameComponent.append(
                    Component.space()
                        .decorations(TextDecoration.entries.associateWith { TextDecoration.State.FALSE })
                        .append(Component.text("(${String.format("%.2f", percentage)})").color(NamedTextColor.AQUA))
                )
            )
        )
        val lore = mutableListOf<Component>()
        if (meta is EnchantmentStorageMeta) {
            meta.storedEnchants
                .filter { it.key.key.namespace != NamespacedKey.MINECRAFT }
                .forEach { (enchantment, level) -> lore.add(enchantment.displayName(level)) }
        } else if (meta.hasEnchants()) {
            meta.enchants
                .filter { it.key.key.namespace != NamespacedKey.MINECRAFT }
                .forEach { (enchantment, level) -> lore.add(enchantment.displayName(level)) }
        }
        lore.addAll(
            player.placeholderComponents(
                "gui-loot",
                "weight" replace weight,
                "min-amount" replace minAmount,
                "max-amount" replace maxAmount
            ).map { it.get() }
        )
        meta.lore(lore)
    }
    return render
}
