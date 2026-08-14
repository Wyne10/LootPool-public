package org.bigcraft.lootpool.api;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface EditableLootPool extends LootPool {
    @NotNull
    EditableLootPool withLoot(@NotNull String key, @NotNull List<@NotNull Loot> lootList);
}