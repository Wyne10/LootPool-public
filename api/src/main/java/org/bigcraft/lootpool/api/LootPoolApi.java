package org.bigcraft.lootpool.api;

public final class LootPoolApi {
    private static LootPoolProvider provider;
    private static LootProvider lootProvider;

    public static void setProvider(LootPoolProvider provider) {
        LootPoolApi.provider = provider;
    }

    public static void setLootProvider(LootProvider lootProvider) {
        LootPoolApi.lootProvider = lootProvider;
    }

    public static LootPoolProvider getProvider() {
        return provider;
    }

    public static LootProvider getLootProvider() {
        return lootProvider;
    }
}
