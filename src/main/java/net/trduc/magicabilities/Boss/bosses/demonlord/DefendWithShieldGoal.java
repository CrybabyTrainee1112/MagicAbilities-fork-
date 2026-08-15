package net.trduc.magicabilitiesfork.Boss.bosses.demonlord;

import net.trduc.magicabilitiesfork.Boss.ai.goal.AbstractGoal;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Condition;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.WorldState;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.WorldStateKeys;

public class DefendWithShieldGoal extends AbstractGoal {
    private static final double DEFEND_THRESHOLD = 0.6;

    public DefendWithShieldGoal() {
        super("defend_with_shield", "Raise shield to block incoming damage",
                Condition.greaterThan(WorldStateKeys.BOSS_HEALTH_PERCENT, DEFEND_THRESHOLD));
    }

    @Override
    public float assessPriority(WorldState worldState) {
        double health = worldState.getValue(WorldStateKeys.BOSS_HEALTH_PERCENT, 1.0);
        if (health > DEFEND_THRESHOLD) {
            return 0.0f;
        }
        return (float) ((DEFEND_THRESHOLD - health) * 1.5);
    }
}
