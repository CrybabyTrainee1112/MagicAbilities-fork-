package net.trduc.magicabilitiesfork.intrinsics;

import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class IntrinsicManager {

    private final Map<UUID, List<Intrinsic>> active = new HashMap<>();
    private final Map<Intrinsic, List<IntrinsicStatModifier>> appliedModifiers = new IdentityHashMap<>();

    public Intrinsic equip(LivingEntity owner, IntrinsicId id) {
        List<Intrinsic> existing = active.get(owner.getUniqueId());
        if (existing != null) {
            for (Intrinsic already : existing) {
                if (already.getId() == id) {
                    return already;
                }
            }
            for (Intrinsic already : new ArrayList<>(existing)) {
                if (already.getId() != id && already.getId().line().equals(id.line())) {
                    if (already.getId().tier() >= id.tier()) {
                        return already;
                    }
                    unequip(owner, already.getId());
                }
            }
        }
        Intrinsic intrinsic = IntrinsicRegistry.create(id, owner);
        active.computeIfAbsent(owner.getUniqueId(), k -> new ArrayList<>()).add(intrinsic);
        applyStatModifiers(owner, intrinsic);
        intrinsic.onEquip();
        return intrinsic;
    }

    public void unequip(LivingEntity owner, IntrinsicId id) {
        List<Intrinsic> list = active.get(owner.getUniqueId());
        if (list == null) {
            return;
        }
        Iterator<Intrinsic> it = list.iterator();
        while (it.hasNext()) {
            Intrinsic intrinsic = it.next();
            if (intrinsic.getId() == id) {
                intrinsic.onUnequip();
                removeStatModifiers(owner, intrinsic);
                it.remove();
            }
        }
        if (list.isEmpty()) {
            active.remove(owner.getUniqueId());
        }
    }

    public void unequipAll(LivingEntity owner) {
        List<Intrinsic> list = active.remove(owner.getUniqueId());
        if (list == null) {
            return;
        }
        for (Intrinsic intrinsic : list) {
            intrinsic.onUnequip();
            removeStatModifiers(owner, intrinsic);
        }
    }

    public List<Intrinsic> getActive(LivingEntity owner) {
        return active.getOrDefault(owner.getUniqueId(), Collections.emptyList());
    }

    public boolean hasAny(LivingEntity owner) {
        List<Intrinsic> list = active.get(owner.getUniqueId());
        return list != null && !list.isEmpty();
    }

    public void tickAll() {
        for (List<Intrinsic> list : active.values()) {
            for (Intrinsic intrinsic : list) {
                intrinsic.onTick();
            }
        }
    }

    public void clearAll() {
        for (Map.Entry<UUID, List<Intrinsic>> entry : active.entrySet()) {
            LivingEntity owner = resolveOwner(entry);
            for (Intrinsic intrinsic : entry.getValue()) {
                intrinsic.onUnequip();
                if (owner != null) {
                    removeStatModifiers(owner, intrinsic);
                }
            }
        }
        active.clear();
    }

    private LivingEntity resolveOwner(Map.Entry<UUID, List<Intrinsic>> entry) {
        List<Intrinsic> list = entry.getValue();
        return list.isEmpty() ? null : list.get(0).getOwner();
    }

    private void applyStatModifiers(LivingEntity owner, Intrinsic intrinsic) {
        List<IntrinsicStatModifier> modifiers = intrinsic.getStatModifiers();
        if (modifiers.isEmpty()) {
            return;
        }
        appliedModifiers.put(intrinsic, modifiers);
        for (IntrinsicStatModifier statModifier : modifiers) {
            AttributeInstance attributeInstance = owner.getAttribute(statModifier.getAttribute());
            if (attributeInstance == null) {
                continue;
            }
            attributeInstance.addModifier(toModifier(intrinsic, statModifier));
        }
    }

    private void removeStatModifiers(LivingEntity owner, Intrinsic intrinsic) {
        List<IntrinsicStatModifier> modifiers = appliedModifiers.remove(intrinsic);
        if (modifiers == null) {
            return;
        }
        for (IntrinsicStatModifier statModifier : modifiers) {
            AttributeInstance attributeInstance = owner.getAttribute(statModifier.getAttribute());
            if (attributeInstance == null) {
                continue;
            }
            attributeInstance.removeModifier(toModifier(intrinsic, statModifier));
        }
    }

    private AttributeModifier toModifier(Intrinsic intrinsic, IntrinsicStatModifier statModifier) {
        return statModifier.toBukkitModifier("intrinsic:" + intrinsic.getId().name());
    }
}
