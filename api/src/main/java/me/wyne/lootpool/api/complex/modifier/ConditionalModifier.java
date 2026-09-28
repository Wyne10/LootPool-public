package me.wyne.lootpool.api.complex.modifier;

import me.wyne.lootpool.api.complex.LootRollContext;
import me.wyne.lootpool.api.complex.LootSerialization;
import me.wyne.lootpool.api.complex.condition.LootCondition;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Applies another modifier only when every one of its {@code conditions} passes — the equivalent of
 * a vanilla item modifier's own {@code conditions} field.
 * <p>
 * Conditional application lives here rather than on {@link LootModifier} itself so that
 * implementing a modifier stays a one-method job. Wrap any modifier to make it conditional:
 * <pre>{@code
 * new ConditionalModifier(new EnchantModifier("boss_enchants", 1, 2, true),
 *                         List.of(new RandomChanceCondition(0.25)))
 * }</pre>
 *
 * @param modifier   the modifier to apply when the conditions pass
 * @param conditions the conditions, all of which must pass
 */
@SerializableAs("ConditionalModifier")
public record ConditionalModifier(@NotNull LootModifier modifier,
                                  @NotNull List<@NotNull LootCondition> conditions) implements ConfigurationSerializable, LootModifier {

    public ConditionalModifier {
        conditions = List.copyOf(conditions);
    }

    @Override
    @NotNull
    public ItemStack apply(@NotNull ItemStack item, @NotNull LootRollContext context) {
        return LootCondition.testAll(conditions, context) ? modifier.apply(item, context) : item;
    }

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("modifier", modifier);
        data.put("conditions", LootSerialization.serializeList(conditions));
        return data;
    }

    /**
     * Reconstructs a {@code ConditionalModifier} from a {@link #serialize()}-style map.
     *
     * @return the modifier, or {@code null} if the wrapped modifier cannot be resolved — the caller
     *         is expected to drop it rather than fail the load
     */
    @Nullable
    public static ConditionalModifier deserialize(@NotNull Map<String, Object> args) {
        LootModifier modifier = LootSerialization.deserializeValue(args.get("modifier"), LootModifier.class);
        if (modifier == null)
            return null;
        return new ConditionalModifier(modifier, LootSerialization.deserializeList(args.get("conditions"), LootCondition.class));
    }

}
