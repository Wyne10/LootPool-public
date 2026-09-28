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
 * Passes when at least one of its {@code conditions} passes, the equivalent of vanilla's
 * {@code any_of} predicate. An empty list fails, since there is nothing that could pass.
 *
 * @param conditions the terms, any one of which is enough to pass
 */
@SerializableAs("AnyOfCondition")
public record AnyOfCondition(@NotNull List<@NotNull LootCondition> conditions) implements ConfigurationSerializable, LootCondition {

    public AnyOfCondition {
        conditions = List.copyOf(conditions);
    }

    @Override
    public boolean test(@NotNull LootRollContext context) {
        for (LootCondition condition : conditions) {
            if (condition.test(context))
                return true;
        }
        return false;
    }

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("conditions", LootSerialization.serializeList(conditions));
        return data;
    }

    /**
     * Reconstructs an {@code AnyOfCondition} from a {@link #serialize()}-style map.
     */
    @NotNull
    public static AnyOfCondition deserialize(@NotNull Map<String, Object> args) {
        return new AnyOfCondition(LootSerialization.deserializeList(args.get("conditions"), LootCondition.class));
    }

}
