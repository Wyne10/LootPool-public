package me.wyne.lootpool.command

import me.wyne.wutils.i18n.kotlin.placeholderComponent
import me.wyne.wutils.i18n.kotlin.reduce
import me.wyne.wutils.i18n.kotlin.replace
import me.wyne.wutils.i18n.kotlin.replaceComponent
import net.kyori.adventure.text.Component
import me.wyne.lootpool.api.CompositeLootPool
import me.wyne.lootpool.api.LootPool
import org.bukkit.command.CommandSender

fun displayCompositeInfo(sender: CommandSender, lootPool: CompositeLootPool) {
    val weightSorted = lootPool.lootPools.toSortedMap(compareByDescending<LootPool> { lootPool.lootPools[it] }.thenBy { it.key.key })
    val totalWeight = weightSorted.values.sumOf { it.toDouble() }
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