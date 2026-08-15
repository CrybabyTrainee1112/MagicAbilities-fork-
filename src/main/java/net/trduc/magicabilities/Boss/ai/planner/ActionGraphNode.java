package net.trduc.magicabilitiesfork.Boss.ai.planner;

import net.trduc.magicabilitiesfork.Boss.ai.skill.Skill;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Condition;
import java.util.*;

public class ActionGraphNode {
    private final Skill skill;
    private final List<ActionGraphNode> incomingEdges;
    private final List<ActionGraphNode> outgoingEdges;

    public ActionGraphNode(Skill skill) {
        this.skill = Objects.requireNonNull(skill, "Skill cannot be null");
        this.incomingEdges = new ArrayList<>();
        this.outgoingEdges = new ArrayList<>();
    }

    public Skill getSkill() {
        return skill;
    }

    public List<ActionGraphNode> getIncomingEdges() {
        return incomingEdges;
    }

    public List<ActionGraphNode> getOutgoingEdges() {
        return outgoingEdges;
    }

    public void addIncomingEdge(ActionGraphNode node) {
        if (!incomingEdges.contains(node)) {
            incomingEdges.add(node);
        }
    }

    public void addOutgoingEdge(ActionGraphNode node) {
        if (!outgoingEdges.contains(node)) {
            outgoingEdges.add(node);
        }
    }

    @Override
    public String toString() {
        return "ActionGraphNode{" + skill.getId() + "}";
    }
}
