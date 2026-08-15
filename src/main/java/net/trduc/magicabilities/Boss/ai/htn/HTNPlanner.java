package net.trduc.magicabilitiesfork.Boss.ai.htn;

import net.trduc.magicabilitiesfork.Boss.ai.worldstate.WorldState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class HTNPlanner {
    private static final int MAX_DECOMPOSITION_DEPTH = 12;
    private static final int MAX_TASKS_EXPANDED = 500;

    public Optional<List<PrimitiveTask>> decompose(Task root, WorldState currentState) {
        return decompose(root, currentState, id -> false);
    }

    public Optional<List<PrimitiveTask>> decompose(Task root, WorldState currentState, Predicate<String> skillUnavailable) {
        int[] expandedCounter = {0};
        return decomposeTask(root, currentState, 0, expandedCounter, skillUnavailable);
    }

    private Optional<List<PrimitiveTask>> decomposeTask(Task task, WorldState state, int depth, int[] expandedCounter,
                                                         Predicate<String> skillUnavailable) {
        if (depth > MAX_DECOMPOSITION_DEPTH) {
            return Optional.empty();
        }
        expandedCounter[0]++;
        if (expandedCounter[0] > MAX_TASKS_EXPANDED) {
            return Optional.empty();
        }

        if (task instanceof SkillTask && skillUnavailable.test(((SkillTask) task).getSkill().getId())) {
            return Optional.empty();
        }

        if (task instanceof PrimitiveTask) {
            PrimitiveTask primitive = (PrimitiveTask) task;
            if (!primitive.getPrecondition().isSatisfied(state)) {
                return Optional.empty();
            }
            List<PrimitiveTask> single = new ArrayList<>(1);
            single.add(primitive);
            return Optional.of(single);
        }

        if (task instanceof CompoundTask) {
            CompoundTask compound = (CompoundTask) task;
            for (Method method : compound.getMethods()) {
                if (!method.getPrecondition().isSatisfied(state)) {
                    continue;
                }
                Optional<List<PrimitiveTask>> decomposed =
                        decomposeSubtasks(method.getSubtasks(), state, depth + 1, expandedCounter, skillUnavailable);
                if (decomposed.isPresent()) {
                    return decomposed;
                }
            }
            return Optional.empty();
        }

        return Optional.empty();
    }

    private Optional<List<PrimitiveTask>> decomposeSubtasks(List<Task> subtasks, WorldState state, int depth, int[] expandedCounter,
                                                             Predicate<String> skillUnavailable) {
        List<PrimitiveTask> result = new ArrayList<>();
        WorldState working = new WorldState(state);

        for (Task subtask : subtasks) {
            Optional<List<PrimitiveTask>> sub = decomposeTask(subtask, working, depth, expandedCounter, skillUnavailable);
            if (!sub.isPresent()) {
                return Optional.empty();
            }
            for (PrimitiveTask primitive : sub.get()) {
                result.add(primitive);
                primitive.getEffect().apply(null, working);
            }
        }
        return Optional.of(result);
    }
}
