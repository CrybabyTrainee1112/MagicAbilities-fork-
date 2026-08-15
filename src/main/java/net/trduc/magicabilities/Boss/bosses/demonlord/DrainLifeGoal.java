package net.trduc.magicabilitiesfork.Boss.bosses.demonlord;

import net.trduc.magicabilitiesfork.Boss.ai.goal.AbstractGoal;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Condition;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.WorldState;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.WorldStateKeys;

public class DrainLifeGoal extends AbstractGoal {
    private static final double SAFE_HEALTH_PERCENT = 0.5;

    public DrainLifeGoal() {
        super("drain_life", "Restore health above " + (int) (SAFE_HEALTH_PERCENT * 100) + "% via blood drain",
                Condition.greaterOrEqual(WorldStateKeys.BOSS_HEALTH_PERCENT, SAFE_HEALTH_PERCENT));
    }

    @Override
    public float assessPriority(WorldState worldState) {
        double health = worldState.getValue(WorldStateKeys.BOSS_HEALTH_PERCENT, 1.0);
        if (health >= SAFE_HEALTH_PERCENT) {
            return 0.05f;
        }
        return (float) Math.min(1.0, (SAFE_HEALTH_PERCENT - health) * 2.0);
    }
}
