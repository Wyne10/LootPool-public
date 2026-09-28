package me.wyne.lootpool.api.complex;

import me.wyne.lootpool.api.Loot;
import me.wyne.lootpool.api.complex.condition.LootCondition;
import me.wyne.lootpool.api.complex.modifier.LootModifier;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * One candidate within a {@link LootRoll}: the unit a single roll selects, by weight, from the
 * roll's entry list. Equivalent to a vanilla loot table entry.
 * <p>
 * An entry may produce any number of items — {@link ItemEntry} produces exactly one,
 * {@link EmptyEntry} none, and {@link PoolEntry} however many the pool it references produces.
 * <p>
 * Implementations must be {@link ConfigurationSerializable} and registered with
 * {@link org.bukkit.configuration.serialization.ConfigurationSerialization}, or they cannot be
 * saved.
 */
public interface LootEntry extends ConfigurationSerializable {

    /**
     * This entry's relative chance of being picked by {@link #getRandom(List, LootRollContext)}.
     * Entries with weight {@code 0} can never be picked.
     */
    int weight();

    /**
     * Conditions that must all pass for this entry to be eligible for selection at all. An entry
     * whose conditions fail is excluded from the weighted draw rather than producing nothing, so the
     * remaining entries keep their relative chances.
     */
    @NotNull
    List<@NotNull LootCondition> conditions();

    /**
     * Modifiers applied to each item this entry produces, before the containing roll's and pool's
     * modifiers.
     */
    @NotNull
    List<@NotNull LootModifier> modifiers();

    /**
     * Produces this entry's items for one roll, <em>without</em> applying {@link #modifiers()} —
     * the caller does that, so that entry, roll and pool modifiers can be applied in one pass.
     *
     * @return the produced items, possibly empty, never {@code null}
     */
    @NotNull
    List<@NotNull ItemStack> generate(@NotNull LootRollContext context);

    /**
     * A best-effort flat view of what this entry can produce, for
     * {@link me.wyne.lootpool.api.LootPool#getLootList()} previews and GUI rendering. Modifiers and
     * conditions are not reflected.
     *
     * @param depth how many further levels of pool references may be resolved; implementations that
     *              delegate must pass {@code depth - 1} and stop at {@code 0}
     * @return the preview entries, possibly empty, never {@code null}
     */
    @NotNull
    List<@NotNull Loot> flatten(int depth);

    /**
     * Weighted-random-selects one entry from {@code entries}, considering only entries whose
     * {@link #conditions()} pass for {@code context}.
     *
     * @return the selected entry, or {@code null} if none are eligible or their total weight is zero
     */
    @Nullable
    static LootEntry getRandom(@NotNull List<@NotNull LootEntry> entries, @NotNull LootRollContext context) {
        List<LootEntry> eligible = new ArrayList<>(entries.size());
        int totalWeight = 0;
        for (LootEntry entry : entries) {
            if (entry.weight() <= 0)
                continue;
            if (!LootCondition.testAll(entry.conditions(), context))
                continue;
            eligible.add(entry);
            totalWeight += entry.weight();
        }

        if (totalWeight == 0)
            return null;

        int randomValue = ThreadLocalRandom.current().nextInt(totalWeight);

        int currentSum = 0;
        for (LootEntry entry : eligible) {
            currentSum += entry.weight();
            if (randomValue < currentSum) {
                return entry;
            }
        }

        return null;
    }

}
