package net.trduc.magicabilitiesfork.Boss.bosses.demonlord;

import net.trduc.magicabilitiesfork.Boss.ai.skill.AbstractSkill;
import net.trduc.magicabilitiesfork.Boss.ai.skill.SkillPlugins;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Condition;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Effect;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.SkillContext;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.WorldStateKeys;
import net.trduc.magicabilitiesfork.Boss.damage.DamageAPI;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class HellPrisonSkill extends AbstractSkill {
    private static final double RADIUS = 5.0;
    private static final int DURATION_TICKS = 80;
    private static final double TICK_DAMAGE = 2.0;
    private static final double FINAL_BURST_DAMAGE = 10.0;
    private static final double CHARGE_GAIN = 0.3;

    public HellPrisonSkill() {
        super(new Builder("hell_prison")
                .cost(1.3)
                .cooldownTicks(360)
                .precondition(Condition.and(
                        Condition.lessOrEqual(WorldStateKeys.NEAREST_THREAT_DISTANCE, RADIUS),
                        Condition.lessOrEqual(WorldStateKeys.TARGET_ROOTED_TICKS, 0)
                ))
                .effect(Effect.composite(
                        Effect.setValue(WorldStateKeys.TARGET_ROOTED_TICKS, DURATION_TICKS),
                        Effect.modifyValue(WorldStateKeys.DEMON_BLOOD_CHARGE, CHARGE_GAIN),
                        Effect.modifyValue(WorldStateKeys.TARGET_HEALTH_PERCENT, -0.30)
                ))
                .targetKey(WorldStateKeys.NEAREST_THREAT)
        );
    }

    @Override
    public void execute(SkillContext context) {
        Mob boss = context.getBoss();
        Location center = boss.getLocation().clone().add(0, 0.5, 0);
        World world = boss.getWorld();
        Plugin plugin = SkillPlugins.get(boss);
        if (plugin == null) {
            return;
        }

        Set<LivingEntity> imprisoned = new HashSet<>();
        for (Entity entity : world.getNearbyEntities(center, RADIUS, 3, RADIUS)) {
            if (entity.equals(boss) || !(entity instanceof LivingEntity)) {
                continue;
            }
            imprisoned.add((LivingEntity) entity);
        }
        if (imprisoned.isEmpty()) {
            return;
        }

        world.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1f, 0.4f);
        world.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 0.3f);

        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (t >= DURATION_TICKS || boss.isDead()) {
                    world.playSound(center, Sound.ENTITY_BLAZE_HURT, 1f, 0.5f);
                    for (LivingEntity le : imprisoned) {
                        if (le.isDead() || !le.isValid()) {
                            continue;
                        }
                        if (le.getLocation().distance(center) <= RADIUS + 0.5) {
                            DamageAPI.dealDamage(boss, le, FINAL_BURST_DAMAGE, context.getCurrentPhase());
                            le.setFireTicks(100);
                        }
                    }
                    world.spawnParticle(Particle.DUST, center.clone().add(0, 2, 0), 50,
                            2.0, 2.0, 2.0, 0, new Particle.DustOptions(Color.fromRGB(255, 70, 0), 2.0f));
                    world.spawnParticle(Particle.LAVA, center, 20, 2.0, 1.0, 2.0, 0.1);
                    cancel();
                    return;
                }

                for (int i = 0; i < 20; i++) {
                    double a = Math.toRadians(i * 18 + t * 9);
                    double yh = 0.5 + Math.sin(t * 0.2 + i * 0.26) * 0.5;
                    Location ring = center.clone().add(Math.cos(a) * RADIUS, yh, Math.sin(a) * RADIUS);
                    world.spawnParticle(Particle.DUST, ring, 1, 0.02, 0.02, 0.02, 0,
                            new Particle.DustOptions(Color.fromRGB(255, 100, 0), 1.1f));
                }

                if (t % 10 == 0) {
                    for (LivingEntity le : imprisoned) {
                        if (le.isDead() || !le.isValid()) {
                            continue;
                        }
                        double dist = le.getLocation().distance(center);
                        if (dist > RADIUS - 0.5) {
                            Vector pull = center.clone().subtract(le.getLocation()).toVector();
                            if (pull.lengthSquared() > 0.01) {
                                le.setVelocity(pull.normalize().multiply(1.6));
                            }
                        }
                        DamageAPI.dealDamage(boss, le, TICK_DAMAGE, context.getCurrentPhase());
                        le.setFireTicks(30);
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 15, 3, false, false));
                    }
                    world.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.3f, 0.6f);
                }
                if (t % 20 == 0) {
                    world.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.25f, 0.4f);
                }
                t++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}
