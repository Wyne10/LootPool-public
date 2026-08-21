package org.bigcraft.lootpool.api;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * The canonical {@link LootPool} implementation: a plain weighted list of {@link Loot} entries,
 * with no delegation, resolution, or re-rolling involved. Use this for a straightforward,
 * self-contained drop table, or to "snapshot" any other {@code LootPool} into a fixed, static one
 * via {@link #BasicLootPool(LootPool)}.
 *
 * @param key      this pool's identifier in the plugin's registry
 * @param lootPool the pool's loot entries
 */
public record BasicLootPool(@NotNull String key, @NotNull List<@NotNull Loot> lootPool) implements ConfigurationSerializable, EditableLootPool {

    /** An empty pool, used as a fallback where a valid but harmless {@link LootPool} is required. */
    public static final BasicLootPool EMPTY = new BasicLootPool("empty", List.of());

    /**
     * Captures a static copy of {@code lootPool}'s current key and {@link LootPool#getLootList()}.
     * For pools whose entries are computed dynamically (e.g. {@link VanillaLootPool}, {@link MultiLootPool}),
     * this fixes them at their value at the time of this call rather than tracking future changes.
     */
    public BasicLootPool(@NotNull LootPool lootPool) {
        this(lootPool.getKey().getKey(), lootPool.getLootList());
    }

    /**
     * Reconstructs a {@code BasicLootPool} from a {@link #serialize()}-style map.
     */
    public BasicLootPool(@NotNull Map<String, Object> args) {
        this(BasicLootPool.deserialize(args));
    }

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("key", key);
        for (int i = 0; i < lootPool.size(); i++) {
            data.put(String.valueOf(i), lootPool.get(i));
        }
        return data;
    }

    /**
     * Reconstructs a {@code BasicLootPool} from a {@link #serialize()}-style map: {@code "key"}
     * plus one {@link Loot} entry per sequential integer index, starting at {@code "0"}.
     * Non-{@link Loot} values at those indices are silently skipped.
     */
    @NotNull
    public static BasicLootPool deserialize(@NotNull Map<String, Object> args) {
        List<Loot> lootPool = new LinkedList<>();
        for (int i = 0; args.containsKey(String.valueOf(i)); i++) {
            Object loot = args.get(String.valueOf(i));
            if (loot instanceof Loot)
                lootPool.add((Loot) loot);
        }
        return new BasicLootPool((String) args.get("key"), List.copyOf(lootPool));
    }

    @Override
    public @NotNull List<@NotNull Loot> getLootList() {
        return List.copyOf(lootPool);
    }

    @Override
    public @NotNull EditableLootPool withLoot(@NotNull String key, @NotNull List<@NotNull Loot> lootList) {
        return new BasicLootPool(key, lootList);
    }

    /**
     * @return a new pool with the same key and entries sorted ascending by {@link Loot#weight()}
     */
    @NotNull
    public BasicLootPool sortByWeight() {
        return new BasicLootPool(key, lootPool.stream().sorted(Comparator.comparingInt(Loot::weight)).toList());
    }

    @Override
    public @NotNull Loot getRandom() {
        return LootPool.getRandom(lootPool);
    }

    @Override
    public @NotNull List<@NotNull ItemStack> populate(int slots) {
        return LootPool.populate(lootPool, slots);
    }

    @Override
    public @NotNull List<@NotNull ItemStack> populate(@NotNull Inventory inventory, int slots) {
        return LootPool.populate(lootPool, inventory, slots);
    }

    @Override
    public @NotNull List<@NotNull ItemStack> populateRandomly(@NotNull Inventory inventory, int slots) {
        return LootPool.populateRandomly(lootPool, inventory, slots);
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

