package me.wyne.lootpool.api.complex.modifier;

import me.wyne.lootpool.api.LootPoolApi;
import me.wyne.lootpool.api.complex.LootRollContext;
import me.wyne.lootpool.api.complex.LootSerialization;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adds or replaces an item's lore, the equivalent of vanilla's {@code set_lore}.
 * <p>
 * Each line is stored as a plain string and rendered through
 * {@link LootPoolApi#getTextRenderer()}, so it uses whatever markup the running plugin has
 * installed — the same syntax as the language files. Italics are disabled, matching how the rest of
 * this plugin renders lore.
 *
 * @param lines the lore markup, one entry per line
 * @param mode  whether to append to the item's existing lore or replace it entirely
 */
@SerializableAs("LoreModifier")
public record LoreModifier(@NotNull List<@NotNull String> lines, @NotNull Mode mode) implements ConfigurationSerializable, LootModifier {

    public LoreModifier {
        lines = List.copyOf(lines);
    }

    /** How a {@link LoreModifier}'s lines combine with the item's existing lore. */
    public enum Mode {
        /** Add the lines below whatever lore the item already has. */
        APPEND,
        /** Discard the item's existing lore and use only these lines. */
        REPLACE
    }

    @Override
    @NotNull
    public ItemStack apply(@NotNull ItemStack item, @NotNull LootRollContext context) {
        if (lines.isEmpty() && mode == Mode.APPEND)
            return item;

        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;

        List<Component> lore = new ArrayList<>();
        if (mode == Mode.APPEND && meta.hasLore() && meta.lore() != null)
            lore.addAll(meta.lore());
        for (String line : lines) {
            lore.add(LootPoolApi.getTextRenderer().apply(line).decoration(TextDecoration.ITALIC, false));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("lines", new ArrayList<>(lines));
        data.put("mode", mode.name());
        return data;
    }

    /**
     * Reconstructs a {@code LoreModifier} from a {@link #serialize()}-style map. An unknown
     * {@code mode} falls back to {@link Mode#APPEND}.
     */
    @NotNull
    public static LoreModifier deserialize(@NotNull Map<String, Object> args) {
        List<String> lines = new ArrayList<>();
        if (args.get("lines") instanceof List<?> raw) {
            for (Object line : raw) {
                if (line != null)
                    lines.add(line.toString());
            }
        }
        Mode mode = LootSerialization.deserializeEnum(args.get("mode"), Mode.class);
        return new LoreModifier(lines, mode != null ? mode : Mode.APPEND);
    }

}
