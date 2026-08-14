package org.bigcraft.lootpool.gui

import me.wyne.wutils.common.event.EventRegistry
import me.wyne.wutils.common.event.RegisterableListener
import me.wyne.wutils.common.kotlin.item.isNotNullOrAir
import me.wyne.wutils.common.kotlin.item.isNullOrAir
import me.wyne.wutils.i18n.kotlin.placeholderComponent
import me.wyne.wutils.i18n.kotlin.placeholderComponents
import me.wyne.wutils.i18n.kotlin.replace
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bigcraft.lootpool.LootPool
import org.bigcraft.lootpool.api.BasicLootPool
import org.bigcraft.lootpool.api.EditableLootPool
import org.bigcraft.lootpool.api.Loot
import org.bigcraft.lootpool.command.nameComponent
import org.bigcraft.lootpool.core.LootPoolManager
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.EnchantmentStorageMeta
import java.util.LinkedList

class LootPoolGui(private val key: String, private val player: Player) : RegisterableListener {

    private val inventory = Bukkit.createInventory(player, 9 * 6, Component.text(key))
    private val nothingItem = MutableLoot(ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        .also { it.editMeta { meta -> meta.displayName(player.placeholderComponent("gui-nothing-item").get()) } },
        0, 1, 1
    )
    private val lootPool = mutableListOf<MutableLoot>().also { it.add(nothingItem) }
    private val lootPoolPercentage: List<Double>
        get() {
            val totalWeight = lootPool.sumOf { it.weight.toDouble() }
            val percentage = lootPool.map { (it.weight / totalWeight) * 100 }
            return percentage
        }
    private var valueMultiplier = 1

    private val eventRegistry = EventRegistry(LootPool.instance)

    private var currentPage = 0

    private var source: EditableLootPool? = null

    constructor(key: String, player: Player, lootPool: EditableLootPool) : this(key, player) {
        source = lootPool
        lootPool.lootList
            .filter { it.item.isNotNullOrAir() }
            .forEach { this.lootPool.add(it.asMutable()) }
        lootPool.lootList
            .firstOrNull { it.item.type == Material.AIR }
            ?.let {
                nothingItem.weight = it.weight
                nothingItem.minAmount = it.minAmount
                nothingItem.maxAmount = it.maxAmount
            }
    }

    init {
        eventRegistry.register(this)
        player.openInventory(inventory)
        run { render() }
    }

    @EventHandler(ignoreCancelled = true)
    private fun onPut(event: InventoryClickEvent) {
        if (!validateClick(event)) return
        // Shift clicking from player inventory to loot pool inventory
        if (event.isShiftClick && event.clickedInventory == player.inventory && event.currentItem.isNotNullOrAir()) {
            if (inventory.firstEmpty() == -1) {
                event.isCancelled = true
                return
            }
            lootPool.add(MutableLoot(event.currentItem!!.clone()))
        }
        // Put item from cursor to empty loot pool slot
        else if (!event.isShiftClick && event.clickedInventory == inventory && event.currentItem.isNullOrAir() && event.cursor.isNotNullOrAir()) {
            lootPool.add(MutableLoot(event.cursor!!.clone()))
        } else return
        event.isCancelled = true
        run { render() }
    }

