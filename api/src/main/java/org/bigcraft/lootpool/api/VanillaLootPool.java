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

public record VanillaLootPool(@NotNull String key, @NotNull NamespacedKey path) implements ConfigurationSerializable, LootPool {

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("key", key);
        data.put("path", path.toString());
        return data;
    }

    @NotNull
    public static VanillaLootPool deserialize(@NotNull Map<String, Object> args) {
        return new VanillaLootPool((String) args.get("key"), NamespacedKey.fromString((String) args.get("path")));
    }

    @Nullable
    public LootTable getLootTable() {
        return Bukkit.getLootTable(path);
    }

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

    @Override
    public @NotNull List<@NotNull ItemStack> populate(int slots) {
        return populateLoot(ThreadLocalRandom.current(), defaultContext()).stream().toList();
    }

    @Override
    public @NotNull List<@NotNull ItemStack> populate(@NotNull Inventory inventory, int slots) {
        fillInventory(inventory, ThreadLocalRandom.current(), defaultContext());
        return Collections.emptyList();
    }

    @Override
    public @NotNull List<@NotNull ItemStack> populateRandomly(@NotNull Inventory inventory, int slots) {
        fillInventory(inventory, ThreadLocalRandom.current(), defaultContext());
        return Collections.emptyList();
    }

    @NotNull
    private static LootContext defaultContext() {
        World world = Bukkit.getWorlds().get(0);
        Location location = world.getSpawnLocation();
        return new LootContext.Builder(location).build();
    }

    @Override
    public @NotNull Collection<ItemStack> populateLoot(@NotNull Random random, @NotNull LootContext context) {
        var lootTable = getLootTable();
        if (lootTable != null)
            return lootTable.populateLoot(random, context);
        return Collections.emptyList();
    }

    @Override
    public void fillInventory(@NotNull Inventory inventory, @NotNull Random random, @NotNull LootContext context) {
        var lootTable = getLootTable();
        if (lootTable != null)
            lootTable.fillInventory(inventory, random, context);
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    public @NotNull NamespacedKey getKey() {
        return NamespacedKey.fromString("lootpool:" + key);
    }

}

