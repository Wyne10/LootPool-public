package me.wyne.lootpool.api.complex.condition;

import me.wyne.lootpool.api.complex.LootRollContext;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Passes when the loot is being generated in one of the named worlds.
 * <p>
 * The world is taken from {@link LootRollContext#location()}, falling back to the context player's
 * world. <strong>Fails closed</strong> when neither is available, and when {@code worlds} is empty.
 *
 * @param worlds the names of the worlds this condition accepts
 */
@SerializableAs("WorldCondition")
public record WorldCondition(@NotNull Set<@NotNull String> worlds) implements ConfigurationSerializable, LootCondition {

    public WorldCondition {
        worlds = Set.copyOf(worlds);
    }

    @Override
    public boolean test(@NotNull LootRollContext context) {
        if (worlds.isEmpty())
            return false;
        @Nullable World world = context.location() != null
                ? context.location().getWorld()
                : (context.player() != null ? context.player().getWorld() : null);
        return world != null && worlds.contains(world.getName());
    }

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("worlds", new ArrayList<>(worlds));
        return data;
    }

    /**
     * Reconstructs a {@code WorldCondition} from a {@link #serialize()}-style map.
     */
    @NotNull
    public static WorldCondition deserialize(@NotNull Map<String, Object> args) {
        Set<String> worlds = new LinkedHashSet<>();
        if (args.get("worlds") instanceof List<?> list) {
            for (Object world : list) {
                if (world != null)
                    worlds.add(world.toString());
            }
        }
        return new WorldCondition(worlds);
    }

}
