package org.bigcraft.lootpool.menu

import me.wyne.wutils.common.Args
import me.wyne.wutils.i18n.I18n
import me.wyne.wutils.i18n.kotlin.component
import org.bigcraft.lootpool.api.Loot
import org.bigcraft.lootpool.command.nameComponent
import org.bigcraft.lootpool.core.LootPoolManager
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.util.io.BukkitObjectOutputStream
import ru.abstractmenus.api.Catalog
import ru.abstractmenus.api.ValueExtractor
import ru.abstractmenus.api.inventory.Menu
import ru.abstractmenus.hocon.api.ConfigNode
import ru.abstractmenus.hocon.api.serialize.NodeSerializer
import java.io.ByteArrayOutputStream
import java.util.Base64
import kotlin.random.Random

object LootExtractor : ValueExtractor {
    override fun extract(obj: Any, placeholder: String): String? {
        if (obj !is Loot) return null
        val item = obj.item.clone()

        if (placeholder.startsWith("name"))
            return I18n.style(item.nameComponent, "name", placeholder)
        else if (placeholder.startsWith("lore")) {
            val args = Args(placeholder, "_")
            return item.itemMeta.loreComponents
                ?.map { it.component }
                ?.map { I18n.style(it, "lore", args[0]) }
                ?.flatMap { it.split("\n", "<br>") }
                ?.getOrElse(args[1].toInt() - 1) { "" } ?: ""
        }

        return when(placeholder) {
            "serialized" -> encodeStack(item)
            "material" -> item.type.name
            "weight" -> obj.weight.toString()
            "minAmount" -> obj.minAmount.toString()
            "maxAmount" -> obj.maxAmount.toString()
            "randomAmount" -> Random.nextInt(obj.minAmount, obj.maxAmount + 1).toString()
            else -> null
        }
    }
}

class LootPoolCatalog(private val lootPoolKey: String) : Catalog<Loot> {
    override fun snapshot(player: Player, menu: Menu): Collection<Loot> =
        LootPoolManager.instance.getLootPool(lootPoolKey)?.lootList ?: emptyList()

    override fun extractor(): ValueExtractor = LootExtractor

    object Factory : NodeSerializer<LootPoolCatalog> {
        override fun deserialize(type: Class<LootPoolCatalog>, node: ConfigNode): LootPoolCatalog =
            LootPoolCatalog(node.node("lootPool").getString(""))
    }
}

fun encodeStack(item: ItemStack): String =
    ByteArrayOutputStream().use { baos ->
        BukkitObjectOutputStream(baos).use { oos ->
            oos.writeObject(item)
            return Base64.getEncoder().encodeToString(baos.toByteArray())
        }
    }