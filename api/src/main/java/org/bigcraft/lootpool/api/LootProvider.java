package org.bigcraft.lootpool.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface LootProvider extends CommonLootProvider {
    @NotNull
    Map<@NotNull String, @NotNull KeyedLoot> getLootMap();
    @Nullable
    KeyedLoot getLoot(@NotNull String key);
    @Nullable
    KeyedLoot removeLoot(@NotNull String key);
    void addLoot(@NotNull KeyedLoot loot);
    void writeLoot(@NotNull KeyedLoot loot);
    void reload();
}
