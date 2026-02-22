package org.bigcraft.lootpool.menu

import org.bigcraft.lootpool.core.LootPoolManager
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import ru.abstractmenus.api.inventory.ItemProperty
import ru.abstractmenus.api.inventory.Menu
import ru.abstractmenus.hocon.api.ConfigNode
import ru.abstractmenus.hocon.api.serialize.NodeSerializer

class LootPoolProperty(private val lootPoolKey: String) : ItemProperty {

    override fun canReplaceMaterial(): Boolean = true

    override fun isApplyMeta(): Boolean = false

    override fun apply(item: ItemStack, meta: ItemMeta, player: Player, menu: Menu) {
        val lootItem = LootPoolManager.instance.getLootPool(lootPoolKey)?.random?.create() ?: return
        item.type = lootItem.type
        item.itemMeta = lootItem.itemMeta
        item.amount = lootItem.amount
    }

    object Factory : NodeSerializer<LootPoolProperty> {
        override fun deserialize(type: Class<LootPoolProperty>, node: ConfigNode): LootPoolProperty =
            LootPoolProperty(node.string)
    }

}