package org.bigcraft.lootpool.gui

import me.wyne.wutils.common.event.EventRegistry
import me.wyne.wutils.common.event.RegisterableListener
import me.wyne.wutils.common.kotlin.item.isNotNullOrAir
import me.wyne.wutils.common.kotlin.item.isNullOrAir
import me.wyne.wutils.i18n.kotlin.placeholderComponent
import me.wyne.wutils.i18n.kotlin.placeholderComponents
import me.wyne.wutils.i18n.kotlin.replace
import org.bigcraft.lootpool.LootPool
import org.bigcraft.lootpool.api.Loot
import org.bigcraft.lootpool.core.LootPoolManager
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.ItemStack

class LootPoolGui(private val key: String, private val player: Player) : RegisterableListener {

    private val inventory = Bukkit.createInventory(player, 9 * 6)
    private val nothingItem = MutableLoot(ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        .also { it.editMeta { meta -> meta.setDisplayNameComponent(player.placeholderComponent("gui-nothing-item").bungee()) } },
        0, 1, 1
    )
    private val lootPool = mutableListOf<MutableLoot>().also { it.add(nothingItem) }

    private val eventRegistry = EventRegistry(LootPool.instance)

    constructor(key: String, player: Player, lootPool: org.bigcraft.lootpool.api.LootPool) : this(key, player) {
        lootPool.lootPool.forEach { this.lootPool.add(it.asMutable()) }
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
        if (event.slot == 0) return
        // Replace loot pool item with item on a cursor
        if (event.clickedInventory == inventory && event.currentItem.isNotNullOrAir() && event.cursor.isNotNullOrAir()) {
            event.isCancelled = true
            val slot = event.slot
            val previousLoot = lootPool[slot]
            val previousItem = previousLoot.item.clone()
            lootPool[slot] = MutableLoot(event.cursor!!.clone(), previousLoot.weight, previousLoot.minAmount, previousLoot.maxAmount)
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
        val loot = lootPool[event.slot]
        if (event.isLeftClick)
            loot.weight--
        if (event.isRightClick)
            loot.weight++
        run { render() }
    }

    @EventHandler(ignoreCancelled = true)
    private fun onShiftLmbRmb(event: InventoryClickEvent) {
        if (!validateClick(event)) return
        if (!validateModificationClick(event)) return
        if (!event.isShiftClick) return
        val loot = lootPool[event.slot]
        if (event.isLeftClick)
            loot.minAmount--
        if (event.isRightClick)
            loot.minAmount++
        run { render() }
    }

    @EventHandler(ignoreCancelled = true)
    private fun onHotbar(event: InventoryClickEvent) {
        if (!validateClick(event)) return
        if (!validateModificationClick(event)) return
        if (event.action != InventoryAction.HOTBAR_SWAP &&
            event.action != InventoryAction.HOTBAR_MOVE_AND_READD) return
        if (event.click != ClickType.NUMBER_KEY) return
        val loot = lootPool[event.slot]
        if (event.hotbarButton == 0)
            loot.maxAmount--
        if (event.hotbarButton == 1)
            loot.maxAmount++
        run { render() }
    }

    @EventHandler(ignoreCancelled = true)
    private fun onQ(event: InventoryClickEvent) {
        if (!validateClick(event)) return
        if (!validateModificationClick(event)) return
        if (event.slot == 0) return
        if (event.action != InventoryAction.DROP_ONE_SLOT &&
            event.action != InventoryAction.DROP_ALL_SLOT) return
        lootPool.removeAt(event.slot)
        inventory.clear()
        run { render() }
    }

    @EventHandler(ignoreCancelled = true)
    private fun onInventoryClose(event: InventoryCloseEvent) {
        if (event.inventory != inventory) return
        eventRegistry.close()
        if (lootPool.size <= 1) return
        lootPool[0] = MutableLoot(ItemStack(Material.AIR), nothingItem.weight, nothingItem.minAmount, nothingItem.maxAmount)
        LootPoolManager.instance.writeLootPool(org.bigcraft.lootpool.api.LootPool(
                key,
                listOf(*lootPool.map { it.asImmutable() }.toTypedArray())
            )
        )
        player.placeholderComponent("success-lootpool-create", "key" replace key).sendMessage(player)
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
        lootPool.forEachIndexed { index, loot ->
            inventory.setItem(index, loot.render(player))
        }
    }

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

private data class MutableLoot(var item: ItemStack, private var _weight: Int, private var _minAmount: Int, private var _maxAmount: Int) {

    constructor(item: ItemStack) : this(item, 0, item.amount, item.amount)

    var weight: Int
        get() = _weight
        set(value) { _weight = value.coerceAtLeast(0) }

    var minAmount: Int
        get() = _minAmount
        set(value) {
            if (value > maxAmount)
                maxAmount = value.coerceAtMost(64)
            _minAmount = value.coerceIn(1, _maxAmount)
        }

    var maxAmount: Int
        get() = _maxAmount
        set(value) {
            if (value < minAmount)
                minAmount = value.coerceAtLeast(1)
            _maxAmount = value.coerceIn(_minAmount, 64)
        }

    fun render(player: Player): ItemStack {
        val render = item.clone()
        render.amount = minAmount
        render.editMeta { meta ->
            meta.loreComponents = player.placeholderComponents(
                "gui-loot",
                "weight" replace weight,
                "min-amount" replace minAmount,
                "max-amount" replace maxAmount).map { it.bungee() }
        }
        return render
    }
}

private fun Loot.asMutable() =
    MutableLoot(this.item.clone(), this.weight, this.minAmount, this.maxAmount)

private fun MutableLoot.asImmutable() =
    Loot(this.item.clone(), this.weight, this.minAmount, this.maxAmount)