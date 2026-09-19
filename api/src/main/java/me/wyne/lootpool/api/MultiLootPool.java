package me.wyne.lootpool.api;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A {@link LootPool} that merges the entries of several other registered pools, referenced by
 * their registry keys, into one flat weighted pool. Every roll draws from the union of all
 * referenced pools' entries, so referenced pools with more/heavier entries proportionally
 * dominate the outcome. Contrast with {@link CompositeLootPool}, which picks one whole sub-pool
 * per roll instead of merging their contents; pick {@code MultiLootPool} when you want a single
 * combined drop table, and {@code CompositeLootPool} when you want to weight entire pools against
 * each other.
 * <p>
 * Referenced pools are resolved lazily via {@link LootPoolApi#getProvider()} on every call, so
 * changes to the registry (or to a referenced pool's own contents) are reflected immediately.
 * Keys in {@link #lootPools()} that don't currently resolve to a registered pool are silently
 * skipped rather than causing an error.
 *
 * @param key       this pool's identifier in the plugin's registry
 * @param lootPools the registry keys of the pools to merge
 */
public record MultiLootPool(@NotNull String key, @NotNull Set<@NotNull String> lootPools) implements ConfigurationSerializable, LootPool {

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("key", key);
        data.put("pools", new ArrayList<>(lootPools));
        return data;
    }

    /**
     * Reconstructs a {@code MultiLootPool} from a {@link #serialize()}-style map.
     */
    @NotNull
    public static MultiLootPool deserialize(@NotNull Map<String, Object> args) {
        return new MultiLootPool((String) args.get("key"), new LinkedHashSet<>((List<String>) args.get("pools")));
    }

    /**
     * Resolves {@link #lootPools()} to the currently registered {@link LootPool} instances,
     * silently dropping any key that no longer resolves.
     *
     * @return the resolved pools, in unspecified order
     */
    @SuppressWarnings("DataFlowIssue")
    @NotNull
    public Set<@NotNull LootPool> getLootPools() {
        return lootPools
                .stream()
                .filter(pool -> LootPoolApi.getProvider().getLootPoolMap().containsKey(pool))
                .map(pool -> LootPoolApi.getProvider().getLootPool(pool))
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Returns the flattened union of every resolved sub-pool's {@link LootPool#getLootList()}.
     */
    @Override
    public @NotNull List<@NotNull Loot> getLootList() {
        return getLootPools()
                .stream()
                .flatMap(lootPool -> lootPool.getLootList().stream())
                .toList();
    }

    @Override
    public @NotNull Loot getRandom() {
        return LootPool.getRandom(getLootList());
    }

    @Override
    public @NotNull List<@NotNull ItemStack> populate(int slots) {
        return LootPool.populate(getLootList(), slots);
    }

    @Override
    public @NotNull List<@NotNull ItemStack> populate(@NotNull Inventory inventory, int slots) {
        return LootPool.populate(getLootList(), inventory, slots);
    }

    @Override
    public @NotNull List<@NotNull ItemStack> populateRandomly(@NotNull Inventory inventory, int slots) {
        return LootPool.populateRandomly(getLootList(), inventory, slots);
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

