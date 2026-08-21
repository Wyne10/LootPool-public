package org.bigcraft.lootpool.api;

import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A single droppable entry within a {@link LootPool}: an item template, its relative selection
 * {@code weight}, and an inclusive stack-size range rolled by {@link #rollAmount()}.
 *
 * @param item      the item template this entry produces; cloned by the {@code create} methods, never mutated
 * @param weight    this entry's relative chance of being picked by {@link LootPool#getRandom(List)}; entries with weight {@code 0} can never be picked
 * @param minAmount the inclusive lower bound of the rolled stack size
 * @param maxAmount the inclusive upper bound of the rolled stack size
 */
public record Loot(@NotNull ItemStack item, int weight, int minAmount, int maxAmount) implements ConfigurationSerializable {

    /** A sentinel entry of one air block, used as a fallback where no real loot is available. */
    public static final Loot EMPTY = new Loot(new ItemStack(Material.AIR), 1, 1, 1);

    public Loot(@NotNull Loot loot) {
        this(loot.item(), loot.weight(), loot.minAmount(), loot.maxAmount());
    }

    /**
     * Reconstructs a {@code Loot} from a {@link #serialize()}-style map.
     */
    public Loot(@NotNull Map<String, Object> args) {
        this(Loot.deserialize(args));
    }

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("item", item);
        data.put("weight", weight);
        data.put("minAmount", minAmount);
        data.put("maxAmount", maxAmount);
        return data;
    }

    /**
     * Reconstructs a {@code Loot} from a {@link #serialize()}-style map.
     */
    @NotNull
    public static Loot deserialize(@NotNull Map<String, Object> args) {
        return new Loot((ItemStack) args.get("item"), NumberConversions.toInt(args.get("weight")), NumberConversions.toInt(args.get("minAmount")), NumberConversions.toInt(args.get("maxAmount")));
    }

    /**
     * Rolls a stack size in the inclusive range between {@link #minAmount()} and {@link #maxAmount()}.
     * The bounds are normalized first (order-independent, clamped to non-negative), so a
     * reversed or negative range still produces a valid amount rather than throwing.
     *
     * @return the rolled amount, or a fixed value if {@code minAmount} and {@code maxAmount} normalize to the same value
     */
    public int rollAmount() {
        int min = Math.max(0, Math.min(minAmount, maxAmount));
        int max = Math.max(0, Math.max(minAmount, maxAmount));
        return min == max ? min : ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    /**
     * @return a clone of {@link #item()} with its amount set via {@link #rollAmount()}
     */
    @NotNull
    public ItemStack create() {
        var item = this.item.clone();
        item.setAmount(rollAmount());
        return item;
    }

    /**
     * Like {@link #create()}, but lets the caller pin the amount to a bound instead of rolling it.
     *
     * @param amount {@link Amount#MIN} or {@link Amount#MAX} to use that bound exactly; {@link Amount#RANDOM}
     *               or {@code null} to roll via {@link #rollAmount()}
     * @return a clone of {@link #item()} with the resolved amount
     */
    @NotNull
    public ItemStack create(@Nullable Amount amount) {
        var item = this.item.clone();
        var finalAmount = rollAmount();
        if (amount != null) {
            switch (amount) {
                case MIN -> finalAmount = minAmount;
                case MAX -> finalAmount = maxAmount;
            }
        }
        item.setAmount(finalAmount);
        return item;
    }

    /**
     * Like {@link #create()}, but sets an explicit {@code amount} instead of rolling or using the
     * configured min/max bounds at all.
     *
     * @return a clone of {@link #item()} with its amount set to {@code amount}
     */
    @NotNull
    public ItemStack create(int amount) {
        var item = this.item.clone();
        item.setAmount(amount);
        return item;
    }

    /** Selects which bound {@link #create(Amount)} should use instead of rolling a random amount. */
    public enum Amount {
        MIN,
        MAX,
        RANDOM
    }

}
