package me.wyne.lootpool.api.complex;

import me.wyne.lootpool.api.CloneableLootPool;
import me.wyne.lootpool.api.Loot;
import me.wyne.lootpool.api.LootPool;
import me.wyne.lootpool.api.complex.condition.LootCondition;
import me.wyne.lootpool.api.complex.modifier.EnchantModifier;
import me.wyne.lootpool.api.complex.modifier.LootModifier;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * A {@link LootPool} shaped like a vanilla loot table: an ordered list of {@link LootRoll}s, each
 * with its own roll count and weighted entries, plus {@link LootModifier}s that transform whatever
 * is produced and {@link LootCondition}s that gate the whole thing.
 * <p>
 * This is the type to use when a drop's <em>composition</em> matters rather than just its
 * probabilities — "3 to 5 commons, 1 tool, 1 guaranteed quest item" is three rolls, where a flat
 * weighted list could only approximate it. It is also the only pool type whose items can vary after
 * being picked: an {@link EnchantModifier} turns one sword template into a spread of differently
 * enchanted swords, instead of needing one entry per combination.
 *
 * <h2>Rolling</h2>
 * {@link #populate(LootRollContext)} runs the real pipeline: pool conditions, then each roll in
 * order, applying entry modifiers, then roll modifiers, then pool modifiers to every item. The
 * number of items produced is whatever the rolls yield.
 * <p>
 * The explicit-{@code slots} overloads truncate that result to {@code slots}; they never pad it, so
 * asking for more items than the rolls produce simply returns fewer. Following
 * {@link me.wyne.lootpool.api.RollLootPool}'s convention, the no-slots convenience methods
 * ({@link #populateList(Inventory)}, {@link #populate(Inventory)}, {@link #populateRandomly(Inventory)},
 * {@link #populateLoot(Random, LootContext)} and {@link #fillInventory(Inventory, Random, LootContext)})
 * use the pool's natural output rather than the inventory's size.
 *
 * <h2>Previews</h2>
 * {@link #getLootList()} returns the flattened union of every entry across every roll, for
 * inspection purposes. It does not reflect the actual roll behavior — roll counts, conditions and
 * modifiers are all absent from it — and neither does {@link #getRandom()}.
 *
 * @param key        this pool's identifier in the plugin's registry
 * @param rolls      the roll groups, evaluated in order
 * @param modifiers  modifiers applied to every item this pool produces, after each roll's
 * @param conditions conditions that must all pass for this pool to produce anything
 */
@SerializableAs("ComplexLootPool")
public record ComplexLootPool(@NotNull String key,
                              @NotNull List<@NotNull LootRoll> rolls,
                              @NotNull List<@NotNull LootModifier> modifiers,
                              @NotNull List<@NotNull LootCondition> conditions)
        implements ConfigurationSerializable, LootPool, ContextualLootPool, CloneableLootPool {

    /** An empty pool, used as a fallback where a valid but harmless pool is required. */
    public static final ComplexLootPool EMPTY = new ComplexLootPool("empty", List.of(), List.of(), List.of());

    /**
     * How many levels of {@link PoolEntry} reference {@link #getLootList()} resolves before giving
     * up, which is what stops two pools referencing each other from recursing forever.
     */
    public static final int FLATTEN_DEPTH = 4;

    public ComplexLootPool {
        rolls = List.copyOf(rolls);
        modifiers = List.copyOf(modifiers);
        conditions = List.copyOf(conditions);
    }

    /**
     * Creates an empty pool with no rolls, modifiers or conditions.
     */
    public ComplexLootPool(@NotNull String key) {
        this(key, List.of(), List.of(), List.of());
    }

    @Override
    @NotNull
    public List<@NotNull ItemStack> populate(@NotNull LootRollContext context) {
        if (!LootCondition.testAll(conditions, context))
            return List.of();

        List<ItemStack> result = new ArrayList<>();
        for (LootRoll roll : rolls) {
            for (ItemStack item : roll.generate(context)) {
                ItemStack modified = LootModifier.applyAll(modifiers, item, context);
                if (modified != null)
                    result.add(modified);
            }
        }
        return result;
    }

    /**
     * Runs the pipeline and truncates the result to {@code slots}. Shorter results are returned
     * as-is rather than padded.
     */
    @Override
    @NotNull
    public List<@NotNull ItemStack> populate(int slots, @NotNull LootRollContext context) {
        if (slots <= 0)
            return List.of();
        List<ItemStack> result = populate(context);
        return result.size() <= slots ? result : List.copyOf(result.subList(0, slots));
    }

    @Override
    @NotNull
    public List<@NotNull ItemStack> populate(@NotNull Inventory inventory, int slots, @NotNull LootRollContext context) {
        return LootPool.populate(populate(slots, context), inventory);
    }

    @Override
    @NotNull
    public List<@NotNull ItemStack> populateRandomly(@NotNull Inventory inventory, int slots, @NotNull LootRollContext context) {
        return LootPool.populateRandomly(populate(slots, context), inventory);
    }

    /**
     * Equivalent to {@link #populate(int, LootRollContext)} with {@link LootRollContext#EMPTY},
     * which means every condition needing a player fails.
     */
    @Override
    @NotNull
    public List<@NotNull ItemStack> populate(int slots) {
        return populate(slots, LootRollContext.EMPTY);
    }

    /**
     * Equivalent to {@link #populate(Inventory, int, LootRollContext)} with {@link LootRollContext#EMPTY}.
     */
    @Override
    @NotNull
    public List<@NotNull ItemStack> populate(@NotNull Inventory inventory, int slots) {
        return populate(inventory, slots, LootRollContext.EMPTY);
    }

    /**
     * Equivalent to {@link #populateRandomly(Inventory, int, LootRollContext)} with
     * {@link LootRollContext#EMPTY}.
     */
    @Override
    @NotNull
    public List<@NotNull ItemStack> populateRandomly(@NotNull Inventory inventory, int slots) {
        return populateRandomly(inventory, slots, LootRollContext.EMPTY);
    }

    /**
     * Overrides the {@link LootPool} default to produce this pool's natural output rather than one
     * item per inventory slot.
     */
    @Override
    @NotNull
    public List<@NotNull ItemStack> populateList(@NotNull Inventory inventory) {
        return populate(LootRollContext.EMPTY);
    }

    /**
     * Equivalent to {@link #populate(Inventory, LootRollContext)} with {@link LootRollContext#EMPTY},
     * overriding the {@link LootPool} default to produce this pool's natural output rather than one
     * item per inventory slot.
     */
    @Override
    @NotNull
    public List<@NotNull ItemStack> populate(@NotNull Inventory inventory) {
        return populate(inventory, LootRollContext.EMPTY);
    }

    /**
     * Equivalent to {@link #populateRandomly(Inventory, LootRollContext)} with
     * {@link LootRollContext#EMPTY}, overriding the {@link LootPool} default to produce this pool's
     * natural output rather than one item per inventory slot.
     */
    @Override
    @NotNull
    public List<@NotNull ItemStack> populateRandomly(@NotNull Inventory inventory) {
        return populateRandomly(inventory, LootRollContext.EMPTY);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Produces this pool's natural output, using {@code context}'s location and killer when they
     * are available.
     */
    @Override
    @NotNull
    public Collection<ItemStack> populateLoot(@NotNull Random random, @NotNull LootContext context) {
        return populate(fromBukkit(context));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Produces this pool's natural output rather than one item per inventory slot.
     */
    @Override
    public void fillInventory(@NotNull Inventory inventory, @NotNull Random random, @NotNull LootContext context) {
        LootPool.populate(populate(fromBukkit(context)), inventory);
    }

    @NotNull
    private static LootRollContext fromBukkit(@NotNull LootContext context) {
        return new LootRollContext(
                context.getKiller() instanceof org.bukkit.entity.Player player ? player : null,
                context.getLocation(),
                context.getLuck());
    }

    /**
     * Returns the flattened union of every entry across every roll, for inspection purposes. This
     * does not reflect the actual roll behavior, which applies roll counts, conditions and
     * modifiers; see {@link #populate(LootRollContext)}.
     */
    @Override
    @NotNull
    public List<@NotNull Loot> getLootList() {
        List<Loot> result = new ArrayList<>();
        for (LootRoll roll : rolls) {
            result.addAll(roll.flatten(FLATTEN_DEPTH));
        }
        return List.copyOf(result);
    }

    /**
     * Weighted-random-selects one entry from {@link #getLootList()}. Like that method, this is a
     * preview: it ignores rolls, conditions and modifiers.
     *
     * @return the selected entry, or {@link Loot#EMPTY} if the pool has no eligible entries
     */
    @Override
    @NotNull
    public Loot getRandom() {
        return LootPool.getRandom(getLootList());
    }

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("key", key);
        data.put("rolls", LootSerialization.serializeList(rolls));
        data.put("modifiers", LootSerialization.serializeList(modifiers));
        data.put("conditions", LootSerialization.serializeList(conditions));
        return data;
    }

    /**
     * Reconstructs a {@code ComplexLootPool} from a {@link #serialize()}-style map.
     */
    @NotNull
    public static ComplexLootPool deserialize(@NotNull Map<String, Object> args) {
        Object key = args.get("key");
        return new ComplexLootPool(
                key != null ? key.toString() : "",
                LootSerialization.deserializeList(args.get("rolls"), LootRoll.class),
                LootSerialization.deserializeList(args.get("modifiers"), LootModifier.class),
                LootSerialization.deserializeList(args.get("conditions"), LootCondition.class));
    }

    /**
     * Returns a copy of this pool registered under {@code key}, with the same rolls, modifiers and conditions.
     */
    @Override
    @NotNull
    public ComplexLootPool withKey(@NotNull String key) {
        return new ComplexLootPool(key, rolls, modifiers, conditions);
    }

    /**
     * Returns this pool's synthetic {@code "lootpool:" + key} registry key.
     */
    @SuppressWarnings("DataFlowIssue")
    @Override
    @NotNull
    public NamespacedKey getKey() {
        return NamespacedKey.fromString("lootpool:" + key);
    }

}
