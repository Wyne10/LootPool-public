package me.wyne.lootpool.api;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * A {@link LootPool} of other registered pools, referenced by registry key with a per-pool
 * weight. Each {@link #getRandom()} or {@code populate}/{@code populateRandomly} call first
 * weighted-randomly picks a single whole sub-pool via {@link #getRandomLootPool()}, then delegates
 * entirely to it for that call - so items from other sub-pools never mix into the same result.
 * Contrast with {@link MultiLootPool}, which merges every referenced pool's entries into one flat
 * pool; pick {@code CompositeLootPool} when you want entire pools to compete with each other
 * (e.g. "80% chance of the common table, 20% chance of the rare table"), and {@code MultiLootPool}
 * when you want their entries combined into a single table.
 * <p>
 * {@link #getLootList()} is the exception: it returns the flattened union of every resolved
 * sub-pool's entries (like {@link MultiLootPool#getLootList()}), for listing/inspection purposes,
 * even though actual rolls only ever draw from one sub-pool at a time.
 * <p>
 * Referenced pools are resolved lazily via {@link LootPoolApi#getProvider()} on every call. Keys
 * in {@link #lootPools()} that don't currently resolve to a registered pool are silently skipped.
 *
 * @param key       this pool's identifier in the plugin's registry
 * @param lootPools the registry keys of the candidate sub-pools, mapped to their selection weight
 */
public record CompositeLootPool(@NotNull String key, @NotNull Map<@NotNull String, @NotNull Integer> lootPools) implements ConfigurationSerializable, LootPool {

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("key", key);
        data.putAll(lootPools);
        return data;
    }

    /**
     * Reconstructs a {@code CompositeLootPool} from a {@link #serialize()}-style map: {@code "key"}
     * plus one weight entry per referenced pool, keyed by that pool's registry key.
     */
    @NotNull
    public static CompositeLootPool deserialize(@NotNull Map<String, Object> args) {
        Map<String, Integer> lootPools = new HashMap<>();
        for (Map.Entry<String, Object> lootPool : args.entrySet()) {
            if (lootPool.getKey().equals("key") || lootPool.getKey().equals("==")) continue;
            lootPools.put(lootPool.getKey(), (int) lootPool.getValue());
        }
        return new CompositeLootPool((String) args.get("key"), Map.copyOf(lootPools));
    }

    /**
     * Resolves {@link #lootPools()} to the currently registered {@link LootPool} instances,
     * silently dropping any key that no longer resolves.
     *
     * @return the resolved sub-pools mapped to their configured selection weight
     */
    @SuppressWarnings("DataFlowIssue")
    @NotNull
    public Map<@NotNull LootPool, @NotNull Integer> getLootPools() {
        return lootPools.entrySet()
                .stream()
                .filter(entry -> LootPoolApi.getProvider().getLootPoolMap().containsKey(entry.getKey()))
                .collect(Collectors.toUnmodifiableMap(entry -> LootPoolApi.getProvider().getLootPool(entry.getKey()), Map.Entry::getValue));
    }

    /**
     * Returns the flattened union of every resolved sub-pool's {@link LootPool#getLootList()},
     * for inspection purposes. This does not reflect the actual roll behavior, which draws from
     * only one sub-pool per call; see {@link #getRandomLootPool()}.
     */
    @Override
    public @NotNull List<@NotNull Loot> getLootList() {
        return getLootPools().keySet().stream()
                .flatMap(lootPool -> lootPool.getLootList().stream())
                .toList();
    }

    /**
     * Weighted-randomly selects one resolved sub-pool, where each pool's chance is proportional
     * to its configured weight in {@link #lootPools()}.
     *
     * @return the selected sub-pool, or {@link BasicLootPool#EMPTY} if no sub-pools resolve or their total weight is zero
     */
    @NotNull
    public LootPool getRandomLootPool() {
        var filteredLootPools = getLootPools();
        int totalWeight = filteredLootPools.values().stream().mapToInt(Integer::intValue).sum();

        if (totalWeight == 0)
            return BasicLootPool.EMPTY;

        int randomValue = ThreadLocalRandom.current().nextInt(totalWeight);

        int currentSum = 0;
        for (Map.Entry<LootPool, Integer> lootPool : filteredLootPools.entrySet()) {
            currentSum += lootPool.getValue();
            if (randomValue < currentSum) {
                return lootPool.getKey();
            }
        }

        return BasicLootPool.EMPTY;
    }

    /**
     * Picks one sub-pool via {@link #getRandomLootPool()} and returns its random entry.
     */
    @Override
    public @NotNull Loot getRandom() {
        return getRandomLootPool().getRandom();
    }

    /**
     * Picks one sub-pool via {@link #getRandomLootPool()} and rolls all {@code slots} items from
     * it, rather than from a mix of the referenced pools.
     */
    @Override
    public @NotNull List<@NotNull ItemStack> populate(int slots) {
        return getRandomLootPool().populate(slots);
    }

    /**
     * Picks one sub-pool via {@link #getRandomLootPool()} and delegates the entire call to it.
     */
    @Override
    public @NotNull List<@NotNull ItemStack> populate(@NotNull Inventory inventory, int slots) {
        return getRandomLootPool().populate(inventory, slots);
    }

    /**
     * Picks one sub-pool via {@link #getRandomLootPool()} and delegates the entire call to it.
     */
    @Override
    public @NotNull List<@NotNull ItemStack> populateRandomly(@NotNull Inventory inventory, int slots) {
        return getRandomLootPool().populateRandomly(inventory, slots);
    }

    /**
     * Returns this pool's synthetic {@code "lootpool:" + key} registry key.
     */
    @SuppressWarnings("DataFlowIssue")
    @Override
    public @NotNull NamespacedKey getKey() {
        return NamespacedKey.fromString("lootpool:" + key);
    }

}

