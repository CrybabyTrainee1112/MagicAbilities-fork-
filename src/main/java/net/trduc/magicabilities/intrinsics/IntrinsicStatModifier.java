package net.trduc.magicabilitiesfork.intrinsics;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;

import java.util.Objects;
import java.util.UUID;

public class IntrinsicStatModifier {

    private final Attribute attribute;
    private final double amount;
    private final AttributeModifier.Operation operation;
    private final UUID modifierId;

    public IntrinsicStatModifier(Attribute attribute, double amount, AttributeModifier.Operation operation) {
        this.attribute = Objects.requireNonNull(attribute, "attribute");
        this.amount = amount;
        this.operation = Objects.requireNonNull(operation, "operation");
        this.modifierId = UUID.randomUUID();
    }

    public Attribute getAttribute() {
        return attribute;
    }

    public double getAmount() {
        return amount;
    }

    public AttributeModifier.Operation getOperation() {
        return operation;
    }

    public UUID getModifierId() {
        return modifierId;
    }

    public AttributeModifier toBukkitModifier(String name) {
        return new AttributeModifier(modifierId, name, amount, operation);
    }
}
