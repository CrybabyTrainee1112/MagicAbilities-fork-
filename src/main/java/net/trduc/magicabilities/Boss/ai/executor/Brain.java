package net.trduc.magicabilitiesfork.Boss.ai.executor;

import net.trduc.magicabilitiesfork.Boss.ai.decision.Decision;
import net.trduc.magicabilitiesfork.Boss.ai.goal.Goal;
import net.trduc.magicabilitiesfork.Boss.ai.htn.GoalTask;
import net.trduc.magicabilitiesfork.Boss.ai.htn.HTNPlanner;
import net.trduc.magicabilitiesfork.Boss.ai.htn.PrimitiveTask;
import net.trduc.magicabilitiesfork.Boss.ai.htn.SkillTask;
import net.trduc.magicabilitiesfork.Boss.ai.htn.Strategy;
import net.trduc.magicabilitiesfork.Boss.ai.memory.Memory;
import net.trduc.magicabilitiesfork.Boss.ai.planner.ActionGraph;
import net.trduc.magicabilitiesfork.Boss.ai.planner.Planner;
import net.trduc.magicabilitiesfork.Boss.ai.sensor.Sensor;
import net.trduc.magicabilitiesfork.Boss.ai.skill.Skill;
import net.trduc.magicabilitiesfork.Boss.ai.skill.SkillExecutor;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.SkillContext;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.WorldState;
import net.trduc.magicabilitiesfork.Boss.phase.PhaseDefinition;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static net.trduc.magicabilitiesfork.MagicAbilitiesfork.magicPlugin;

public class Brain {
    private static boolean debugEnabled = false;

