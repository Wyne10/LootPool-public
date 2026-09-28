package me.wyne.lootpool.api;

import org.jetbrains.annotations.NotNull;

/**
 * A {@link LootPool} that can be copied under a different registry key, without mutating the
 * original instance. This is what makes a pool type cloneable by {@code /lootpool clone}, which
 * works on any implementation rather than only on the ones an editing GUI understands.
 * <p>
 * Unlike {@link EditableLootPool#withLoot(String, java.util.List)}, which replaces a pool's
 * contents with a flat loot list, this preserves whatever the implementation actually holds — a
 * {@link CompositeLootPool}'s sub-pool weights, a {@link RollLootPool}'s roll range, a
 * {@link me.wyne.lootpool.api.complex.ComplexLootPool}'s rolls and modifiers — so the copy behaves
 * identically to the original.
 * <p>
 * It is deliberately a separate interface rather than a method on {@link LootPool}, so that
 * third-party pool implementations keep compiling and running unchanged; they simply do not appear
 * as clone targets until they opt in.
 */
public interface CloneableLootPool extends LootPool {
    /**
     * Returns a copy of this pool, of the same implementation type, registered under {@code key}.
     * Does not modify this instance.
     *
     * @param key the new pool's identifier in the plugin's registry
     */
    @NotNull
    CloneableLootPool withKey(@NotNull String key);
}
