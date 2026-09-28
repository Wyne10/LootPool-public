package me.wyne.lootpool.api.complex;

import me.wyne.lootpool.api.Loot;
import me.wyne.lootpool.api.complex.condition.LootCondition;
import me.wyne.lootpool.api.complex.modifier.LootModifier;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link LootEntry} producing a single item from a {@link Loot} template — the ordinary case, and
 * the equivalent of a vanilla {@code item} entry.
 * <p>
 * The template carries the item, the selection weight and the stack-size range, so this entry's
 * {@link #weight()} is {@link Loot#weight()}; there is no second weight to keep in sync. A template
 * holding {@link Material#AIR} produces nothing, matching how an air entry means "no drop"
 * elsewhere in this plugin — though {@link EmptyEntry} states that intent more clearly.
 *
 * @param loot       the item template, its weight and its amount range
 * @param conditions conditions that must pass for this entry to be eligible for selection
 * @param modifiers  modifiers applied to the produced item
 */
@SerializableAs("ItemEntry")
public record ItemEntry(@NotNull Loot loot,
                        @NotNull List<@NotNull LootCondition> conditions,
                        @NotNull List<@NotNull LootModifier> modifiers) implements ConfigurationSerializable, LootEntry {

    public ItemEntry {
        conditions = List.copyOf(conditions);
        modifiers = List.copyOf(modifiers);
    }

    /**
     * Creates an entry with no conditions or modifiers.
     */
    public ItemEntry(@NotNull Loot loot) {
        this(loot, List.of(), List.of());
    }

    @Override
    public int weight() {
        return loot.weight();
    }

    @Override
    @NotNull
    public List<@NotNull ItemStack> generate(@NotNull LootRollContext context) {
        if (loot.item().getType() == Material.AIR)
            return List.of();
        return List.of(loot.create());
    }

    @Override
    @NotNull
    public List<@NotNull Loot> flatten(int depth) {
        return loot.item().getType() == Material.AIR ? List.of() : List.of(loot);
    }

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("loot", loot);
        data.put("conditions", LootSerialization.serializeList(conditions));
        data.put("modifiers", LootSerialization.serializeList(modifiers));
        return data;
    }

    /**
     * Reconstructs an {@code ItemEntry} from a {@link #serialize()}-style map.
     *
     * @return the entry, or {@code null} if its {@link Loot} template cannot be resolved — the
     *         caller is expected to drop it rather than fail the load
     */
    @Nullable
    public static ItemEntry deserialize(@NotNull Map<String, Object> args) {
        Loot loot = LootSerialization.deserializeValue(args.get("loot"), Loot.class);
        if (loot == null)
            return null;
        return new ItemEntry(
                loot,
                LootSerialization.deserializeList(args.get("conditions"), LootCondition.class),
                LootSerialization.deserializeList(args.get("modifiers"), LootModifier.class));
    }

}
