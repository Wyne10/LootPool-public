package me.wyne.lootpool.core

import me.wyne.lootpool.api.complex.condition.AllOfCondition
import me.wyne.lootpool.api.complex.condition.AnyOfCondition
import me.wyne.lootpool.api.complex.ComplexLootPool
import me.wyne.lootpool.api.complex.modifier.ConditionalModifier
import me.wyne.lootpool.api.complex.modifier.DamageModifier
import me.wyne.lootpool.api.complex.EmptyEntry
import me.wyne.lootpool.api.complex.modifier.EnchantModifier
import me.wyne.lootpool.api.complex.condition.InvertedCondition
import me.wyne.lootpool.api.complex.ItemEntry
import me.wyne.lootpool.api.complex.condition.LootCondition
import me.wyne.lootpool.api.complex.LootEntry
import me.wyne.lootpool.api.complex.modifier.LootModifier
import me.wyne.lootpool.api.complex.LootRoll
import me.wyne.lootpool.api.complex.modifier.LoreModifier
import me.wyne.lootpool.api.complex.modifier.NameModifier
import me.wyne.lootpool.api.complex.condition.PermissionCondition
import me.wyne.lootpool.api.complex.PoolEntry
import me.wyne.lootpool.api.complex.condition.RandomChanceCondition
import me.wyne.lootpool.api.complex.modifier.SetAttributesModifier
import me.wyne.lootpool.api.complex.condition.TimeCondition
import me.wyne.lootpool.api.complex.condition.WeatherCondition
import me.wyne.lootpool.api.complex.condition.WorldCondition
import me.wyne.lootpool.condition.PlaceholderCondition

const val POOL_PATH = "pool"

class ComplexOwner(
    val path: String,
    val modifiers: ListHandle<LootModifier>?,
    val conditions: ListHandle<LootCondition>
)

class ComplexPathException(path: String) : RuntimeException("Invalid complex loot pool path: $path")

class ComplexPoolEditor(initial: ComplexLootPool, private val onWrite: (ComplexLootPool) -> Unit) {

    var pool: ComplexLootPool = initial
        private set

    private fun update(updated: ComplexLootPool) {
        pool = updated
        onWrite(updated)
    }

    val rolls: ListHandle<LootRoll> = ListHandle(
        { pool.rolls() },
        { update(ComplexLootPool(pool.key(), it, pool.modifiers(), pool.conditions())) }
    )

    val modifiers: ListHandle<LootModifier> = ListHandle(
        { pool.modifiers() },
        { update(ComplexLootPool(pool.key(), pool.rolls(), it, pool.conditions())) }
    )

    val conditions: ListHandle<LootCondition> = ListHandle(
        { pool.conditions() },
        { update(ComplexLootPool(pool.key(), pool.rolls(), pool.modifiers(), it)) }
    )

    fun entries(rollIndex: Int): ListHandle<LootEntry> =
        rolls.nested(rollIndex, { it.entries() }) { roll, list ->
            LootRoll(roll.minRolls(), roll.maxRolls(), list, roll.modifiers(), roll.conditions())
        }

    fun rollModifiers(rollIndex: Int): ListHandle<LootModifier> =
        rolls.nested(rollIndex, { it.modifiers() }) { roll, list ->
            LootRoll(roll.minRolls(), roll.maxRolls(), roll.entries(), list, roll.conditions())
        }

    fun rollConditions(rollIndex: Int): ListHandle<LootCondition> =
        rolls.nested(rollIndex, { it.conditions() }) { roll, list ->
            LootRoll(roll.minRolls(), roll.maxRolls(), roll.entries(), roll.modifiers(), list)
        }

    fun entryModifiers(rollIndex: Int, entryIndex: Int): ListHandle<LootModifier>? {
        if (entries(rollIndex).getOrNull(entryIndex) is EmptyEntry) return null
        return entries(rollIndex).nested(entryIndex, { it.modifiers() }, ::withModifiers)
    }

    fun entryConditions(rollIndex: Int, entryIndex: Int): ListHandle<LootCondition> =
        entries(rollIndex).nested(entryIndex, { it.conditions() }, ::withConditions)

