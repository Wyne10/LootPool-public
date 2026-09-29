package me.wyne.lootpool.api.complex;

import me.wyne.lootpool.api.LootPool;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A {@link LootPool} that can take a {@link LootRollContext} into account when rolling.
 * <p>
 * This is an opt-in addition rather than a change to {@link LootPool}, so existing implementations
 * and callers are unaffected. Callers that know who the loot is for should prefer the context
 * overloads:
 * <pre>{@code
 * List<ItemStack> drops = lootPool instanceof ContextualLootPool contextual
 *         ? contextual.populate(slots, LootRollContext.of(player))
 *         : lootPool.populate(slots);
 * }</pre>
 * The context-free {@link LootPool} methods remain valid on implementations of this interface; they
 * behave as though {@link LootRollContext#EMPTY} had been passed.
 */
public interface ContextualLootPool extends LootPool {

    /**
     * Rolls this pool's natural number of items — whatever its own roll counts produce — rather than
     * a caller-supplied count.
     *
     * @return the rolled items, in roll order
     */
    @NotNull
    List<@NotNull ItemStack> populate(@NotNull LootRollContext context);

    /**
     * Rolls {@code slots} items, evaluating conditions and modifiers against {@code context}.
     *
     * @param slots the maximum number of items to produce
     * @return the rolled items, in roll order
     */
    @NotNull
    List<@NotNull ItemStack> populate(int slots, @NotNull LootRollContext context);

    /**
     * Rolls {@code slots} items against {@code context} and places them into {@code inventory}'s
     * empty slots in slot order.
     *
     * @return the rolled items that did not fit
     */
    @NotNull
    List<@NotNull ItemStack> populate(@NotNull Inventory inventory, int slots, @NotNull LootRollContext context);

    /**
     * Like {@link #populate(Inventory, int, LootRollContext)}, but each item goes into a randomly
     * chosen empty slot instead of filling slots in order.
     *
     * @return the rolled items that did not fit
     */
    @NotNull
    List<@NotNull ItemStack> populateRandomly(@NotNull Inventory inventory, int slots, @NotNull LootRollContext context);

    /**
     * Rolls this pool's natural number of items against {@code context} and places them into
     * {@code inventory}'s empty slots in slot order.
     * <p>
     * Note that this is <em>not</em> the context-carrying equivalent of
     * {@link LootPool#populate(Inventory)}, which rolls one item per slot: here the pool's own roll
     * counts decide how many items there are, and a pool that produces fewer than the inventory
     * holds leaves the rest of it empty.
     *
     * @return the rolled items that did not fit
     */
    @NotNull
    default List<@NotNull ItemStack> populate(@NotNull Inventory inventory, @NotNull LootRollContext context) {
        return LootPool.populate(populate(context), inventory);
    }

    /**
     * Like {@link #populate(Inventory, LootRollContext)}, but each item goes into a randomly chosen
     * empty slot instead of filling slots in order.
     *
     * @return the rolled items that did not fit
     */
    @NotNull
    default List<@NotNull ItemStack> populateRandomly(@NotNull Inventory inventory, @NotNull LootRollContext context) {
        return LootPool.populateRandomly(populate(context), inventory);
    }

}
