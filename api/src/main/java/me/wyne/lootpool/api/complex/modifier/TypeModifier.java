package me.wyne.lootpool.api.complex.modifier;

import me.wyne.lootpool.api.complex.LootRollContext;
import me.wyne.lootpool.api.complex.LootSerialization;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Replaces the item's material, keeping its stack size, the equivalent of vanilla's
 * {@code set_item}.
 * <p>
 * Meta that the new material cannot carry is dropped by Bukkit on conversion - turning an enchanted
 * sword into a stone block keeps neither the enchantments nor the damage - so put this before the
 * modifiers that decorate the item rather than after them.
 * <p>
 * {@link Material#AIR} is not a way to remove an item: the result would still be counted as one of
 * the pool's drops. Use {@link DiscardModifier} for that.
 *
 * @param material the material to give the item
 */
@SerializableAs("TypeModifier")
public record TypeModifier(@NotNull Material material) implements ConfigurationSerializable, LootModifier {

    @Override
    @NotNull
    public ItemStack apply(@NotNull ItemStack item, @NotNull LootRollContext context) {
        item.setType(material);
        return item;
    }

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("material", material.name());
        return data;
    }

    /**
     * Reconstructs a {@code TypeModifier} from a {@link #serialize()}-style map.
     *
     * @return the modifier, or {@code null} if {@code material} does not name a material that
     *         exists on this server - the caller is expected to drop it rather than fail the load
     */
    @Nullable
    public static TypeModifier deserialize(@NotNull Map<String, Object> args) {
        @Nullable Material material = LootSerialization.deserializeEnum(args.get("material"), Material.class);
        return material != null ? new TypeModifier(material) : null;
    }

}
