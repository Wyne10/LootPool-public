package me.wyne.lootpool.command

import me.wyne.wutils.i18n.kotlin.placeholderComponent
import me.wyne.wutils.i18n.kotlin.reduce
import me.wyne.wutils.i18n.kotlin.replace
import me.wyne.wutils.i18n.kotlin.replaceComponent
import net.kyori.adventure.text.Component
import me.wyne.lootpool.api.LootPool
import me.wyne.lootpool.api.MultiLootPool
import org.bukkit.command.CommandSender

fun displayMultiInfo(sender: CommandSender, lootPool: MultiLootPool) {
    val weights = lootPool.lootPools.associateWith { pool -> pool.lootList.sumOf { it.weight } }
    val totalWeight = weights.values.sumOf { it.toDouble() }
    val weightSorted = weights.entries.sortedWith(
        compareByDescending<Map.Entry<LootPool, Int>> { it.value }.thenBy { it.key.key.key }
    )
    val lootList = weightSorted
        .map { entry ->
            val percentage = if (totalWeight == 0.0) 0.0 else (entry.value / totalWeight) * 100
            sender.placeholderComponent(
                "info-lootpool-loot",
                "loot-name" replace entry.key.key.key,
                "loot-type" replace entry.key.javaClass.simpleName,
                "weight" replace entry.value,
                "min-amount" replace 0,
                "max-amount" replace 0,
                "percentage" replace String.format("%.2f", percentage)
            )
        }.reduce() ?: Component.empty()
    sender.placeholderComponent(
        "info-lootpool",
        "key" replace lootPool.key.key,
        "type" replace lootPool.javaClass.simpleName,
        "rolls" replace ""
    ).replace("loot-list" replaceComponent lootList)
        .sendMessage(sender)
}