    @EventHandler(ignoreCancelled = true)
    private fun onReplace(event: InventoryClickEvent) {
        if (!validateClick(event)) return
        if (event.slot == 0 && currentPage == 0) return
        // Replace loot pool item with item on a cursor
        if (event.clickedInventory == inventory && event.currentItem.isNotNullOrAir() && event.cursor.isNotNullOrAir()) {
            event.isCancelled = true
            val index = event.index
            val previousLoot = lootPool[index]
            val previousItem = previousLoot.item.clone()
            lootPool[index] = MutableLoot(event.cursor!!.clone(), previousLoot.weight, previousLoot.minAmount, previousLoot.maxAmount)
            run {
                event.cursor = previousItem
                render()
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private fun onLmbRmb(event: InventoryClickEvent) {
        if (!validateClick(event)) return
        if (!validateModificationClick(event)) return
        if (event.isShiftClick) return
        val loot = lootPool[event.index]
        if (event.isLeftClick)
            loot.weight -= valueMultiplier
        if (event.isRightClick)
            loot.weight += valueMultiplier
        run { render() }
    }

    @EventHandler(ignoreCancelled = true)
    private fun onShiftLmbRmb(event: InventoryClickEvent) {
        if (!validateClick(event)) return
        if (!validateModificationClick(event)) return
        if (!event.isShiftClick) return
        val loot = lootPool[event.index]
        if (event.isLeftClick)
            loot.minAmount -= valueMultiplier
        if (event.isRightClick)
            loot.minAmount += valueMultiplier
        run { render() }
    }

    @EventHandler(ignoreCancelled = true)
    private fun onHotbar(event: InventoryClickEvent) {
        if (!validateClick(event)) return
        if (!validateModificationClick(event)) return
        if (event.action != InventoryAction.HOTBAR_SWAP &&
            event.action != InventoryAction.HOTBAR_MOVE_AND_READD) return
        if (event.click != ClickType.NUMBER_KEY) return
        val loot = lootPool[event.index]
        if (event.hotbarButton == 0)
            loot.maxAmount -= valueMultiplier
        if (event.hotbarButton == 1)
            loot.maxAmount += valueMultiplier
        run { render() }
    }

    @EventHandler(ignoreCancelled = true)
    private fun onQ(event: InventoryClickEvent) {
        if (!validateClick(event)) return
        if (!validateModificationClick(event)) return
        if (event.action != InventoryAction.DROP_ONE_SLOT &&
            event.action != InventoryAction.DROP_ALL_SLOT) return
        if (event.slot == 0 && currentPage == 0) {
            nothingItem.weight = 0
            return
        }
        lootPool.removeAt(event.index)
        inventory.clear()
        run { render() }
    }

    @EventHandler(ignoreCancelled = true)
    private fun onF(event: InventoryClickEvent) {
        if (!validateClick(event)) return
        if (!validateModificationClick(event)) return
        if (event.click != ClickType.SWAP_OFFHAND) return
        valueMultiplier = if (valueMultiplier == 1) 10 else 1
    }

    @EventHandler(ignoreCancelled = true)
    private fun onInventoryClose(event: InventoryCloseEvent) {
        if (event.inventory != inventory) return
        eventRegistry.close()
        if (lootPool.size <= 1) return
        if (nothingItem.weight > 0)
            lootPool[0] = MutableLoot(ItemStack(Material.AIR), nothingItem.weight, nothingItem.minAmount, nothingItem.maxAmount)
        else
            lootPool.removeAt(0)
        val lootList = LinkedList(lootPool.map { it.asImmutable() })
        LootPoolManager.instance.writeLootPool(
            source?.withLoot(key, lootList) ?: BasicLootPool(key, lootList)
        )
        player.placeholderComponent("success-lootpool-create", "key" replace key).sendMessage(player)
    }

    @EventHandler(ignoreCancelled = true)
    private fun onPage(event: InventoryClickEvent) {
        if (event.inventory != inventory) return
        if (event.click == ClickType.LEFT && event.slotType == InventoryType.SlotType.OUTSIDE)
            currentPage = (currentPage - 1).coerceAtLeast(0)
        else if (event.click == ClickType.RIGHT && event.slotType == InventoryType.SlotType.OUTSIDE)
            currentPage = (currentPage + 1).coerceIn(0, lootPool.size / (9 * 6))
        else
            return
        inventory.clear()
        run { render() }
    }

    private fun validateClick(event: InventoryClickEvent): Boolean {
        if (event.inventory != inventory) return false
        if (event.action in CANCELLED_ACTIONS) {
            event.isCancelled = true
            return false
        }
        if (event.click in CANCELLED_CLICKS) {
            event.isCancelled = true
            return false
        }
        if (event.clickedInventory == player.inventory && event.currentItem.isNotNullOrAir() && event.cursor.isNotNullOrAir()) {
            event.isCancelled = true
            return false
        }
        return true
    }

    private fun validateModificationClick(event: InventoryClickEvent): Boolean {
        if (event.clickedInventory != inventory) return false
        event.isCancelled = true
        if (event.currentItem.isNullOrAir()) return false
        if (event.cursor.isNotNullOrAir()) return false
        return true
    }

    private fun render() {
        lootPool
            .drop(9 * 6 * currentPage)
            .take(9 * 6)
            .forEachIndexed { index, loot ->
                inventory.setItem(index, loot.render(lootPoolPercentage.getOrElse(index + (9 * 6 * currentPage)) { 0.0 }, player))
            }
    }

    private fun run(runnable: Runnable) {
        Bukkit.getScheduler().runTask(LootPool.instance, runnable)
    }

    private val InventoryClickEvent.index: Int
        get() = (9 * 6 * currentPage) + slot

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

private data class MutableLoot(var item: ItemStack, private var _weight: Int, private var _minAmount: Int, private var _maxAmount: Int) {

    constructor(item: ItemStack) : this(item, 0, item.amount, item.amount)

    var weight: Int
        get() = _weight
        set(value) { _weight = value.coerceAtLeast(0) }

    var minAmount: Int
        get() = _minAmount
        set(value) {
            val set = if (value < 1) 64 else if (value > item.maxStackSize) 1 else value
            if (set > maxAmount)
                maxAmount = set.coerceAtMost(item.maxStackSize)
            _minAmount = set.coerceIn(1, _maxAmount)
        }

    var maxAmount: Int
        get() = _maxAmount
        set(value) {
            val set = if (value < 1) 64 else if (value > item.maxStackSize) 1 else value
            if (set < minAmount)
                minAmount = set.coerceAtLeast(1)
            _maxAmount = set.coerceIn(_minAmount, item.maxStackSize)
        }

    fun render(percentage: Double, player: Player): ItemStack {
        val render = item.clone()
        render.amount = maxAmount
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
            lore.addAll(player.placeholderComponents(
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
}

private fun Loot.asMutable() =
    MutableLoot(this.item.clone(), this.weight, this.minAmount, this.maxAmount)

private fun MutableLoot.asImmutable() =
    Loot(this.item.clone(), this.weight, this.minAmount, this.maxAmount)