package me.wyne.lootpool.api.complex.condition;

import me.wyne.lootpool.api.complex.LootRollContext;
import me.wyne.lootpool.api.complex.LootSerialization;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Passes when every one of its {@code conditions} passes, the equivalent of vanilla's
 * {@code all_of} predicate. An empty list passes, matching the behaviour of an absent condition.
 *
 * @param conditions the terms, all of which must pass
 */
@SerializableAs("AllOfCondition")
public record AllOfCondition(@NotNull List<@NotNull LootCondition> conditions) implements ConfigurationSerializable, LootCondition {

    public AllOfCondition {
        conditions = List.copyOf(conditions);
    }

    @Override
    public boolean test(@NotNull LootRollContext context) {
        return LootCondition.testAll(conditions, context);
    }

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("conditions", LootSerialization.serializeList(conditions));
        return data;
    }

    /**
     * Reconstructs an {@code AllOfCondition} from a {@link #serialize()}-style map.
     */
    @NotNull
    public static AllOfCondition deserialize(@NotNull Map<String, Object> args) {
        return new AllOfCondition(LootSerialization.deserializeList(args.get("conditions"), LootCondition.class));
    }

}
