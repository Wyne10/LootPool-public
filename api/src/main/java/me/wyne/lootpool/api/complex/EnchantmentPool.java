package me.wyne.lootpool.api.complex;

import me.wyne.lootpool.api.complex.modifier.EnchantModifier;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A named, reusable set of weighted {@link EnchantmentEntry}s, drawn from by
 * {@link EnchantModifier}. Enchantment pools live in their own registry — see
 * {@link EnchantmentPoolProvider} — so one pool can be shared by any number of loot pools.
 *
 * @param key     this pool's identifier in the enchantment registry
 * @param entries the pool's weighted enchantment entries
 */
@SerializableAs("EnchantmentPool")
public record EnchantmentPool(@NotNull String key, @NotNull List<@NotNull EnchantmentEntry> entries) implements ConfigurationSerializable {

    /** An empty pool, used as a fallback where a valid but harmless pool is required. */
    public static final EnchantmentPool EMPTY = new EnchantmentPool("empty", List.of());

    public EnchantmentPool {
        entries = List.copyOf(entries);
    }

    /**
     * Draws up to {@code count} distinct enchantments and rolls a level for each.
     * <p>
     * An entry is never drawn twice. When {@code onlyCompatible} is set, entries that
     * {@code target} cannot carry, or that conflict with an already-drawn enchantment, are skipped;
     * books are exempt from the "can this item carry it" check, matching vanilla, since an enchanted
     * book may hold any enchantment. Fewer than {@code count} enchantments are returned when the
     * pool runs out of eligible entries.
     *
     * @param count          how many enchantments to draw
     * @param target         the item the enchantments are destined for
     * @param onlyCompatible whether to respect item compatibility and enchantment conflicts
     * @return the drawn enchantments mapped to their rolled levels, in draw order
     */
    @NotNull
    public Map<@NotNull Enchantment, @NotNull Integer> roll(int count, @NotNull ItemStack target, boolean onlyCompatible) {
        Map<Enchantment, Integer> result = new LinkedHashMap<>();
        if (count <= 0 || entries.isEmpty())
            return result;

        boolean isBook = target.getType() == Material.BOOK || target.getType() == Material.ENCHANTED_BOOK;
        List<EnchantmentEntry> remaining = new ArrayList<>(entries);

        while (result.size() < count && !remaining.isEmpty()) {
            EnchantmentEntry entry = drawFrom(remaining);
            if (entry == null)
                break;
            remaining.remove(entry);

            if (result.containsKey(entry.enchantment()))
                continue;
            if (onlyCompatible && !isCompatible(entry.enchantment(), target, result.keySet(), isBook))
                continue;

            result.put(entry.enchantment(), entry.rollLevel());
        }

        return result;
    }

    private static boolean isCompatible(@NotNull Enchantment enchantment, @NotNull ItemStack target,
                                        @NotNull Iterable<@NotNull Enchantment> chosen, boolean isBook) {
        if (!isBook && !enchantment.canEnchantItem(target))
            return false;
        for (Enchantment other : chosen) {
            if (enchantment.conflictsWith(other))
                return false;
        }
        return true;
    }

    @Nullable
    private static EnchantmentEntry drawFrom(@NotNull List<@NotNull EnchantmentEntry> candidates) {
        int totalWeight = 0;
        for (EnchantmentEntry entry : candidates) {
            if (entry.weight() > 0)
                totalWeight += entry.weight();
        }

        if (totalWeight == 0)
            return null;

        int randomValue = ThreadLocalRandom.current().nextInt(totalWeight);

        int currentSum = 0;
        for (EnchantmentEntry entry : candidates) {
            if (entry.weight() <= 0)
                continue;
            currentSum += entry.weight();
            if (randomValue < currentSum) {
                return entry;
            }
        }

        return null;
    }

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("key", key);
        data.put("entries", LootSerialization.serializeList(entries));
        return data;
    }

    /**
     * Reconstructs an {@code EnchantmentPool} from a {@link #serialize()}-style map. Entries naming
     * an enchantment that does not exist on this server are dropped.
     */
    @NotNull
    public static EnchantmentPool deserialize(@NotNull Map<String, Object> args) {
        Object key = args.get("key");
        return new EnchantmentPool(
                key != null ? key.toString() : "",
                LootSerialization.deserializeList(args.get("entries"), EnchantmentEntry.class));
    }

}
