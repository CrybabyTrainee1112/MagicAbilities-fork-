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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.magicPlugin;

public class VortexCollapseSkill extends AbstractSkill {
    private static final long CHANNEL_TICKS = 50L;
    private static final double CHANNEL_RADIUS = 6.0;
    private static final double CHANNEL_TICK_DAMAGE = 1.5;
    private static final double RELEASE_RADIUS = 7.0;
    private static final double RELEASE_BASE_DAMAGE = 22.0;

    public VortexCollapseSkill() {
        super(new Builder("vortex_collapse")
                .cost(3.0)
                .cooldownTicks(500)
                .precondition(Condition.and(
                        Condition.greaterOrEqual(WorldStateKeys.BOSS_CURRENT_PHASE, 2),
                        Condition.lessOrEqual(WorldStateKeys.NEAREST_THREAT_DISTANCE, 12.0)
                ))
                .effect(Effect.composite(
                        Effect.modifyValue(WorldStateKeys.TARGET_HEALTH_PERCENT, -0.25),
                        Effect.setValue(WorldStateKeys.PLAYERS_CLUSTERED, 0)
                ))
        );
    }

    @Override
    public void execute(SkillContext context) {
        Mob boss = context.getBoss();
        int phase = context.getCurrentPhase();
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.6f, 1.4f);

        new BukkitRunnable() {
            long t = 0;

            @Override
            public void run() {
                if (!boss.isValid() || boss.isDead()) {
                    cancel();
                    return;
                }
                Location center = boss.getLocation();

                if (t < CHANNEL_TICKS) {
                    double intensity = t / (double) CHANNEL_TICKS;
                    center.getWorld().spawnParticle(Particle.DUST, center.clone().add(0, 1, 0), 15,
                            CHANNEL_RADIUS * 0.3, 1.0, CHANNEL_RADIUS * 0.3, 0,
                            new Particle.DustOptions(Color.fromRGB(30, 80, 200), 1.4f));
                    center.getWorld().spawnParticle(Particle.CLOUD, center, 6, 1.0, 0.6, 1.0, 0.03);

                    for (Entity entity : center.getWorld().getNearbyEntities(center, CHANNEL_RADIUS, 8, CHANNEL_RADIUS)) {
                        if (!(entity instanceof Player)) {
                            continue;
                        }
                        LivingEntity victim = (LivingEntity) entity;
                        double strength = 0.3 + intensity * 0.5;
                        Vector pull = center.toVector().subtract(entity.getLocation().toVector());
                        if (pull.lengthSquared() > 0.0001) {
                            pull.normalize();
                        }
                        pull.multiply(strength);
                        pull.setY(pull.getY() * 0.2);
                        entity.setVelocity(pull);
                        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 6, 4, false, false));
                        if (t % 12 == 0) {
                            DamageAPI.dealDamage(boss, victim, CHANNEL_TICK_DAMAGE, phase);
                        }
                    }

                    if (t == CHANNEL_TICKS - 10) {
                        center.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 0.5f);
                    }
                } else if (t == CHANNEL_TICKS) {
                    release(boss, center, phase);
                } else if (t > CHANNEL_TICKS + 20) {
                    cancel();
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0L, 1L);
    }

    private void release(Mob boss, Location center, int phase) {
        center.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1f, 0.5f);
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.6f);
        center.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.7f, 0.8f);

        center.getWorld().spawnParticle(Particle.DUST, center, 200, 5, 5, 5, 0,
                new Particle.DustOptions(Color.fromRGB(80, 160, 255), 2.2f));
        center.getWorld().spawnParticle(Particle.CLOUD, center, 150, 5, 5, 5, 0.4);

        for (Entity entity : center.getWorld().getNearbyEntities(center, RELEASE_RADIUS, 8, RELEASE_RADIUS)) {
            if (!(entity instanceof Player)) {
                continue;
            }
            LivingEntity victim = (LivingEntity) entity;
            double dist = Math.max(0.5, entity.getLocation().distance(center));
            double dmg = Math.max(8, RELEASE_BASE_DAMAGE - dist * 1.8);
            DamageAPI.dealDamage(boss, victim, dmg, phase);

            victim.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 60, 2, false, false));
            victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, false, false));
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 2, false, false));

            Vector kb = entity.getLocation().subtract(center).toVector();
            if (kb.lengthSquared() > 0.0001) {
                kb.normalize();
            }
            kb.multiply(3.5).setY(0.5);
            entity.setVelocity(kb);
        }
    }
}
