package org.bigcraft.lootpool.api;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public record KeyedLoot(String key, Loot loot) implements ConfigurationSerializable, Keyed {
    
    public KeyedLoot(KeyedLoot keyedLoot) {
        this(keyedLoot.key(), keyedLoot.loot());
    }

    public KeyedLoot(Map<String, Object> args) {
        this(deserialize(args));
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("key", key);
        data.put("loot", loot);
        return data;
    }

    public static KeyedLoot deserialize(Map<String, Object> args) {
        return new KeyedLoot((String) args.get("key"), (Loot) args.get("loot"));
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    public @NotNull NamespacedKey getKey() {
        return NamespacedKey.fromString("lootpool:" + key);
    }
}
