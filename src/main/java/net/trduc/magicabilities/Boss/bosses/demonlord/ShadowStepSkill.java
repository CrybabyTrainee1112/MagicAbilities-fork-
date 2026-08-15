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

public class ShadowStepSkill extends AbstractSkill {
    private static final double MIN_USEFUL_DISTANCE = 6.0;
    private static final double MAX_RANGE = 14.0;
    private static final double LANDING_DAMAGE = 10.0;
    private static final double LANDING_RADIUS = 2.5;

    public ShadowStepSkill() {
        super(new Builder("shadow_step")
                .cost(0.8)
                .cooldownTicks(240)
                .precondition(Condition.and(
                        Condition.greaterThan(WorldStateKeys.NEAREST_THREAT_DISTANCE, MIN_USEFUL_DISTANCE),
                        Condition.lessOrEqual(WorldStateKeys.NEAREST_THREAT_DISTANCE, MAX_RANGE)
                ))
                .effect(Effect.setValue(WorldStateKeys.NEAREST_THREAT_DISTANCE, 1.5))
                .targetKey(WorldStateKeys.NEAREST_THREAT)
        );
    }

    @Override
    public void execute(SkillContext context) {
        Mob boss = context.getBoss();
        LivingEntity target = context.getTarget();
        World world = boss.getWorld();
        if (target == null || target.isDead()) {
            return;
        }
        Plugin plugin = SkillPlugins.get(boss);
        if (plugin == null) {
            return;
        }

        Location behind = findSafeLanding(boss, target);
        if (behind == null) {
            return;
        }

        Location fromLoc = boss.getLocation().clone().add(0, 1, 0);
        world.spawnParticle(Particle.DUST, fromLoc, 20, 0.4, 0.8, 0.4, 0,
                new Particle.DustOptions(Color.fromRGB(15, 3, 20), 0.5f));
        world.spawnParticle(Particle.SOUL_FIRE_FLAME, fromLoc, 8, 0.3, 0.5, 0.3, 0.04);
        world.playSound(fromLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.4f);

        boss.teleport(behind);

        Location toLoc = boss.getLocation().clone().add(0, 1, 0);
        world.spawnParticle(Particle.DUST, toLoc, 25, 0.5, 0.8, 0.5, 0,
                new Particle.DustOptions(Color.fromRGB(255, 70, 0), 2.0f));
        world.spawnParticle(Particle.FLAME, toLoc, 15, 0.4, 0.6, 0.4, 0.07);
        world.playSound(toLoc, Sound.ENTITY_BLAZE_HURT, 0.9f, 0.5f);
        world.playSound(toLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.5f, 1.5f);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (boss.isDead()) {
                    return;
                }
                for (Entity entity : world.getNearbyEntities(boss.getLocation(), LANDING_RADIUS, LANDING_RADIUS, LANDING_RADIUS)) {
                    if (entity.equals(boss) || !(entity instanceof LivingEntity)) {
                        continue;
                    }
                    LivingEntity le = (LivingEntity) entity;
                    DamageAPI.dealDamage(boss, le, LANDING_DAMAGE, context.getCurrentPhase());
                    le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 3, false, true));
                    le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 25, 0, false, true));
                    Vector away = le.getLocation().subtract(boss.getLocation()).toVector();
                    if (away.lengthSquared() > 0.01) {
                        le.setVelocity(away.normalize().multiply(1.2).setY(0.5));
                    }
                }
            }
        }.runTaskLater(plugin, 1L);
    }

    private Location findSafeLanding(Mob boss, LivingEntity target) {
        double[] angleOffsetsDeg = {0, 45, -45, 90, -90, 135, -135, 180};
        float targetYaw = target.getLocation().getYaw();

        for (double angleOffset : angleOffsetsDeg) {
            Vector back = target.getLocation().getDirection().normalize().multiply(-1.8);
            back = rotateAroundY(back, angleOffset);

            for (int dy = 0; dy <= 2; dy++) {
                Location candidate = target.getLocation().clone().add(back.getX(), dy, back.getZ());
                candidate.setYaw(targetYaw + 180);
                if (isSafeLanding(candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private Vector rotateAroundY(Vector v, double angleDeg) {
        double rad = Math.toRadians(angleDeg);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double x = v.getX() * cos - v.getZ() * sin;
        double z = v.getX() * sin + v.getZ() * cos;
        return new Vector(x, v.getY(), z);
    }

    private boolean isSafeLanding(Location loc) {
        if (loc.getWorld() == null) {
            return false;
        }
        Location feet = loc.clone();
        Location head = loc.clone().add(0, 1, 0);
        return feet.getBlock().isPassable() && !feet.getBlock().isLiquid()
                && head.getBlock().isPassable() && !head.getBlock().isLiquid();
    }
}
