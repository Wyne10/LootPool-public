@file:Suppress("UNCHECKED_CAST")

package org.bigcraft.lootpool.command

import dev.jorel.commandapi.CommandAPIBukkit
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.IntegerArgument
import dev.jorel.commandapi.arguments.ListArgumentBuilder
import dev.jorel.commandapi.arguments.MapArgumentBuilder
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.CommandExecutor
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import me.wyne.wutils.i18n.kotlin.placeholderComponent
import me.wyne.wutils.i18n.kotlin.plain
import me.wyne.wutils.i18n.kotlin.reduce
import me.wyne.wutils.i18n.kotlin.replace
import me.wyne.wutils.i18n.kotlin.replaceComponent
import net.kyori.adventure.text.Component
import org.bigcraft.lootpool.api.CompositeLootPool
import org.bigcraft.lootpool.api.Loot
import org.bigcraft.lootpool.api.LootPool
import org.bigcraft.lootpool.api.LootPoolProvider
import org.bigcraft.lootpool.api.MultiLootPool
import org.bigcraft.lootpool.api.RollLootPool
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender

class InfoCommand<T : LootPool>(lootPoolProvider: LootPoolProvider, lootPoolType: Class<T> = LootPool::class.java as Class<T>) : SubCommand("info") {
    override val command: CommandAPICommand = super.command
        .withShortDescription("Display loot pool contents.")
        .withFullDescription(
            """
                Display the contents of a loot pool in chat.
                Each entry shows its type, amount range, weight and drop percentage.
                "amount" caps how many entries are shown (defaults to 15),
                any remaining entries are collapsed into a single summary line.
                "sort" orders the entries: "default", "name", "weight",
                "min-amount", "max-amount".
            """.trimIndent()
        )
        .withPermission("lootpool.info")
        .withArguments(lootPoolKey("key") { lootPoolProvider.getMapOf(lootPoolType) })
        .withOptionalArguments(IntegerArgument("amount", 1))
        .withOptionalArguments(sortArgument("sort"))
        .executes(CommandExecutor { sender, args ->
            val key = args.getByClass("key", String::class.java)!!
            assertLootPoolExists(key, sender, lootPoolProvider.getMapOf(lootPoolType))
            val lootPool = lootPoolProvider.getLootPool(key)!!
            if (lootPool is CompositeLootPool)
                return@CommandExecutor displayCompositeInfo(sender, lootPool)
            if (lootPool is MultiLootPool)
                return@CommandExecutor displayMultiInfo(sender, lootPool)
            val amount = args.getByClassOrDefault("amount", Int::class.java, 15)
            val sort = LootSort.fromArgument(args.getByClass("sort", String::class.java))
            val totalWeight = lootPool.lootList.sumOf { it.weight.toDouble() }
            val sorted = sort.sort(lootPool.lootList)
            val shown = sorted.take(amount)
            val remaining = sorted.size - shown.size
            val lootComponents = shown.map { loot ->
                val percentage = if (totalWeight == 0.0) 0.0 else (loot.weight / totalWeight) * 100
                sender.placeholderComponent(
                    "info-lootpool-loot",
                    "loot-type" replace loot.item.type.name,
                    "weight" replace loot.weight,
                    "min-amount" replace loot.minAmount,
                    "max-amount" replace loot.maxAmount,
                    "percentage" replace String.format("%.2f", percentage)
                ).replace("loot-name" replaceComponent loot.item.nameComponent)
            }.toMutableList()
            if (remaining > 0)
                lootComponents.add(sender.placeholderComponent("info-lootpool-more", "amount" replace remaining))
            val lootList = lootComponents.reduce() ?: Component.empty()
            val rolls = (lootPool as? RollLootPool)?.let { " [${it.minRolls}..${it.maxRolls}]" } ?: ""
            sender.placeholderComponent(
                "info-lootpool",
                "key" replace key,
                "type" replace lootPool.javaClass.simpleName,
                "rolls" replace rolls
            ).replace("loot-list" replaceComponent lootList)
                .sendMessage(sender)
        })
}

