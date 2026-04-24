package org.bigcraft.lootpool.api;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public record SnapshotLootPool(@NotNull String key, @NotNull SortedMap<@NotNull Integer, @NotNull Loot> lootPool) implements ConfigurationSerializable, LootPool {

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("key", key);
        lootPool.forEach((key, loot) -> data.put(key.toString(), loot));
        return data;
    }

    @NotNull
    public static SnapshotLootPool deserialize(@NotNull Map<String, Object> args) {
        SortedMap<Integer, Loot> lootPool = new TreeMap<>();
        for (Map.Entry<String, Object> entry : args.entrySet()) {
            if (entry.getKey().equals("key") || entry.getKey().equals("==")) continue;
            lootPool.put(Integer.parseInt(entry.getKey()), (Loot) entry.getValue());
        }
        return new SnapshotLootPool((String) args.get("key"), Collections.unmodifiableSortedMap(lootPool));
    }

    @Override
    public @NotNull List<@NotNull Loot> getLootList() {
        return List.copyOf(lootPool.values());
    }

    @Override
    public @NotNull Loot getRandom() {
        return LootPool.getRandom(getLootList());
    }

    @Override
    public @NotNull List<@NotNull ItemStack> populate(int slots) {
        List<ItemStack> result = new ArrayList<>();
        lootPool.entrySet().stream()
                .limit(slots)
                .forEach(entry -> result.add(entry.getValue().create()));
        return result;
    }

    @Override
    public @NotNull List<@NotNull ItemStack> populate(@NotNull Inventory inventory, int slots) {
        List<ItemStack> exceed = new ArrayList<>();
        lootPool.entrySet().stream()
                .limit(slots)
                .forEach(entry -> {
                    if (inventory.getSize() <= entry.getKey()) {
                        exceed.add(entry.getValue().create());
                        return;
                    }
                    var item = inventory.getItem(entry.getKey());
                    if ((item != null && item.getType() != Material.AIR)) {
                        exceed.add(entry.getValue().create());
                        return;
                    }
                    inventory.setItem(entry.getKey(), entry.getValue().create());
                });
        return exceed;
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

