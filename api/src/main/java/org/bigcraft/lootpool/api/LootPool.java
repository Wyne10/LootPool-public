package org.bigcraft.lootpool.api;

import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.*;

public record LootPool(List<Loot> lootPool) implements ConfigurationSerializable {

    public LootPool(LootPool lootPool) {
        this(lootPool.lootPool());
    }

    public LootPool(Map<String, Object> args) {
        this(LootPool.deserialize(args));
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        for (int i = 0; i < lootPool.size(); i++) {
            data.put(String.valueOf(i), lootPool.get(i));
        }
        return data;
    }

    @SuppressWarnings("unchecked")
    public static LootPool deserialize(Map<String, Object> args) {
        List<Loot> lootPool = new ArrayList<>();
        for (Object loot : args.values()) {
            if (loot instanceof Loot)
                lootPool.add((Loot) loot);
        }
        return new LootPool(List.copyOf(lootPool));
    }

    public LootPool sortByWeights() {
        return new LootPool(lootPool.stream().sorted(Comparator.comparingInt(Loot::weight)).toList());
    }

}

