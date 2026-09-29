package me.wyne.lootpool.api.complex.modifier;

import me.wyne.lootpool.api.complex.LootRollContext;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Brings the item's stack size into a range without re-rolling it, the equivalent of vanilla's
 * {@code limit_count}. An amount already inside the range is left exactly as it is.
 * <p>
 * Useful on a pool or a roll as a backstop over entries that set their own amounts - it caps
 * whatever they produced rather than replacing it, which is what {@link AmountModifier} does.
 *
 * @param minAmount the inclusive lower bound, clamped to at least {@code 1}
 * @param maxAmount the inclusive upper bound, clamped to at least {@code 1}
 */
@SerializableAs("LimitModifier")
public record LimitModifier(int minAmount, int maxAmount) implements ConfigurationSerializable, LootModifier {

    /**
     * Clamps {@code amount} into this modifier's range, normalized order-independently and never
     * dropping below {@code 1}.
     */
    public int limit(int amount) {
        int min = Math.max(1, Math.min(minAmount, maxAmount));
        int max = Math.max(1, Math.max(minAmount, maxAmount));
        return Math.max(min, Math.min(max, amount));
    }

    @Override
    @NotNull
    public ItemStack apply(@NotNull ItemStack item, @NotNull LootRollContext context) {
        item.setAmount(limit(item.getAmount()));
        return item;
    }

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("minAmount", minAmount);
        data.put("maxAmount", maxAmount);
        return data;
    }

    /**
     * Reconstructs a {@code LimitModifier} from a {@link #serialize()}-style map.
     */
    @NotNull
    public static LimitModifier deserialize(@NotNull Map<String, Object> args) {
        return new LimitModifier(
                NumberConversions.toInt(args.get("minAmount")),
                NumberConversions.toInt(args.get("maxAmount")));
    }

}
