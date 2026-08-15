package net.trduc.magicabilitiesfork.Boss.bosses.demonlord;

import net.trduc.magicabilitiesfork.Boss.ai.skill.AbstractSkill;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Condition;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Effect;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.SkillContext;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.WorldStateKeys;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SanguineChainsSkill extends AbstractSkill {
    private static final double RANGE = 12.0;
    private static final int ROOT_DURATION_TICKS = 100;

    public SanguineChainsSkill() {
        super(new Builder("sanguine_chains")
                .cooldownTicks(160)
                .precondition(Condition.and(
                        Condition.lessOrEqual(WorldStateKeys.NEAREST_THREAT_DISTANCE, RANGE),
                        Condition.lessOrEqual(WorldStateKeys.TARGET_ROOTED_TICKS, 0)
                ))
                .effect(Effect.setValue(WorldStateKeys.TARGET_ROOTED_TICKS, ROOT_DURATION_TICKS))
                .targetKey(WorldStateKeys.NEAREST_THREAT)
        );
    }

    @Override
    public void execute(SkillContext context) {
        Mob boss = context.getBoss();
        LivingEntity target = context.getTarget();
        if (target == null || target.isDead()) {
            return;
        }
        if (boss.getLocation().distance(target.getLocation()) > RANGE) {
            return;
        }

        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ROOT_DURATION_TICKS, 6, false, true));

        Location from = boss.getLocation().add(0, 1, 0);
        Location to = target.getLocation().add(0, 1, 0);
        double distance = from.distance(to);
        int steps = Math.max(1, (int) (distance * 2));
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            Location point = from.clone().add(to.clone().subtract(from).toVector().multiply(t));
            boss.getWorld().spawnParticle(Particle.DUST, point, 2, 0.05, 0.05, 0.05, 0,
                    new Particle.DustOptions(Color.fromRGB(150, 0, 0), 1.0f));
        }
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.4f, 1.4f);
    }
}
