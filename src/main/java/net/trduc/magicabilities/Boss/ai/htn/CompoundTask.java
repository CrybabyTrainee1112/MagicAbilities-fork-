package net.trduc.magicabilitiesfork.Boss.ai.htn;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class CompoundTask implements Task {
    private final String id;
    private final List<Method> methods;

    public CompoundTask(String id, List<Method> methods) {
        this.id = Objects.requireNonNull(id, "CompoundTask ID cannot be null");
        this.methods = Collections.unmodifiableList(Objects.requireNonNull(methods, "Methods cannot be null"));
        if (this.methods.isEmpty()) {
            throw new IllegalArgumentException("CompoundTask '" + id + "' must have at least one method");
        }
    }

    @Override
    public String getId() {
        return id;
    }

    public List<Method> getMethods() {
        return methods;
    }

    @Override
    public String toString() {
        return "CompoundTask{" + id + ", methods=" + methods.size() + "}";
    }
}
