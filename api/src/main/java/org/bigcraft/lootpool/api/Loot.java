package org.bigcraft.lootpool.api;

import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public record Loot(@NotNull ItemStack item, int weight, int minAmount, int maxAmount) implements ConfigurationSerializable {

    public static final Loot EMPTY = new Loot(new ItemStack(Material.AIR), 0, 1, 1);

    public Loot(@NotNull Loot loot) {
        this(loot.item(), loot.weight(), loot.minAmount(), loot.maxAmount());
    }

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

    @NotNull
    public static Loot deserialize(@NotNull Map<String, Object> args) {
        return new Loot((ItemStack) args.get("item"), NumberConversions.toInt(args.get("weight")), NumberConversions.toInt(args.get("minAmount")), NumberConversions.toInt(args.get("maxAmount")));
    }

    @NotNull
    public ItemStack create() {
        var item = this.item.clone();
        item.setAmount(ThreadLocalRandom.current().nextInt(minAmount, maxAmount + 1));
        return item;
    }

    @NotNull
    public ItemStack create(@Nullable Amount amount) {
        var item = this.item.clone();
        var finalAmount = ThreadLocalRandom.current().nextInt(minAmount, maxAmount + 1);
        if (amount != null) {
            switch (amount) {
                case MIN -> finalAmount = minAmount;
                case MAX -> finalAmount = maxAmount;
            }
        }
        item.setAmount(finalAmount);
        return item;
    }

    @NotNull
    public ItemStack create(int amount) {
        var item = this.item.clone();
        item.setAmount(amount);
        return item;
    }

    public enum Amount {
        MIN,
        MAX,
        RANDOM
    }

}
