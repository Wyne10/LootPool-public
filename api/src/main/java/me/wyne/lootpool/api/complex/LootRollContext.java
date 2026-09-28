package me.wyne.lootpool.api.complex;

import me.wyne.lootpool.api.complex.condition.LootCondition;
import me.wyne.lootpool.api.complex.condition.PermissionCondition;
import me.wyne.lootpool.api.complex.modifier.LootModifier;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The situational information a {@link ComplexLootPool} roll is evaluated against: who the loot is
 * being generated for, where, and with how much luck. {@link LootCondition}s read it to decide
 * whether to pass and {@link LootModifier}s to decide what to apply.
 * <p>
 * Everything on it is optional, because the {@link me.wyne.lootpool.api.LootPool} methods that
 * predate this class carry no context at all; those paths use {@link #EMPTY}. Conditions that
 * cannot be evaluated without a piece of context are expected to <em>fail closed</em> — see
 * {@link PermissionCondition} for the canonical example.
 *
 * @param player   the player the loot is destined for, or {@code null} if it is not being generated for anyone in particular
 * @param location where the loot is being generated, or {@code null} if unknown; defaults to {@code player}'s location when one is given
 * @param luck     the luck value to apply, normally {@code 0}
 */
public record LootRollContext(@Nullable Player player, @Nullable Location location, float luck) {

    /** A context carrying nothing, used by every {@code populate} overload that takes no context. */
    public static final LootRollContext EMPTY = new LootRollContext(null, null, 0f);

    /**
     * @return a context for {@code player}, using their current location
     */
    @NotNull
    public static LootRollContext of(@NotNull Player player) {
        return new LootRollContext(player, player.getLocation(), 0f);
    }

    /**
     * @return a context for {@code player} at an explicit {@code location}
     */
    @NotNull
    public static LootRollContext of(@NotNull Player player, @NotNull Location location) {
        return new LootRollContext(player, location, 0f);
    }

    /**
     * @return a context carrying only a location, for loot not generated for any particular player
     */
    @NotNull
    public static LootRollContext of(@NotNull Location location) {
        return new LootRollContext(null, location, 0f);
    }

    /**
     * @return a copy of this context with {@code luck} replaced
     */
    @NotNull
    public LootRollContext withLuck(float luck) {
        return new LootRollContext(player, location, luck);
    }

}
