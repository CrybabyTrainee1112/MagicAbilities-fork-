package net.trduc.magicabilitiesfork.Boss.ai.htn;

import net.trduc.magicabilitiesfork.Boss.ai.goal.Goal;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Condition;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Effect;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.WorldState;

import java.util.Objects;

public class GoalTask implements PrimitiveTask {
    private final Goal goal;

    public GoalTask(Goal goal) {
        this.goal = Objects.requireNonNull(goal, "Goal cannot be null");
    }

    public Goal getGoal() {
        return goal;
    }

    @Override
    public String getId() {
        return "goal:" + goal.getId();
    }

    @Override
    public Condition getPrecondition() {
        return new Condition() {
            @Override
            public boolean isSatisfied(WorldState state) {
                return !goal.isComplete(state);
            }

            @Override
            public String getDescription() {
                return "!isComplete(" + goal.getId() + ")";
            }
        };
    }

    @Override
    public Effect getEffect() {
        return new Effect() {
            @Override
            public void apply(net.trduc.magicabilitiesfork.Boss.ai.worldstate.SkillContext context, WorldState state) {
            }

            @Override
            public String getDescription() {
                return "GoalTask(" + goal.getId() + ") - effect unknown until GOAP resolves it";
            }
        };
    }

    @Override
    public String toString() {
        return "GoalTask{" + goal.getId() + "}";
    }
}
