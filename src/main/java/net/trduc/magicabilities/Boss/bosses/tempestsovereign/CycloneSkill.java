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

public class CycloneSkill extends AbstractSkill {
    private static final long CHANNEL_TICKS = 60L;
    private static final double RADIUS = 7.0;
    private static final double RELEASE_DAMAGE = 12.0;

    public CycloneSkill() {
        super(new Builder("cyclone")
                .cost(2.0)
                .cooldownTicks(220)
                .precondition(Condition.equals(WorldStateKeys.PLAYERS_CLUSTERED, 1))
                .effect(Effect.composite(
                        Effect.modifyValue(WorldStateKeys.TARGET_HEALTH_PERCENT, -0.12),
                        Effect.setValue(WorldStateKeys.PLAYERS_CLUSTERED, 0)
                ))
        );
    }

    @Override
    public void execute(SkillContext context) {
        Mob boss = context.getBoss();
        int phase = context.getCurrentPhase();
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 0.8f, 0.5f);

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
                    center.getWorld().spawnParticle(Particle.DUST, center.clone().add(0, 1, 0), 12,
                            RADIUS * 0.4, 1.0, RADIUS * 0.4, 0,
                            new Particle.DustOptions(Color.fromRGB(200, 220, 255), 1.3f));
                    for (Entity entity : center.getWorld().getNearbyEntities(center, RADIUS, 6, RADIUS)) {
                        if (!(entity instanceof Player)) {
                            continue;
                        }
                        LivingEntity victim = (LivingEntity) entity;
                        Vector pull = center.toVector().subtract(entity.getLocation().toVector());
                        if (pull.lengthSquared() > 0.0001) {
                            pull.normalize();
                        }
                        pull.multiply(0.3).setY(0.08);
                        entity.setVelocity(pull);
                        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 10, 2, false, false));
                    }
                } else if (t == CHANNEL_TICKS) {
                    release(boss, center, phase);
                    cancel();
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0L, 1L);
    }

    private void release(Mob boss, Location center, int phase) {
        center.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 1.4f);
        center.getWorld().spawnParticle(Particle.CLOUD, center, 100, RADIUS * 0.4, 1.2, RADIUS * 0.4, 0.3);

        for (Entity entity : center.getWorld().getNearbyEntities(center, RADIUS, 6, RADIUS)) {
            if (!(entity instanceof Player)) {
                continue;
            }
            LivingEntity victim = (LivingEntity) entity;
            DamageAPI.dealDamage(boss, victim, RELEASE_DAMAGE, phase);
            Vector launch = new Vector(0, 1.4, 0);
            Vector outward = entity.getLocation().subtract(center).toVector();
            if (outward.lengthSquared() > 0.0001) {
                outward.normalize();
            }
            entity.setVelocity(launch.add(outward.multiply(1.0)));
        }
    }
}
