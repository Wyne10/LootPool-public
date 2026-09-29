package me.wyne.lootpool.api.complex.modifier;

import me.wyne.lootpool.api.complex.LootRollContext;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Smelts the item as a furnace would, the equivalent of vanilla's {@code furnace_smelt}. Use it to
 * make a mob drop its cooked food or its smelted ore without configuring a second entry.
 * <p>
 * Only the stack's material changes; the amount is kept, so three raw beef become three steaks
 * rather than the one a furnace would give per operation. An item with no furnace recipe is left
 * alone, as is any meta the new material can still carry.
 */
@SerializableAs("SmeltModifier")
public record SmeltModifier() implements ConfigurationSerializable, LootModifier {

    /**
     * Resolved furnace results, keyed by input material. Scanning every recipe on the server for
     * each rolled item would be wasteful, and recipes only change when the server reloads them -
     * {@link #clearCache()} exists for that.
     */
    private static final Map<Material, Material> RESULTS = new ConcurrentHashMap<>();

    /**
     * Forgets the resolved furnace results, so a datapack or plugin that has changed the smelting
     * recipes is picked up without a restart.
     */
    public static void clearCache() {
        RESULTS.clear();
    }

    /**
     * Resolves what a furnace would turn {@code material} into.
     *
     * @return the smelted material, or {@code material} itself when nothing smelts it
     */
    @NotNull
    public static Material smelted(@NotNull Material material) {
        return RESULTS.computeIfAbsent(material, SmeltModifier::findResult);
    }

    @NotNull
    private static Material findResult(@NotNull Material material) {
        ItemStack probe = new ItemStack(material);
        Iterator<Recipe> recipes = Bukkit.recipeIterator();
        while (recipes.hasNext()) {
            Recipe recipe = recipes.next();
            // Blasting and smoking are cooking recipes too; only the furnace is "smelting".
            if (!(recipe instanceof FurnaceRecipe))
                continue;
            if (((CookingRecipe<?>) recipe).getInputChoice().test(probe))
                return recipe.getResult().getType();
        }
        return material;
    }

    @Override
    @NotNull
    public ItemStack apply(@NotNull ItemStack item, @NotNull LootRollContext context) {
        Material result = smelted(item.getType());
        if (result != item.getType())
            item.setType(result);
        return item;
    }

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        return new LinkedHashMap<>();
    }

    /**
     * Reconstructs a {@code SmeltModifier} from a {@link #serialize()}-style map. It carries no
     * settings, so the map is ignored.
     */
    @NotNull
    public static SmeltModifier deserialize(@NotNull Map<String, Object> args) {
        return new SmeltModifier();
    }

}
