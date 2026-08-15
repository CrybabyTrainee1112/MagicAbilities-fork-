package net.trduc.magicabilitiesfork.Boss.bosses.tempestsovereign;

import net.trduc.magicabilitiesfork.Boss.ai.goal.Goal;
import net.trduc.magicabilitiesfork.Boss.ai.htn.Strategy;
import net.trduc.magicabilitiesfork.Boss.ai.planner.ActionGraph;
import net.trduc.magicabilitiesfork.Boss.ai.planner.GraphBuilder;
import net.trduc.magicabilitiesfork.Boss.ai.sensor.Sensor;
import net.trduc.magicabilitiesfork.Boss.ai.skill.Skill;
import net.trduc.magicabilitiesfork.Boss.core.BossFactory;
import net.trduc.magicabilitiesfork.Boss.core.BossType;
import net.trduc.magicabilitiesfork.Boss.phase.PhaseDefinition;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class TempestSovereignBossType {
    public static final String ID = "tempest_sovereign";

    private TempestSovereignBossType() {
    }

    public static BossType create() {
        List<Skill> goapSkills = Arrays.asList(
                new TempestClawSkill(),
                new PressureBurstSkill(),
                new GaleSlashSkill()
        );

        List<Skill> allSkills = new ArrayList<>(goapSkills);
        allSkills.add(new VortexCollapseSkill());
        allSkills.add(new AirSlashChainSkill());
        allSkills.add(new VacuumSphereSkill());
        allSkills.add(new JetStreamSkill());
        allSkills.add(new AirBarrageSkill());
        allSkills.add(new CompressionFieldSkill());
        allSkills.add(new SupersonicStepSkill());
        allSkills.add(new CycloneSkill());
        allSkills.add(new WindBurstSkill());
        allSkills.add(new GaleStepSkill());
        allSkills.add(new TempestSkill());
        allSkills.add(new SkyLeapSkill());

        WearDownTargetGoal wearDownTargetGoal = new WearDownTargetGoal();
        ControlAirspaceGoal controlAirspaceGoal = new ControlAirspaceGoal();
        List<Goal> goals = Arrays.asList(wearDownTargetGoal, controlAirspaceGoal);

        List<Sensor> sensors = Collections.singletonList(new TempestSovereignCoreSensor());

        TempestCombatStrategy tempestCombatStrategy = new TempestCombatStrategy(wearDownTargetGoal, controlAirspaceGoal);
        List<Strategy> phase2Strategies = Collections.singletonList(tempestCombatStrategy);

        List<PhaseDefinition> phases = Arrays.asList(
                new PhaseDefinition(1, 0.5,
                        goals,
                        Collections.emptyList(),
                        1.0,
                        "\u00a7bTempest Sovereign: \"You dare breathe my air?\""),
                new PhaseDefinition(2, 0.0,
                        goals,
                        phase2Strategies,
                        1.2,
                        "\u00a7b\u00a7lTempest Sovereign: \"Witness the eye of the storm!\"")
        );

        ActionGraph actionGraph = new GraphBuilder(goapSkills).build();

        return new BossType(
                ID,
                EntityType.BREEZE,
                "\u00a7bTempest Sovereign",
                400,
                1.5,
                actionGraph,
                allSkills,
                goals,
                Collections.singletonList(tempestCombatStrategy),
                sensors,
                phases
        );
    }

    public static void register() {
        BossFactory.registerBossType(create());
    }
}
