package me.wyne.lootpool.api.complex.modifier;

import me.wyne.lootpool.api.complex.LootRollContext;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Wears an item down by a random fraction of its durability, the equivalent of vanilla's
 * {@code set_damage}. A fraction of {@code 0} leaves the item pristine and {@code 1} leaves it one
 * hit from breaking.
 * <p>
 * A no-op on items that have no durability.
 *
 * @param minFraction the inclusive lower bound of the damage fraction, clamped to {@code [0, 1]}
 * @param maxFraction the inclusive upper bound of the damage fraction, clamped to {@code [0, 1]}
 */
@SerializableAs("DamageModifier")
public record DamageModifier(double minFraction, double maxFraction) implements ConfigurationSerializable, LootModifier {

    /**
     * Rolls a damage fraction in the inclusive range between {@link #minFraction()} and
     * {@link #maxFraction()}, normalized order-independently and clamped to {@code [0, 1]}.
     */
    public double rollFraction() {
        double min = clamp(Math.min(minFraction, maxFraction));
        double max = clamp(Math.max(minFraction, maxFraction));
        return min == max ? min : ThreadLocalRandom.current().nextDouble(min, max);
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }

    @Override
    @NotNull
    public ItemStack apply(@NotNull ItemStack item, @NotNull LootRollContext context) {
        short maxDurability = item.getType().getMaxDurability();
        if (maxDurability <= 0)
            return item;

        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable damageable))
            return item;

        int damage = (int) Math.round(maxDurability * rollFraction());
        damageable.setDamage(Math.max(0, Math.min(maxDurability - 1, damage)));
        item.setItemMeta(meta);
        return item;
    }

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("minFraction", minFraction);
        data.put("maxFraction", maxFraction);
        return data;
    }

    /**
     * Reconstructs a {@code DamageModifier} from a {@link #serialize()}-style map.
     */
    @NotNull
    public static DamageModifier deserialize(@NotNull Map<String, Object> args) {
        return new DamageModifier(
                NumberConversions.toDouble(args.get("minFraction")),
                NumberConversions.toDouble(args.get("maxFraction")));
    }

}
