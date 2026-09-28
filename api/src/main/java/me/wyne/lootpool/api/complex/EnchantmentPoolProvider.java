package me.wyne.lootpool.api.complex;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * The registry of {@link EnchantmentPool}s, the enchantment-side counterpart to
 * {@link me.wyne.lootpool.api.LootPoolProvider}. Obtain the running implementation from
 * {@link me.wyne.lootpool.api.LootPoolApi#getEnchantmentProvider()} or from Bukkit's
 * {@link org.bukkit.plugin.ServicesManager}.
 */
public interface EnchantmentPoolProvider {

    /**
     * @return every loaded enchantment pool, keyed by its registry key
     */
    @NotNull
    Map<@NotNull String, @NotNull EnchantmentPool> getEnchantmentPoolMap();

    /**
     * @return the pool registered under {@code key}, or {@code null} if there is none
     */
    @Nullable
    EnchantmentPool getEnchantmentPool(@NotNull String key);

    /**
     * Removes the pool registered under {@code key}, deleting its backing file.
     *
     * @return the removed pool, or {@code null} if there was none
     */
    @Nullable
    EnchantmentPool removeEnchantmentPool(@NotNull String key);

    /**
     * Registers {@code enchantmentPool} in memory only, without persisting it.
     */
    void addEnchantmentPool(@NotNull EnchantmentPool enchantmentPool);

    /**
     * Registers {@code enchantmentPool} and writes it to its backing file.
     */
    void writeEnchantmentPool(@NotNull EnchantmentPool enchantmentPool);

}