    /**
     * Resolves a dot-separated owner path to the modifier and condition lists it names.
     *
     * `pool`, `0`, `0.2`, `0.c1`, `0.2.m0`, `0.2.m0.c1` — a bare integer is a roll index at the
     * first level and an entry index at the second, `c<i>` descends into a composite condition's
     * children and `m<i>` into a conditional modifier's conditions.
     */
    fun resolve(path: String): ComplexOwner {
        val parts = path.trim().split('.').filter { it.isNotEmpty() }
        if (parts.isEmpty()) throw ComplexPathException(path)

        var modifiers: ListHandle<LootModifier>?
        var conditions: ListHandle<LootCondition>
        var consumed: Int

        if (parts[0].equals(POOL_PATH, true)) {
            modifiers = this.modifiers
            conditions = this.conditions
            consumed = 1
        } else {
            val rollIndex = parts[0].toIntOrNull() ?: throw ComplexPathException(path)
            if (rollIndex !in rolls.indices) throw ComplexPathException(path)

            val entryIndex = parts.getOrNull(1)?.toIntOrNull()
            if (entryIndex != null) {
                if (entryIndex !in entries(rollIndex).indices) throw ComplexPathException(path)
                modifiers = entryModifiers(rollIndex, entryIndex)
                conditions = entryConditions(rollIndex, entryIndex)
                consumed = 2
            } else {
                modifiers = rollModifiers(rollIndex)
                conditions = rollConditions(rollIndex)
                consumed = 1
            }
        }

        while (consumed < parts.size) {
            val part = parts[consumed]
            val index = part.drop(1).toIntOrNull() ?: throw ComplexPathException(path)
            when (part.firstOrNull()) {
                'c' -> {
                    val owner = conditions
                    when (owner.getOrNull(index)) {
                        is AllOfCondition -> conditions = owner.nested(index, { (it as AllOfCondition).conditions() }) { _, list -> AllOfCondition(list) }
                        is AnyOfCondition -> conditions = owner.nested(index, { (it as AnyOfCondition).conditions() }) { _, list -> AnyOfCondition(list) }
                        else -> throw ComplexPathException(path)
                    }
                    modifiers = null
                }
                'm' -> {
                    val owner = modifiers ?: throw ComplexPathException(path)
                    if (owner.getOrNull(index) !is ConditionalModifier) throw ComplexPathException(path)
                    conditions = owner.nested(index, { (it as ConditionalModifier).conditions() }) { modifier, list ->
                        ConditionalModifier((modifier as ConditionalModifier).modifier(), list)
                    }
                    modifiers = null
                }
                else -> throw ComplexPathException(path)
            }
            consumed++
        }

        return ComplexOwner(path, modifiers, conditions)
    }

    fun suggest(): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        result += POOL_PATH to "The pool itself — ${pool.modifiers().size} modifier(s), ${pool.conditions().size} condition(s)"
        collect(POOL_PATH, modifiers, conditions, result, 0)

        rolls.items.forEachIndexed { rollIndex, roll ->
            val rollPath = rollIndex.toString()
            result += rollPath to "Roll $rollIndex [${roll.minRolls()}..${roll.maxRolls()}] — ${roll.entries().size} entry(s)"
            collect(rollPath, rollModifiers(rollIndex), rollConditions(rollIndex), result, 0)

            roll.entries().forEachIndexed { entryIndex, entry ->
                val entryPath = "$rollIndex.$entryIndex"
                result += entryPath to describe(entry)
                collect(entryPath, entryModifiers(rollIndex, entryIndex), entryConditions(rollIndex, entryIndex), result, 0)
            }
        }
        return result
    }

    private fun collect(
        prefix: String,
        modifiers: ListHandle<LootModifier>?,
        conditions: ListHandle<LootCondition>,
        result: MutableList<Pair<String, String>>,
        depth: Int
    ) {
        if (depth >= MAX_SUGGEST_DEPTH) return

        modifiers?.items?.forEachIndexed { index, modifier ->
            if (modifier !is ConditionalModifier) return@forEachIndexed
            val path = "$prefix.m$index"
            result += path to "Conditions of ${describe(modifier)}"
            collect(path, null, modifiers.nested(index, { (it as ConditionalModifier).conditions() }) { m, list ->
                ConditionalModifier((m as ConditionalModifier).modifier(), list)
            }, result, depth + 1)
        }

        conditions.items.forEachIndexed { index, condition ->
            val child = when (condition) {
                is AllOfCondition -> conditions.nested(index, { (it as AllOfCondition).conditions() }) { _, list -> AllOfCondition(list) }
                is AnyOfCondition -> conditions.nested(index, { (it as AnyOfCondition).conditions() }) { _, list -> AnyOfCondition(list) }
                else -> return@forEachIndexed
            }
            val path = "$prefix.c$index"
            result += path to "Children of ${describe(condition)}"
            collect(path, null, child, result, depth + 1)
        }
    }

    companion object {
        private const val MAX_SUGGEST_DEPTH = 3
    }

}

