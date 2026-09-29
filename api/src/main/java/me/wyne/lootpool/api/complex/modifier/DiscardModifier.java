package me.wyne.lootpool.api.complex.modifier;

import me.wyne.lootpool.api.complex.LootRollContext;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Drops the item from the loot entirely, the equivalent of vanilla's {@code discard}. The roll
 * still happened - it simply produces nothing - which is different from an entry that yields a
 * stack of air, since that would still be counted as one of the pool's drops.
 * <p>
 * On its own this makes a modifier list produce nothing at all, so it is meant to be wrapped in a
 * {@link ConditionalModifier}: "discard unless the player has the permission", "discard a quarter
 * of the time". Anything after it in the same list never runs, because the item is already gone.
 */
@SerializableAs("DiscardModifier")
public record DiscardModifier() implements ConfigurationSerializable, LootModifier {

    @Override
    @Nullable
    public ItemStack apply(@NotNull ItemStack item, @NotNull LootRollContext context) {
        return null;
    }

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        return new LinkedHashMap<>();
    }

    /**
     * Reconstructs a {@code DiscardModifier} from a {@link #serialize()}-style map. It carries no
     * settings, so the map is ignored.
     */
    @NotNull
    public static DiscardModifier deserialize(@NotNull Map<String, Object> args) {
        return new DiscardModifier();
    }

}
