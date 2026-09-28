package me.wyne.lootpool.api.complex.modifier;

import me.wyne.lootpool.api.LootPoolApi;
import me.wyne.lootpool.api.complex.EnchantmentPool;
import me.wyne.lootpool.api.complex.EnchantmentPoolProvider;
import me.wyne.lootpool.api.complex.LootRollContext;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Applies a random selection of enchantments drawn from a named {@link EnchantmentPool}, each at a
 * level rolled from that entry's own range. Roughly vanilla's {@code enchant_randomly}, but with
 * the candidate set and its level ranges configured rather than derived from enchanting-table
 * mechanics.
 * <p>
 * Books receive their enchantments as stored enchantments ({@link EnchantmentStorageMeta}), so a
 * {@link Material#BOOK} entry becomes a usable enchanted book. Everything else is enchanted
 * directly, ignoring vanilla level caps.
 *
 * @param pool           the registry key of the {@link EnchantmentPool} to draw from
 * @param minEnchants    the inclusive lower bound on how many enchantments to apply
 * @param maxEnchants    the inclusive upper bound on how many enchantments to apply
 * @param onlyCompatible whether to skip enchantments the item cannot carry, and ones that conflict with an already-drawn enchantment
 */
@SerializableAs("EnchantModifier")
public record EnchantModifier(@NotNull String pool, int minEnchants, int maxEnchants, boolean onlyCompatible) implements ConfigurationSerializable, LootModifier {

    /**
     * Resolves {@link #pool()} in the enchantment registry.
     *
     * @return the referenced pool, or {@code null} if it does not currently resolve
     */
    @Nullable
    public EnchantmentPool getEnchantmentPool() {
        EnchantmentPoolProvider provider = LootPoolApi.getEnchantmentProvider();
        return provider != null ? provider.getEnchantmentPool(pool) : null;
    }

    /**
     * Rolls how many enchantments to apply, normalized the same way as
     * {@link me.wyne.lootpool.api.Loot#rollAmount()} (order-independent, clamped to non-negative).
     */
    public int rollCount() {
        int min = Math.max(0, Math.min(minEnchants, maxEnchants));
        int max = Math.max(0, Math.max(minEnchants, maxEnchants));
        return min == max ? min : ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    @Override
    @NotNull
    public ItemStack apply(@NotNull ItemStack item, @NotNull LootRollContext context) {
        EnchantmentPool enchantmentPool = getEnchantmentPool();
        if (enchantmentPool == null)
            return item;

        Map<Enchantment, Integer> enchantments = enchantmentPool.roll(rollCount(), item, onlyCompatible);
        if (enchantments.isEmpty())
            return item;

        if (item.getType() == Material.BOOK)
            item.setType(Material.ENCHANTED_BOOK);

        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;

        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            enchantments.forEach((enchantment, level) -> storageMeta.addStoredEnchant(enchantment, level, true));
        } else {
            enchantments.forEach((enchantment, level) -> meta.addEnchant(enchantment, level, true));
        }
        item.setItemMeta(meta);
        return item;
    }

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pool", pool);
        data.put("minEnchants", minEnchants);
        data.put("maxEnchants", maxEnchants);
        data.put("onlyCompatible", onlyCompatible);
        return data;
    }

    /**
     * Reconstructs an {@code EnchantModifier} from a {@link #serialize()}-style map.
     */
    @NotNull
    public static EnchantModifier deserialize(@NotNull Map<String, Object> args) {
        Object pool = args.get("pool");
        return new EnchantModifier(
                pool != null ? pool.toString() : "",
                NumberConversions.toInt(args.get("minEnchants")),
                NumberConversions.toInt(args.get("maxEnchants")),
                !(args.get("onlyCompatible") instanceof Boolean onlyCompatible) || onlyCompatible);
    }

}
