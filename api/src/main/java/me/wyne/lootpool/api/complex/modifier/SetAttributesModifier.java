package me.wyne.lootpool.api.complex.modifier;

import me.wyne.lootpool.api.complex.LootRollContext;
import me.wyne.lootpool.api.complex.LootSerialization;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adds attribute modifiers to an item, the equivalent of vanilla's {@code set_attributes}. Each
 * {@link AttributeEntry} rolls its own amount, so the same configuration produces a spread of
 * stat values across drops.
 *
 * @param entries the attribute modifications to apply
 * @param replace whether to clear the item's existing modifiers for an attribute before adding to
 *                it; when {@code false}, the new modifiers stack on top of whatever is already there
 */
@SerializableAs("SetAttributesModifier")
public record SetAttributesModifier(@NotNull List<@NotNull AttributeEntry> entries, boolean replace) implements ConfigurationSerializable, LootModifier {

    public SetAttributesModifier {
        entries = List.copyOf(entries);
    }

    @Override
    @NotNull
    public ItemStack apply(@NotNull ItemStack item, @NotNull LootRollContext context) {
        if (entries.isEmpty())
            return item;

        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;

        if (replace) {
            for (AttributeEntry entry : entries) {
                meta.removeAttributeModifier(entry.attribute());
            }
        }
        for (AttributeEntry entry : entries) {
            meta.addAttributeModifier(entry.attribute(), entry.createModifier());
        }
        item.setItemMeta(meta);
        return item;
    }

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("entries", LootSerialization.serializeList(entries));
        data.put("replace", replace);
        return data;
    }

    /**
     * Reconstructs a {@code SetAttributesModifier} from a {@link #serialize()}-style map. Entries
     * naming an attribute, operation or slot that does not exist on this server are dropped.
     */
    @NotNull
    public static SetAttributesModifier deserialize(@NotNull Map<String, Object> args) {
        return new SetAttributesModifier(
                LootSerialization.deserializeList(args.get("entries"), AttributeEntry.class),
                args.get("replace") instanceof Boolean replace && replace);
    }

}
