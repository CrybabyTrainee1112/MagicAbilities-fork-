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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.magicPlugin;

public class GaleStepSkill extends AbstractSkill {
    private static final double MIN_RANGE = 4.0;
    private static final double MAX_RANGE = 8.0;
    private static final double DASH_DISTANCE = 7.0;
    private static final int ZONE_COUNT = 6;
    private static final double ZONE_RADIUS = 1.5;
    private static final long ZONE_DURATION_TICKS = 60L;
    private static final long ZONE_TICK_INTERVAL = 10L;
    private static final double ZONE_DAMAGE = 3.0;
    private static final Random RANDOM = new Random();

    public GaleStepSkill() {
        super(new Builder("gale_step")
                .cost(1.4)
                .cooldownTicks(150)
                .precondition(Condition.and(
                        Condition.greaterOrEqual(WorldStateKeys.NEAREST_THREAT_DISTANCE, MIN_RANGE),
                        Condition.lessOrEqual(WorldStateKeys.NEAREST_THREAT_DISTANCE, MAX_RANGE)
                ))
                .effect(Effect.modifyValue(WorldStateKeys.NEAREST_THREAT_DISTANCE, -DASH_DISTANCE))
        );
    }

    @Override
    public void execute(SkillContext context) {
        Mob boss = context.getBoss();
        LivingEntity target = context.getTarget();
        int phase = context.getCurrentPhase();

        Location bossLoc = boss.getLocation();
        Vector direction = target != null
                ? target.getLocation().toVector().subtract(bossLoc.toVector())
                : bossLoc.getDirection();
        direction.setY(0);
        if (direction.lengthSquared() > 0.0001) {
            direction.normalize();
        } else {
            direction = bossLoc.getDirection().setY(0);
        }

        bossLoc.getWorld().spawnParticle(Particle.CLOUD, bossLoc, 20, 0.3, 0.5, 0.3, 0.05);
        Location destination = bossLoc.clone().add(direction.clone().multiply(DASH_DISTANCE));
        destination.setDirection(direction);
        boss.teleport(destination);
        destination.getWorld().playSound(destination, Sound.ENTITY_PHANTOM_FLAP, 1f, 1.3f);

        List<Location> zones = new ArrayList<>();
        for (int i = 1; i <= ZONE_COUNT; i++) {
            double frac = i / (double) ZONE_COUNT;
            zones.add(bossLoc.clone().add(direction.clone().multiply(DASH_DISTANCE * frac)));
        }

        new BukkitRunnable() {
            long t = 0;

            @Override
            public void run() {
                if (t >= ZONE_DURATION_TICKS) {
                    cancel();
                    return;
                }
                for (Location zone : zones) {
                    zone.getWorld().spawnParticle(Particle.CLOUD, zone, 2, 0.4, 0.2, 0.4, 0.01);
                }
                if (t % ZONE_TICK_INTERVAL == 0) {
                    for (Location zone : zones) {
                        for (Entity entity : zone.getWorld().getNearbyEntities(zone, ZONE_RADIUS, ZONE_RADIUS, ZONE_RADIUS)) {
                            if (!(entity instanceof Player)) {
                                continue;
                            }
                            LivingEntity victim = (LivingEntity) entity;
                            DamageAPI.dealDamage(boss, victim, ZONE_DAMAGE, phase);
                            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 0, false, false));
                            Vector push = new Vector(RANDOM.nextDouble() - 0.5, 0.1, RANDOM.nextDouble() - 0.5);
                            entity.setVelocity(push.multiply(0.6));
                        }
                    }
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0L, 1L);
    }
}
