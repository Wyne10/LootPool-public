package org.bigcraft.lootpool.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface LootPoolProvider extends CommonLootProvider {
    @NotNull
    Map<@NotNull String, @NotNull LootPool> getLootPoolMap();
    @Nullable
    LootPool getLootPool(@NotNull String key);
    @Nullable
    LootPool removeLootPool(@NotNull String key);
    void addLootPool(@NotNull LootPool lootPool);
    void writeLootPool(@NotNull LootPool lootPool);
    void reload();
}
