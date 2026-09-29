package me.wyne.lootpool.api.complex.modifier;

import me.wyne.lootpool.api.complex.LootRollContext;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Sets the item's stack size to a freshly rolled amount, the equivalent of vanilla's
 * {@code set_count}. This is the same roll an {@link me.wyne.lootpool.api.Loot} entry already makes
 * from its own amount range, made available as a modifier so a roll or a whole pool can impose one
 * range on everything it produces.
 * <p>
 * Whatever amount the item arrived with is discarded; use {@link LimitModifier} to keep the rolled
 * amount and only bring it into range.
 *
 * @param minAmount the inclusive lower bound of the rolled amount, clamped to at least {@code 1}
 * @param maxAmount the inclusive upper bound of the rolled amount, clamped to at least {@code 1}
 */
@SerializableAs("AmountModifier")
public record AmountModifier(int minAmount, int maxAmount) implements ConfigurationSerializable, LootModifier {

    /**
     * Rolls an amount in the inclusive range between {@link #minAmount()} and {@link #maxAmount()},
     * normalized the same way as {@link me.wyne.lootpool.api.Loot#rollAmount()} (order-independent)
     * but clamped to at least {@code 1}, since a zero-sized stack would linger in the results as an
     * item that is not really there.
     */
    public int rollAmount() {
        int min = Math.max(1, Math.min(minAmount, maxAmount));
        int max = Math.max(1, Math.max(minAmount, maxAmount));
        return min == max ? min : ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    @Override
    @NotNull
    public ItemStack apply(@NotNull ItemStack item, @NotNull LootRollContext context) {
        item.setAmount(rollAmount());
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
     * Reconstructs an {@code AmountModifier} from a {@link #serialize()}-style map.
     */
    @NotNull
    public static AmountModifier deserialize(@NotNull Map<String, Object> args) {
        return new AmountModifier(
                NumberConversions.toInt(args.get("minAmount")),
                NumberConversions.toInt(args.get("maxAmount")));
    }

}
