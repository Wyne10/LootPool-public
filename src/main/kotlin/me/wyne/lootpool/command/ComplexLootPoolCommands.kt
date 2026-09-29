@file:Suppress("UNCHECKED_CAST")

package me.wyne.lootpool.command

import dev.jorel.commandapi.CommandAPIBukkit
import dev.jorel.commandapi.StringTooltip
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.CommandArguments
import me.wyne.lootpool.api.LootPool
import me.wyne.lootpool.api.LootPoolProvider
import me.wyne.lootpool.api.complex.condition.AllOfCondition
import me.wyne.lootpool.api.complex.condition.AnyOfCondition
import me.wyne.lootpool.api.complex.ComplexLootPool
import me.wyne.lootpool.api.complex.modifier.ConditionalModifier
import me.wyne.lootpool.core.ComplexOwner
import me.wyne.lootpool.core.ComplexPathException
import me.wyne.lootpool.core.ComplexPoolEditor
import me.wyne.lootpool.core.POOL_PATH
import me.wyne.lootpool.core.describe
import me.wyne.wutils.i18n.kotlin.placeholderComponent
import me.wyne.wutils.i18n.kotlin.replace
import me.wyne.wutils.i18n.kotlin.replaceComponent
import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender

fun displayComplexInfo(sender: CommandSender, pool: ComplexLootPool, amount: Int, depth: Int) {
    val editor = ComplexPoolEditor(pool) {}
    val key = pool.key()
    val lines = mutableListOf<Component>()

    fun node(level: Int, label: String, description: String, command: String) {
        lines.add(
            sender.placeholderComponent(
                "info-complex-node",
                "indent" replace "  ".repeat(level),
                "path" replace label,
                "description" replace description,
                "command" replace command
            ).get()
        )
    }

    /**
     * Renders the modifier and condition lists a path owns, then descends into the conditional
     * modifiers and condition groups among them, which own lists of their own.
     */
    fun details(level: Int, path: String, nesting: Int) {
        if (depth < 3 || nesting > MAX_INFO_NESTING) return
        val owner = runCatching { editor.resolve(path) }.getOrNull() ?: return

        owner.modifiers?.items?.forEachIndexed { index, modifier ->
            node(level, "$path m$index", describe(modifier), "/lootpool complex modifier remove $key $path $index")
            if (modifier is ConditionalModifier)
                details(level + 1, "$path.m$index", nesting + 1)
        }
        owner.conditions.items.forEachIndexed { index, condition ->
            node(level, "$path c$index", describe(condition), "/lootpool complex condition remove $key $path $index")
            if (condition is AllOfCondition || condition is AnyOfCondition)
                details(level + 1, "$path.c$index", nesting + 1)
        }
    }

    details(0, POOL_PATH, 0)

    editor.rolls.items.forEachIndexed { rollIndex, roll ->
        node(
            0,
            rollIndex.toString(),
            "Roll [${roll.minRolls()}..${roll.maxRolls()}] - ${roll.entries().size} entry(s)",
            "/lootpool complex roll set $key $rollIndex "
        )
        if (depth < 2) return@forEachIndexed
        details(1, rollIndex.toString(), 0)

        val totalWeight = roll.entries().sumOf { it.weight().toDouble() }
        val shown = roll.entries().take(amount)
        shown.forEachIndexed { entryIndex, entry ->
            val percentage = if (totalWeight == 0.0) 0.0 else (entry.weight() / totalWeight) * 100
            node(
                1,
                "$rollIndex.$entryIndex",
                "${describe(entry)} ${String.format("%.2f", percentage)}%",
                "/lootpool complex entry remove $key $rollIndex $entryIndex"
            )
            details(2, "$rollIndex.$entryIndex", 0)
        }
        val remaining = roll.entries().size - shown.size
        if (remaining > 0)
            lines.add(
                sender.placeholderComponent("info-complex-more", "indent" replace "  ", "amount" replace remaining).get()
            )
    }

    sender.placeholderComponent("info-complex", "key" replace key, "rolls" replace pool.rolls().size)
        .replace("node-list" replaceComponent (lines.reduceOrNull { a, b -> a.append(Component.newline()).append(b) } ?: Component.empty()))
        .sendMessage(sender)
}

