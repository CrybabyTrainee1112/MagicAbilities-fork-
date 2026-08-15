package net.trduc.magicabilitiesfork.Boss.bosses.demonlord;

import net.trduc.magicabilitiesfork.Boss.ai.skill.AbstractSkill;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Condition;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Effect;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.SkillContext;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.WorldStateKeys;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Mob;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class FurySkill extends AbstractSkill {
    private static final double HEAL_FRACTION = 0.05;
    private static final int BUFF_DURATION_TICKS = 100;

    public FurySkill() {
        super(new Builder("fury")
                .cost(0.7)
                .cooldownTicks(300)
                .precondition(Condition.lessThan(WorldStateKeys.BOSS_HEALTH_PERCENT, 0.35))
                .effect(Effect.modifyValue(WorldStateKeys.BOSS_HEALTH_PERCENT, HEAL_FRACTION))
        );
    }

    @Override
    public void execute(SkillContext context) {
        Mob boss = context.getBoss();

        boss.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, BUFF_DURATION_TICKS, 2, false, true));
        boss.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, BUFF_DURATION_TICKS, 1, false, true));
        boss.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, BUFF_DURATION_TICKS + 20, 0, false, false));

        AttributeInstance maxHealthAttr = boss.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr != null) {
            double maxHealth = maxHealthAttr.getValue();
            boss.setHealth(Math.min(maxHealth, boss.getHealth() + maxHealth * HEAL_FRACTION));
        }

        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_WITHER_HURT, 0.9f, 0.6f);
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 0.7f, 1.2f);
        boss.getWorld().spawnParticle(Particle.DUST, boss.getLocation().add(0, 1, 0), 40,
                1.5, 1.5, 1.5, 0, new Particle.DustOptions(Color.fromRGB(200, 5, 5), 2.0f));
        boss.getWorld().spawnParticle(Particle.FLAME, boss.getLocation().add(0, 1, 0), 20, 1.5, 1.0, 1.5, 0.08);
    }
}
