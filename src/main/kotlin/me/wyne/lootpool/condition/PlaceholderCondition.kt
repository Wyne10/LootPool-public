package me.wyne.lootpool.condition

import me.clip.placeholderapi.PlaceholderAPI
import me.wyne.lootpool.LootPool
import me.wyne.lootpool.api.complex.condition.LootCondition
import me.wyne.lootpool.api.complex.LootRollContext
import org.bukkit.Bukkit
import org.bukkit.configuration.serialization.ConfigurationSerializable
import org.bukkit.configuration.serialization.SerializableAs

@SerializableAs("PlaceholderCondition")
data class PlaceholderCondition(
    val placeholder: String,
    val operator: Operator,
    val value: String
) : ConfigurationSerializable, LootCondition {

    enum class Operator(val argument: String) {
        EQUALS("=="),
        NOT_EQUALS("!="),
        CONTAINS("contains"),
        GREATER(">"),
        GREATER_OR_EQUAL(">="),
        LESS("<"),
        LESS_OR_EQUAL("<=");

        companion object {
            fun of(argument: String): Operator? =
                entries.firstOrNull { it.argument.equals(argument, true) || it.name.equals(argument, true) }
        }
    }

    override fun test(context: LootRollContext): Boolean {
        val player = context.player()
        if (!available) return false

        val resolved = PlaceholderAPI.setPlaceholders(player, placeholder)
        val expected = PlaceholderAPI.setPlaceholders(player, value)

        return when (operator) {
            Operator.EQUALS -> resolved.equals(expected, true)
            Operator.NOT_EQUALS -> !resolved.equals(expected, true)
            Operator.CONTAINS -> resolved.contains(expected, true)
            Operator.GREATER -> compare(resolved, expected) { a, b -> a > b }
            Operator.GREATER_OR_EQUAL -> compare(resolved, expected) { a, b -> a >= b }
            Operator.LESS -> compare(resolved, expected) { a, b -> a < b }
            Operator.LESS_OR_EQUAL -> compare(resolved, expected) { a, b -> a <= b }
        }
    }

    private fun compare(left: String, right: String, comparison: (Double, Double) -> Boolean): Boolean {
        val a = left.trim().toDoubleOrNull() ?: return false
        val b = right.trim().toDoubleOrNull() ?: return false
        return comparison(a, b)
    }

    override fun serialize(): MutableMap<String, Any> =
        mutableMapOf(
            "placeholder" to placeholder,
            "operator" to operator.name,
            "value" to value
        )

    companion object {
        private val available: Boolean by lazy {
            val present = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null
            if (!present)
                LootPool.logger.warn("PlaceholderAPI not found, placeholder conditions never pass")
            present
        }

        @JvmStatic
        fun deserialize(args: Map<String, Any>): PlaceholderCondition =
            PlaceholderCondition(
                args["placeholder"]?.toString() ?: "",
                Operator.of(args["operator"]?.toString() ?: "") ?: Operator.EQUALS,
                args["value"]?.toString() ?: ""
            )
    }

}
