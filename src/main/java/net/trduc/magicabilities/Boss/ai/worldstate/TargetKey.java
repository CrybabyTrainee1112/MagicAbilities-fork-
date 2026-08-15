package net.trduc.magicabilitiesfork.Boss.ai.worldstate;

import java.util.Objects;

public class TargetKey {
    private final String name;

    public TargetKey(String name) {
        this.name = Objects.requireNonNull(name, "TargetKey name cannot be null");
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TargetKey targetKey = (TargetKey) o;
        return name.equals(targetKey.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
