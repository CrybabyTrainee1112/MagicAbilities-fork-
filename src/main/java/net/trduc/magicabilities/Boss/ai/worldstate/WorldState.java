package net.trduc.magicabilitiesfork.Boss.ai.worldstate;

import org.bukkit.Location;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class WorldState {
    private final Map<WorldKey, Double> worldValues;
    private final Map<TargetKey, Location> targetLocations;

    public WorldState() {
        this.worldValues = new HashMap<>();
        this.targetLocations = new HashMap<>();
    }

    public WorldState(WorldState other) {
        this.worldValues = new HashMap<>(other.worldValues);
        this.targetLocations = new HashMap<>(other.targetLocations);
    }


    public void setValue(WorldKey key, double value) {
        Objects.requireNonNull(key, "WorldKey cannot be null");
        worldValues.put(key, value);
    }

    public double getValue(WorldKey key, double defaultValue) {
        Objects.requireNonNull(key, "WorldKey cannot be null");
        return worldValues.getOrDefault(key, defaultValue);
    }

    public double getValue(WorldKey key) {
        Objects.requireNonNull(key, "WorldKey cannot be null");
        Double value = worldValues.get(key);
        return value != null ? value : 0.0;
    }

    public boolean hasValue(WorldKey key) {
        Objects.requireNonNull(key, "WorldKey cannot be null");
        return worldValues.containsKey(key);
    }

    public void removeValue(WorldKey key) {
        Objects.requireNonNull(key, "WorldKey cannot be null");
        worldValues.remove(key);
    }


    public void setLocation(TargetKey key, Location location) {
        Objects.requireNonNull(key, "TargetKey cannot be null");
        targetLocations.put(key, location);
    }

    public Location getLocation(TargetKey key) {
        Objects.requireNonNull(key, "TargetKey cannot be null");
        return targetLocations.get(key);
    }

    public boolean hasLocation(TargetKey key) {
        Objects.requireNonNull(key, "TargetKey cannot be null");
        return targetLocations.containsKey(key);
    }

    public void removeLocation(TargetKey key) {
        Objects.requireNonNull(key, "TargetKey cannot be null");
        targetLocations.remove(key);
    }


    public void clear() {
        worldValues.clear();
        targetLocations.clear();
    }

    public Map<WorldKey, Double> getAllWorldValues() {
        return new HashMap<>(worldValues);
    }

    public Map<TargetKey, Location> getAllTargetLocations() {
        return new HashMap<>(targetLocations);
    }

    @Override
    public String toString() {
        return "WorldState{" +
                "worldValues=" + worldValues +
                ", targetLocations=" + targetLocations +
                '}';
    }
}
