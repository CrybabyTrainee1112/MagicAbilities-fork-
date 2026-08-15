package net.trduc.magicabilitiesfork.Boss.ai.goal;

import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Condition;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.WorldState;
import java.util.*;
import java.util.Objects;

public abstract class AbstractGoal implements Goal {
    protected final String id;
    protected final List<Condition> targetConditions;
    protected final String description;

    protected AbstractGoal(String id, String description, Condition... conditions) {
        this.id = Objects.requireNonNull(id, "Goal ID cannot be null");
        this.description = Objects.requireNonNull(description, "Description cannot be null");
        this.targetConditions = Collections.unmodifiableList(Arrays.asList(
                Objects.requireNonNull(conditions, "Conditions cannot be null")
        ));
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Collection<Condition> getTargetConditions() {
        return targetConditions;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "Goal{" +
                "id='" + id + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
