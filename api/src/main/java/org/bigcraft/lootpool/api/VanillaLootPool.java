package org.bigcraft.lootpool.api;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A {@link LootPool} that delegates entirely to a vanilla Minecraft {@link LootTable}, identified
 * by its {@code path} {@link NamespacedKey} (e.g. {@code minecraft:chests/simple_dungeon}). Use
 * this to expose an existing vanilla table through the {@code LootPool} API rather than
 * redefining its contents.
 * <p>
 * Because the underlying table is opaque, this implementation cannot roll a fixed number of
 * items or place items into a caller-chosen random subset of slots: {@link #populate(int)},
 * {@link #populate(Inventory, int)} and {@link #populateRandomly(Inventory, int)} all ignore the
 * {@code slots} argument and instead roll the vanilla table with a generic {@link LootContext}
 * built from the first loaded world's spawn location (not the context of any real event). As a
 * result {@link #populateRandomly(Inventory, int)} behaves identically to
 * {@link #populate(Inventory, int)} - both fill the inventory via {@link LootTable#fillInventory};
 * vanilla tables do not support inventory-side random placement. {@link #getLootList()} re-rolls
 * the table on every call and reports each drop as its own single-weight {@link Loot} entry, so it
 * does not reflect a fixed, enumerable pool of possible drops.
 *
 * @param key  this pool's identifier in the plugin's registry (distinct from {@code path})
 * @param path the {@link NamespacedKey} of the vanilla loot table to delegate to
 */
public record VanillaLootPool(@NotNull String key, @NotNull NamespacedKey path) implements ConfigurationSerializable, LootPool {

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("key", key);
        data.put("path", path.toString());
        return data;
    }

    /**
     * Reconstructs a {@code VanillaLootPool} from a {@link #serialize()}-style map.
     */
    @NotNull
    public static VanillaLootPool deserialize(@NotNull Map<String, Object> args) {
        return new VanillaLootPool((String) args.get("key"), NamespacedKey.fromString((String) args.get("path")));
    }

    /**
     * Resolves the vanilla {@link LootTable} at {@link #path()}.
     *
     * @return the vanilla loot table, or {@code null} if {@code path} does not name a registered table
     */
    @Nullable
    public LootTable getLootTable() {
        return Bukkit.getLootTable(path);
    }

    /**
     * Rolls the vanilla table once (via {@link #defaultContext()}) and reports each dropped item
     * as its own {@link Loot} entry with weight {@code 1} and a fixed amount. Calling this again
     * produces a different result, since it performs a fresh roll rather than returning a cached list.
     */
    @Override
    public @NotNull List<@NotNull Loot> getLootList() {
        List<Loot> lootList = new ArrayList<>();
        for (ItemStack item : populateLoot(ThreadLocalRandom.current(), defaultContext())) {
            if (item == null)
                continue;
            lootList.add(new Loot(item, 1, item.getAmount(), item.getAmount()));
        }
        return List.copyOf(lootList);
    }

    @Override
    public @NotNull Loot getRandom() {
        return LootPool.getRandom(getLootList());
    }

    /**
     * {@code slots} is ignored; this rolls the vanilla table once via {@link #defaultContext()}
     * and returns whatever it drops.
     */
    @Override
    public @NotNull List<@NotNull ItemStack> populate(int slots) {
        return populateLoot(ThreadLocalRandom.current(), defaultContext()).stream().toList();
    }

    /**
     * {@code slots} is ignored; this fills {@code inventory} via {@link LootTable#fillInventory}
     * using {@link #defaultContext()} and always reports no leftover items.
     */
    @Override
    public @NotNull List<@NotNull ItemStack> populate(@NotNull Inventory inventory, int slots) {
        fillInventory(inventory, ThreadLocalRandom.current(), defaultContext());
        return Collections.emptyList();
    }

    /**
     * {@code slots} is ignored, and placement is not actually randomized: this delegates to the
     * same vanilla {@link LootTable#fillInventory} logic as {@link #populate(Inventory, int)}.
     */
    @Override
    public @NotNull List<@NotNull ItemStack> populateRandomly(@NotNull Inventory inventory, int slots) {
        fillInventory(inventory, ThreadLocalRandom.current(), defaultContext());
        return Collections.emptyList();
    }

    /**
     * Builds a generic {@link LootContext} anchored at the first loaded world's spawn location,
     * used since this pool has no real event context (killer, tool, block, etc.) to draw from.
     */
    @NotNull
    private static LootContext defaultContext() {
        World world = Bukkit.getWorlds().get(0);
        Location location = world.getSpawnLocation();
        return new LootContext.Builder(location).build();
    }

    /**
     * Delegates to the resolved vanilla {@link LootTable}, or returns an empty collection if
     * {@link #path()} does not resolve to a registered table.
     */
    @Override
    public @NotNull Collection<ItemStack> populateLoot(@NotNull Random random, @NotNull LootContext context) {
        var lootTable = getLootTable();
        if (lootTable != null)
            return lootTable.populateLoot(random, context);
        return Collections.emptyList();
    }

    /**
     * Delegates to the resolved vanilla {@link LootTable}, or does nothing if {@link #path()}
     * does not resolve to a registered table.
     */
    @Override
    public void fillInventory(@NotNull Inventory inventory, @NotNull Random random, @NotNull LootContext context) {
        var lootTable = getLootTable();
        if (lootTable != null)
            lootTable.fillInventory(inventory, random, context);
    }

    /**
     * Returns this pool's synthetic {@code "lootpool:" + key} registry key, not {@link #path()}.
     */
    @SuppressWarnings("DataFlowIssue")
    @Override
    public @NotNull NamespacedKey getKey() {
        return NamespacedKey.fromString("lootpool:" + key);
    }

}

