package me.wyne.lootpool.api;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A {@link LootPool} that wraps another registered pool (referenced by {@code pool}'s registry
 * key) and rolls a random number of items from it, similar to a vanilla loot table's "rolls"
 * count. Use this when the referenced pool's contents shouldn't change, but the number of items
 * drawn per use should vary within {@code [minRolls, maxRolls]}.
 * <p>
 * The random roll count only applies to the inventory/no-slots convenience methods
 * ({@link #populateList(Inventory)}, {@link #populate(Inventory)}, {@link #populateRandomly(Inventory)},
 * {@link #populateLoot(Random, LootContext)} and {@link #fillInventory(Inventory, Random, LootContext)}),
 * all of which use {@link #rollSlots()} in place of the inventory size. The explicit-{@code slots}
 * overloads ({@link #populate(int)}, {@link #populate(Inventory, int)}, {@link #populateRandomly(Inventory, int)})
 * ignore {@link #rollSlots()} entirely and pass the caller-supplied {@code slots} straight through
 * to the referenced pool. {@link #getLootList()} and {@link #getRandom()} likewise expose the
 * referenced pool's entries directly, unaffected by the roll count.
 *
 * @param key      this pool's identifier in the plugin's registry
 * @param pool     the registry key of the pool to roll from
 * @param minRolls the inclusive lower bound of the roll count
 * @param maxRolls the inclusive upper bound of the roll count
 */
public record RollLootPool(@NotNull String key, @NotNull String pool, int minRolls, int maxRolls) implements ConfigurationSerializable, LootPool {

    /**
     * Reconstructs a {@code RollLootPool} from a {@link #serialize()}-style map.
     */
    public RollLootPool(@NotNull Map<String, Object> args) {
        this(RollLootPool.deserialize(args));
    }

    private RollLootPool(@NotNull RollLootPool lootPool) {
        this(lootPool.key, lootPool.pool, lootPool.minRolls, lootPool.maxRolls);
    }

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("key", key);
        data.put("pool", pool);
        data.put("minRolls", minRolls);
        data.put("maxRolls", maxRolls);
        return data;
    }

    /**
     * Reconstructs a {@code RollLootPool} from a {@link #serialize()}-style map.
     */
    @NotNull
    public static RollLootPool deserialize(@NotNull Map<String, Object> args) {
        return new RollLootPool(
                (String) args.get("key"),
                (String) args.get("pool"),
                NumberConversions.toInt(args.get("minRolls")),
                NumberConversions.toInt(args.get("maxRolls")));
    }

    /**
     * Resolves {@link #pool()} in the registry.
     *
     * @return the referenced pool, or {@code null} if {@code pool} does not currently resolve to a registered pool
     */
    @Nullable
    public LootPool getLootPool() {
        return LootPoolApi.getProvider().getLootPool(pool);
    }

    /**
     * Rolls a roll count in the inclusive range between {@link #minRolls()} and {@link #maxRolls()},
     * normalized the same way as {@link Loot#rollAmount()} (order-independent, clamped to non-negative).
     *
     * @return the rolled count, or a fixed value if the bounds normalize to the same value
     */
    public int rollSlots() {
        int min = Math.max(0, Math.min(minRolls, maxRolls));
        int max = Math.max(0, Math.max(minRolls, maxRolls));
        return min == max ? min : ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    /**
     * Delegates directly to the referenced pool; not affected by {@link #rollSlots()}.
     *
     * @return the referenced pool's loot list, or an empty list if {@link #getLootPool()} is {@code null}
     */
    @Override
    public @NotNull List<@NotNull Loot> getLootList() {
        LootPool lootPool = getLootPool();
        return lootPool != null ? lootPool.getLootList() : List.of();
    }

    /**
     * @return the referenced pool's random entry, or {@link Loot#EMPTY} if {@link #getLootPool()} is {@code null}
     */
    @Override
    public @NotNull Loot getRandom() {
        LootPool lootPool = getLootPool();
        return lootPool != null ? lootPool.getRandom() : Loot.EMPTY;
    }

    /**
     * Delegates directly to the referenced pool with the given {@code slots}; not affected by {@link #rollSlots()}.
     */
    @Override
    public @NotNull List<@NotNull ItemStack> populate(int slots) {
        LootPool lootPool = getLootPool();
        return lootPool != null ? lootPool.populate(slots) : List.of();
    }

    /**
     * Delegates directly to the referenced pool with the given {@code slots}; not affected by {@link #rollSlots()}.
     */
    @Override
    public @NotNull List<@NotNull ItemStack> populate(@NotNull Inventory inventory, int slots) {
        LootPool lootPool = getLootPool();
        return lootPool != null ? lootPool.populate(inventory, slots) : List.of();
    }

    /**
     * Delegates directly to the referenced pool with the given {@code slots}; not affected by {@link #rollSlots()}.
     */
    @Override
    public @NotNull List<@NotNull ItemStack> populateRandomly(@NotNull Inventory inventory, int slots) {
        LootPool lootPool = getLootPool();
        return lootPool != null ? lootPool.populateRandomly(inventory, slots) : List.of();
    }

    /**
     * Overrides the {@link LootPool} default to roll {@link #rollSlots()} items instead of {@code inventory}'s size.
     */
    @Override
    public @NotNull List<@NotNull ItemStack> populateList(@NotNull Inventory inventory) {
        return populate(rollSlots());
    }

    /**
     * Overrides the {@link LootPool} default to roll {@link #rollSlots()} items instead of {@code inventory}'s size.
     */
    @Override
    public @NotNull List<@NotNull ItemStack> populate(@NotNull Inventory inventory) {
        return populate(inventory, rollSlots());
    }

    /**
     * Overrides the {@link LootPool} default to roll {@link #rollSlots()} items instead of {@code inventory}'s size.
     */
    @Override
    public @NotNull List<@NotNull ItemStack> populateRandomly(@NotNull Inventory inventory) {
        return populateRandomly(inventory, rollSlots());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Uses {@link #rollSlots()} as the roll count instead of {@link #getLootList()}'s size.
     */
    @Override
    public @NotNull Collection<ItemStack> populateLoot(@NotNull Random random, @NotNull LootContext context) {
        return populate(rollSlots());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Uses {@link #rollSlots()} as the roll count instead of {@code inventory}'s size.
     */
    @Override
    public void fillInventory(@NotNull Inventory inventory, @NotNull Random random, @NotNull LootContext context) {
        populate(inventory, rollSlots());
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
