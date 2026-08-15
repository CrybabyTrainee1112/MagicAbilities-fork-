package net.trduc.magicabilitiesfork.Boss.bosses.demonlord;

import net.trduc.magicabilitiesfork.Boss.ai.sensor.Sensor;
import net.trduc.magicabilitiesfork.Boss.ai.sensor.SensorScope;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.SkillContext;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.WorldState;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.WorldStateKeys;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class DemonLordCoreSensor implements Sensor {
    private static final double AWARENESS_RADIUS = 40.0;
    private static final double CLUSTER_RADIUS = 6.0;
    private static final int CLUSTER_MIN_PLAYERS = 2;
    private static final double NO_TARGET_DISTANCE_SENTINEL = 999.0;

    @Override
    public String getId() {
        return "demon_lord_core_sensor";
    }

    @Override
    public SensorScope getScope() {
        return SensorScope.LOCAL;
    }

    @Override
    public int getThrottleTicks() {
        return 4;
    }

    @Override
    public void sense(SkillContext context, WorldState worldState) {
        Mob boss = context.getBoss();

        AttributeInstance bossMaxHealthAttr = boss.getAttribute(Attribute.MAX_HEALTH);
        if (bossMaxHealthAttr != null && bossMaxHealthAttr.getValue() > 0.0) {
            worldState.setValue(WorldStateKeys.BOSS_HEALTH_PERCENT, boss.getHealth() / bossMaxHealthAttr.getValue());
        }
        worldState.setValue(WorldStateKeys.BOSS_CURRENT_PHASE, context.getCurrentPhase());

        LivingEntity target = context.getTarget();
        if (target != null && !target.isDead()) {
            worldState.setValue(WorldStateKeys.NEAREST_THREAT_DISTANCE, boss.getLocation().distance(target.getLocation()));

            AttributeInstance targetMaxHealthAttr = target.getAttribute(Attribute.MAX_HEALTH);
            double targetMaxHealth = targetMaxHealthAttr != null ? targetMaxHealthAttr.getValue() : 0.0;
            worldState.setValue(WorldStateKeys.TARGET_HEALTH_PERCENT,
                    targetMaxHealth > 0.0 ? target.getHealth() / targetMaxHealth : 1.0);
        } else {
            worldState.setValue(WorldStateKeys.NEAREST_THREAT_DISTANCE, NO_TARGET_DISTANCE_SENTINEL);
            worldState.setValue(WorldStateKeys.TARGET_HEALTH_PERCENT, 1.0);
        }

        List<Player> nearbyPlayers = boss.getWorld()
                .getNearbyEntities(boss.getLocation(), AWARENESS_RADIUS, AWARENESS_RADIUS, AWARENESS_RADIUS)
                .stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .collect(Collectors.toList());

        worldState.setValue(WorldStateKeys.NEARBY_PLAYER_COUNT, nearbyPlayers.size());

        long clusteredNearBoss = nearbyPlayers.stream()
                .filter(p -> p.getLocation().distance(boss.getLocation()) <= CLUSTER_RADIUS)
                .count();
        worldState.setValue(WorldStateKeys.PLAYERS_CLUSTERED, clusteredNearBoss >= CLUSTER_MIN_PLAYERS ? 1 : 0);

        double rootedTicksRemaining = worldState.getValue(WorldStateKeys.TARGET_ROOTED_TICKS, 0.0);
        if (rootedTicksRemaining > 0) {
            worldState.setValue(WorldStateKeys.TARGET_ROOTED_TICKS, Math.max(0.0, rootedTicksRemaining - getThrottleTicks()));
        }

        double reinforcementsTicksRemaining = worldState.getValue(WorldStateKeys.REINFORCEMENTS_COOLDOWN_TICKS, 0.0);
        if (reinforcementsTicksRemaining > 0) {
            worldState.setValue(WorldStateKeys.REINFORCEMENTS_COOLDOWN_TICKS,
                    Math.max(0.0, reinforcementsTicksRemaining - getThrottleTicks()));
        }
    }
}