fun withModifiers(entry: LootEntry, modifiers: List<LootModifier>): LootEntry = when (entry) {
    is ItemEntry -> ItemEntry(entry.loot(), entry.conditions(), modifiers)
    is PoolEntry -> PoolEntry(entry.pool(), entry.weight(), entry.conditions(), modifiers)
    else -> entry
}

fun withConditions(entry: LootEntry, conditions: List<LootCondition>): LootEntry = when (entry) {
    is ItemEntry -> ItemEntry(entry.loot(), conditions, entry.modifiers())
    is PoolEntry -> PoolEntry(entry.pool(), entry.weight(), conditions, entry.modifiers())
    is EmptyEntry -> EmptyEntry(entry.weight(), conditions)
    else -> entry
}

fun withWeight(entry: LootEntry, weight: Int): LootEntry = when (entry) {
    is ItemEntry -> ItemEntry(
        me.wyne.lootpool.api.Loot(entry.loot().item(), weight, entry.loot().minAmount(), entry.loot().maxAmount()),
        entry.conditions(), entry.modifiers()
    )
    is PoolEntry -> PoolEntry(entry.pool(), weight, entry.conditions(), entry.modifiers())
    is EmptyEntry -> EmptyEntry(weight, entry.conditions())
    else -> entry
}

fun describe(entry: LootEntry): String = when (entry) {
    is ItemEntry -> "Item ${entry.loot().item().type} x${entry.loot().minAmount()}-${entry.loot().maxAmount()} (weight ${entry.weight()})"
    is PoolEntry -> "Pool '${entry.pool()}' (weight ${entry.weight()})"
    is EmptyEntry -> "Nothing (weight ${entry.weight()})"
    else -> "${entry.javaClass.simpleName} (weight ${entry.weight()})"
}

fun describe(modifier: LootModifier): String = when (modifier) {
    is ConditionalModifier -> "Conditional ${describe(modifier.modifier())}"
    is EnchantModifier -> "Enchant from '${modifier.pool()}' x${modifier.minEnchants()}-${modifier.maxEnchants()}"
    is DamageModifier -> "Damage ${percent(modifier.minFraction())}-${percent(modifier.maxFraction())}"
    is SetAttributesModifier -> "Attributes (${modifier.entries().size})"
    is NameModifier -> "Name '${modifier.text()}'"
    is LoreModifier -> "Lore ${modifier.mode().name.lowercase()} (${modifier.lines().size} line(s))"
    else -> modifier.javaClass.simpleName
}

fun describe(condition: LootCondition): String = when (condition) {
    is RandomChanceCondition -> "Chance ${percent(condition.chance())}"
    is AllOfCondition -> "All of (${condition.conditions().size})"
    is AnyOfCondition -> "Any of (${condition.conditions().size})"
    is InvertedCondition -> "Not ${describe(condition.condition())}"
    is PermissionCondition -> "Permission '${condition.node()}'"
    is WorldCondition -> "World in ${condition.worlds().joinToString(", ")}"
    is TimeCondition -> "Time ${condition.minTime()}..${condition.maxTime()}"
    is WeatherCondition -> "Weather rain=${condition.raining() ?: "any"} thunder=${condition.thundering() ?: "any"}"
    is PlaceholderCondition -> "Placeholder '${condition.placeholder}' ${condition.operator.argument} '${condition.value}'"
    else -> condition.javaClass.simpleName
}

private fun percent(fraction: Double): String = "${Math.round(fraction * 100)}%"
