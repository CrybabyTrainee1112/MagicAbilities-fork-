package net.trduc.magicabilitiesfork.Boss.ai.htn;

import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Condition;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Method {
    private final String id;
    private final Condition precondition;
    private final List<Task> subtasks;

    public Method(String id, Condition precondition, List<Task> subtasks) {
        this.id = Objects.requireNonNull(id, "Method ID cannot be null");
        this.precondition = Objects.requireNonNull(precondition, "Precondition cannot be null");
        this.subtasks = Collections.unmodifiableList(Objects.requireNonNull(subtasks, "Subtasks cannot be null"));
        if (this.subtasks.isEmpty()) {
            throw new IllegalArgumentException("Method '" + id + "' must have at least one subtask");
        }
    }

    public String getId() {
        return id;
    }

    public Condition getPrecondition() {
        return precondition;
    }

    public List<Task> getSubtasks() {
        return subtasks;
    }

    @Override
    public String toString() {
        return "Method{" + id + ", subtasks=" + subtasks.size() + "}";
    }
}
