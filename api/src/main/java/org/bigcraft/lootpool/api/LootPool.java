package org.bigcraft.lootpool.api;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A pool of weighted {@link Loot} entries that can be rolled into {@link ItemStack}s or used to
 * fill a Bukkit {@link Inventory}. Extends {@link LootTable} so any {@code LootPool} can be used
 * anywhere the platform expects a vanilla loot table.
 * <p>
 * Implementations vary in how {@link #getLootList()} is produced (a static list, a resolved
 * reference to other pools, a re-rolled vanilla table, etc.) and in how {@code slots} is
 * interpreted by {@link #populate(int)} and friends; see the individual implementations for
 * their exact contract.
 */
public interface LootPool extends LootTable {
    /**
     * Returns the loot entries that make up this pool. Depending on the implementation this may
     * be a fixed configured list or a value computed/resolved on each call.
     *
     * @return an immutable snapshot of this pool's loot entries, never {@code null}
     */
    @NotNull
    List<@NotNull Loot> getLootList();

    /**
     * Picks a single {@link Loot} entry using weighted random selection over {@link #getLootList()}.
     *
     * @return the selected loot entry, or {@link Loot#EMPTY} if the pool has no eligible entries
     */
    @NotNull
    Loot getRandom();

    /**
     * Rolls {@code slots} independent {@link ItemStack}s from this pool, without touching any inventory.
     *
     * @param slots the number of items to roll
     * @return the rolled items, in roll order
     */
    @NotNull
    List<@NotNull ItemStack> populate(int slots);

    /**
     * Rolls loot into {@code inventory}'s empty slots, one roll per empty slot.
     *
     * @return the items that could not be placed because the inventory ran out of empty slots
     */
    @NotNull
    default List<@NotNull ItemStack> populateList(@NotNull Inventory inventory) {
        return populate(inventory.getSize());
    }

    /**
     * Rolls {@code slots} items from this pool and places them into {@code inventory}'s empty
     * slots in slot order.
     *
     * @param slots the number of items to roll
     * @return the rolled items that did not fit because the inventory had fewer empty slots than {@code slots}
     */
    @NotNull
    List<@NotNull ItemStack> populate(@NotNull Inventory inventory, int slots);

    /**
     * Equivalent to {@link #populate(Inventory, int)} with {@code slots} set to the inventory's size.
     *
     * @return the rolled items that did not fit into an empty slot
     */
    @NotNull
    default List<@NotNull ItemStack> populate(@NotNull Inventory inventory) {
        return populate(inventory, inventory.getSize());
    }

    /**
     * Rolls {@code slots} items from this pool and places them into {@code slots} randomly chosen
     * empty slots of {@code inventory}, instead of filling slots in order.
     *
     * @param slots the number of items to roll
     * @return the rolled items that did not fit because the inventory had fewer empty slots than {@code slots}
     */
    @NotNull
    List<@NotNull ItemStack> populateRandomly(@NotNull Inventory inventory, int slots);

    /**
     * Equivalent to {@link #populateRandomly(Inventory, int)} with {@code slots} set to the inventory's size.
     *
     * @return the rolled items that did not fit into an empty slot
     */
    @NotNull
    default List<@NotNull ItemStack> populateRandomly(@NotNull Inventory inventory)  {
        return populateRandomly(inventory, inventory.getSize());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Implemented in terms of {@link #populate(int)}, rolling as many items as this pool has
     * loot entries. {@code random} and {@code context} are ignored by the default implementation.
     */
    @Override
    default @NotNull Collection<ItemStack> populateLoot(@NotNull Random random, @NotNull LootContext context) {
        return populate(getLootList().size());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Implemented in terms of {@link #populate(Inventory, int)}. {@code random} and {@code context}
     * are ignored by the default implementation.
     */
    @Override
    default void fillInventory(@NotNull Inventory inventory, @NotNull Random random, @NotNull LootContext context) {
        populate(inventory, inventory.getSize());
    }

    /**
     * Weighted-random-selects a single entry from {@code lootPool}, where each entry's chance is
     * proportional to its {@link Loot#weight()}.
     *
     * @param lootPool the candidate entries
     * @return the selected entry, or {@link Loot#EMPTY} if the total weight is zero
     */
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

    /**
     * Rolls {@code slots} independent items from {@code lootPool} via {@link #getRandom(List)}.
     *
     * @param slots the number of items to roll
     * @return the rolled items, in roll order
     */
    @NotNull
    static List<@NotNull ItemStack> populate(@NotNull List<@NotNull Loot> lootPool, int slots) {
        List<ItemStack> result = new ArrayList<>();
        for (int i = 0; i < slots; i++) {
            result.add(getRandom(lootPool).create());
        }
        return result;
    }

    /**
     * Places items from {@code itemPool} into {@code inventory}'s empty slots in slot order,
     * one item per slot, until either the pool or the empty slots run out.
     *
     * @param itemPool the items to place; consumed from the front
     * @return the items that did not fit because there were fewer empty slots than items
     */
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

    /**
     * Rolls up to {@code slots} items from {@code lootPool} into {@code inventory}'s empty slots
     * in slot order. Any rolled items beyond the number of available empty slots are rolled fresh
     * via {@link #populate(List, int)} rather than reusing the ones that didn't fit.
     *
     * @param slots the number of items to roll
     * @return the items rolled for the slots that exceeded the inventory's empty-slot count
     */
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

    /**
     * Like {@link #populate(List, Inventory)}, but each item is placed into a randomly chosen
     * empty slot instead of filling slots in order.
     *
     * @param itemPool the items to place; consumed from the front
     * @return the items that did not fit because there were fewer empty slots than items
     */
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

    /**
     * Like {@link #populate(List, Inventory, int)}, but each rolled item is placed into a
     * randomly chosen empty slot instead of filling slots in order. Items rolled beyond the
     * number of available empty slots are rolled fresh via {@link #populate(List, int)}.
     *
     * @param slots the number of items to roll
     * @return the items rolled for the slots that exceeded the inventory's empty-slot count
     */
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

    /**
     * Collects the indices of {@code inventory}'s empty slots (slots that are {@code null} or {@link Material#AIR}).
     *
     * @return a mutable list of empty slot indices, in ascending order
     */
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
