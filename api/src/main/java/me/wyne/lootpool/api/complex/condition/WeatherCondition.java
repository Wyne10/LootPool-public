package me.wyne.lootpool.api.complex.condition;

import me.wyne.lootpool.api.complex.LootRollContext;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Passes when the world's weather matches, the equivalent of vanilla's {@code weather_check}
 * predicate. Either check may be {@code null}, meaning "don't care"; a condition with both
 * {@code null} passes for any weather.
 * <p>
 * The world is taken from {@link LootRollContext#location()}, falling back to the context player's
 * world. <strong>Fails closed</strong> when neither is available.
 *
 * @param raining    the required rain state, or {@code null} to ignore rain
 * @param thundering the required thunder state, or {@code null} to ignore thunder
 */
@SerializableAs("WeatherCondition")
public record WeatherCondition(@Nullable Boolean raining, @Nullable Boolean thundering) implements ConfigurationSerializable, LootCondition {

    @Override
    public boolean test(@NotNull LootRollContext context) {
        @Nullable World world = context.location() != null
                ? context.location().getWorld()
                : (context.player() != null ? context.player().getWorld() : null);
        if (world == null)
            return false;

        if (raining != null && world.hasStorm() != raining)
            return false;
        return thundering == null || world.isThundering() == thundering;
    }

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<>();
        if (raining != null)
            data.put("raining", raining);
        if (thundering != null)
            data.put("thundering", thundering);
        return data;
    }

    /**
     * Reconstructs a {@code WeatherCondition} from a {@link #serialize()}-style map. Absent keys
     * become {@code null}, preserving the "don't care" state.
     */
    @NotNull
    public static WeatherCondition deserialize(@NotNull Map<String, Object> args) {
        return new WeatherCondition(
                args.get("raining") instanceof Boolean raining ? raining : null,
                args.get("thundering") instanceof Boolean thundering ? thundering : null);
    }

}
