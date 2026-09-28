package me.wyne.lootpool.api.complex.modifier;

import me.wyne.lootpool.api.LootPoolApi;
import me.wyne.lootpool.api.complex.LootRollContext;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sets an item's display name, the equivalent of vanilla's {@code set_name}.
 * <p>
 * {@code text} is stored as a plain string and rendered through
 * {@link LootPoolApi#getTextRenderer()}, so it uses whatever markup the running plugin has
 * installed — the same syntax as the language files. Italics are disabled, matching how the rest of
 * this plugin renders item names.
 *
 * @param text the display name markup
 */
@SerializableAs("NameModifier")
public record NameModifier(@NotNull String text) implements ConfigurationSerializable, LootModifier {

    @Override
    @NotNull
    public ItemStack apply(@NotNull ItemStack item, @NotNull LootRollContext context) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;

        meta.displayName(LootPoolApi.getTextRenderer().apply(text).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("text", text);
        return data;
    }

    /**
     * Reconstructs a {@code NameModifier} from a {@link #serialize()}-style map.
     */
    @NotNull
    public static NameModifier deserialize(@NotNull Map<String, Object> args) {
        Object text = args.get("text");
        return new NameModifier(text != null ? text.toString() : "");
    }

}
