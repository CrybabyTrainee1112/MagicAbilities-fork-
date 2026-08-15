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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.magicPlugin;

public class SupersonicStepSkill extends AbstractSkill {
    private static final double MIN_RANGE = 12.0;
    private static final double MAX_RANGE = 25.0;
    private static final int STAGES = 3;
    private static final double STAGE_DISTANCE = 5.0;
    private static final long TICKS_BETWEEN_STAGES = 4L;
    private static final double LANDING_RADIUS = 2.5;
    private static final double LANDING_DAMAGE = 5.0;

    public SupersonicStepSkill() {
        super(new Builder("supersonic_step")
                .cost(1.6)
                .cooldownTicks(180)
                .precondition(Condition.and(
                        Condition.greaterOrEqual(WorldStateKeys.NEAREST_THREAT_DISTANCE, MIN_RANGE),
                        Condition.lessOrEqual(WorldStateKeys.NEAREST_THREAT_DISTANCE, MAX_RANGE)
                ))
                .effect(Effect.modifyValue(WorldStateKeys.NEAREST_THREAT_DISTANCE, -(STAGES * STAGE_DISTANCE)))
        );
    }

    @Override
    public void execute(SkillContext context) {
        Mob boss = context.getBoss();
        LivingEntity target = context.getTarget();
        if (target == null) {
            return;
        }
        int phase = context.getCurrentPhase();

        new BukkitRunnable() {
            int stage = 0;

            @Override
            public void run() {
                if (stage >= STAGES || !boss.isValid() || boss.isDead() || !target.isValid() || target.isDead()) {
                    cancel();
                    return;
                }
                Location bossLoc = boss.getLocation();
                Vector direction = target.getLocation().toVector().subtract(bossLoc.toVector());
                direction.setY(0);
                if (direction.lengthSquared() > 0.0001) {
                    direction.normalize();
                } else {
                    direction = bossLoc.getDirection().setY(0);
                }

                Location origin = bossLoc.clone();
                Location destination = bossLoc.clone().add(direction.clone().multiply(STAGE_DISTANCE));
                destination.setDirection(direction);

                origin.getWorld().spawnParticle(Particle.CLOUD, origin, 25, 0.4, 0.6, 0.4, 0.05);
                boss.teleport(destination);
                destination.getWorld().spawnParticle(Particle.CLOUD, destination, 30, 0.5, 0.6, 0.5, 0.08);
                destination.getWorld().playSound(destination, Sound.ENTITY_BREEZE_JUMP, 1f, 1.3f);

                for (Entity entity : destination.getWorld().getNearbyEntities(destination, LANDING_RADIUS, LANDING_RADIUS, LANDING_RADIUS)) {
                    if (!(entity instanceof Player)) {
                        continue;
                    }
                    LivingEntity victim = (LivingEntity) entity;
                    DamageAPI.dealDamage(boss, victim, LANDING_DAMAGE, phase);
                    victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 1, false, false));
                }
                stage++;
            }
        }.runTaskTimer(magicPlugin, 0L, TICKS_BETWEEN_STAGES);
    }
}
