package net.trduc.magicabilitiesfork.Boss.bosses.tempestsovereign;

import net.trduc.magicabilitiesfork.Boss.ai.skill.AbstractSkill;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Condition;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Effect;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.SkillContext;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.WorldStateKeys;
import net.trduc.magicabilitiesfork.Boss.damage.DamageAPI;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.util.Vector;

public class TempestClawSkill extends AbstractSkill {
    private static final double TRIGGER_RANGE = 3.0;
    private static final double DAMAGE = 4.0;
    private static final double KNOCKBACK = 0.35;

    public TempestClawSkill() {
        super(new Builder("tempest_claw")
                .cost(1.0)
                .cooldownTicks(30)
                .precondition(Condition.lessOrEqual(WorldStateKeys.NEAREST_THREAT_DISTANCE, TRIGGER_RANGE))
                .effect(Effect.modifyValue(WorldStateKeys.TARGET_HEALTH_PERCENT, -0.02))
        );
    }

    @Override
    public void execute(SkillContext context) {
        Mob boss = context.getBoss();
        LivingEntity target = context.getTarget();
        if (target == null) {
            return;
        }
        Location bossLoc = boss.getLocation();

        target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0.0);
        boss.getWorld().playSound(bossLoc, Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 0.8f);

        DamageAPI.dealDamage(boss, target, DAMAGE, context.getCurrentPhase());

        Vector push = target.getLocation().toVector().subtract(bossLoc.toVector());
        push.setY(0);
        if (push.lengthSquared() > 0.0001) {
            push.normalize();
        }
        push.multiply(KNOCKBACK).setY(0.25);
        target.setVelocity(push);
    }
}
