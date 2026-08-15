package net.trduc.magicabilitiesfork.Boss.bosses.tempestsovereign;

import net.trduc.magicabilitiesfork.Boss.ai.skill.AbstractSkill;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Condition;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Effect;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.SkillContext;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.WorldStateKeys;
import net.trduc.magicabilitiesfork.Boss.damage.DamageAPI;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.magicPlugin;

public class GaleSlashSkill extends AbstractSkill {
    private static final double DAMAGE = 9.0;
    private static final double MIN_RANGE = 4.0;
    private static final double MAX_RANGE = 20.0;
    private static final double SPEED = 1.6;
    private static final double HIT_RADIUS = 1.1;
    private static final long MAX_TRAVEL_TICKS = 40L;

    public GaleSlashSkill() {
        super(new Builder("gale_slash")
                .cost(1.5)
                .cooldownTicks(120)
                .precondition(Condition.and(
                        Condition.greaterOrEqual(WorldStateKeys.NEAREST_THREAT_DISTANCE, MIN_RANGE),
                        Condition.lessOrEqual(WorldStateKeys.NEAREST_THREAT_DISTANCE, MAX_RANGE)
                ))
                .effect(Effect.modifyValue(WorldStateKeys.TARGET_HEALTH_PERCENT, -0.06))
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

        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1f, 1.5f);

        Location start = boss.getEyeLocation();
        Vector direction = target.getEyeLocation().toVector().subtract(start.toVector());
        if (direction.lengthSquared() < 0.0001) {
            return;
        }
        direction.normalize();
        Set<Entity> alreadyHit = new HashSet<>();

        new BukkitRunnable() {
            Location current = start.clone();
            long t = 0;

            @Override
            public void run() {
                if (t > MAX_TRAVEL_TICKS || current.getWorld() == null) {
                    cancel();
                    return;
                }
                current.add(direction.clone().multiply(SPEED));

                for (int i = 0; i < 3; i++) {
                    double angle = Math.toRadians(t * 35 + i * 120);
                    Vector side = new Vector(Math.cos(angle) * 0.5, Math.sin(angle) * 0.3, Math.sin(angle) * 0.5);
                    current.getWorld().spawnParticle(Particle.DUST, current.clone().add(side), 2, 0.05, 0.05, 0.05, 0,
                            new Particle.DustOptions(Color.fromRGB(230, 240, 255), 1.1f));
                }
                current.getWorld().spawnParticle(Particle.CLOUD, current, 3, 0.12, 0.08, 0.12, 0.02);

                for (Entity entity : current.getWorld().getNearbyEntities(current, HIT_RADIUS, HIT_RADIUS, HIT_RADIUS)) {
                    if (!(entity instanceof Player) || alreadyHit.contains(entity)) {
                        continue;
                    }
                    alreadyHit.add(entity);
                    LivingEntity victim = (LivingEntity) entity;
                    DamageAPI.dealDamage(boss, victim, DAMAGE, phase);
                    victim.setVelocity(direction.clone().multiply(1.6).setY(0.35));
                    current.getWorld().spawnParticle(Particle.CLOUD, current, 20, 0.4, 0.4, 0.4, 0.15);
                    current.getWorld().playSound(current, Sound.ENTITY_PHANTOM_HURT, 0.5f, 1.6f);
                }

                if (!current.getBlock().isPassable()) {
                    cancel();
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0L, 1L);
    }
}
