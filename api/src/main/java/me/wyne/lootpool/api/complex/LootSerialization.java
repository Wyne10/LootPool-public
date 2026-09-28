package me.wyne.lootpool.api.complex;

import me.wyne.lootpool.api.complex.condition.LootCondition;
import me.wyne.lootpool.api.complex.modifier.LootModifier;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Helpers shared by every {@link ConfigurationSerializable} type in this package, and available to
 * third-party {@link LootModifier}/{@link LootCondition}/{@link LootEntry} implementations.
 * <p>
 * Bukkit usually hands a {@code deserialize} method fully-reconstructed nested values, but not
 * always: values read back out of a nested list, or out of a {@code MemorySection} that was never
 * round-tripped through YAML, arrive as raw {@link Map}s still carrying their {@code "=="} type tag.
 * {@link #deserializeList(Object, Class)} tolerates both shapes, which is what keeps a pool from
 * silently loading with all of its modifiers missing.
 */
public final class LootSerialization {

    private LootSerialization() {
    }

    /**
     * Reads a list of serializable values written by {@link #serializeList(List)}.
     * <p>
     * Elements that are already of {@code type} are kept as-is; elements that are still raw maps are
     * run back through {@link ConfigurationSerialization#deserializeObject(Map)}. Anything that does
     * not resolve to {@code type} is dropped rather than failing the whole load.
     *
     * @param raw  the value stored under the list's key, possibly {@code null}
     * @param type the element type to keep
     * @return an immutable list of the elements that resolved, never {@code null}
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <T> List<@NotNull T> deserializeList(@Nullable Object raw, @NotNull Class<T> type) {
        if (!(raw instanceof List<?> list))
            return List.of();

        List<T> result = new ArrayList<>(list.size());
        for (Object element : list) {
            Object resolved = element;
            if (!type.isInstance(resolved) && resolved instanceof Map<?, ?> map) {
                try {
                    resolved = ConfigurationSerialization.deserializeObject((Map<String, ?>) map);
                } catch (IllegalArgumentException e) {
                    resolved = null;
                }
            }
            if (type.isInstance(resolved))
                result.add(type.cast(resolved));
        }
        return List.copyOf(result);
    }

    /**
     * Reads a single serializable value, tolerating the same raw-map shape as {@link #deserializeList(Object, Class)}.
     *
     * @return the resolved value, or {@code null} if it is absent or of the wrong type
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> T deserializeValue(@Nullable Object raw, @NotNull Class<T> type) {
        if (type.isInstance(raw))
            return type.cast(raw);
        if (!(raw instanceof Map<?, ?> map))
            return null;
        try {
            Object resolved = ConfigurationSerialization.deserializeObject((Map<String, ?>) map);
            return type.isInstance(resolved) ? type.cast(resolved) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Writes {@code values} as a plain {@link ArrayList}, which is what Bukkit's YAML representer
     * expects; it recurses into each element's own {@code serialize()} on its own.
     */
    @NotNull
    public static List<Object> serializeList(@NotNull List<? extends ConfigurationSerializable> values) {
        return new ArrayList<>(values);
    }

    /**
     * Reads an enum constant by {@link Enum#name()}, tolerating a missing or unknown value.
     *
     * @return the constant, or {@code null} if {@code raw} is not the name of one
     */
    @Nullable
    public static <E extends Enum<E>> E deserializeEnum(@Nullable Object raw, @NotNull Class<E> type) {
        if (type.isInstance(raw))
            return type.cast(raw);
        if (!(raw instanceof String name))
            return null;
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
