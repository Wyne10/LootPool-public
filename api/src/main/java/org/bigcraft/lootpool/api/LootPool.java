package org.bigcraft.lootpool.api;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public interface LootPool extends LootTable {
    @NotNull
    List<@NotNull Loot> getLootList();

    @NotNull
    Loot getRandom();

    @NotNull
    List<@NotNull ItemStack> populate(int slots);

    @NotNull
    default List<@NotNull ItemStack> populateList(@NotNull Inventory inventory) {
        return populate(inventory.getSize());
    }

    @NotNull
    List<@NotNull ItemStack> populate(@NotNull Inventory inventory, int slots);

    @NotNull
    default List<@NotNull ItemStack> populate(@NotNull Inventory inventory) {
        return populate(inventory, inventory.getSize());
    }

    @NotNull
    List<@NotNull ItemStack> populateRandomly(@NotNull Inventory inventory, int slots);

    @NotNull
    default List<@NotNull ItemStack> populateRandomly(@NotNull Inventory inventory)  {
        return populateRandomly(inventory, inventory.getSize());
    }

    @Override
    default @NotNull Collection<ItemStack> populateLoot(@NotNull Random random, @NotNull LootContext context) {
        return populate(getLootList().size());
    }

    @Override
    default void fillInventory(@NotNull Inventory inventory, @NotNull Random random, @NotNull LootContext context) {
        populate(inventory, inventory.getSize());
    }

    @NotNull
    static Loot getRandom(@NotNull List<@NotNull Loot> lootPool) {
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
    static List<@NotNull ItemStack> populate(@NotNull List<@NotNull Loot> lootPool, int slots) {
        List<ItemStack> result = new ArrayList<>();
        for (int i = 0; i < slots; i++) {
            result.add(getRandom(lootPool).create());
        }
        return result;
    }

    @SuppressWarnings("DataFlowIssue")
    @NotNull
    static List<@NotNull ItemStack> populate(@NotNull List<@NotNull ItemStack> itemPool, @NotNull Inventory inventory) {
        List<ItemStack> itemList = new ArrayList<>(itemPool);
        Queue<Integer> emptySlots = getEmptySlots(inventory);
        int toPopulate = Math.min(emptySlots.size(), itemPool.size());
        while (toPopulate > 0) {
            int slot = emptySlots.poll();
            inventory.setItem(slot, itemList.remove(0));
            toPopulate--;
        }
        return itemList;
    }

    @SuppressWarnings("DataFlowIssue")
    @NotNull
    static List<@NotNull ItemStack> populate(@NotNull List<@NotNull Loot> lootPool, @NotNull Inventory inventory, int slots) {
        Queue<Integer> emptySlots = getEmptySlots(inventory);
        int exceed = slots - emptySlots.size();
        int toPopulate = Math.min(emptySlots.size(), slots);
        while (toPopulate > 0) {
            int slot = emptySlots.poll();
            inventory.setItem(slot, getRandom(lootPool).create());
            toPopulate--;
        }
        return populate(lootPool, exceed);
    }

    @NotNull
    static List<@NotNull ItemStack> populateRandomly(@NotNull List<@NotNull ItemStack> itemPool, @NotNull Inventory inventory) {
        List<ItemStack> itemList = new ArrayList<>(itemPool);
        var emptySlots = getEmptySlots(inventory);
        int toPopulate = Math.min(emptySlots.size(), itemPool.size());
        while (toPopulate > 0) {
            int index = ThreadLocalRandom.current().nextInt(emptySlots.size());
            int slot = emptySlots.get(index);
            emptySlots.remove(index);
            inventory.setItem(slot, itemList.remove(0));
            toPopulate--;
        }
        return itemList;
    }

    @NotNull
    static List<@NotNull ItemStack> populateRandomly(@NotNull List<@NotNull Loot> lootPool, @NotNull Inventory inventory, int slots) {
        var emptySlots = getEmptySlots(inventory);
        int exceed = slots - emptySlots.size();
        int toPopulate = Math.min(emptySlots.size(), slots);
        while (toPopulate > 0) {
            int index = ThreadLocalRandom.current().nextInt(emptySlots.size());
            int slot = emptySlots.get(index);
            emptySlots.remove(index);
            inventory.setItem(slot, getRandom(lootPool).create());
            toPopulate--;
        }
        return populate(lootPool, exceed);
    }

    @NotNull
    static LinkedList<Integer> getEmptySlots(@NotNull Inventory inventory) {
        var slots = new LinkedList<Integer>();
        for (int i = 0; i < inventory.getSize(); i++) {
            var item = inventory.getItem(i);
            if (item == null || item.getType() == Material.AIR)
                slots.add(i);
        }
        return slots;
    }
}
