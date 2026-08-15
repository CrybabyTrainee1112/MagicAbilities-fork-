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

public class TempestSkill extends AbstractSkill {
    private static final long DURATION_TICKS = 120L;
    private static final long DAMAGE_INTERVAL = 15L;
    private static final double RADIUS = 4.0;
    private static final double DAMAGE = 4.0;

    public TempestSkill() {
        super(new Builder("tempest")
                .cost(1.8)
                .cooldownTicks(280)
                .precondition(Condition.greaterOrEqual(WorldStateKeys.NEARBY_PLAYER_COUNT, 3))
                .effect(Effect.modifyValue(WorldStateKeys.TARGET_HEALTH_PERCENT, -0.15))
        );
    }

    @Override
    public void execute(SkillContext context) {
        Mob boss = context.getBoss();
        LivingEntity target = context.getTarget();
        int phase = context.getCurrentPhase();
        Location center = target != null ? target.getLocation() : boss.getLocation();

        center.getWorld().playSound(center, Sound.ENTITY_BREEZE_WIND_BURST, 1f, 0.6f);

        new BukkitRunnable() {
            long t = 0;

            @Override
            public void run() {
                if (t >= DURATION_TICKS || center.getWorld() == null) {
                    cancel();
                    return;
                }
                double angle = t * 0.3;
                for (int ring = 0; ring < 3; ring++) {
                    double r = 0.8 + ring * 1.0;
                    double x = Math.cos(angle + ring) * r;
                    double z = Math.sin(angle + ring) * r;
                    center.getWorld().spawnParticle(Particle.CLOUD, center.clone().add(x, ring * 0.6, z), 1, 0, 0, 0, 0);
                }

                if (t % DAMAGE_INTERVAL == 0) {
                    for (Entity entity : center.getWorld().getNearbyEntities(center, RADIUS, RADIUS, RADIUS)) {
                        if (!(entity instanceof Player)) {
                            continue;
                        }
                        LivingEntity victim = (LivingEntity) entity;
                        DamageAPI.dealDamage(boss, victim, DAMAGE, phase);
                        victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 0, false, false));
                        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 25, 2, false, false));

                        Vector toCenter = center.toVector().subtract(entity.getLocation().toVector());
                        Vector tangent = new Vector(-toCenter.getZ(), 0, toCenter.getX());
                        if (tangent.lengthSquared() > 0.0001) {
                            tangent.normalize();
                        }
                        entity.setVelocity(tangent.multiply(0.5).setY(0.2));
                    }
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0L, 1L);
    }
}
