package me.wyne.lootpool.api.complex;

import me.wyne.lootpool.api.Loot;
import me.wyne.lootpool.api.complex.condition.LootCondition;
import me.wyne.lootpool.api.complex.modifier.LootModifier;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * One roll group inside a {@link ComplexLootPool}, the equivalent of a vanilla loot table "pool".
 * <p>
 * A roll draws a random number of times within {@code [minRolls, maxRolls]}, and each draw picks
 * one {@link LootEntry} by weight. Because a pool holds several of these, a drop's composition
 * becomes declarative — "3 to 5 commons, then 1 tool, then exactly 1 boss item" is three rolls
 * rather than one list of weights you have to reason about statistically.
 *
 * @param minRolls   the inclusive lower bound of the number of draws
 * @param maxRolls   the inclusive upper bound of the number of draws
 * @param entries    the candidates, one of which is picked per draw
 * @param modifiers  modifiers applied to every item this roll produces, after each entry's own
 * @param conditions conditions that must all pass for this roll to happen at all
 */
@SerializableAs("LootRoll")
public record LootRoll(int minRolls, int maxRolls,
                       @NotNull List<@NotNull LootEntry> entries,
                       @NotNull List<@NotNull LootModifier> modifiers,
                       @NotNull List<@NotNull LootCondition> conditions) implements ConfigurationSerializable {

    public LootRoll {
        entries = List.copyOf(entries);
        modifiers = List.copyOf(modifiers);
        conditions = List.copyOf(conditions);
    }

    /**
     * Creates an empty roll drawing once.
     */
    public LootRoll() {
        this(1, 1, List.of(), List.of(), List.of());
    }

    /**
     * Creates a roll with entries but no modifiers or conditions.
     */
    public LootRoll(int minRolls, int maxRolls, @NotNull List<@NotNull LootEntry> entries) {
        this(minRolls, maxRolls, entries, List.of(), List.of());
    }

    /**
     * Rolls how many draws to make, normalized the same way as {@link Loot#rollAmount()}
     * (order-independent, clamped to non-negative).
     *
     * @return the number of draws, or a fixed value if the bounds normalize to the same value
     */
    public int rollCount() {
        int min = Math.max(0, Math.min(minRolls, maxRolls));
        int max = Math.max(0, Math.max(minRolls, maxRolls));
        return min == max ? min : ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    /**
     * Runs this roll: checks {@link #conditions()}, makes {@link #rollCount()} weighted draws over
     * the entries whose own conditions pass, and applies each entry's modifiers followed by this
     * roll's.
     *
     * @return the produced items, possibly empty, never {@code null}
     */
    @NotNull
    public List<@NotNull ItemStack> generate(@NotNull LootRollContext context) {
        if (!LootCondition.testAll(conditions, context))
            return List.of();

        List<ItemStack> result = new ArrayList<>();
        int count = rollCount();
        for (int i = 0; i < count; i++) {
            LootEntry entry = LootEntry.getRandom(entries, context);
            if (entry == null)
                continue;
            for (ItemStack item : entry.generate(context)) {
                ItemStack modified = LootModifier.applyAll(entry.modifiers(), item, context);
                if (modified == null)
                    continue;
                ItemStack finished = LootModifier.applyAll(modifiers, modified, context);
                if (finished != null)
                    result.add(finished);
            }
        }
        return result;
    }

    /**
     * A best-effort flat view of every entry this roll can draw, for previews. Roll counts,
     * modifiers and conditions are not reflected.
     *
     * @param depth how many further levels of pool references may be resolved
     */
    @NotNull
    public List<@NotNull Loot> flatten(int depth) {
        List<Loot> result = new ArrayList<>();
        for (LootEntry entry : entries) {
            result.addAll(entry.flatten(depth));
        }
        return List.copyOf(result);
    }

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("minRolls", minRolls);
        data.put("maxRolls", maxRolls);
        data.put("entries", LootSerialization.serializeList(entries));
        data.put("modifiers", LootSerialization.serializeList(modifiers));
        data.put("conditions", LootSerialization.serializeList(conditions));
        return data;
    }

    /**
     * Reconstructs a {@code LootRoll} from a {@link #serialize()}-style map.
     */
    @NotNull
    public static LootRoll deserialize(@NotNull Map<String, Object> args) {
        return new LootRoll(
                NumberConversions.toInt(args.get("minRolls")),
                NumberConversions.toInt(args.get("maxRolls")),
                LootSerialization.deserializeList(args.get("entries"), LootEntry.class),
                LootSerialization.deserializeList(args.get("modifiers"), LootModifier.class),
                LootSerialization.deserializeList(args.get("conditions"), LootCondition.class));
    }

}
