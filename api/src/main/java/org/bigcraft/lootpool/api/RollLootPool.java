package org.bigcraft.lootpool.api;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public record RollLootPool(@NotNull String key, @NotNull List<@NotNull Loot> lootPool, int minRolls, int maxRolls) implements ConfigurationSerializable, EditableLootPool {

    public RollLootPool(@NotNull LootPool lootPool, int minRolls, int maxRolls) {
        this(lootPool.getKey().getKey(), lootPool.getLootList(), minRolls, maxRolls);
    }

    public RollLootPool(@NotNull Map<String, Object> args) {
        this(RollLootPool.deserialize(args));
    }

    private RollLootPool(@NotNull RollLootPool lootPool) {
        this(lootPool.key, lootPool.lootPool, lootPool.minRolls, lootPool.maxRolls);
    }

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("key", key);
        data.put("minRolls", minRolls);
        data.put("maxRolls", maxRolls);
        for (int i = 0; i < lootPool.size(); i++) {
            data.put(String.valueOf(i), lootPool.get(i));
        }
        return data;
    }

    @NotNull
    public static RollLootPool deserialize(@NotNull Map<String, Object> args) {
        List<Loot> lootPool = new LinkedList<>();
        for (int i = 0; args.containsKey(String.valueOf(i)); i++) {
            Object loot = args.get(String.valueOf(i));
            if (loot instanceof Loot)
                lootPool.add((Loot) loot);
        }
        return new RollLootPool(
                (String) args.get("key"),
                List.copyOf(lootPool),
                NumberConversions.toInt(args.get("minRolls")),
                NumberConversions.toInt(args.get("maxRolls")));
    }

    public int rollSlots() {
        int min = Math.max(0, Math.min(minRolls, maxRolls));
        int max = Math.max(0, Math.max(minRolls, maxRolls));
        return min == max ? min : ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    @Override
    public @NotNull List<@NotNull Loot> getLootList() {
        return List.copyOf(lootPool);
    }

    @Override
    public @NotNull EditableLootPool withLoot(@NotNull String key, @NotNull List<@NotNull Loot> lootList) {
        return new RollLootPool(key, lootList, minRolls, maxRolls);
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

    @Override
    public @NotNull List<@NotNull ItemStack> populate() {
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
