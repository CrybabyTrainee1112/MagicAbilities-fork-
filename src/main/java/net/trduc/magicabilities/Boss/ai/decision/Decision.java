package net.trduc.magicabilitiesfork.Boss.ai.decision;

import net.trduc.magicabilitiesfork.Boss.ai.goal.Goal;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.SkillContext;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.WorldState;
import java.util.Collection;
import java.util.Optional;

public class Decision {
    private Goal currentGoal;

    public Optional<Goal> chooseGoal(Collection<Goal> availableGoals, WorldState worldState) {
        if (availableGoals.isEmpty()) {
            return Optional.empty();
        }

        Goal bestGoal = null;
        float bestPriority = -1f;

        for (Goal goal : availableGoals) {
            float priority = goal.assessPriority(worldState);
            if (priority > bestPriority) {
                bestPriority = priority;
                bestGoal = goal;
            }
        }

        return Optional.ofNullable(bestGoal);
    }

    public boolean shouldSwitchGoal(Goal currentGoal, Collection<Goal> availableGoals, WorldState worldState) {
        if (currentGoal == null) {
            return true;
        }

        Optional<Goal> bestGoal = chooseGoal(availableGoals, worldState);
        if (!bestGoal.isPresent()) {
            return false;
        }

        float currentPriority = currentGoal.assessPriority(worldState);
        float bestPriority = bestGoal.get().assessPriority(worldState);

        return bestPriority > currentPriority * 1.2f;
    }

    public Goal getCurrentGoal() {
        return currentGoal;
    }

    public void setCurrentGoal(Goal goal) {
        currentGoal = goal;
    }
}
