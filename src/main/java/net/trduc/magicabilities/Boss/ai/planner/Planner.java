package net.trduc.magicabilitiesfork.Boss.ai.planner;

import net.trduc.magicabilitiesfork.Boss.ai.goal.Goal;
import net.trduc.magicabilitiesfork.Boss.ai.skill.Skill;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Condition;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.WorldState;
import java.util.*;

public class Planner {
    private final ActionGraph graph;
    private static final int MAX_PLAN_DEPTH = 20;
    private static final int MAX_NODES_EXPLORED = 1000;
    private static final float REPETITION_PENALTY_PER_RECENT_CAST = 0.4f;

    public Planner(ActionGraph graph) {
        this.graph = Objects.requireNonNull(graph, "ActionGraph cannot be null");
    }

    public Collection<Skill> getAllSkills() {
        return graph.getSkills();
    }

    public Optional<List<Skill>> findPlan(Goal goal, WorldState currentState) {
        return findPlan(goal, currentState, Collections.emptyMap(), Collections.emptySet());
    }

    public Optional<List<Skill>> findPlan(Goal goal, WorldState currentState, Map<String, Integer> recentCastCounts) {
        return findPlan(goal, currentState, recentCastCounts, Collections.emptySet());
    }

    public Optional<List<Skill>> findPlan(Goal goal, WorldState currentState, Map<String, Integer> recentCastCounts,
                                          Set<String> unavailableSkillIds) {
        Objects.requireNonNull(goal, "Goal cannot be null");
        Objects.requireNonNull(currentState, "CurrentState cannot be null");
        Objects.requireNonNull(recentCastCounts, "recentCastCounts cannot be null (use Collections.emptyMap())");
        Objects.requireNonNull(unavailableSkillIds, "unavailableSkillIds cannot be null (use Collections.emptySet())");

        if (goal.isComplete(currentState)) {
            return Optional.of(Collections.emptyList());
        }

        PriorityQueue<PlanNode> openSet = new PriorityQueue<>();
        Set<String> closedSet = new HashSet<>();

        float initialH = heuristic(goal, currentState);
        openSet.offer(new PlanNode(currentState, new ArrayList<>(), 0f, initialH));

        int nodesExplored = 0;

        while (!openSet.isEmpty() && nodesExplored < MAX_NODES_EXPLORED) {
            PlanNode current = openSet.poll();
            nodesExplored++;

            if (goal.isComplete(current.getState())) {
                return Optional.of(current.getPlan());
            }

            if (current.getDepth() >= MAX_PLAN_DEPTH) {
                continue;
            }

            String stateHash = hashState(current.getState());
            if (closedSet.contains(stateHash)) {
                continue;
            }
            closedSet.add(stateHash);

            Collection<Skill> candidates = candidateSkills(current);

            for (Skill skill : candidates) {
                if (!skill.getPrecondition().isSatisfied(current.getState())) {
                    continue;
                }
                if (unavailableSkillIds.contains(skill.getId())) {
                    continue;
                }

                WorldState nextState = new WorldState(current.getState());
                skill.getEffect().apply(null, nextState);

                List<Skill> nextPlan = new ArrayList<>(current.getPlan());
                nextPlan.add(skill);

                float repetitionPenalty = recentCastCounts.getOrDefault(skill.getId(), 0)
                        * REPETITION_PENALTY_PER_RECENT_CAST;
                float g = current.getGCost() + (float) skill.getCost() + repetitionPenalty;
                float h = heuristic(goal, nextState);

                PlanNode nextNode = new PlanNode(nextState, nextPlan, g, h);
                openSet.offer(nextNode);
            }
        }

        return Optional.empty();
    }

    private Collection<Skill> candidateSkills(PlanNode current) {
        List<Skill> plan = current.getPlan();
        if (plan.isEmpty()) {
            return graph.getSkills();
        }

        Skill lastSkill = plan.get(plan.size() - 1);
        ActionGraphNode lastNode = graph.getNode(lastSkill.getId());
        if (lastNode == null || lastNode.getOutgoingEdges().isEmpty()) {
            return graph.getSkills();
        }

        List<Skill> next = new ArrayList<>(lastNode.getOutgoingEdges().size());
        for (ActionGraphNode node : lastNode.getOutgoingEdges()) {
            next.add(node.getSkill());
        }
        return next;
    }

    private float heuristic(Goal goal, WorldState state) {
        long unsatisfied = goal.getTargetConditions().stream()
                .filter(cond -> !cond.isSatisfied(state))
                .count();
        return (float)unsatisfied;
    }

    private String hashState(WorldState state) {
        Map<String, Double> sorted = new TreeMap<>();
        state.getAllWorldValues().forEach((key, value) -> sorted.put(key.getName(), value));
        StringBuilder sb = new StringBuilder();
        sorted.forEach((name, value) -> sb.append(name).append("=").append(value).append("|"));
        return sb.toString();
    }
}
