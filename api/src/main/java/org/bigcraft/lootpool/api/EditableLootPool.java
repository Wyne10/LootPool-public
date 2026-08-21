package org.bigcraft.lootpool.api;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A {@link LootPool} that supports being rebuilt with a different key and/or loot list, without
 * mutating the original instance. Implemented by pool types (e.g. {@link BasicLootPool}) whose
 * contents can be replaced wholesale, such as by an in-game editing GUI or command.
 */
public interface EditableLootPool extends LootPool {
    /**
     * Returns a new pool of the same implementation type with {@code key} and {@code lootList}
     * in place of this pool's own. Does not modify this instance.
     */
    @NotNull
    EditableLootPool withLoot(@NotNull String key, @NotNull List<@NotNull Loot> lootList);
}