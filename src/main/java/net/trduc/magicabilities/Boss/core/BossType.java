package net.trduc.magicabilitiesfork.Boss.core;

import net.trduc.magicabilitiesfork.Boss.ai.goal.Goal;
import net.trduc.magicabilitiesfork.Boss.ai.skill.Skill;
import net.trduc.magicabilitiesfork.Boss.ai.htn.Strategy;
import net.trduc.magicabilitiesfork.Boss.ai.sensor.Sensor;
import net.trduc.magicabilitiesfork.Boss.ai.planner.ActionGraph;
import net.trduc.magicabilitiesfork.Boss.phase.PhaseDefinition;
import org.bukkit.entity.EntityType;

import java.util.*;

public class BossType {
    private final String id;
    private final EntityType entityType;
    private final String displayName;
    private final int maxHealth;
    private final double scale;
    private final ActionGraph actionGraph;
    private final List<Skill> skills;
    private final List<Goal> goals;
    private final List<Strategy> strategies;
    private final List<Sensor> sensors;
    private final List<PhaseDefinition> phases;

    public BossType(String id, EntityType entityType, String displayName, int maxHealth, double scale,
                    ActionGraph actionGraph, List<Skill> skills, List<Goal> goals,
                    List<Strategy> strategies, List<Sensor> sensors, List<PhaseDefinition> phases) {
        this.id = Objects.requireNonNull(id, "Boss ID cannot be null");
        this.entityType = Objects.requireNonNull(entityType, "EntityType cannot be null");
        this.displayName = Objects.requireNonNull(displayName, "Display name cannot be null");
        this.maxHealth = maxHealth;
        this.scale = scale;
        this.actionGraph = Objects.requireNonNull(actionGraph, "ActionGraph cannot be null");
        this.skills = Collections.unmodifiableList(Objects.requireNonNull(skills, "Skills cannot be null"));
        this.goals = Collections.unmodifiableList(Objects.requireNonNull(goals, "Goals cannot be null"));
        this.strategies = Collections.unmodifiableList(Objects.requireNonNull(strategies, "Strategies cannot be null"));
        this.sensors = Collections.unmodifiableList(Objects.requireNonNull(sensors, "Sensors cannot be null"));
        this.phases = Collections.unmodifiableList(Objects.requireNonNull(phases, "Phases cannot be null"));
        if (this.phases.isEmpty()) {
            throw new IllegalArgumentException("BossType '" + id + "' must define at least one PhaseDefinition");
        }
    }

    public String getId() {
        return id;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public double getScale() {
        return scale;
    }

    public ActionGraph getActionGraph() {
        return actionGraph;
    }

    public List<Skill> getSkills() {
        return skills;
    }

    public List<Goal> getGoals() {
        return goals;
    }

    public List<Strategy> getStrategies() {
        return strategies;
    }

    public List<Sensor> getSensors() {
        return sensors;
    }

    public List<PhaseDefinition> getPhases() {
        return phases;
    }

    @Override
    public String toString() {
        return "BossType{" +
                "id='" + id + '\'' +
                ", displayName='" + displayName + '\'' +
                ", scale=" + scale +
                ", skills=" + skills.size() +
                ", goals=" + goals.size() +
                ", strategies=" + strategies.size() +
                ", phases=" + phases.size() +
                '}';
    }
}
