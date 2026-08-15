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

public class ShieldDefenseSkill extends AbstractSkill {
    private static final double HEALTH_THRESHOLD = 0.6;
    private static final int BUFF_DURATION_TICKS = 80;
    private static final double HEAL_FRACTION = 0.05;

    public ShieldDefenseSkill() {
        super(new Builder("shield_defense")
                .cost(0.7)
                .cooldownTicks(100)
                .precondition(Condition.lessOrEqual(WorldStateKeys.BOSS_HEALTH_PERCENT, HEALTH_THRESHOLD))
                .effect(Effect.composite(
                        Effect.modifyValue(WorldStateKeys.BOSS_HEALTH_PERCENT, HEAL_FRACTION)
                ))
        );
    }

    @Override
    public void execute(SkillContext context) {
        Mob boss = context.getBoss();

        AttributeInstance maxHealthAttr = boss.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr != null) {
            double maxHealth = maxHealthAttr.getValue();
            boss.setHealth(Math.min(maxHealth, boss.getHealth() + maxHealth * HEAL_FRACTION));
        }

        boss.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, BUFF_DURATION_TICKS, 1, false, true));

        boss.getWorld().spawnParticle(Particle.DUST, boss.getLocation().add(0, 0.5, 0), 40,
                0.4, 0.3, 0.4, 0, new Particle.DustOptions(Color.fromRGB(200, 200, 200), 1.5f));
        boss.getWorld().playSound(boss.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.8f, 1.0f);
    }
}
