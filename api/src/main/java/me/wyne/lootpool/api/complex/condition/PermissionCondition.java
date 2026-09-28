package me.wyne.lootpool.api.complex.condition;

import me.wyne.lootpool.api.complex.LootRollContext;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Passes when the context's player holds a permission node.
 * <p>
 * <strong>Fails closed:</strong> a context with no player cannot hold a permission, so this
 * condition returns {@code false} whenever {@link LootRollContext#player()} is {@code null} — which
 * includes every context-free {@code populate} call. Attach it only to loot that is always rolled
 * for a known player.
 *
 * @param node the permission node to check
 */
@SerializableAs("PermissionCondition")
public record PermissionCondition(@NotNull String node) implements ConfigurationSerializable, LootCondition {

    @Override
    public boolean test(@NotNull LootRollContext context) {
        return context.player() != null && context.player().hasPermission(node);
    }

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("node", node);
        return data;
    }

    /**
     * Reconstructs a {@code PermissionCondition} from a {@link #serialize()}-style map.
     */
    @NotNull
    public static PermissionCondition deserialize(@NotNull Map<String, Object> args) {
        Object node = args.get("node");
        return new PermissionCondition(node != null ? node.toString() : "");
    }

}
