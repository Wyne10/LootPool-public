package me.wyne.lootpool.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * The plugin's central registry of loaded {@link LootPool}s, looked up via {@link LootPoolApi#getProvider()}
 * (typically obtained from {@link LootPoolApi} rather than implemented by consumers) or Bukkit's
 * {@code ServicesManager}. Referencing pool types such as {@link MultiLootPool}, {@link CompositeLootPool}
 * and {@link RollLootPool} resolve the pools they refer to through this registry.
 */
public interface LootPoolProvider {
    /**
     * @return all currently registered pools, keyed by their registry key
     */
    @NotNull
    Map<@NotNull String, @NotNull LootPool> getLootPoolMap();

    /**
     * @return the subset of {@link #getLootPoolMap()} whose values are assignable to {@code clazz}
     */
    @NotNull
    Map<@NotNull String, @NotNull LootPool> getMapOf(@NotNull Class<? extends LootPool> clazz);

    /**
     * @return the pool registered under {@code key}, or {@code null} if none is registered
     */
    @Nullable
    LootPool getLootPool(@NotNull String key);

    /**
     * Unregisters the pool at {@code key} and deletes its backing file on disk, if any.
     *
     * @return the removed pool, or {@code null} if none was registered under {@code key}
     */
    @Nullable
    LootPool removeLootPool(@NotNull String key);

    /**
     * Registers {@code lootPool} in memory under its own key, overwriting any existing pool with
     * the same key. Does not persist it to disk; see {@link #writeLootPool(LootPool)}.
     */
    void addLootPool(@NotNull LootPool lootPool);

    /**
     * Registers {@code lootPool} (as {@link #addLootPool(LootPool)}) and persists it to its
     * backing YAML file.
     */
    void writeLootPool(@NotNull LootPool lootPool);

    /**
     * Reloads the plugin configuration and re-reads all persisted pools from disk.
     */
    void reload();
}
