package net.trduc.magicabilitiesfork.Boss.ai.htn;

import net.trduc.magicabilitiesfork.Boss.ai.worldstate.SkillContext;

import java.util.Objects;

public abstract class AbstractStrategy implements Strategy {
    protected final String id;
    protected final Task rootTask;
    protected final float priority;

    protected AbstractStrategy(String id, Task rootTask, float priority) {
        this.id = Objects.requireNonNull(id, "Strategy ID cannot be null");
        this.rootTask = Objects.requireNonNull(rootTask, "Root task cannot be null");
        this.priority = priority;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Task getRootTask() {
        return rootTask;
    }

    @Override
    public float getPriority(SkillContext context) {
        return priority;
    }

    @Override
    public abstract boolean shouldActivate(SkillContext context);

    @Override
    public String toString() {
        return "Strategy{" + id + ", priority=" + priority + '}';
    }
}
