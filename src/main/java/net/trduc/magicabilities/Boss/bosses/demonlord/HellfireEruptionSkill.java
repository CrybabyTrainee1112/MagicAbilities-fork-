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

public class HellfireEruptionSkill extends AbstractSkill {
    private static final double RADIUS = 8.0;
    private static final double MAX_DAMAGE = 22.0;
    private static final double MIN_DAMAGE = 10.0;
    private static final int TELEGRAPH_TICKS = 20;

    public HellfireEruptionSkill() {
        super(new Builder("hellfire_eruption")
                .cost(2.2)
                .cooldownTicks(400)
                .precondition(Condition.equals(WorldStateKeys.PLAYERS_CLUSTERED, 1))
                .effect(Effect.composite(
                        Effect.modifyValue(WorldStateKeys.DEMON_BLOOD_CHARGE, 0.3),
                        Effect.modifyValue(WorldStateKeys.TARGET_HEALTH_PERCENT, -0.35),
                        Effect.setValue(WorldStateKeys.PLAYERS_CLUSTERED, 0)
                ))
        );
    }

    @Override
    public void execute(SkillContext context) {
        Mob boss = context.getBoss();
        Location center = boss.getLocation().clone();
        World world = boss.getWorld();
        Plugin plugin = SkillPlugins.get(boss);
        if (plugin == null) {
            return;
        }

        world.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1f, 0.3f);
        world.playSound(center, Sound.ENTITY_GHAST_SHOOT, 0.8f, 0.5f);
        world.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 0.2f);

        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (t > TELEGRAPH_TICKS || boss.isDead()) {
                    cancel();
                    return;
                }
                double rad = t * (RADIUS / TELEGRAPH_TICKS) * 1.2;
                for (int j = 0; j < 36; j++) {
                    double ang = Math.toRadians(j * 10);
                    Location ring = center.clone().add(Math.cos(ang) * rad, 0.05, Math.sin(ang) * rad);
                    world.spawnParticle(Particle.DUST, ring, 1, 0.02, 0.01, 0.02, 0,
                            new Particle.DustOptions(Color.fromRGB(255, 160, 30), 1.3f));
                }
                t++;
            }
        }.runTaskTimer(plugin, 0, 1);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (boss.isDead()) {
                    return;
                }
                world.playSound(center, Sound.ENTITY_WITHER_DEATH, 1f, 0.5f);
                world.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.9f, 0.6f);

                for (Entity entity : world.getNearbyEntities(center, RADIUS, 5, RADIUS)) {
                    if (entity.equals(boss) || !(entity instanceof LivingEntity)) {
                        continue;
                    }
                    LivingEntity le = (LivingEntity) entity;
                    double dist = le.getLocation().distance(center);
                    if (dist > RADIUS) {
                        continue;
                    }
                    double falloff = Math.max(0.0, 1.0 - dist / RADIUS);
                    double damage = MIN_DAMAGE + (MAX_DAMAGE - MIN_DAMAGE) * falloff;
                    DamageAPI.dealDamage(boss, le, damage, context.getCurrentPhase());
                    le.setFireTicks(100);
                    le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, true));

                    Vector away = le.getLocation().subtract(center).toVector();
                    if (away.lengthSquared() > 0.01) {
                        le.setVelocity(away.normalize().multiply(1.0).setY(0.6));
                    }
                    world.spawnParticle(Particle.DUST, le.getLocation().add(0, 1, 0), 12,
                            0.3, 0.4, 0.3, 0, new Particle.DustOptions(Color.fromRGB(255, 70, 0), 2.0f));
                    world.playSound(le.getLocation(), Sound.ENTITY_BLAZE_HURT, 0.6f, 0.8f);
                }

                world.spawnParticle(Particle.DUST, center.clone().add(0, 1, 0), 60,
                        2.0, 1.5, 2.0, 0, new Particle.DustOptions(Color.fromRGB(255, 160, 30), 2.2f));
                world.spawnParticle(Particle.FLAME, center, 30, 2.0, 1.0, 2.0, 0.08);
            }
        }.runTaskLater(plugin, TELEGRAPH_TICKS);
    }
}