class RemoveCommand<T : LootPool>(lootPoolProvider: LootPoolProvider, lootPoolType: Class<T> = LootPool::class.java as Class<T>) : SubCommand("remove") {
    override val command: CommandAPICommand = super.command
        .withShortDescription("Remove a loot pool.")
        .withFullDescription(
            """
                Permanently delete an existing loot pool by its key.
            """.trimIndent()
        )
        .withPermission("lootpool.remove")
        .withArguments(lootPoolKey("key") { lootPoolProvider.getMapOf(lootPoolType) })
        .executes(CommandExecutor { sender, args ->
            val key = args.getByClass("key", String::class.java)!!
            assertLootPoolExists(key, sender, lootPoolProvider.getMapOf(lootPoolType))
            lootPoolProvider.removeLootPool(key)
            sender.placeholderComponent("success-lootpool-remove", "key" replace key).sendMessage(sender)
        })
}

class ComposeCommand<T : LootPool>(lootPoolProvider: LootPoolProvider, lootPoolType: Class<T> = LootPool::class.java as Class<T>) : SubCommand("compose") {
    override val command: CommandAPICommand = super.command
        .withShortDescription("Compose a composite loot pool.")
        .withFullDescription(
            """
                Create a new composite loot pool from existing pools.
                Provide a new unique key followed by a map of pools and their integer weights,
                e.g. "pool1:3 pool2:1".
                A composite loot pool rolls one of its child pools according to those weights.
            """.trimIndent()
        )
        .withPermission("lootpool.create")
        .withArguments(StringArgument("key"))
        .withArguments(
            MapArgumentBuilder<String, Int>("pools")
                .withKeyMapper { s -> s }
                .withValueMapper { s -> s.toInt() }
                .withKeyList { lootPoolProvider.getMapOf(lootPoolType).keys.toList() }
                .withoutValueList(true)
                .build())
        .executes(CommandExecutor { sender, args ->
            val key = args.getByClass("key", String::class.java)!!
            assertLootPoolNotExists(key, sender, lootPoolProvider.getMapOf(lootPoolType))
            val pools = args.getByClassOrDefault("pools", Map::class.java, emptyMap<String, Int>()) as Map<String, Int>
            lootPoolProvider.writeLootPool(CompositeLootPool(key, pools.toMap()))
            sender.placeholderComponent("success-lootpool-create", "key" replace key).sendMessage(sender)
        })
}

class IncludeCommand<T : LootPool>(lootPoolProvider: LootPoolProvider, lootPoolType: Class<T> = LootPool::class.java as Class<T>) : SubCommand("include") {
    override val command: CommandAPICommand = super.command
        .withShortDescription("Include pools in a multi loot pool.")
        .withFullDescription(
            """
                Create a new multi loot pool from existing pools.
                Provide a new unique key followed by a list of pool keys to include.
                A multi loot pool treats all included pools as a single combined pool,
                rolling from their pooled loot.
            """.trimIndent()
        )
        .withPermission("lootpool.create")
        .withArguments(StringArgument("key"))
        .withArguments(
            ListArgumentBuilder<String>("pools")
                .withList { _ -> lootPoolProvider.getMapOf(lootPoolType).keys }
                .withStringMapper()
                .buildGreedy())
        .executes(CommandExecutor { sender, args ->
            val key = args.getByClass("key", String::class.java)!!
            assertLootPoolNotExists(key, sender, lootPoolProvider.getMapOf(lootPoolType))
            val pools = args.getByClassOrDefault("pools", List::class.java, emptyList<String>()) as List<String>
            lootPoolProvider.writeLootPool(MultiLootPool(key, pools.toSet()))
            sender.placeholderComponent("success-lootpool-create", "key" replace key).sendMessage(sender)
        })
}

