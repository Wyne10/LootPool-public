package org.bigcraft.lootpool.api;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public record LootPool(@NotNull String key, @NotNull List<@NotNull Loot> lootPool) implements ConfigurationSerializable, LootTable {

    public LootPool(@NotNull LootPool lootPool) {
        this(lootPool.key(), lootPool.lootPool());
    }

    public LootPool(@NotNull Map<String, Object> args) {
        this(LootPool.deserialize(args));
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

    @NotNull
    public static LootPool deserialize(@NotNull Map<String, Object> args) {
        List<Loot> lootPool = new ArrayList<>();
        for (Object loot : args.values()) {
            if (loot instanceof Loot)
                lootPool.add((Loot) loot);
        }
        return new LootPool((String) args.get("key"), List.copyOf(lootPool));
    }

    @NotNull
    public LootPool sortByWeight() {
        return new LootPool(key, lootPool.stream().sorted(Comparator.comparingInt(Loot::weight)).toList());
    }

    @NotNull
    public Loot getRandom() {
        int totalWeight = 0;
        for (Loot loot : lootPool) {
            totalWeight += loot.weight();
        }

        if (totalWeight == 0)
            return Loot.EMPTY;

        int randomValue = ThreadLocalRandom.current().nextInt(totalWeight);

        int currentSum = 0;
        for (Loot loot : lootPool) {
            currentSum += loot.weight();
            if (randomValue < currentSum) {
                return loot;
            }
        }

        return Loot.EMPTY;
    }

    @NotNull
    public List<@NotNull ItemStack> populate(int slots) {
        List<ItemStack> result = new ArrayList<>();
        for (int i = 0; i < slots; i++) {
            result.add(getRandom().create());
        }
        return result;
    }

    @SuppressWarnings("DataFlowIssue")
    public void populate(@NotNull Inventory inventory, int slots) {
        Queue<Integer> emptySlots = getEmptySlots(inventory);
        int toPopulate = Math.min(emptySlots.size(), slots);
        while (toPopulate > 0) {
            int slot = emptySlots.poll();
            inventory.setItem(slot, getRandom().create());
            toPopulate--;
        }
    }

    public void populateRandomly(@NotNull Inventory inventory, int slots) {
        var emptySlots = getEmptySlots(inventory);
        int toPopulate = Math.min(emptySlots.size(), slots);
        while (toPopulate > 0) {
            int index = ThreadLocalRandom.current().nextInt(emptySlots.size());
            int slot = emptySlots.get(index);
            emptySlots.remove(index);
            inventory.setItem(slot, getRandom().create());
            toPopulate--;
        }
    }

    @NotNull
    private LinkedList<Integer> getEmptySlots(@NotNull Inventory inventory) {
        var slots = new LinkedList<Integer>();
        for (int i = 0; i < inventory.getSize(); i++) {
            var item = inventory.getItem(i);
            if (item == null || item.getType() == Material.AIR)
                slots.add(i);
        }
        return slots;
    }

    @Override
    public @NotNull Collection<ItemStack> populateLoot(@NotNull Random random, @NotNull LootContext context) {
        return lootPool.stream().map(Loot::create).toList();
    }

    @Override
    public void fillInventory(@NotNull Inventory inventory, @NotNull Random random, @NotNull LootContext context) {
        populate(inventory, inventory.getSize());
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    public @NotNull NamespacedKey getKey() {
        return NamespacedKey.fromString("lootpool:" + key);
    }

}

