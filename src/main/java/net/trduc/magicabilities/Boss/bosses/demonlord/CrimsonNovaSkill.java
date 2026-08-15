package net.trduc.magicabilitiesfork.Boss.bosses.demonlord;

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

import java.util.Collection;

public class CrimsonNovaSkill extends AbstractSkill {
    private static final double RADIUS = 6.0;
    private static final double DAMAGE = 8.0;
    private static final double CHARGE_GAIN = 0.35;

    public CrimsonNovaSkill() {
        super(new Builder("crimson_nova")
                .cost(2.0)
                .cooldownTicks(140)
                .precondition(Condition.equals(WorldStateKeys.PLAYERS_CLUSTERED, 1))
                .effect(Effect.composite(
                        Effect.modifyValue(WorldStateKeys.DEMON_BLOOD_CHARGE, CHARGE_GAIN),
                        Effect.modifyValue(WorldStateKeys.TARGET_HEALTH_PERCENT, -0.2),
                        Effect.setValue(WorldStateKeys.PLAYERS_CLUSTERED, 0)
                ))
        );
    }

    @Override
    public void execute(SkillContext context) {
        Mob boss = context.getBoss();
        Location center = boss.getLocation();

        Collection<Entity> nearby = boss.getWorld().getNearbyEntities(center, RADIUS, RADIUS, RADIUS);
        for (Entity entity : nearby) {
            if (entity instanceof Player) {
                DamageAPI.dealDamage(boss, (LivingEntity) entity, DAMAGE, context.getCurrentPhase());
            }
        }

        boss.getWorld().spawnParticle(Particle.DUST, center.clone().add(0, 0.2, 0), 80,
                RADIUS * 0.6, 0.2, RADIUS * 0.6, 0,
                new Particle.DustOptions(Color.fromRGB(80, 0, 0), 1.5f));
        boss.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.6f);
    }
}
