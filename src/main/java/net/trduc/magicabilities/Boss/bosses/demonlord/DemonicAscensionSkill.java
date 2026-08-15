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

public class DemonicAscensionSkill extends AbstractSkill {
    private static final double CHARGE_COST = 1.0;
    private static final double HEAL_FRACTION = 0.25;
    private static final int BUFF_DURATION_TICKS = 200;

    public DemonicAscensionSkill() {
        super(new Builder("demonic_ascension")
                .cooldownTicks(600)
                .precondition(Condition.greaterOrEqual(WorldStateKeys.DEMON_BLOOD_CHARGE, CHARGE_COST))
                .effect(Effect.composite(
                        Effect.modifyValue(WorldStateKeys.BOSS_HEALTH_PERCENT, HEAL_FRACTION),
                        Effect.setValue(WorldStateKeys.DEMON_BLOOD_CHARGE, 0.0)
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

        boss.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, BUFF_DURATION_TICKS, 1, false, true));
        boss.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, BUFF_DURATION_TICKS, 1, false, true));

        boss.getWorld().spawnParticle(Particle.DUST, boss.getLocation().add(0, 1, 0), 150,
                1.0, 1.5, 1.0, 0, new Particle.DustOptions(Color.fromRGB(120, 0, 0), 2.0f));
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.5f);
    }
}
