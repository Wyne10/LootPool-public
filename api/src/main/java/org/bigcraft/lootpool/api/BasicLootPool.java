package org.bigcraft.lootpool.api;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public record BasicLootPool(@NotNull String key, @NotNull List<@NotNull Loot> lootPool) implements ConfigurationSerializable, LootPool {

    public BasicLootPool(@NotNull LootPool lootPool) {
        this(lootPool.getKey().getKey(), lootPool.getLootList());
    }

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

    @Override
    public @NotNull List<@NotNull Loot> getLootList() {
        return List.copyOf(lootPool);
    }

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

    @NotNull
    public BasicLootPool sortByWeight() {
        return new BasicLootPool(key, lootPool.stream().sorted(Comparator.comparingInt(Loot::weight)).toList());
    }

    public Loot getRandom() {
        return LootPool.getRandom(lootPool);
    }

    public @NotNull List<@NotNull ItemStack> populate(int slots) {
        return LootPool.populate(lootPool, slots);
    }


    public void populate(@NotNull Inventory inventory, int slots) {
        LootPool.populate(lootPool, inventory, slots);
    }

    public void populateRandomly(@NotNull Inventory inventory, int slots) {
        LootPool.populateRandomly(lootPool, inventory, slots);
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    public @NotNull NamespacedKey getKey() {
        return NamespacedKey.fromString("lootpool:" + key);
    }

}

