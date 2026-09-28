package me.wyne.lootpool.api;

import me.wyne.lootpool.api.complex.EnchantmentPool;
import me.wyne.lootpool.api.complex.EnchantmentPoolProvider;
import me.wyne.lootpool.api.complex.modifier.LoreModifier;
import me.wyne.lootpool.api.complex.modifier.NameModifier;
import me.wyne.lootpool.api.complex.modifier.EnchantModifier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Static access point for the plugin's {@link LootPoolProvider}. Pool implementations that need
 * to resolve other pools by key (e.g. {@link MultiLootPool}, {@link CompositeLootPool}, {@link RollLootPool})
 * read the provider through {@link #getProvider()}.
 * <p>
 * The provider is set once by the plugin during startup via {@link #setProvider(LootPoolProvider)};
 * this holder is not synchronized, and {@link #getProvider()} returns {@code null} until that call
 * has happened.
 * <p>
 * The same holder also carries the {@link EnchantmentPoolProvider} used by
 * {@link EnchantModifier}, and the text renderer used by
 * {@link NameModifier} and {@link LoreModifier}.
 */
public final class LootPoolApi {
    private static LootPoolProvider provider;
    private static EnchantmentPoolProvider enchantmentProvider;
    private static Function<String, Component> textRenderer = LegacyComponentSerializer.legacyAmpersand()::deserialize;

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

    /**
     * Installs the active {@link EnchantmentPoolProvider}, the registry of reusable
     * {@link EnchantmentPool}s. Intended to be called once by the plugin during startup.
     */
    public static void setEnchantmentProvider(EnchantmentPoolProvider enchantmentProvider) {
        LootPoolApi.enchantmentProvider = enchantmentProvider;
    }

    /**
     * @return the active enchantment provider, or {@code null} if
     *         {@link #setEnchantmentProvider(EnchantmentPoolProvider)} has not been called yet
     */
    @Nullable
    public static EnchantmentPoolProvider getEnchantmentProvider() {
        return enchantmentProvider;
    }

    /**
     * Installs the function that turns the plain strings stored by {@link NameModifier} and
     * {@link LoreModifier} into components.
     * <p>
     * The plugin installs its own text format during startup so that loot text uses the same markup
     * as the language files. Until then — and for anyone using this API standalone — the default is
     * {@link LegacyComponentSerializer#legacyAmpersand()}, i.e. {@code &c}-style colour codes.
     */
    public static void setTextRenderer(@NotNull Function<String, Component> textRenderer) {
        LootPoolApi.textRenderer = textRenderer;
    }

    /**
     * @return the active text renderer, never {@code null}
     */
    @NotNull
    public static Function<String, Component> getTextRenderer() {
        return textRenderer;
    }
}
