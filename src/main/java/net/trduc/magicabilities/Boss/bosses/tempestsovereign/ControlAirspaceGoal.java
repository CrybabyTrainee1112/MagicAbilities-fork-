package net.trduc.magicabilitiesfork.Boss.bosses.tempestsovereign;

import net.trduc.magicabilitiesfork.Boss.ai.goal.AbstractGoal;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Condition;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.WorldState;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.WorldStateKeys;

public class ControlAirspaceGoal extends AbstractGoal {
    public ControlAirspaceGoal() {
        super("control_airspace", "Punish clustered players until they spread out",
                Condition.equals(WorldStateKeys.PLAYERS_CLUSTERED, 0));
    }

    @Override
    public float assessPriority(WorldState worldState) {
        boolean clustered = worldState.getValue(WorldStateKeys.PLAYERS_CLUSTERED, 0) >= 1.0;
        return clustered ? 0.9f : 0.0f;
    }
}