    public static void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
    }

    private static final int STRATEGY_RECONSIDER_INTERVAL_TICKS = 20;

    private final Mob boss;
    private final SkillExecutor skillExecutor;
    private final Memory memory;
    private final Decision decision;
    private final Planner planner;
    private final HTNPlanner htnPlanner;
    private final List<Sensor> sensors;
    private final Map<Integer, PhaseDefinition> phasesByNumber;
    private final List<Goal> fallbackGoals;
    private final List<Strategy> fallbackStrategies;

    private Queue<Skill> currentPlan;
    private Queue<PrimitiveTask> currentDecomposition;
    private Strategy activeStrategy;
    private Goal activeGoapGoal;
    private int sensorUpdateCounter;
    private int ticksSinceLastDecomposition;
    private CompletableFuture<Optional<List<Skill>>> planningFuture;
    private Goal planningGoal;

    public Brain(Mob boss, SkillExecutor skillExecutor, ActionGraph actionGraph,
                 List<Sensor> sensors, List<PhaseDefinition> phases,
                 List<Goal> fallbackGoals, List<Strategy> fallbackStrategies) {
        this.boss = Objects.requireNonNull(boss, "Boss cannot be null");
        this.skillExecutor = Objects.requireNonNull(skillExecutor, "SkillExecutor cannot be null");
        this.memory = new Memory();
        this.decision = new Decision();
        this.planner = new Planner(actionGraph);
        this.htnPlanner = new HTNPlanner();
        this.sensors = Objects.requireNonNull(sensors, "Sensors cannot be null");
        this.fallbackGoals = Objects.requireNonNull(fallbackGoals, "Fallback goals cannot be null");
        this.fallbackStrategies = Objects.requireNonNull(fallbackStrategies, "Fallback strategies cannot be null");

        Objects.requireNonNull(phases, "Phases cannot be null");
        Map<Integer, PhaseDefinition> byNumber = new HashMap<>();
        for (PhaseDefinition phase : phases) {
            byNumber.put(phase.getPhaseNumber(), phase);
        }
        this.phasesByNumber = Collections.unmodifiableMap(byNumber);

        this.currentPlan = new LinkedList<>();
        this.currentDecomposition = new LinkedList<>();
        this.sensorUpdateCounter = 0;
    }

    public void tick(LivingEntity currentTarget, int currentPhase) {
        updateSensors(currentTarget, currentPhase);

        SkillContext context = new SkillContext(boss, currentTarget, currentPhase, memory.getWorldState());

        List<Goal> availableGoals = resolveGoalsForPhase(currentPhase);
        List<Strategy> availableStrategies = resolveStrategiesForPhase(currentPhase);

        Strategy topStrategy = findTopStrategy(availableStrategies, context);
        boolean coastingOnFinalGoalTask = activeStrategy != null
                && currentDecomposition.size() == 1
                && currentDecomposition.peek() instanceof GoalTask;
        boolean strategyChanged = topStrategy != null && activeStrategy == null;
        boolean strategyEscalated = topStrategy != null && activeStrategy != null
                && topStrategy.getPriority(context) > activeStrategy.getPriority(context);
        boolean timeToReconsider = topStrategy != null && topStrategy == activeStrategy
                && coastingOnFinalGoalTask
                && ticksSinceLastDecomposition >= STRATEGY_RECONSIDER_INTERVAL_TICKS;

        if (strategyChanged || strategyEscalated || timeToReconsider) {
            activateStrategy(topStrategy, context);
            ticksSinceLastDecomposition = 0;
        } else {
            ticksSinceLastDecomposition++;
        }

        if (activeStrategy != null) {
            runStrategy(context);
        } else {
            runGoap(context, availableGoals);
        }
    }

    private void updateSensors(LivingEntity currentTarget, int currentPhase) {
        SkillContext context = new SkillContext(boss, currentTarget, currentPhase, memory.getWorldState());

        for (Sensor sensor : sensors) {
            if (!sensor.isActive(context)) {
                continue;
            }

            int throttleTicks = Math.max(1, sensor.getThrottleTicks());
            if (sensorUpdateCounter % throttleTicks == 0) {
                sensor.sense(context, memory.getWorldState());
            }
        }
        sensorUpdateCounter++;
    }

    private List<Goal> resolveGoalsForPhase(int currentPhase) {
        PhaseDefinition def = phasesByNumber.get(currentPhase);
        if (def == null) {
            return fallbackGoals;
        }
        Collection<Goal> phaseGoals = def.getAvailableGoals();
        return phaseGoals.isEmpty() ? fallbackGoals : new ArrayList<>(phaseGoals);
    }

    private List<Strategy> resolveStrategiesForPhase(int currentPhase) {
        PhaseDefinition def = phasesByNumber.get(currentPhase);
        if (def == null) {
            return fallbackStrategies;
        }
        Collection<Strategy> phaseStrategies = def.getAvailableStrategies();
        return phaseStrategies.isEmpty() ? fallbackStrategies : new ArrayList<>(phaseStrategies);
    }

    private Strategy findTopStrategy(List<Strategy> availableStrategies, SkillContext context) {
        Strategy topStrategy = null;
        float topPriority = -1f;

        for (Strategy strategy : availableStrategies) {
            if (strategy.shouldActivate(context)) {
                float priority = strategy.getPriority(context);
                if (priority > topPriority) {
                    topPriority = priority;
                    topStrategy = strategy;
                }
            }
        }

        return topStrategy;
    }

    private void activateStrategy(Strategy strategy, SkillContext context) {
        activeStrategy = strategy;
        Optional<List<PrimitiveTask>> decomposition = htnPlanner.decompose(strategy.getRootTask(), context.getWorldState(),
                skillId -> !skillExecutor.isCooldownExpired(skillId));

        currentDecomposition = new LinkedList<>(decomposition.orElseGet(Collections::emptyList));
        currentPlan.clear();
        activeGoapGoal = null;

        if (currentDecomposition.isEmpty()) {
            activeStrategy = null;
        }
    }

    private void runStrategy(SkillContext context) {
        if (currentDecomposition.isEmpty()) {
            activeStrategy.onComplete(context);
            activeStrategy = null;
            memory.setDirty(true);
            return;
        }

        PrimitiveTask nextTask = currentDecomposition.peek();

        if (nextTask instanceof SkillTask) {
            Skill skill = ((SkillTask) nextTask).getSkill();
            if (skillExecutor.tryExecute(skill, context)) {
                memory.recordCast(skill);
                currentDecomposition.poll();
            } else if (!skillExecutor.isCooldownExpired(skill.getId())) {
                currentDecomposition.poll();
            }
        } else if (nextTask instanceof GoalTask) {
            Goal goal = ((GoalTask) nextTask).getGoal();

            if (goal.isComplete(context.getWorldState())) {
                currentDecomposition.poll();
                currentPlan.clear();
                activeGoapGoal = null;
                return;
            }

            if (activeGoapGoal != goal) {
                activeGoapGoal = goal;
                decision.setCurrentGoal(goal);
                currentPlan.clear();
                requestPlan(goal);
            }

            executeGoapStep(context);
        }
        else {
            currentDecomposition.poll();
        }
    }

    private void runGoap(SkillContext context, List<Goal> availableGoals) {
        if (currentPlan.isEmpty() || memory.isDirty()) {
            memory.setDirty(false);

            Optional<Goal> bestGoal = decision.chooseGoal(availableGoals, memory.getWorldState());
            if (bestGoal.isPresent()) {
                Goal currentGoal = decision.getCurrentGoal();
                if (currentGoal == null || decision.shouldSwitchGoal(currentGoal, availableGoals, memory.getWorldState())) {
                    decision.setCurrentGoal(bestGoal.get());
                    activeGoapGoal = bestGoal.get();
                    requestPlan(bestGoal.get());
                }
            }
        }

        executeGoapStep(context);
    }

    private void executeGoapStep(SkillContext context) {
        if (!currentPlan.isEmpty()) {
            Skill nextSkill = currentPlan.peek();
            if (skillExecutor.tryExecute(nextSkill, context)) {
                memory.recordCast(nextSkill);
                currentPlan.poll();
            } else {
                memory.setDirty(true);
            }
        }
    }

    private void requestPlan(Goal goal) {
        if (planningFuture != null && !planningFuture.isDone() && planningGoal == goal) {
            return;
        }
        planningGoal = goal;

        WorldState stateSnapshot = new WorldState(memory.getWorldState());
        Map<String, Integer> castCountsSnapshot = memory.snapshotRecentCastCounts();
        Set<String> onCooldownSnapshot = new HashSet<>();
        for (Skill skill : planner.getAllSkills()) {
            if (!skillExecutor.isCooldownExpired(skill.getId())) {
                onCooldownSnapshot.add(skill.getId());
            }
        }
        planningFuture = CompletableFuture.supplyAsync(() ->
                planner.findPlan(goal, stateSnapshot, castCountsSnapshot, onCooldownSnapshot)
        );

        planningFuture.whenComplete((optionalPlan, exception) ->
                org.bukkit.Bukkit.getScheduler().runTask(magicPlugin, () -> {
                    if (goal != planningGoal) {
                        return;
                    }
                    if (exception != null) {
                        System.err.println("Planning error: " + exception.getMessage());
                        memory.setDirty(true);
                    } else if (optionalPlan.isPresent()) {
                        currentPlan.clear();
                        currentPlan.addAll(optionalPlan.get());
                        if (debugEnabled) {
                            org.bukkit.Bukkit.getLogger().info("[Boss] " + boss.getName() + " plan for goal '"
                                    + goal.getId() + "': " + optionalPlan.get().size() + " skill(s) - "
                                    + optionalPlan.get().stream().map(net.trduc.magicabilitiesfork.Boss.ai.skill.Skill::getId)
                                            .collect(java.util.stream.Collectors.joining(", ")));
                        }
                    } else if (debugEnabled) {
                        org.bukkit.Bukkit.getLogger().info("[Boss] " + boss.getName()
                                + " found NO plan for goal '" + goal.getId() + "' (no skill precondition satisfiable)");
                    }
                })
        );
    }

    public void cancelPlan() {
        currentPlan.clear();
        currentDecomposition.clear();
        activeStrategy = null;
        activeGoapGoal = null;
        ticksSinceLastDecomposition = 0;
        if (planningFuture != null && !planningFuture.isDone()) {
            planningFuture.cancel(false);
        }
        planningGoal = null;
        memory.setDirty(true);
    }

    public Memory getMemory() {
        return memory;
    }

    public Goal getCurrentGoal() {
        return decision.getCurrentGoal();
    }

    public Strategy getActiveStrategy() {
        return activeStrategy;
    }
}
