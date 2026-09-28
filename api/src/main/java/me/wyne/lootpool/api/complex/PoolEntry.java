package me.wyne.lootpool.api.complex;

import me.wyne.lootpool.api.Loot;
import me.wyne.lootpool.api.LootPool;
import me.wyne.lootpool.api.LootPoolApi;
import me.wyne.lootpool.api.LootPoolProvider;
import me.wyne.lootpool.api.RollLootPool;
import me.wyne.lootpool.api.complex.condition.LootCondition;
import me.wyne.lootpool.api.complex.modifier.LootModifier;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link LootEntry} that delegates to another registered pool, the equivalent of a vanilla
 * {@code loot_table} entry. Use it to compose pools: a "tools" roll can reference an existing
 * {@code tools} pool rather than duplicating its entries.
 * <p>
 * How many items the referenced pool produces is the pool's own business: a
 * {@link ContextualLootPool} such as another {@link ComplexLootPool} rolls its natural output, a
 * {@link RollLootPool} rolls its own roll count, and any other pool produces a single weighted
 * draw. Dangling references produce nothing rather than failing.
 *
 * @param pool       the registry key of the pool to delegate to
 * @param weight     this entry's relative chance of being picked
 * @param conditions conditions that must pass for this entry to be eligible for selection
 * @param modifiers  modifiers applied to every item the referenced pool produces
 */
@SerializableAs("PoolEntry")
public record PoolEntry(@NotNull String pool, int weight,
                        @NotNull List<@NotNull LootCondition> conditions,
                        @NotNull List<@NotNull LootModifier> modifiers) implements ConfigurationSerializable, LootEntry {

    public PoolEntry {
        conditions = List.copyOf(conditions);
        modifiers = List.copyOf(modifiers);
    }

    /**
     * Creates an entry with no conditions or modifiers.
     */
    public PoolEntry(@NotNull String pool, int weight) {
        this(pool, weight, List.of(), List.of());
    }

    /**
     * Resolves {@link #pool()} in the registry.
     *
     * @return the referenced pool, or {@code null} if it does not currently resolve
     */
    @Nullable
    public LootPool getLootPool() {
        LootPoolProvider provider = LootPoolApi.getProvider();
        return provider != null ? provider.getLootPool(pool) : null;
    }

    @Override
    @NotNull
    public List<@NotNull ItemStack> generate(@NotNull LootRollContext context) {
        LootPool lootPool = getLootPool();
        if (lootPool == null)
            return List.of();
        if (lootPool instanceof ContextualLootPool contextual)
            return contextual.populate(context);
        if (lootPool instanceof RollLootPool rollLootPool)
            return rollLootPool.populate(rollLootPool.rollSlots());
        return lootPool.populate(1);
    }

    @Override
    @NotNull
    public List<@NotNull Loot> flatten(int depth) {
        if (depth <= 0)
            return List.of();
        LootPool lootPool = getLootPool();
        return lootPool != null ? lootPool.getLootList() : List.of();
    }

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pool", pool);
        data.put("weight", weight);
        data.put("conditions", LootSerialization.serializeList(conditions));
        data.put("modifiers", LootSerialization.serializeList(modifiers));
        return data;
    }

    /**
     * Reconstructs a {@code PoolEntry} from a {@link #serialize()}-style map.
     */
    @NotNull
    public static PoolEntry deserialize(@NotNull Map<String, Object> args) {
        Object pool = args.get("pool");
        return new PoolEntry(
                pool != null ? pool.toString() : "",
                NumberConversions.toInt(args.get("weight")),
                LootSerialization.deserializeList(args.get("conditions"), LootCondition.class),
                LootSerialization.deserializeList(args.get("modifiers"), LootModifier.class));
    }

}
