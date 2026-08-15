package net.trduc.magicabilitiesfork.Boss.bosses.tempestsovereign;

import net.trduc.magicabilitiesfork.Boss.ai.skill.AbstractSkill;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Condition;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Effect;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.SkillContext;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.WorldStateKeys;
import net.trduc.magicabilitiesfork.Boss.damage.DamageAPI;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.magicPlugin;

public class AirSlashChainSkill extends AbstractSkill {
    private static final double TRIGGER_RANGE = 4.0;
    private static final double[] HIT_DAMAGE = {7.0, 10.0, 13.0};
    private static final long TICKS_BETWEEN_HITS = 6L;

    public AirSlashChainSkill() {
        super(new Builder("air_slash_chain")
                .cost(1.2)
                .cooldownTicks(110)
                .precondition(Condition.lessOrEqual(WorldStateKeys.NEAREST_THREAT_DISTANCE, TRIGGER_RANGE))
                .effect(Effect.modifyValue(WorldStateKeys.TARGET_HEALTH_PERCENT, -0.10))
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

        new BukkitRunnable() {
            int hit = 0;

            @Override
            public void run() {
                if (hit >= HIT_DAMAGE.length || !boss.isValid() || !target.isValid() || target.isDead()) {
                    cancel();
                    return;
                }
                boss.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0, 1, 0), 4, 0.3, 0.3, 0.3, 0.0);
                boss.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.9f + hit * 0.1f);

                DamageAPI.dealDamage(boss, target, HIT_DAMAGE[hit], phase);
                Vector push = target.getLocation().toVector().subtract(boss.getLocation().toVector());
                push.setY(0);
                if (push.lengthSquared() > 0.0001) {
                    push.normalize();
                }
                push.multiply(0.4).setY(0.15 + hit * 0.05);
                target.setVelocity(push);

                if (hit == HIT_DAMAGE.length - 1) {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 20, 0, false, false));
                }
                hit++;
            }
        }.runTaskTimer(magicPlugin, 0L, TICKS_BETWEEN_HITS);
    }
}
