package net.trduc.magicabilitiesfork.Boss.bosses.demonlord;

import net.trduc.magicabilitiesfork.Boss.ai.skill.AbstractSkill;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Condition;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Effect;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.SkillContext;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.WorldStateKeys;
import net.trduc.magicabilitiesfork.Boss.damage.DamageAPI;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

public class BloodSiphonSkill extends AbstractSkill {
    private static final double RANGE = 8.0;
    private static final double DAMAGE = 6.0;
    private static final double HEAL_FRACTION = 0.06;
    private static final double CHARGE_GAIN = 0.2;

    public BloodSiphonSkill() {
        super(new Builder("blood_siphon")
                .cost(1.5)
                .cooldownTicks(60)
                .precondition(Condition.and(
                        Condition.lessOrEqual(WorldStateKeys.NEAREST_THREAT_DISTANCE, RANGE),
                        Condition.lessThan(WorldStateKeys.BOSS_HEALTH_PERCENT, 0.95)
                ))
                .effect(Effect.composite(
                        Effect.modifyValue(WorldStateKeys.BOSS_HEALTH_PERCENT, HEAL_FRACTION),
                        Effect.modifyValue(WorldStateKeys.DEMON_BLOOD_CHARGE, CHARGE_GAIN)
                ))
                .targetKey(WorldStateKeys.NEAREST_THREAT)
        );
    }

    @Override
    public void execute(SkillContext context) {
        Mob boss = context.getBoss();
        LivingEntity target = context.getTarget();
        if (target == null || target.isDead()) {
            return;
        }
        if (boss.getLocation().distance(target.getLocation()) > RANGE) {
            return;
        }

        DamageAPI.dealDamage(boss, target, DAMAGE, context.getCurrentPhase());

        AttributeInstance maxHealthAttr = boss.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr != null) {
            double maxHealth = maxHealthAttr.getValue();
            boss.setHealth(Math.min(maxHealth, boss.getHealth() + maxHealth * HEAL_FRACTION));
        }

        boss.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1, 0), 20,
                0.3, 0.5, 0.3, 0, new Particle.DustOptions(Color.fromRGB(139, 0, 0), 1.2f));
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_WITHER_HURT, 0.6f, 0.7f);
    }
}
