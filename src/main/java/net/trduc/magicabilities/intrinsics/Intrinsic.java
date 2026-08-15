package net.trduc.magicabilitiesfork.intrinsics;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Collections;
import java.util.List;

public abstract class Intrinsic {

    private final LivingEntity owner;

    protected Intrinsic(LivingEntity owner) {
        this.owner = owner;
    }

    public LivingEntity getOwner() {
        return owner;
    }

    public abstract IntrinsicId getId();

    public String getDisplayName() {
        return getId().name();
    }

    public List<IntrinsicStatModifier> getStatModifiers() {
        return Collections.emptyList();
    }

    public void onEquip() {
    }

    public void onUnequip() {
    }

    public void onKill(LivingEntity victim) {
    }

    public double onDamageDealt(LivingEntity victim, double baseDamage) {
        return baseDamage;
    }

    public void onDamaged(EntityDamageEvent event) {
    }

    public boolean onAirJump(Player player) {
        return false;
    }

    public void onOwnerDeath(EntityDeathEvent event) {
    }

    public void onTick() {
    }
}
