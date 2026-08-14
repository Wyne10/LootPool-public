package org.bigcraft.lootpool.api;

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

public record RollLootPool(@NotNull String key, @NotNull String pool, int minRolls, int maxRolls) implements ConfigurationSerializable, LootPool {

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

    @NotNull
    public static RollLootPool deserialize(@NotNull Map<String, Object> args) {
        return new RollLootPool(
                (String) args.get("key"),
                (String) args.get("pool"),
                NumberConversions.toInt(args.get("minRolls")),
                NumberConversions.toInt(args.get("maxRolls")));
    }

    @Nullable
    public LootPool getLootPool() {
        return LootPoolApi.getProvider().getLootPool(pool);
    }

    public int rollSlots() {
        int min = Math.max(0, Math.min(minRolls, maxRolls));
        int max = Math.max(0, Math.max(minRolls, maxRolls));
        return min == max ? min : ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    @Override
    public @NotNull List<@NotNull Loot> getLootList() {
        LootPool lootPool = getLootPool();
        return lootPool != null ? lootPool.getLootList() : List.of();
    }

    @Override
    public @NotNull Loot getRandom() {
        LootPool lootPool = getLootPool();
        return lootPool != null ? lootPool.getRandom() : Loot.EMPTY;
    }

    @Override
    public @NotNull List<@NotNull ItemStack> populate(int slots) {
        LootPool lootPool = getLootPool();
        return lootPool != null ? lootPool.populate(slots) : List.of();
    }

    @Override
    public @NotNull List<@NotNull ItemStack> populate(@NotNull Inventory inventory, int slots) {
        LootPool lootPool = getLootPool();
        return lootPool != null ? lootPool.populate(inventory, slots) : List.of();
    }

    @Override
    public @NotNull List<@NotNull ItemStack> populateRandomly(@NotNull Inventory inventory, int slots) {
        LootPool lootPool = getLootPool();
        return lootPool != null ? lootPool.populateRandomly(inventory, slots) : List.of();
    }

    @Override
    public @NotNull List<@NotNull ItemStack> populateList(@NotNull Inventory inventory) {
        return populate(rollSlots());
    }

    @Override
    public @NotNull List<@NotNull ItemStack> populate(@NotNull Inventory inventory) {
        return populate(inventory, rollSlots());
    }

    @Override
    public @NotNull List<@NotNull ItemStack> populateRandomly(@NotNull Inventory inventory) {
        return populateRandomly(inventory, rollSlots());
    }

    @Override
    public @NotNull Collection<ItemStack> populateLoot(@NotNull Random random, @NotNull LootContext context) {
        return populate(rollSlots());
    }

    @Override
    public void fillInventory(@NotNull Inventory inventory, @NotNull Random random, @NotNull LootContext context) {
        populate(inventory, rollSlots());
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    public @NotNull NamespacedKey getKey() {
        return NamespacedKey.fromString("lootpool:" + key);
    }

}
