package me.wyne.lootpool.api.complex.condition;

import me.wyne.lootpool.api.complex.LootRollContext;
import me.wyne.lootpool.api.complex.LootSerialization;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Passes when its wrapped {@code condition} fails, the equivalent of vanilla's {@code inverted}
 * predicate.
 *
 * @param condition the term to negate
 */
@SerializableAs("InvertedCondition")
public record InvertedCondition(@NotNull LootCondition condition) implements ConfigurationSerializable, LootCondition {

    @Override
    public boolean test(@NotNull LootRollContext context) {
        return !condition.test(context);
    }

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("condition", condition);
        return data;
    }

    /**
     * Reconstructs an {@code InvertedCondition} from a {@link #serialize()}-style map.
     * <p>
     * Falls back to negating a never-passing term — i.e. always passing — if the wrapped condition
     * cannot be resolved, so a single unknown condition type does not fail the whole pool load.
     */
    @NotNull
    public static InvertedCondition deserialize(@NotNull Map<String, Object> args) {
        @Nullable LootCondition condition = LootSerialization.deserializeValue(args.get("condition"), LootCondition.class);
        return new InvertedCondition(condition != null ? condition : new RandomChanceCondition(0));
    }

}
