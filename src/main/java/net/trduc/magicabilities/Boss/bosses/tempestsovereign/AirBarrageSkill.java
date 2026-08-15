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
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class AirBarrageSkill extends AbstractSkill {
    private static final int SHOT_COUNT = 5;
    private static final double SPREAD_DEGREES = 50.0;
    private static final double SHOT_RADIUS = 2.5;
    private static final double SHOT_RANGE = 10.0;
    private static final double DAMAGE = 5.0;

    public AirBarrageSkill() {
        super(new Builder("air_barrage")
                .cost(1.4)
                .cooldownTicks(160)
                .precondition(Condition.equals(WorldStateKeys.PLAYERS_CLUSTERED, 1))
                .effect(Effect.composite(
                        Effect.modifyValue(WorldStateKeys.TARGET_HEALTH_PERCENT, -0.10),
                        Effect.setValue(WorldStateKeys.PLAYERS_CLUSTERED, 0)
                ))
        );
    }

    @Override
    public void execute(SkillContext context) {
        Mob boss = context.getBoss();
        LivingEntity target = context.getTarget();
        int phase = context.getCurrentPhase();
        Location bossLoc = boss.getEyeLocation();

        Vector baseDirection = target != null
                ? target.getEyeLocation().toVector().subtract(bossLoc.toVector())
                : bossLoc.getDirection();
        baseDirection.setY(0);
        if (baseDirection.lengthSquared() < 0.0001) {
            baseDirection = bossLoc.getDirection();
        }
        baseDirection.normalize();

        boss.getWorld().playSound(bossLoc, Sound.ENTITY_BREEZE_SHOOT, 1f, 0.8f);

        for (int i = 0; i < SHOT_COUNT; i++) {
            double angleOffset = -SPREAD_DEGREES / 2 + (SPREAD_DEGREES / (SHOT_COUNT - 1)) * i;
            Vector shotDirection = rotateY(baseDirection, Math.toRadians(angleOffset));
            Location shotCenter = bossLoc.clone().add(shotDirection.clone().multiply(SHOT_RANGE));

            shotCenter.getWorld().spawnParticle(Particle.CLOUD, shotCenter, 20, 1.2, 0.6, 1.2, 0.05);
            shotCenter.getWorld().playSound(shotCenter, Sound.ENTITY_GENERIC_EXPLODE, 0.4f, 1.6f);

            for (Entity entity : shotCenter.getWorld().getNearbyEntities(shotCenter, SHOT_RADIUS, SHOT_RADIUS, SHOT_RADIUS)) {
                if (!(entity instanceof Player)) {
                    continue;
                }
                LivingEntity victim = (LivingEntity) entity;
                DamageAPI.dealDamage(boss, victim, DAMAGE, phase);
                Vector kb = entity.getLocation().subtract(shotCenter).toVector();
                if (kb.lengthSquared() > 0.0001) {
                    kb.normalize();
                }
                kb.multiply(1.2).setY(0.3);
                entity.setVelocity(kb);
            }
        }
    }

    private static Vector rotateY(Vector v, double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        double x = v.getX() * cos - v.getZ() * sin;
        double z = v.getX() * sin + v.getZ() * cos;
        return new Vector(x, v.getY(), z);
    }
}
