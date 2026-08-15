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

import java.util.HashSet;
import java.util.Set;

import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.magicPlugin;

public class JetStreamSkill extends AbstractSkill {
    private static final double MIN_RANGE = 6.0;
    private static final double MAX_RANGE = 12.0;
    private static final double SPEED = 0.9;
    private static final long DURATION_TICKS = 20L;
    private static final double HIT_RADIUS = 1.3;
    private static final double DAMAGE = 8.0;

    public JetStreamSkill() {
        super(new Builder("jet_stream")
                .cost(1.3)
                .cooldownTicks(140)
                .precondition(Condition.and(
                        Condition.greaterOrEqual(WorldStateKeys.NEAREST_THREAT_DISTANCE, MIN_RANGE),
                        Condition.lessOrEqual(WorldStateKeys.NEAREST_THREAT_DISTANCE, MAX_RANGE)
                ))
                .effect(Effect.modifyValue(WorldStateKeys.NEAREST_THREAT_DISTANCE, -6.0))
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

        Vector direction = target.getLocation().toVector().subtract(boss.getLocation().toVector());
        direction.setY(0);
        if (direction.lengthSquared() < 0.0001) {
            return;
        }
        direction.normalize();
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_BREEZE_IDLE_AIR, 1f, 1.2f);
        Set<Entity> alreadyHit = new HashSet<>();

        new BukkitRunnable() {
            long t = 0;

            @Override
            public void run() {
                if (t >= DURATION_TICKS || !boss.isValid() || boss.isDead()) {
                    boss.setVelocity(new Vector(0, 0, 0));
                    cancel();
                    return;
                }
                boss.setVelocity(direction.clone().multiply(SPEED));
                Location loc = boss.getLocation();
                loc.getWorld().spawnParticle(Particle.CLOUD, loc, 6, 0.4, 0.2, 0.4, 0.02);

                for (Entity entity : loc.getWorld().getNearbyEntities(loc, HIT_RADIUS, HIT_RADIUS, HIT_RADIUS)) {
                    if (!(entity instanceof Player) || alreadyHit.contains(entity)) {
                        continue;
                    }
                    alreadyHit.add(entity);
                    LivingEntity victim = (LivingEntity) entity;
                    DamageAPI.dealDamage(boss, victim, DAMAGE, phase);
                    victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 1, false, false));
                    entity.setVelocity(direction.clone().multiply(1.2).setY(0.3));
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0L, 1L);
    }
}
