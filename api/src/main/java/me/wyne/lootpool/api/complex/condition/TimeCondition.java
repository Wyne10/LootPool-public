package me.wyne.lootpool.api.complex.condition;

import me.wyne.lootpool.api.complex.LootRollContext;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Passes when the world's time of day falls within an inclusive tick range, the equivalent of
 * vanilla's {@code time_check} predicate. A day is 24000 ticks; {@code 0} is sunrise, {@code 6000}
 * noon, {@code 13000} nightfall.
 * <p>
 * Ranges wrap: {@code minTime = 22000, maxTime = 2000} matches the hours around midnight. Both
 * bounds are taken modulo 24000, so out-of-range values behave sensibly rather than never matching.
 * <p>
 * The world is taken from {@link LootRollContext#location()}, falling back to the context player's
 * world. <strong>Fails closed</strong> when neither is available.
 *
 * @param minTime the inclusive start of the range, in ticks
 * @param maxTime the inclusive end of the range, in ticks
 */
@SerializableAs("TimeCondition")
public record TimeCondition(long minTime, long maxTime) implements ConfigurationSerializable, LootCondition {

    /** The length of a Minecraft day, in ticks. */
    public static final long DAY_LENGTH = 24000L;

    @Override
    public boolean test(@NotNull LootRollContext context) {
        @Nullable World world = context.location() != null
                ? context.location().getWorld()
                : (context.player() != null ? context.player().getWorld() : null);
        if (world == null)
            return false;

        long time = Math.floorMod(world.getTime(), DAY_LENGTH);
        long min = Math.floorMod(minTime, DAY_LENGTH);
        long max = Math.floorMod(maxTime, DAY_LENGTH);
        return min <= max
                ? time >= min && time <= max
                : time >= min || time <= max;
    }

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("minTime", minTime);
        data.put("maxTime", maxTime);
        return data;
    }

    /**
     * Reconstructs a {@code TimeCondition} from a {@link #serialize()}-style map.
     */
    @NotNull
    public static TimeCondition deserialize(@NotNull Map<String, Object> args) {
        return new TimeCondition(
                NumberConversions.toLong(args.get("minTime")),
                NumberConversions.toLong(args.get("maxTime")));
    }

}
