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
import org.bukkit.util.Vector;

public class WindBurstSkill extends AbstractSkill {
    private static final double TRIGGER_RANGE = 4.0;
    private static final double RADIUS = 4.5;
    private static final double DAMAGE = 6.0;

    public WindBurstSkill() {
        super(new Builder("wind_burst")
                .cost(1.0)
                .cooldownTicks(80)
                .precondition(Condition.lessOrEqual(WorldStateKeys.NEAREST_THREAT_DISTANCE, TRIGGER_RANGE))
                .effect(Effect.modifyValue(WorldStateKeys.TARGET_HEALTH_PERCENT, -0.05))
        );
    }

    @Override
    public void execute(SkillContext context) {
        Mob boss = context.getBoss();
        int phase = context.getCurrentPhase();
        Location center = boss.getLocation();

        center.getWorld().playSound(center, Sound.ENTITY_BREEZE_WIND_BURST, 1f, 1.5f);
        center.getWorld().spawnParticle(Particle.CLOUD, center, 40, RADIUS * 0.5, 0.4, RADIUS * 0.5, 0.1);

        for (Entity entity : center.getWorld().getNearbyEntities(center, RADIUS, RADIUS, RADIUS)) {
            if (entity.equals(boss) || !(entity instanceof Player)) {
                continue;
            }
            LivingEntity victim = (LivingEntity) entity;
            DamageAPI.dealDamage(boss, victim, DAMAGE, phase);
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 1, false, false));

            Vector kb = entity.getLocation().subtract(center).toVector();
            kb.setY(0);
            if (kb.lengthSquared() > 0.0001) {
                kb.normalize();
            }
            kb.multiply(1.6).setY(0.35);
            entity.setVelocity(kb);
        }
    }
}
