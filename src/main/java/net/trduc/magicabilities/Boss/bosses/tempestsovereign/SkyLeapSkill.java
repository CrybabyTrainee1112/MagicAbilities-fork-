package net.trduc.magicabilitiesfork.Boss.bosses.tempestsovereign;

import net.trduc.magicabilitiesfork.Boss.ai.skill.AbstractSkill;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Condition;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Effect;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.SkillContext;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.WorldStateKeys;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Mob;
import org.bukkit.util.Vector;

public class SkyLeapSkill extends AbstractSkill {
    private static final double LAUNCH_VELOCITY = 1.1;

    public SkyLeapSkill() {
        super(new Builder("sky_leap")
                .cost(0.8)
                .cooldownTicks(200)
                .precondition(Condition.greaterOrEqual(WorldStateKeys.NEARBY_PLAYER_COUNT, 4))
                .effect(Effect.setValue(WorldStateKeys.PLAYERS_CLUSTERED, 0))
        );
    }

    @Override
    public void execute(SkillContext context) {
        Mob boss = context.getBoss();
        Location loc = boss.getLocation();

        boss.setVelocity(new Vector(0, LAUNCH_VELOCITY, 0));
        loc.getWorld().spawnParticle(Particle.CLOUD, loc, 30, 0.5, 0.3, 0.5, 0.1);
        loc.getWorld().playSound(loc, Sound.ENTITY_BREEZE_JUMP, 1f, 1.4f);
    }
}
