package org.bigcraft.lootpool.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public interface CommonLootProvider {
    @NotNull
    Set<@NotNull String> getLootKeys();
    @Nullable
    List<@NotNull Loot> getLootList(@NotNull String key);
}
