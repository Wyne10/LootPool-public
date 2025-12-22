package org.bigcraft.lootpool.api;

import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.NumberConversions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public record Loot(ItemStack item, int weight, int minAmount, int maxAmount) implements ConfigurationSerializable {

    public static final Loot EMPTY = new Loot(new ItemStack(Material.AIR), 1, 1, 1);

    public Loot(Loot loot) {
        this(loot.item(), loot.weight(), loot.minAmount(), loot.maxAmount());
    }

    public Loot(Map<String, Object> args) {
        this(Loot.deserialize(args));
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("item", item);
        data.put("weight", weight);
        data.put("minAmount", minAmount);
        data.put("maxAmount", maxAmount);
        return data;
    }

    public static Loot deserialize(Map<String, Object> args) {
        return new Loot((ItemStack) args.get("item"), NumberConversions.toInt(args.get("weight")), NumberConversions.toInt(args.get("minAmount")), NumberConversions.toInt(args.get("maxAmount")));
    }

    public ItemStack create() {
        var item = this.item.clone();
        item.setAmount(ThreadLocalRandom.current().nextInt(minAmount, maxAmount + 1));
        return item;
    }

}
