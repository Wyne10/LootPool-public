package org.bigcraft.lootpool.api;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public record CompositeLootPool(@NotNull String key, @NotNull Map<@NotNull String, @NotNull Integer> lootPools) implements ConfigurationSerializable, LootPool {

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("key", key);
        data.putAll(lootPools);
        return data;
    }

    @NotNull
    public static CompositeLootPool deserialize(@NotNull Map<String, Object> args) {
        Map<String, Integer> lootPools = new HashMap<>();
        for (Map.Entry<String, Object> lootPool : args.entrySet()) {
            if (lootPool.getKey().equals("key") || lootPool.getKey().equals("==")) continue;
            lootPools.put(lootPool.getKey(), (int) lootPool.getValue());
        }
        return new CompositeLootPool((String) args.get("key"), Map.copyOf(lootPools));
    }

    @SuppressWarnings("DataFlowIssue")
    @NotNull
    public Map<@NotNull LootPool, @NotNull Integer> getLootPools() {
        return lootPools.entrySet()
                .stream()
                .filter(entry -> LootPoolApi.getProvider().getLootPoolMap().containsKey(entry.getKey()))
                .collect(Collectors.toUnmodifiableMap(entry -> LootPoolApi.getProvider().getLootPool(entry.getKey()), Map.Entry::getValue));
    }

    @Override
    public @NotNull List<@NotNull Loot> getLootList() {
        return getLootPools().keySet().stream()
                .flatMap(lootPool -> lootPool.getLootList().stream())
                .toList();
    }

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

    @Override
    public @NotNull Loot getRandom() {
        return getRandomLootPool().getRandom();
    }

    @Override
    public @NotNull List<@NotNull ItemStack> populate(int slots) {
        return getRandomLootPool().populate(slots);
    }

    @Override
    public @NotNull List<@NotNull ItemStack> populate(@NotNull Inventory inventory, int slots) {
        return getRandomLootPool().populate(inventory, slots);
    }

    @Override
    public @NotNull List<@NotNull ItemStack> populateRandomly(@NotNull Inventory inventory, int slots) {
        return getRandomLootPool().populateRandomly(inventory, slots);
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    public @NotNull NamespacedKey getKey() {
        return NamespacedKey.fromString("lootpool:" + key);
    }

}

