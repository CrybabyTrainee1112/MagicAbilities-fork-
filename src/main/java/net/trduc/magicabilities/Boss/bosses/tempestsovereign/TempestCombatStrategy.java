package net.trduc.magicabilitiesfork.Boss.bosses.tempestsovereign;

import net.trduc.magicabilitiesfork.Boss.ai.htn.AbstractStrategy;
import net.trduc.magicabilitiesfork.Boss.ai.htn.CompoundTask;
import net.trduc.magicabilitiesfork.Boss.ai.htn.GoalTask;
import net.trduc.magicabilitiesfork.Boss.ai.htn.Method;
import net.trduc.magicabilitiesfork.Boss.ai.htn.SkillTask;
import net.trduc.magicabilitiesfork.Boss.ai.htn.Task;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Condition;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.SkillContext;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.WorldStateKeys;

import java.util.Arrays;

public class TempestCombatStrategy extends AbstractStrategy {

    public TempestCombatStrategy(WearDownTargetGoal wearDownTargetGoal, ControlAirspaceGoal controlAirspaceGoal) {
        super("tempest_combat", buildRootTask(wearDownTargetGoal, controlAirspaceGoal), 0.85f);
    }

    private static Task buildRootTask(WearDownTargetGoal wearDownTargetGoal, ControlAirspaceGoal controlAirspaceGoal) {
        Method compressionFieldCombo = new Method(
                "compression_field_combo",
                Condition.lessOrEqual(WorldStateKeys.BOSS_HEALTH_PERCENT, 0.3),
                Arrays.asList(new SkillTask(new CompressionFieldSkill()), new GoalTask(wearDownTargetGoal))
        );

        Method vortexCollapseCombo = new Method(
                "vortex_collapse_combo",
                Condition.and(
                        Condition.greaterOrEqual(WorldStateKeys.BOSS_CURRENT_PHASE, 2),
                        Condition.lessOrEqual(WorldStateKeys.NEAREST_THREAT_DISTANCE, 10.0)
                ),
                Arrays.asList(new SkillTask(new VortexCollapseSkill()), new GoalTask(wearDownTargetGoal))
        );

        Method swarmRepositionCombo = new Method(
                "swarm_reposition_combo",
                Condition.greaterOrEqual(WorldStateKeys.NEARBY_PLAYER_COUNT, 4),
                Arrays.asList(new SkillTask(new SkyLeapSkill()), new SkillTask(new VacuumSphereSkill()),
                        new GoalTask(controlAirspaceGoal))
        );

        Method zoneDenialCombo = new Method(
                "zone_denial_combo",
                Condition.greaterOrEqual(WorldStateKeys.NEARBY_PLAYER_COUNT, 3),
                Arrays.asList(new SkillTask(new TempestSkill()), new GoalTask(wearDownTargetGoal))
        );

        Method clusterControlCombo = new Method(
                "cluster_control_combo",
                Condition.equals(WorldStateKeys.PLAYERS_CLUSTERED, 1),
                Arrays.asList(new SkillTask(new CycloneSkill()), new GoalTask(controlAirspaceGoal))
        );
        Method clusterControlComboAlt = new Method(
                "cluster_control_combo_alt",
                Condition.equals(WorldStateKeys.PLAYERS_CLUSTERED, 1),
                Arrays.asList(new SkillTask(new AirBarrageSkill()), new GoalTask(controlAirspaceGoal))
        );
        Method clusterControlComboAlt2 = new Method(
                "cluster_control_combo_alt2",
                Condition.equals(WorldStateKeys.PLAYERS_CLUSTERED, 1),
                Arrays.asList(new SkillTask(new VacuumSphereSkill()), new GoalTask(controlAirspaceGoal))
        );

        Method supersonicGapCloseCombo = new Method(
                "supersonic_gap_close_combo",
                Condition.and(
                        Condition.greaterOrEqual(WorldStateKeys.NEAREST_THREAT_DISTANCE, 12.0),
                        Condition.lessOrEqual(WorldStateKeys.NEAREST_THREAT_DISTANCE, 25.0)
                ),
                Arrays.asList(new SkillTask(new SupersonicStepSkill()), new GoalTask(wearDownTargetGoal))
        );
        Method jetStreamGapCloseCombo = new Method(
                "jet_stream_gap_close_combo",
                Condition.and(
                        Condition.greaterOrEqual(WorldStateKeys.NEAREST_THREAT_DISTANCE, 6.0),
                        Condition.lessOrEqual(WorldStateKeys.NEAREST_THREAT_DISTANCE, 12.0)
                ),
                Arrays.asList(new SkillTask(new JetStreamSkill()), new GoalTask(wearDownTargetGoal))
        );
        Method galeStepCombo = new Method(
                "gale_step_combo",
                Condition.and(
                        Condition.greaterOrEqual(WorldStateKeys.NEAREST_THREAT_DISTANCE, 4.0),
                        Condition.lessOrEqual(WorldStateKeys.NEAREST_THREAT_DISTANCE, 8.0)
                ),
                Arrays.asList(new SkillTask(new GaleStepSkill()), new GoalTask(wearDownTargetGoal))
        );

        Method meleeCombo = new Method(
                "melee_combo",
                Condition.lessOrEqual(WorldStateKeys.NEAREST_THREAT_DISTANCE, 4.0),
                Arrays.asList(new SkillTask(new AirSlashChainSkill()), new SkillTask(new WindBurstSkill()),
                        new GoalTask(wearDownTargetGoal))
        );

        Method generalPressure = new Method(
                "general_pressure",
                Condition.always(),
                Arrays.asList(new SkillTask(new PressureBurstSkill()), new GoalTask(wearDownTargetGoal))
        );

        return new CompoundTask("tempest_combat_root", Arrays.asList(
                compressionFieldCombo,
                vortexCollapseCombo,
                swarmRepositionCombo,
                zoneDenialCombo,
                clusterControlCombo,
                clusterControlComboAlt,
                clusterControlComboAlt2,
                supersonicGapCloseCombo,
                jetStreamGapCloseCombo,
                galeStepCombo,
                meleeCombo,
                generalPressure
        ));
    }

    @Override
    public boolean shouldActivate(SkillContext context) {
        return context.hasTarget();
    }
}
