package me.wyne.lootpool.api;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * A {@link LootPool} that pins each {@link Loot} entry to a specific inventory slot index, rather
 * than treating the pool as a flat weighted list. Use this to reproduce an exact, fixed layout of
 * items (a "snapshot" of a container) instead of randomly distributing loot across slots.
 * <p>
 * Only {@link #populate(Inventory, int)} honors the pinned slot positions. {@link #populate(int)}
 * (no inventory) and {@link #getRandom()} fall back to treating the entries as an ordinary
 * weighted/ordered list, and {@link #populateRandomly(Inventory, int)} discards the pinned
 * positions entirely and scatters rolled items into random empty slots instead.
 *
 * @param key      this pool's identifier in the plugin's registry
 * @param lootPool the loot entries keyed by their target inventory slot index, ascending
 */
public record SnapshotLootPool(@NotNull String key, @NotNull SortedMap<@NotNull Integer, @NotNull Loot> lootPool) implements ConfigurationSerializable, LootPool {

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("key", key);
        lootPool.forEach((key, loot) -> data.put(key.toString(), loot));
        return data;
    }

    /**
     * Reconstructs a {@code SnapshotLootPool} from a {@link #serialize()}-style map: {@code "key"}
     * plus one entry per slot index, keyed by that index as a string.
     */
    @NotNull
    public static SnapshotLootPool deserialize(@NotNull Map<String, Object> args) {
        SortedMap<Integer, Loot> lootPool = new TreeMap<>();
        for (Map.Entry<String, Object> entry : args.entrySet()) {
            if (entry.getKey().equals("key") || entry.getKey().equals("==")) continue;
            lootPool.put(Integer.parseInt(entry.getKey()), (Loot) entry.getValue());
        }
        return new SnapshotLootPool((String) args.get("key"), Collections.unmodifiableSortedMap(lootPool));
    }

    /**
     * Returns the entries in ascending slot-index order, discarding the slot indices themselves.
     */
    @Override
    public @NotNull List<@NotNull Loot> getLootList() {
        return List.copyOf(lootPool.values());
    }

    @Override
    public @NotNull Loot getRandom() {
        return LootPool.getRandom(getLootList());
    }

    /**
     * Rolls the first {@code slots} entries in ascending slot-index order, ignoring their pinned
     * slot indices (there is no inventory to place them into).
     */
    @Override
    public @NotNull List<@NotNull ItemStack> populate(int slots) {
        List<ItemStack> result = new ArrayList<>();
        lootPool.entrySet().stream()
                .limit(slots)
                .forEach(entry -> result.add(entry.getValue().create()));
        return result;
    }

    /**
     * Places the first {@code slots} entries (in ascending slot-index order) into their pinned
     * slot in {@code inventory}.
     *
     * @return the rolled items whose pinned slot was out of bounds or already occupied
     */
    @Override
    public @NotNull List<@NotNull ItemStack> populate(@NotNull Inventory inventory, int slots) {
        List<ItemStack> exceed = new ArrayList<>();
        lootPool.entrySet().stream()
                .limit(slots)
                .forEach(entry -> {
                    if (inventory.getSize() <= entry.getKey()) {
                        exceed.add(entry.getValue().create());
                        return;
                    }
                    var item = inventory.getItem(entry.getKey());
                    if ((item != null && item.getType() != Material.AIR)) {
                        exceed.add(entry.getValue().create());
                        return;
                    }
                    inventory.setItem(entry.getKey(), entry.getValue().create());
                });
        return exceed;
    }

    /**
     * Unlike {@link #populate(Inventory, int)}, this ignores the pinned slot indices entirely and
     * scatters {@code slots} rolled items into random empty slots of {@code inventory}.
     */
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

