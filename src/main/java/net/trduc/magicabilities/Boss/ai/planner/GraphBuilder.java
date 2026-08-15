package net.trduc.magicabilitiesfork.Boss.ai.planner;

import net.trduc.magicabilitiesfork.Boss.ai.skill.Skill;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.WorldState;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Effect;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Condition;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.WorldKey;
import java.util.*;
import java.util.stream.Collectors;

public class GraphBuilder {
    private final List<Skill> skills;
    private Map<String, ActionGraphNode> nodes;

    public GraphBuilder(List<Skill> skills) {
        this.skills = Objects.requireNonNull(skills, "Skills list cannot be null");
    }

    public ActionGraph build() {
        nodes = new HashMap<>();
        for (Skill skill : skills) {
            nodes.put(skill.getId(), new ActionGraphNode(skill));
        }

        connectNodes();

        return new ActionGraph(new ArrayList<>(nodes.values()), new ArrayList<>(skills));
    }

    private void connectNodes() {
        for (Skill skillA : skills) {
            for (Skill skillB : skills) {
                if (skillA.getId().equals(skillB.getId())) {
                    continue;
                }

                if (canConnect(skillA, skillB)) {
                    ActionGraphNode nodeA = nodes.get(skillA.getId());
                    ActionGraphNode nodeB = nodes.get(skillB.getId());

                    nodeA.addOutgoingEdge(nodeB);
                    nodeB.addIncomingEdge(nodeA);
                }
            }
        }
    }

    private boolean canConnect(Skill skillA, Skill skillB) {
        Set<WorldKey> modifiedByA = new HashSet<>(skillA.getEffect().getModifiedKeys());
        Set<WorldKey> neededByB = new HashSet<>(skillB.getPrecondition().getRelevantKeys());

        if (modifiedByA.isEmpty() || neededByB.isEmpty()) {
            return true;
        }

        return !Collections.disjoint(modifiedByA, neededByB);
    }
}
