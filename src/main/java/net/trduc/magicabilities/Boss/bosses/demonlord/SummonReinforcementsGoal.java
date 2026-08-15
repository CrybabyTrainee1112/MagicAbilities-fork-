package net.trduc.magicabilitiesfork.Boss.bosses.demonlord;

import net.trduc.magicabilitiesfork.Boss.ai.goal.AbstractGoal;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Condition;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.WorldState;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.WorldStateKeys;

public class SummonReinforcementsGoal extends AbstractGoal {
    private static final double SWARM_THRESHOLD = 3.0;

    public SummonReinforcementsGoal() {
        super("summon_reinforcements", "Summon soul warriors while swarmed by players",
                Condition.greaterThan(WorldStateKeys.REINFORCEMENTS_COOLDOWN_TICKS, 0.0));
    }

    @Override
    public float assessPriority(WorldState worldState) {
        double nearbyPlayers = worldState.getValue(WorldStateKeys.NEARBY_PLAYER_COUNT, 0.0);
        double cooldownRemaining = worldState.getValue(WorldStateKeys.REINFORCEMENTS_COOLDOWN_TICKS, 0.0);
        if (nearbyPlayers < SWARM_THRESHOLD || cooldownRemaining > 0) {
            return 0.0f;
        }
        return 0.75f;
    }
}
