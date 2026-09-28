package me.wyne.lootpool.api.complex.modifier;

import me.wyne.lootpool.api.complex.ComplexLootPool;
import me.wyne.lootpool.api.complex.LootEntry;
import me.wyne.lootpool.api.complex.LootRoll;
import me.wyne.lootpool.api.complex.LootRollContext;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A transformation applied to an item after it has been rolled, equivalent to a vanilla item
 * modifier (loot table "function"). Modifiers can be attached to a {@link LootEntry}, a
 * {@link LootRoll} or the whole {@link ComplexLootPool}; they are applied entry-first, then roll,
 * then pool.
 * <p>
 * The interface is deliberately a single method: conditional application is expressed by wrapping a
 * modifier in a {@link ConditionalModifier} rather than by every implementation having to carry a
 * condition list of its own.
 * <p>
 * Implementations must be {@link ConfigurationSerializable} and registered with
 * {@link org.bukkit.configuration.serialization.ConfigurationSerialization}, or they cannot be
 * saved. They must not mutate {@code item} in place unless they return it.
 */
public interface LootModifier extends ConfigurationSerializable {

    /**
     * Applies this modifier to {@code item}.
     *
     * @param item    the item to transform; owned by the caller's roll, safe to mutate and return
     * @param context the context the containing pool is being rolled against
     * @return the transformed item, which may be {@code item} itself
     */
    @NotNull
    ItemStack apply(@NotNull ItemStack item, @NotNull LootRollContext context);

    /**
     * Applies every modifier in {@code modifiers} to {@code item}, in order, feeding each one the
     * previous one's result.
     *
     * @return the item after the last modifier, or {@code item} unchanged if {@code modifiers} is empty
     */
    @NotNull
    static ItemStack applyAll(@NotNull List<@NotNull LootModifier> modifiers, @NotNull ItemStack item, @NotNull LootRollContext context) {
        ItemStack result = item;
        for (LootModifier modifier : modifiers) {
            result = modifier.apply(result, context);
        }
        return result;
    }

}
