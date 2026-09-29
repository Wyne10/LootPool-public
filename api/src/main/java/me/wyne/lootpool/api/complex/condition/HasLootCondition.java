package me.wyne.lootpool.api.complex.condition;

import me.wyne.lootpool.api.Loot;
import me.wyne.lootpool.api.LootPool;
import me.wyne.lootpool.api.LootPoolApi;
import me.wyne.lootpool.api.LootPoolProvider;
import me.wyne.lootpool.api.complex.LootRollContext;
import me.wyne.lootpool.api.complex.LootSerialization;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Passes on what the player is already carrying from another pool's loot, so a pool can stop
 * handing out a drop somebody already has, or gate a reward behind collecting a set.
 * <p>
 * The referenced pool is read through {@link LootPool#getLootList()}, which is the flat preview of
 * everything it can produce. Stack sizes are always ignored - this asks whether the player has the
 * item at all, not how many.
 * <p>
 * {@code matchMeta} decides how closely an item has to match. Left off, only the material is
 * compared, which is what you want for ordinary loot: modifiers rewrite an item's name,
 * enchantments and damage after it is rolled, so the stored template rarely resembles what the
 * player actually received. Turned on, {@link ItemStack#isSimilar(ItemStack)} is used instead, so
 * the name and enchantments must match as well - useful for a pool holding a specific named item,
 * and near-useless over entries whose meta is applied by modifiers.
 * <p>
 * <strong>Fails closed</strong> when the context carries no player, when {@code pool} does not
 * resolve, and when the referenced pool is empty.
 *
 * @param pool      the registry key of the pool whose items to look for
 * @param mode      whether the player needs one of those items or all of them
 * @param matchMeta whether an item has to match the template exactly, rather than by material
 */
@SerializableAs("HasLootCondition")
public record HasLootCondition(@NotNull String pool, @NotNull Mode mode, boolean matchMeta) implements ConfigurationSerializable, LootCondition {

    /** How much of the referenced pool the player has to be carrying. */
    public enum Mode {
        /** Passes as soon as the player has any one of the pool's items. */
        ANY,
        /** Passes only when the player has every distinct item the pool can produce. */
        ALL
    }

    /**
     * Resolves {@link #pool()} in the registry.
     *
     * @return the referenced pool, or {@code null} if it does not currently resolve
     */
    @Nullable
    public LootPool getLootPool() {
        LootPoolProvider provider = LootPoolApi.getProvider();
        return provider != null ? provider.getLootPool(pool) : null;
    }

    /**
     * The item templates the referenced pool can produce, ignoring air entries.
     */
    @NotNull
    public List<@NotNull ItemStack> templates() {
        LootPool lootPool = getLootPool();
        if (lootPool == null)
            return List.of();
        List<ItemStack> templates = new ArrayList<>();
        for (Loot loot : lootPool.getLootList()) {
            if (loot.item().getType() != Material.AIR)
                templates.add(loot.item());
        }
        return templates;
    }

    @Override
    public boolean test(@NotNull LootRollContext context) {
        if (context.player() == null)
            return false;

        List<ItemStack> templates = templates();
        if (templates.isEmpty())
            return false;

        ItemStack[] carried = context.player().getInventory().getContents();
        for (ItemStack template : templates) {
            boolean has = carries(carried, template);
            if (mode == Mode.ALL && !has)
                return false;
            if (mode == Mode.ANY && has)
                return true;
        }
        return mode == Mode.ALL;
    }

    private boolean carries(@NotNull ItemStack[] carried, @NotNull ItemStack template) {
        for (ItemStack item : carried) {
            if (item == null || item.getType() == Material.AIR)
                continue;
            if (matchMeta ? item.isSimilar(template) : item.getType() == template.getType())
                return true;
        }
        return false;
    }

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pool", pool);
        data.put("mode", mode.name());
        data.put("matchMeta", matchMeta);
        return data;
    }

    /**
     * Reconstructs a {@code HasLootCondition} from a {@link #serialize()}-style map. An unknown
     * {@code mode} falls back to {@link Mode#ANY}, and an absent {@code matchMeta} to material-only matching.
     */
    @NotNull
    public static HasLootCondition deserialize(@NotNull Map<String, Object> args) {
        Object pool = args.get("pool");
        Mode mode = LootSerialization.deserializeEnum(args.get("mode"), Mode.class);
        return new HasLootCondition(
                pool != null ? pool.toString() : "",
                mode != null ? mode : Mode.ANY,
                args.get("matchMeta") instanceof Boolean matchMeta && matchMeta);
    }

}
