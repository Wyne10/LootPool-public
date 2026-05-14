package org.bigcraft.lootpool.api;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public record MultiLootPool(@NotNull String key, @NotNull Set<@NotNull String> lootPools) implements ConfigurationSerializable, LootPool {

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("key", key);
        data.put("pools", new ArrayList<>(lootPools));
        return data;
    }

    @NotNull
    public static MultiLootPool deserialize(@NotNull Map<String, Object> args) {
        return new MultiLootPool((String) args.get("key"), new LinkedHashSet<>((List<String>) args.get("pools")));
    }

    @SuppressWarnings("DataFlowIssue")
    @NotNull
    public Set<@NotNull LootPool> getLootPools() {
        return lootPools
                .stream()
                .filter(pool -> LootPoolApi.getProvider().getLootPoolMap().containsKey(pool))
                .map(pool -> LootPoolApi.getProvider().getLootPool(pool))
                .collect(Collectors.toUnmodifiableSet());
    }

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

    @SuppressWarnings("DataFlowIssue")
    @Override
    public @NotNull NamespacedKey getKey() {
        return NamespacedKey.fromString("lootpool:" + key);
    }

}

