package org.bigcraft.lootpool.api;

public final class LootPoolApi {
    private static LootPoolProvider provider;

    public static void setProvider(LootPoolProvider provider) {
        LootPoolApi.provider = provider;
    }

    public static LootPoolProvider getProvider() {
        return provider;
    }
}
