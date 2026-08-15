package net.trduc.magicabilitiesfork.Boss.damage;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.persistence.PersistentDataType;

public class DamageAPI {

    public static final NamespacedKey MASTERY_DAMAGE_KEY =
            new NamespacedKey("magicabilitiesfork", "boss_mastery_dmg_mult");

    public static final NamespacedKey PHASE_DAMAGE_KEY =
            new NamespacedKey("magicabilitiesfork", "boss_phase_dmg_mult");

    public static double calculateDamage(double baseDamage, int currentPhase,
                                        LivingEntity target, double phaseMultiplier) {
        if (baseDamage <= 0) return 0;

        double adjusted = baseDamage * phaseMultiplier;


        return Math.max(0.5, adjusted);
    }

    public static void dealDamage(LivingEntity boss, LivingEntity target, double amount) {
        if (target.isDead() || amount <= 0) return;

        target.damage(amount, boss);
    }

    public static void dealDamage(Mob boss, LivingEntity target, double baseDamage, int currentPhase) {
        if (target.isDead() || baseDamage <= 0) return;

        double phaseMultiplier = boss.getPersistentDataContainer()
                .getOrDefault(PHASE_DAMAGE_KEY, PersistentDataType.DOUBLE, getPhaseMultiplier(currentPhase));
        double masteryMultiplier = boss.getPersistentDataContainer()
                .getOrDefault(MASTERY_DAMAGE_KEY, PersistentDataType.DOUBLE, 1.0);

        double finalDamage = calculateDamage(baseDamage * masteryMultiplier, currentPhase, target, phaseMultiplier);
        target.damage(finalDamage, boss);
    }

    public static double getPhaseMultiplier(int phase) {
        switch (phase) {
            case 1: return 1.0;
            case 2: return 1.2;
            case 3: return 1.5;
            default: return 1.0 + (phase - 1) * 0.3;
        }
    }
}

