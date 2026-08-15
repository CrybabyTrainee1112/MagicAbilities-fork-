package net.trduc.magicabilitiesfork.Boss.ai.worldstate;

import java.util.Objects;

public class WorldKey {
    private final String name;

    public WorldKey(String name) {
        this.name = Objects.requireNonNull(name, "WorldKey name cannot be null");
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorldKey worldKey = (WorldKey) o;
        return name.equals(worldKey.name);
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
