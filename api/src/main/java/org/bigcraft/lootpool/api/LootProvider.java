package org.bigcraft.lootpool.api;

import org.jetbrains.annotations.Nullable;

public interface LootProvider {
    @Nullable
    KeyedLoot getLoot(String key);
    @Nullable
    KeyedLoot removeLoot(String key);
    void addLoot(KeyedLoot loot);
    void writeLoot(KeyedLoot loot);
    void reload();
}