class RollCommand<T : LootPool>(lootPoolProvider: LootPoolProvider, lootPoolType: Class<T> = LootPool::class.java as Class<T>) : SubCommand("roll") {
    override val command: CommandAPICommand = super.command
        .withShortDescription("Wrap a loot pool with a random roll count.")
        .withFullDescription(
            """
                Create a new roll loot pool that wraps an existing loot pool of any type.
                Provide a new unique key and the pool to wrap.
                A roll loot pool delegates all loot to the wrapped pool, but when used without an
                explicit slot count it populates a random amount of slots within the roll range.
                Optionally provide a minimum and maximum number of rolls; omit the maximum to roll a
                fixed amount, or omit both to default to a single roll.
                The wrapped pool keeps existing on its own and can still be used directly.
            """.trimIndent()
        )
        .withPermission("lootpool.create")
        .withArguments(StringArgument("key"))
        .withArguments(lootPoolKey("pool") { lootPoolProvider.getMapOf(lootPoolType) })
        .withOptionalArguments(IntegerArgument("minRolls", 1), IntegerArgument("maxRolls", 1))
        .executes(CommandExecutor { sender, args ->
            val key = args.getByClass("key", String::class.java)!!
            assertLootPoolNotExists(key, sender, lootPoolProvider.getMapOf(lootPoolType))
            val pool = args.getByClass("pool", String::class.java)!!
            assertLootPoolExists(pool, sender, lootPoolProvider.getMapOf(lootPoolType))
            val minRolls = args.getByClass("minRolls", Int::class.java) ?: 1
            val maxRolls = args.getByClass("maxRolls", Int::class.java) ?: minRolls
            lootPoolProvider.writeLootPool(RollLootPool(key, pool, minRolls, maxRolls.coerceAtLeast(minRolls)))
            sender.placeholderComponent("success-lootpool-create", "key" replace key).sendMessage(sender)
        })
}

class PreviewCommand<T : LootPool>(lootPoolProvider: LootPoolProvider, lootPoolType: Class<T> = LootPool::class.java as Class<T>) : SubCommand("preview") {
    override val command: CommandAPICommand = super.command
        .withShortDescription("Preview a loot pool in a GUI.")
        .withFullDescription(
            """
                Populate a chest GUI with a roll from the loot pool to preview it visually.
                "size" sets the number of chest rows (1-6, defaults to 3).
            """.trimIndent()
        )
        .withPermission("lootpool.info")
        .withArguments(lootPoolKey("key") { lootPoolProvider.getMapOf(lootPoolType) })
        .withOptionalArguments(IntegerArgument("size", 1, 6))
        .executesPlayer(PlayerCommandExecutor { sender, args ->
            val key = args.getByClass("key", String::class.java)!!
            assertLootPoolExists(key, sender, lootPoolProvider.getMapOf(lootPoolType))
            val lootPool = lootPoolProvider.getLootPool(key)!!
            val size = args.getByClassOrDefault("size", Int::class.java, 3)
            val inventory = Bukkit.createInventory(sender, size * 9, Component.text(lootPool.key.key))
            lootPool.populate(inventory)
            sender.openInventory(inventory)
        })
}

enum class LootSort(val argument: String) {
    DEFAULT("default") {
        override fun sort(entries: List<Loot>) = entries.sortedByDescending { it.weight }
    },
    NAME("name") {
        override fun sort(entries: List<Loot>) = entries.sortedBy { it.item.nameComponent.plain }
    },
    WEIGHT("weight") {
        override fun sort(entries: List<Loot>) = entries.sortedByDescending { it.weight }
    },
    MIN_AMOUNT("min-amount") {
        override fun sort(entries: List<Loot>) = entries.sortedByDescending { it.minAmount }
    },
    MAX_AMOUNT("max-amount") {
        override fun sort(entries: List<Loot>) = entries.sortedByDescending { it.maxAmount }
    };

    abstract fun sort(entries: List<Loot>): List<Loot>

    companion object {
        fun fromArgument(argument: String?): LootSort =
            entries.firstOrNull { it.argument == argument } ?: DEFAULT
    }
}

fun sortArgument(nodeName: String): Argument<String> =
    StringArgument(nodeName)
        .replaceSuggestions(ArgumentSuggestions.strings(*LootSort.entries.map { it.argument }.toTypedArray()))

fun lootPoolKey(nodeName: String, lootPoolMap: () -> Map<String, LootPool>): Argument<String> =
    StringArgument(nodeName)
        .replaceSuggestions(ArgumentSuggestions.stringCollection { _ -> lootPoolMap().keys })

fun assertLootPoolExists(key: String, sender: CommandSender, lootPoolMap: Map<String, LootPool>) {
    if (lootPoolMap.containsKey(key)) return
    throw CommandAPIBukkit.failWithAdventureComponent(
        sender.placeholderComponent("error-lootpool-not-found", "key" replace key).get()
    )
}

fun assertLootPoolNotExists(key: String, sender: CommandSender, lootPoolMap: Map<String, LootPool>) {
    if (!lootPoolMap.containsKey(key)) return
    throw CommandAPIBukkit.failWithAdventureComponent(
        sender.placeholderComponent("error-lootpool-already-exists", "key" replace key).get()
    )
}