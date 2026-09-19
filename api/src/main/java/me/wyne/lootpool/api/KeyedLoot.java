package me.wyne.lootpool.api;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link LootPool} wrapping exactly one {@link Loot} entry. {@link #getRandom()} always returns
 * that entry (its weight is irrelevant since there is nothing to compete against), making this
 * the simplest way to expose a single fixed drop as a {@code LootPool}.
 *
 * @param key  this pool's identifier in the plugin's registry
 * @param loot the single loot entry this pool always produces
 */
public record KeyedLoot(@NotNull String key, @NotNull Loot loot) implements ConfigurationSerializable, LootPool {

    public KeyedLoot(@NotNull KeyedLoot keyedLoot) {
        this(keyedLoot.key(), keyedLoot.loot());
    }

    /**
     * Reconstructs a {@code KeyedLoot} from a {@link #serialize()}-style map.
     */
    public KeyedLoot(@NotNull Map<String, Object> args) {
        this(deserialize(args));
    }

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("key", key);
        data.put("loot", loot);
        return data;
    }

    @NotNull
    public static KeyedLoot deserialize(@NotNull Map<String, Object> args) {
        return new KeyedLoot((String) args.get("key"), (Loot) args.get("loot"));
    }

    @Override
    public @NotNull List<@NotNull Loot> getLootList() {
        return List.of(loot);
    }

    @Override
    public @NotNull Loot getRandom() {
        return loot;
    }

    @Override
    public @NotNull List<@NotNull ItemStack> populate(int slots) {
        return LootPool.populate(getLootList(), slots);
    }

    @Override
    public @NotNull List<@NotNull ItemStack> populate(@NotNull Inventory inventory, int slots) {
        return LootPool.populate(getLootList(), inventory, slots);
    }

    @Override
    public @NotNull List<@NotNull ItemStack> populateRandomly(@NotNull Inventory inventory, int slots) {
        return LootPool.populateRandomly(getLootList(), inventory, slots);
    }

    /**
     * Returns this pool's synthetic {@code "lootpool:" + key} registry key.
     */
    @SuppressWarnings("DataFlowIssue")
    @Override
    public @NotNull NamespacedKey getKey() {
        return NamespacedKey.fromString("lootpool:" + key);
    }

}
