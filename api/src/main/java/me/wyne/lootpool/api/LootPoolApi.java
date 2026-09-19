package me.wyne.lootpool.api;

/**
 * Static access point for the plugin's {@link LootPoolProvider}. Pool implementations that need
 * to resolve other pools by key (e.g. {@link MultiLootPool}, {@link CompositeLootPool}, {@link RollLootPool})
 * read the provider through {@link #getProvider()}.
 * <p>
 * The provider is set once by the plugin during startup via {@link #setProvider(LootPoolProvider)};
 * this holder is not synchronized, and {@link #getProvider()} returns {@code null} until that call
 * has happened.
 */
public final class LootPoolApi {
    private static LootPoolProvider provider;

    /**
     * Installs the active {@link LootPoolProvider}. Intended to be called once by the plugin during startup.
     */
    public static void setProvider(LootPoolProvider provider) {
        LootPoolApi.provider = provider;
    }

    /**
     * @return the active provider, or {@code null} if {@link #setProvider(LootPoolProvider)} has not been called yet
     */
    public static LootPoolProvider getProvider() {
        return provider;
    }
}
