package net.trduc.magicabilitiesfork.Boss.ai.memory;

import net.trduc.magicabilitiesfork.Boss.ai.skill.Skill;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.WorldState;
import java.util.*;

public class Memory {
    private final WorldState worldState;
    private final LinkedList<Skill> castHistory;
    private boolean dirtyFlag;
    private final Set<String> goalRelevantFacts;

    public Memory() {
        this.worldState = new WorldState();
        this.castHistory = new LinkedList<>();
        this.dirtyFlag = false;
        this.goalRelevantFacts = new HashSet<>();
    }

    public WorldState getWorldState() {
        return worldState;
    }

    public LinkedList<Skill> getCastHistory() {
        return castHistory;
    }

    public void recordCast(Skill skill) {
        castHistory.addFirst(skill);
        while (castHistory.size() > 20) {
            castHistory.removeLast();
        }
    }

    public Map<String, Integer> snapshotRecentCastCounts() {
        Map<String, Integer> counts = new HashMap<>();
        for (Skill skill : castHistory) {
            counts.merge(skill.getId(), 1, Integer::sum);
        }
        return Collections.unmodifiableMap(counts);
    }

    public boolean isDirty() {
        return dirtyFlag;
    }

    public void setDirty(boolean dirty) {
        this.dirtyFlag = dirty;
    }

    public void setGoalRelevantFacts(Set<String> factNames) {
        goalRelevantFacts.clear();
        goalRelevantFacts.addAll(Objects.requireNonNull(factNames, "Fact names cannot be null"));
    }

    public boolean hasGoalRelevantFactChanged() {
        return dirtyFlag;
    }

    public void clear() {
        worldState.clear();
        castHistory.clear();
        dirtyFlag = false;
    }

    @Override
    public String toString() {
        return "Memory{" +
                "dirtyFlag=" + dirtyFlag +
                ", recentCasts=" + (castHistory.size() > 0 ? castHistory.getFirst().getId() : "none") +
                '}';
    }
}
