package me.wyne.lootpool.gui

import me.wyne.lootpool.api.complex.EnchantmentEntry
import me.wyne.lootpool.api.complex.EnchantmentPool
import me.wyne.lootpool.api.complex.EnchantmentPoolProvider
import me.wyne.wutils.i18n.kotlin.placeholderComponent
import me.wyne.wutils.i18n.kotlin.replace
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.EnchantmentStorageMeta

/**
 * The enchantment pool editor: every enchantment registered on this server gets a slot. A plain
 * book means the enchantment is not in the pool; giving it a weight turns it into an enchanted
 * book holding that enchantment at its upper level, so the pool reads at a glance.
 */
class EnchantmentPoolScreen(
    session: EditSession,
    private val key: String,
    private val provider: EnchantmentPoolProvider,
    source: EnchantmentPool?
) : ButtonScreen(session, ROWS, Component.text(key)) {

    private class Draft(var weight: Int, var minLevel: Int, var maxLevel: Int)

    private val enchantments: List<Enchantment> = Enchantment.values().sortedBy { it.key.key }
    private val drafts = mutableMapOf<Enchantment, Draft>()

    override val contentCount: Int get() = enchantments.size
    override val isRoot: Boolean get() = true

    init {
        source?.entries()?.forEach { entry ->
            drafts[entry.enchantment()] = Draft(entry.weight(), entry.minLevel(), entry.maxLevel())
        }
    }

    private val totalWeight: Double
        get() = drafts.values.sumOf { it.weight.toDouble() }

    override fun renderContent() {
        enchantments.drop(page * contentSize)
            .take(contentSize)
            .forEachIndexed { slot, enchantment ->
                inventory.setItem(slot, render(enchantment, drafts[enchantment]))
            }
    }

    private fun render(enchantment: Enchantment, draft: Draft?): ItemStack {
        val enabled = draft != null && draft.weight > 0
        val stack = ItemStack(if (enabled) Material.ENCHANTED_BOOK else Material.BOOK)
        stack.editMeta { meta ->
            val level = if (enabled) draft.maxLevel else 1
            val name = enchantment.displayName(level).decoration(TextDecoration.ITALIC, false)
            if (enabled) {
                val total = totalWeight
                val percentage = if (total == 0.0) 0.0 else (draft.weight / total) * 100
                meta.displayName(
                    Component.empty().decoration(TextDecoration.ITALIC, false)
                        .append(name)
                        .append(Component.space())
                        .append(Component.text("(${String.format("%.2f", percentage)})").color(NamedTextColor.AQUA))
                )
                if (meta is EnchantmentStorageMeta)
                    meta.addStoredEnchant(enchantment, level.coerceAtLeast(1), true)
            } else {
                meta.displayName(name.color(NamedTextColor.GRAY))
            }
            val lore = player.placeholderComponent("gui-enchantment-key", "enchantment" replace enchantment.key.key)
                .get().decoration(TextDecoration.ITALIC, false)
            meta.lore(
                listOf(lore) + player.placeholderComponent("gui-enchantment-vanilla-max", "max" replace enchantment.maxLevel)
                    .get().decoration(TextDecoration.ITALIC, false)
            )
        }
        return stack.lore(
            "gui-enchantment-entry",
            "weight" replace (drafts[enchantment]?.weight ?: 0),
            "min-level" replace (drafts[enchantment]?.minLevel ?: 1),
            "max-level" replace (drafts[enchantment]?.maxLevel ?: 1)
        )
    }

    override fun onContentClick(event: InventoryClickEvent, index: Int) {
        event.isCancelled = true
        val enchantment = enchantments.getOrNull(index) ?: return
        val draft = drafts.getOrPut(enchantment) { Draft(0, 1, 1) }

        when {
            event.action == InventoryAction.DROP_ONE_SLOT || event.action == InventoryAction.DROP_ALL_SLOT -> {
                drafts.remove(enchantment)
                session.markDirty()
                session.refresh()
                return
            }
            event.click == ClickType.NUMBER_KEY && event.hotbarButton == 0 -> draft.maxLevel -= session.step
            event.click == ClickType.NUMBER_KEY && event.hotbarButton == 1 -> draft.maxLevel += session.step
            event.isShiftClick && event.isLeftClick -> draft.minLevel -= session.step
            event.isShiftClick && event.isRightClick -> draft.minLevel += session.step
            event.isLeftClick -> draft.weight -= session.step
            event.isRightClick -> draft.weight += session.step
            else -> return
        }

        draft.weight = draft.weight.coerceAtLeast(0)
        draft.minLevel = draft.minLevel.coerceAtLeast(1)
        draft.maxLevel = draft.maxLevel.coerceAtLeast(draft.minLevel)
        draft.minLevel = draft.minLevel.coerceAtMost(draft.maxLevel)
        session.markDirty()
        session.refresh()
    }

    fun commit() {
        if (!session.dirty) return
        val entries = enchantments.mapNotNull { enchantment ->
            val draft = drafts[enchantment] ?: return@mapNotNull null
            if (draft.weight <= 0) return@mapNotNull null
            EnchantmentEntry(enchantment, draft.weight, draft.minLevel, draft.maxLevel)
        }
        provider.writeEnchantmentPool(EnchantmentPool(key, entries))
        player.placeholderComponent("success-enchantment-pool-modify", "key" replace key).sendMessage(player)
    }


    companion object {
        private const val ROWS = 6
    }
}

/** Opens the enchantment pool editor. */
fun openEnchantmentPoolGui(player: Player, key: String, provider: EnchantmentPoolProvider) {
    val session = EditSession(player)
    val screen = EnchantmentPoolScreen(session, key, provider, provider.getEnchantmentPool(key))
    session.commitWith { screen.commit() }
    session.push(screen)
}
