package me.wyne.lootpool.api.complex.modifier;

import me.wyne.lootpool.api.complex.LootSerialization;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * One attribute modification within a {@link SetAttributesModifier}: which attribute to change, how,
 * by how much, and in which equipment slot it applies.
 *
 * @param attribute the attribute to modify
 * @param operation how {@code amount} combines with the attribute's base value
 * @param minAmount the inclusive lower bound of the rolled amount
 * @param maxAmount the inclusive upper bound of the rolled amount
 * @param slot      the slot the modifier applies in, or {@code null} to apply in every slot
 */
@SerializableAs("AttributeEntry")
public record AttributeEntry(@NotNull Attribute attribute, @NotNull AttributeModifier.Operation operation,
                             double minAmount, double maxAmount, @Nullable EquipmentSlot slot) implements ConfigurationSerializable {

    /**
     * Rolls an amount in the inclusive range between {@link #minAmount()} and {@link #maxAmount()},
     * normalized order-independently. Negative amounts are allowed — they weaken the attribute.
     */
    public double rollAmount() {
        double min = Math.min(minAmount, maxAmount);
        double max = Math.max(minAmount, maxAmount);
        return min == max ? min : ThreadLocalRandom.current().nextDouble(min, max);
    }

    /**
     * Builds a fresh Bukkit {@link AttributeModifier} with a random UUID and a rolled amount.
     */
    @NotNull
    public AttributeModifier createModifier() {
        double amount = rollAmount();
        return slot != null
                ? new AttributeModifier(UUID.randomUUID(), attribute.name(), amount, operation, slot)
                : new AttributeModifier(UUID.randomUUID(), attribute.name(), amount, operation);
    }

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("attribute", attribute.name());
        data.put("operation", operation.name());
        data.put("minAmount", minAmount);
        data.put("maxAmount", maxAmount);
        if (slot != null)
            data.put("slot", slot.name());
        return data;
    }

    /**
     * Reconstructs an {@code AttributeEntry} from a {@link #serialize()}-style map.
     *
     * @return the entry, or {@code null} if {@code attribute} or {@code operation} does not name a
     *         constant that exists on this server — the caller is expected to drop it rather than
     *         fail the load
     */
    @Nullable
    public static AttributeEntry deserialize(@NotNull Map<String, Object> args) {
        Attribute attribute = LootSerialization.deserializeEnum(args.get("attribute"), Attribute.class);
        AttributeModifier.Operation operation = LootSerialization.deserializeEnum(args.get("operation"), AttributeModifier.Operation.class);
        if (attribute == null || operation == null)
            return null;

        return new AttributeEntry(
                attribute,
                operation,
                NumberConversions.toDouble(args.get("minAmount")),
                NumberConversions.toDouble(args.get("maxAmount")),
                LootSerialization.deserializeEnum(args.get("slot"), EquipmentSlot.class));
    }

}
