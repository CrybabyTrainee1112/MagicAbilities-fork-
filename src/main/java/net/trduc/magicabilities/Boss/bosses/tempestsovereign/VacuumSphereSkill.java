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

public class VacuumSphereSkill extends AbstractSkill {
    private static final long PULL_TICKS = 40L;
    private static final double PULL_RADIUS = 8.0;
    private static final double BURST_RADIUS = 6.0;
    private static final double MAX_BURST_DAMAGE = 22.0;

    public VacuumSphereSkill() {
        super(new Builder("vacuum_sphere")
                .cost(1.8)
                .cooldownTicks(200)
                .precondition(Condition.equals(WorldStateKeys.PLAYERS_CLUSTERED, 1))
                .effect(Effect.composite(
                        Effect.modifyValue(WorldStateKeys.TARGET_HEALTH_PERCENT, -0.15),
                        Effect.setValue(WorldStateKeys.PLAYERS_CLUSTERED, 0)
                ))
        );
    }

    @Override
    public void execute(SkillContext context) {
        Mob boss = context.getBoss();
        int phase = context.getCurrentPhase();
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 0.7f, 0.7f);

        new BukkitRunnable() {
            long t = 0;

            @Override
            public void run() {
                if (!boss.isValid() || boss.isDead()) {
                    cancel();
                    return;
                }
                Location center = boss.getLocation().add(0, 1, 0);

                if (t < PULL_TICKS) {
                    center.getWorld().spawnParticle(Particle.DUST, center, 10, PULL_RADIUS * 0.3, 0.8, PULL_RADIUS * 0.3, 0,
                            new Particle.DustOptions(Color.fromRGB(120, 180, 255), 1.2f));
                    for (Entity entity : center.getWorld().getNearbyEntities(center, PULL_RADIUS, 6, PULL_RADIUS)) {
                        if (!(entity instanceof Player)) {
                            continue;
                        }
                        Vector pull = center.toVector().subtract(entity.getLocation().toVector());
                        if (pull.lengthSquared() > 0.0001) {
                            pull.normalize();
                        }
                        pull.multiply(0.35).setY(pull.getY() * 0.15);
                        entity.setVelocity(pull);
                    }
                } else if (t == PULL_TICKS) {
                    release(boss, center, phase);
                    cancel();
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0L, 1L);
    }

    private void release(Mob boss, Location center, int phase) {
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.3f);
        center.getWorld().spawnParticle(Particle.CLOUD, center, 80, BURST_RADIUS * 0.5, 1.0, BURST_RADIUS * 0.5, 0.2);

        for (Entity entity : center.getWorld().getNearbyEntities(center, BURST_RADIUS, 6, BURST_RADIUS)) {
            if (!(entity instanceof Player)) {
                continue;
            }
            LivingEntity victim = (LivingEntity) entity;
            double dist = Math.max(0.5, entity.getLocation().distance(center));
            double dmg = Math.max(6, MAX_BURST_DAMAGE - dist * 2.2);
            DamageAPI.dealDamage(boss, victim, dmg, phase);
            victim.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 25, 1, false, false));

            Vector kb = entity.getLocation().subtract(center).toVector();
            if (kb.lengthSquared() > 0.0001) {
                kb.normalize();
            }
            kb.multiply(1.8).setY(0.4);
            entity.setVelocity(kb);
        }
    }
}
