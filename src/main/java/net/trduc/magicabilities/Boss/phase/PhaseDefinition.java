package net.trduc.magicabilitiesfork.Boss.phase;

import net.trduc.magicabilitiesfork.Boss.ai.goal.Goal;
import net.trduc.magicabilitiesfork.Boss.ai.htn.Strategy;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class PhaseDefinition {
    private final int phaseNumber;
    private final double healthThresholdPercent;
    private final List<Goal> availableGoals;
    private final List<Strategy> availableStrategies;
    private final double damageMultiplier;
    private final String phaseTransitionMessage;

    public PhaseDefinition(int phaseNumber, double healthThresholdPercent,
                          List<Goal> availableGoals, List<Strategy> availableStrategies,
                          double damageMultiplier, String phaseTransitionMessage) {
        this.phaseNumber = phaseNumber;
        this.healthThresholdPercent = Math.max(0.0, Math.min(1.0, healthThresholdPercent));
        this.availableGoals = Collections.unmodifiableList(
                Objects.requireNonNull(availableGoals, "Available goals cannot be null")
        );
        this.availableStrategies = Collections.unmodifiableList(
                Objects.requireNonNull(availableStrategies, "Available strategies cannot be null")
        );
        this.damageMultiplier = Math.max(0.0, damageMultiplier);
        this.phaseTransitionMessage = Objects.requireNonNull(phaseTransitionMessage, "Message cannot be null");
    }

    public int getPhaseNumber() {
        return phaseNumber;
    }

    public double getHealthThresholdPercent() {
        return healthThresholdPercent;
    }

    public Collection<Goal> getAvailableGoals() {
        return availableGoals;
    }

    public Collection<Strategy> getAvailableStrategies() {
        return availableStrategies;
    }

    public double getDamageMultiplier() {
        return damageMultiplier;
    }

    public String getPhaseTransitionMessage() {
        return phaseTransitionMessage;
    }

    @Override
    public String toString() {
        return "Phase" + phaseNumber +
                "{health>=" + (int)(healthThresholdPercent * 100) + "%" +
                ", dmgMult=" + damageMultiplier +
                ", goals=" + availableGoals.size() +
                ", strategies=" + availableStrategies.size() + "}";
    }
}
