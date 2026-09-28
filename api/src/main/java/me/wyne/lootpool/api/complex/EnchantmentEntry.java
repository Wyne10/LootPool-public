package me.wyne.lootpool.api.complex;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * One weighted candidate in an {@link EnchantmentPool}: an enchantment and the inclusive level
 * range it is rolled at.
 *
 * @param enchantment the enchantment to apply
 * @param weight      this entry's relative chance of being drawn; entries with weight {@code 0} are never drawn
 * @param minLevel    the inclusive lower bound of the rolled level
 * @param maxLevel    the inclusive upper bound of the rolled level
 */
@SerializableAs("EnchantmentEntry")
public record EnchantmentEntry(@NotNull Enchantment enchantment, int weight, int minLevel, int maxLevel) implements ConfigurationSerializable {

    /**
     * Rolls a level in the inclusive range between {@link #minLevel()} and {@link #maxLevel()},
     * normalized the same way as {@link me.wyne.lootpool.api.Loot#rollAmount()} (order-independent,
     * clamped so the result is at least {@code 1}).
     */
    public int rollLevel() {
        int min = Math.max(1, Math.min(minLevel, maxLevel));
        int max = Math.max(1, Math.max(minLevel, maxLevel));
        return min == max ? min : ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("enchantment", enchantment.getKey().toString());
        data.put("weight", weight);
        data.put("minLevel", minLevel);
        data.put("maxLevel", maxLevel);
        return data;
    }

    /**
     * Reconstructs an {@code EnchantmentEntry} from a {@link #serialize()}-style map.
     *
     * @return the entry, or {@code null} if {@code enchantment} does not name an enchantment that
     *         exists on this server — the caller is expected to drop it rather than fail the load
     */
    @Nullable
    @SuppressWarnings("deprecation")
    public static EnchantmentEntry deserialize(@NotNull Map<String, Object> args) {
        Object raw = args.get("enchantment");
        if (raw == null)
            return null;

        NamespacedKey key = NamespacedKey.fromString(raw.toString());
        Enchantment enchantment = key != null ? Enchantment.getByKey(key) : Enchantment.getByName(raw.toString());
        if (enchantment == null)
            return null;

        return new EnchantmentEntry(
                enchantment,
                NumberConversions.toInt(args.get("weight")),
                NumberConversions.toInt(args.get("minLevel")),
                NumberConversions.toInt(args.get("maxLevel")));
    }

}
