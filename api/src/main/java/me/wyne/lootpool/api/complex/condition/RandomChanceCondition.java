package me.wyne.lootpool.api.complex.condition;

import me.wyne.lootpool.api.complex.LootRollContext;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Passes with a fixed probability, the equivalent of vanilla's {@code random_chance} predicate.
 * Needs nothing from the context, so it is the one condition that behaves identically on every
 * code path.
 *
 * @param chance the probability of passing, clamped to {@code [0, 1]}; {@code 0} never passes and {@code 1} always does
 */
@SerializableAs("RandomChanceCondition")
public record RandomChanceCondition(double chance) implements ConfigurationSerializable, LootCondition {

    @Override
    public boolean test(@NotNull LootRollContext context) {
        if (chance <= 0)
            return false;
        if (chance >= 1)
            return true;
        return ThreadLocalRandom.current().nextDouble() < chance;
    }

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("chance", chance);
        return data;
    }

    /**
     * Reconstructs a {@code RandomChanceCondition} from a {@link #serialize()}-style map.
     */
    @NotNull
    public static RandomChanceCondition deserialize(@NotNull Map<String, Object> args) {
        return new RandomChanceCondition(NumberConversions.toDouble(args.get("chance")));
    }

}
