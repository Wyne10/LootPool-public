package me.wyne.lootpool.api.complex.condition;

import me.wyne.lootpool.api.complex.LootRollContext;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Passes when the loot is being generated in one of the named biomes, so a pool can drop desert
 * loot in a desert and tundra loot in a tundra without needing a pool per region.
 * <p>
 * The location is taken from {@link LootRollContext#location()}, falling back to where the context
 * player is standing. <strong>Fails closed</strong> when neither is available, and when
 * {@code biomes} is empty.
 * <p>
 * Biomes are stored by name rather than as the enum, so a name this server does not know simply
 * never matches instead of failing the pool load - which is what keeps a pool portable across
 * versions that added or removed biomes.
 *
 * @param biomes the names of the biomes this condition accepts, upper-cased
 */
@SerializableAs("BiomeCondition")
public record BiomeCondition(@NotNull Set<@NotNull String> biomes) implements ConfigurationSerializable, LootCondition {

    public BiomeCondition {
        Set<String> normalized = new LinkedHashSet<>();
        for (String biome : biomes) {
            normalized.add(biome.toUpperCase());
        }
        biomes = Set.copyOf(normalized);
    }

    @Override
    public boolean test(@NotNull LootRollContext context) {
        if (biomes.isEmpty())
            return false;
        @Nullable Location location = context.location() != null
                ? context.location()
                : (context.player() != null ? context.player().getLocation() : null);
        if (location == null || location.getWorld() == null)
            return false;
        return biomes.contains(location.getBlock().getBiome().name());
    }

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("biomes", new ArrayList<>(biomes));
        return data;
    }

    /**
     * Reconstructs a {@code BiomeCondition} from a {@link #serialize()}-style map.
     */
    @NotNull
    public static BiomeCondition deserialize(@NotNull Map<String, Object> args) {
        Set<String> biomes = new LinkedHashSet<>();
        if (args.get("biomes") instanceof List<?> list) {
            for (Object biome : list) {
                if (biome != null)
                    biomes.add(biome.toString());
            }
        }
        return new BiomeCondition(biomes);
    }

}
