package net.trduc.magicabilitiesfork.Boss.bosses.demonlord;

import net.trduc.magicabilitiesfork.Boss.ai.goal.AbstractGoal;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Condition;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.WorldState;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.WorldStateKeys;

public class FuryGoal extends AbstractGoal {
    private static final double FURY_THRESHOLD = 0.35;

    public FuryGoal() {
        super("fury", "Trigger Fury and recover above " + (int) (FURY_THRESHOLD * 100) + "% health",
                Condition.greaterThan(WorldStateKeys.BOSS_HEALTH_PERCENT, FURY_THRESHOLD));
    }

    @Override
    public float assessPriority(WorldState worldState) {
        double health = worldState.getValue(WorldStateKeys.BOSS_HEALTH_PERCENT, 1.0);
        if (health >= FURY_THRESHOLD) {
            return 0.0f;
        }
        return (float) Math.min(1.0, (FURY_THRESHOLD - health) * 3.0);
    }
}
