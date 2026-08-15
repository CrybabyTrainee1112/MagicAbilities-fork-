package net.trduc.magicabilitiesfork.Boss.ai.planner;

import net.trduc.magicabilitiesfork.Boss.ai.skill.Skill;
import java.util.*;
import java.util.Objects;

public class ActionGraph {
    private final List<ActionGraphNode> nodes;
    private final List<Skill> skills;
    private final Map<String, ActionGraphNode> nodesBySkillId;

    public ActionGraph(List<ActionGraphNode> nodes, List<Skill> skills) {
        this.nodes = Collections.unmodifiableList(Objects.requireNonNull(nodes, "Nodes cannot be null"));
        this.skills = Collections.unmodifiableList(Objects.requireNonNull(skills, "Skills cannot be null"));

        Map<String, ActionGraphNode> map = new HashMap<>();
        for (ActionGraphNode node : nodes) {
            map.put(node.getSkill().getId(), node);
        }
        this.nodesBySkillId = Collections.unmodifiableMap(map);
    }

    public List<ActionGraphNode> getNodes() {
        return nodes;
    }

    public List<Skill> getSkills() {
        return skills;
    }

    public ActionGraphNode getNode(String skillId) {
        return nodesBySkillId.get(skillId);
    }

    public int getSize() {
        return nodes.size();
    }

    @Override
    public String toString() {
        return "ActionGraph{" +
                "nodes=" + nodes.size() +
                ", skills=" + skills.size() +
                '}';
    }
}
