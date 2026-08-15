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

import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.magicPlugin;

public class CompressionFieldSkill extends AbstractSkill {
    private static final long DURATION_TICKS = 80L;
    private static final long TICK_INTERVAL = 4L;
    private static final double RADIUS = 4.0;
    private static final double DAMAGE_PER_TICK = 3.0;

    public CompressionFieldSkill() {
        super(new Builder("compression_field")
                .cost(1.0)
                .cooldownTicks(240)
                .precondition(Condition.lessOrEqual(WorldStateKeys.BOSS_HEALTH_PERCENT, 0.3))
                .effect(Effect.setValue(WorldStateKeys.BOSS_HEALTH_PERCENT, 0.3))
        );
    }

    @Override
    public void execute(SkillContext context) {
        Mob boss = context.getBoss();
        int phase = context.getCurrentPhase();

        boss.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, (int) DURATION_TICKS, 1, false, false));
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_BREEZE_IDLE_GROUND, 1f, 0.6f);

        new BukkitRunnable() {
            long t = 0;

            @Override
            public void run() {
                if (t >= DURATION_TICKS || !boss.isValid() || boss.isDead()) {
                    cancel();
                    return;
                }
                Location center = boss.getLocation();
                center.getWorld().spawnParticle(Particle.CLOUD, center, 15, RADIUS * 0.5, 0.6, RADIUS * 0.5, 0.03);

                if (t % TICK_INTERVAL == 0) {
                    for (Entity entity : center.getWorld().getNearbyEntities(center, RADIUS, RADIUS, RADIUS)) {
                        if (!(entity instanceof Player)) {
                            continue;
                        }
                        LivingEntity victim = (LivingEntity) entity;
                        DamageAPI.dealDamage(boss, victim, DAMAGE_PER_TICK, phase);
                    }
                }
                t++;
            }
        }.runTaskTimer(magicPlugin, 0L, 1L);
    }
}
