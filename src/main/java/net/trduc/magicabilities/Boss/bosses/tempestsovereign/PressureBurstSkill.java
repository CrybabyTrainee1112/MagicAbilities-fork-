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
import org.bukkit.util.Vector;

public class PressureBurstSkill extends AbstractSkill {
    private static final double RADIUS = 5.0;
    private static final double BASE_DAMAGE = 11.0;
    private static final double TRIGGER_RANGE = 8.0;

    public PressureBurstSkill() {
        super(new Builder("pressure_burst")
                .cost(1.5)
                .cooldownTicks(100)
                .precondition(Condition.lessOrEqual(WorldStateKeys.NEAREST_THREAT_DISTANCE, TRIGGER_RANGE))
                .effect(Effect.modifyValue(WorldStateKeys.TARGET_HEALTH_PERCENT, -0.08))
        );
    }

    @Override
    public void execute(SkillContext context) {
        Mob boss = context.getBoss();
        Location loc = boss.getLocation();

        boss.getWorld().playSound(loc, Sound.ENTITY_BREEZE_WIND_BURST, 1f, 1.2f);
        boss.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 2.0f);

        boss.getWorld().spawnParticle(Particle.DUST, loc, 60, RADIUS * 0.6, 0.5, RADIUS * 0.6, 0,
                new Particle.DustOptions(Color.fromRGB(80, 160, 255), 1.5f));
        boss.getWorld().spawnParticle(Particle.DUST, loc, 30, RADIUS * 0.8, 0.7, RADIUS * 0.8, 0,
                new Particle.DustOptions(Color.fromRGB(230, 240, 255), 1.3f));
        boss.getWorld().spawnParticle(Particle.CLOUD, loc, 25, RADIUS * 0.5, 0.4, RADIUS * 0.5, 0.15);

        for (Entity entity : loc.getWorld().getNearbyEntities(loc, RADIUS, RADIUS, RADIUS)) {
            if (!(entity instanceof Player)) {
                continue;
            }
            LivingEntity victim = (LivingEntity) entity;
            double dist = Math.max(0.5, entity.getLocation().distance(loc));
            double dmg = Math.max(3, BASE_DAMAGE - dist * 1.5);
            DamageAPI.dealDamage(boss, victim, dmg, context.getCurrentPhase());

            victim.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 30, 1, false, false));
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 25, 0, false, false));

            Vector kb = entity.getLocation().subtract(loc).toVector();
            if (kb.lengthSquared() > 0.0001) {
                kb.normalize();
            }
            kb.multiply(2.2).setY(0.5);
            entity.setVelocity(kb);
        }
    }
}
