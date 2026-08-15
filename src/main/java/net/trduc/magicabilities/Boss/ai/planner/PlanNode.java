package net.trduc.magicabilitiesfork.Boss.ai.planner;

import net.trduc.magicabilitiesfork.Boss.ai.goal.Goal;
import net.trduc.magicabilitiesfork.Boss.ai.skill.Skill;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Condition;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.WorldState;
import java.util.*;

public class PlanNode implements Comparable<PlanNode> {
    private final WorldState state;
    private final List<Skill> plan;
    private final int depth;
    private final float g_cost;
    private final float h_cost;
    private final float f_cost;

    public PlanNode(WorldState state, List<Skill> plan, float g_cost, float h_cost) {
        this.state = state;
        this.plan = plan;
        this.depth = plan.size();
        this.g_cost = g_cost;
        this.h_cost = h_cost;
        this.f_cost = g_cost + h_cost;
    }

    public WorldState getState() {
        return state;
    }

    public List<Skill> getPlan() {
        return plan;
    }

    public int getDepth() {
        return depth;
    }

    public float getGCost() {
        return g_cost;
    }

    public float getHCost() {
        return h_cost;
    }

    public float getFCost() {
        return f_cost;
    }

    @Override
    public int compareTo(PlanNode other) {
        if (Float.compare(this.f_cost, other.f_cost) != 0) {
            return Float.compare(this.f_cost, other.f_cost);
        }
        return Integer.compare(this.depth, other.depth);
    }

    @Override
    public String toString() {
        return String.format("PlanNode(depth=%d, g=%.1f, h=%.1f, f=%.1f)", depth, g_cost, h_cost, f_cost);
    }
}
