package net.trduc.magicabilitiesfork.Boss.ai.goal;

import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Condition;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.SkillContext;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.WorldState;
import java.util.Collection;
import java.util.Objects;

public interface Goal {
    String getId();

    Collection<Condition> getTargetConditions();

    float assessPriority(WorldState worldState);

    default boolean isComplete(WorldState worldState) {
        return getTargetConditions().stream()
                .allMatch(cond -> cond.isSatisfied(worldState));
    }

    String getDescription();
}
