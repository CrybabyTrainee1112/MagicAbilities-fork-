package net.trduc.magicabilitiesfork.Boss.bosses.demonlord;

import net.trduc.magicabilitiesfork.Boss.ai.goal.AbstractGoal;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Condition;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.WorldState;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.WorldStateKeys;

public class PressureTargetGoal extends AbstractGoal {
    private static final double CARE_RADIUS = 30.0;

    public PressureTargetGoal() {
        super("pressure_target", "Reduce current target's health to 0",
                Condition.lessOrEqual(WorldStateKeys.TARGET_HEALTH_PERCENT, 0.0));
    }

    @Override
    public float assessPriority(WorldState worldState) {
        double distance = worldState.getValue(WorldStateKeys.NEAREST_THREAT_DISTANCE, 999.0);
        if (distance > CARE_RADIUS) {
            return 0.1f;
        }
        return (float) Math.max(0.2, 1.0 - (distance / CARE_RADIUS));
    }
}
