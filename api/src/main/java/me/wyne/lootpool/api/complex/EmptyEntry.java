package me.wyne.lootpool.api.complex;

import me.wyne.lootpool.api.Loot;
import me.wyne.lootpool.api.complex.condition.LootCondition;
import me.wyne.lootpool.api.complex.modifier.LootModifier;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link LootEntry} that produces nothing, the equivalent of a vanilla {@code empty} entry. Its
 * weight is what makes a roll able to come up with no drop at all: an entry of weight {@code 3}
 * among entries totalling {@code 12} gives that roll a one-in-four chance of yielding nothing.
 *
 * @param weight     this entry's relative chance of being picked
 * @param conditions conditions that must pass for this entry to be eligible for selection
 */
@SerializableAs("EmptyEntry")
public record EmptyEntry(int weight, @NotNull List<@NotNull LootCondition> conditions) implements ConfigurationSerializable, LootEntry {

    public EmptyEntry {
        conditions = List.copyOf(conditions);
    }

    /**
     * Creates an entry with no conditions.
     */
    public EmptyEntry(int weight) {
        this(weight, List.of());
    }

    @Override
    @NotNull
    public List<@NotNull LootModifier> modifiers() {
        return List.of();
    }

    @Override
    @NotNull
    public List<@NotNull ItemStack> generate(@NotNull LootRollContext context) {
        return List.of();
    }

    @Override
    @NotNull
    public List<@NotNull Loot> flatten(int depth) {
        return List.of();
    }

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("weight", weight);
        data.put("conditions", LootSerialization.serializeList(conditions));
        return data;
    }

    /**
     * Reconstructs an {@code EmptyEntry} from a {@link #serialize()}-style map.
     */
    @NotNull
    public static EmptyEntry deserialize(@NotNull Map<String, Object> args) {
        return new EmptyEntry(
                NumberConversions.toInt(args.get("weight")),
                LootSerialization.deserializeList(args.get("conditions"), LootCondition.class));
    }

}