private const val MAX_INFO_NESTING = 3

fun complexPoolKey(nodeName: String, provider: LootPoolProvider): Argument<String> =
    lootPoolKey(nodeName) { provider.getMapOf(ComplexLootPool::class.java) }

fun elementPath(nodeName: String, provider: LootPoolProvider): Argument<String> =
    StringArgument(nodeName)
        .replaceSuggestions(ArgumentSuggestions.stringsWithTooltips { info ->
            val key = info.previousArgs().getByClass("key", String::class.java)
                ?: return@stringsWithTooltips emptyArray()
            val pool = provider.getLootPool(key) as? ComplexLootPool
                ?: return@stringsWithTooltips emptyArray()
            ComplexPoolEditor(pool) {}.suggest()
                .map { StringTooltip.ofString(it.key, it.value) }
                .toTypedArray()
        })

fun editorFor(args: CommandArguments, sender: CommandSender, provider: LootPoolProvider): ComplexPoolEditor {
    val key = args.getByClass("key", String::class.java)!!
    assertLootPoolExists(key, sender, provider.getMapOf(LootPool::class.java))
    val pool = provider.getLootPool(key) as? ComplexLootPool
        ?: throw CommandAPIBukkit.failWithAdventureComponent(
            sender.placeholderComponent("error-not-a-complex-lootpool", "key" replace key).get()
        )
    return ComplexPoolEditor(pool) { provider.writeLootPool(it) }
}

fun ownerFor(args: CommandArguments, sender: CommandSender, provider: LootPoolProvider): ComplexOwner {
    val editor = editorFor(args, sender, provider)
    val path = args.getByClass("path", String::class.java)!!
    return try {
        editor.resolve(path)
    } catch (e: ComplexPathException) {
        throw CommandAPIBukkit.failWithAdventureComponent(
            sender.placeholderComponent("error-invalid-path", "path" replace path).get()
        )
    }
}

fun rollIndex(editor: ComplexPoolEditor, args: CommandArguments, sender: CommandSender): Int {
    val index = args.getByClass("roll", Int::class.java)!!
    if (index in editor.rolls.indices) return index
    throw CommandAPIBukkit.failWithAdventureComponent(
        sender.placeholderComponent(
            "error-invalid-roll",
            "key" replace editor.pool.key(),
            "roll" replace index
        ).get()
    )
}

fun entryIndex(editor: ComplexPoolEditor, roll: Int, args: CommandArguments, sender: CommandSender): Int {
    val index = args.getByClass("entry", Int::class.java)!!
    if (index in editor.entries(roll).indices) return index
    throw CommandAPIBukkit.failWithAdventureComponent(
        sender.placeholderComponent("error-invalid-entry", "roll" replace roll, "entry" replace index).get()
    )
}

fun failInvalidIndex(sender: CommandSender, path: String, index: Int): Nothing =
    throw CommandAPIBukkit.failWithAdventureComponent(
        sender.placeholderComponent("error-invalid-index", "path" replace path, "index" replace index).get()
    )

fun failInvalidEntryType(sender: CommandSender, args: CommandArguments): Nothing =
    throw CommandAPIBukkit.failWithAdventureComponent(
        sender.placeholderComponent(
            "error-wrong-entry-type",
            "roll" replace args.getByClassOrDefault("roll", Int::class.java, 0),
            "entry" replace args.getByClassOrDefault("entry", Int::class.java, 0)
        ).get()
    )

fun assertLootNotEmpty(item: org.bukkit.inventory.ItemStack?, sender: CommandSender) {
    if (item != null && item.type != org.bukkit.Material.AIR) return
    throw CommandAPIBukkit.failWithAdventureComponent(
        sender.placeholderComponent("error-empty-loot").get()
    )
}

fun modified(sender: CommandSender, args: CommandArguments) {
    sender.placeholderComponent(
        "success-lootpool-modify",
        "key" replace args.getByClassOrDefault("key", String::class.java, "")
    ).sendMessage(sender)
}