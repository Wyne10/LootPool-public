package me.wyne.lootpool.api.complex.condition;

import me.wyne.lootpool.api.complex.*;
import me.wyne.lootpool.api.complex.modifier.ConditionalModifier;
import me.wyne.lootpool.api.complex.modifier.LootModifier;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A predicate gating part of a {@link ComplexLootPool}, equivalent to a vanilla loot table
 * condition. The same interface is used at every level: a condition can gate the whole pool, a
 * single {@link LootRoll}, an individual {@link LootEntry}, or — via {@link ConditionalModifier} —
 * one {@link LootModifier}.
 * <p>
 * Implementations must be {@link ConfigurationSerializable} and registered with
 * {@link org.bukkit.configuration.serialization.ConfigurationSerialization}, or they cannot be
 * saved. Implementations are expected to be immutable and safe to evaluate repeatedly.
 * <p>
 * A condition that cannot be evaluated because {@code context} lacks what it needs should
 * <em>fail closed</em> (return {@code false}) rather than guess.
 */
public interface LootCondition extends ConfigurationSerializable {

    /**
     * @return whether this condition passes for {@code context}
     */
    boolean test(@NotNull LootRollContext context);

    /**
     * Tests every condition in {@code conditions}, short-circuiting on the first failure.
     *
     * @return {@code true} if all of them pass, including when {@code conditions} is empty
     */
    static boolean testAll(@NotNull List<@NotNull LootCondition> conditions, @NotNull LootRollContext context) {
        for (LootCondition condition : conditions) {
            if (!condition.test(context))
                return false;
        }
        return true;
    }

}
