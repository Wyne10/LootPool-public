package org.bigcraft.lootpool.menu

import org.bigcraft.lootpool.core.LootManager
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import ru.abstractmenus.api.inventory.ItemProperty
import ru.abstractmenus.api.inventory.Menu
import ru.abstractmenus.hocon.api.ConfigNode
import ru.abstractmenus.hocon.api.serialize.NodeSerializer

class KeyedLootProperty(private val lootKey: String) : ItemProperty {

    override fun canReplaceMaterial(): Boolean = true

    override fun isApplyMeta(): Boolean = false

    override fun apply(item: ItemStack, meta: ItemMeta, player: Player, menu: Menu) {
        val lootItem = LootManager.instance.getLoot(lootKey)?.loot()?.create() ?: return
        item.type = lootItem.type
        item.itemMeta = lootItem.itemMeta
        item.amount = lootItem.amount
    }

    object Factory : NodeSerializer<KeyedLootProperty> {
        override fun deserialize(type: Class<KeyedLootProperty>, node: ConfigNode): KeyedLootProperty =
            KeyedLootProperty(node.string)
    }

}