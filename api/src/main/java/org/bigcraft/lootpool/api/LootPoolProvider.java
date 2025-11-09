package org.bigcraft.lootpool.api;

import org.jetbrains.annotations.Nullable;

public interface LootPoolProvider {
    @Nullable
    LootPool getLootPool(String key);
    @Nullable
    LootPool removeLootPool(String key);
    void addLootPool(String ley, LootPool lootPool);
    void writeLootPool(String key, LootPool lootPool);
    void reload();
}
