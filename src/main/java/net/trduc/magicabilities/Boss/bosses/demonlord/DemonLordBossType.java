package net.trduc.magicabilitiesfork.Boss.bosses.demonlord;

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

public final class DemonLordBossType {
    public static final String ID = "demon_lord";

    private DemonLordBossType() {
    }

    public static BossType create() {
        List<Skill> goapSkills = Arrays.asList(
                new BloodSiphonSkill(),
                new CrimsonNovaSkill(),
                new HellPrisonSkill(),
                new HellfireEruptionSkill(),
                new HellsTongueSkill(),
                new ShadowStepSkill(),
                new SummonSoulWarriorsSkill(),
                new FurySkill(),
                new ShieldDefenseSkill(),
                new SanguineChainsSkill(),
                new HellfireBrandSkill()
        );

        List<Skill> allSkills = new ArrayList<>(goapSkills);
        allSkills.add(new JudgmentSkill());
        allSkills.add(new DemonicAscensionSkill());

        PressureTargetGoal pressureTargetGoal = new PressureTargetGoal();
        DrainLifeGoal drainLifeGoal = new DrainLifeGoal();
        ControlClusterGoal controlClusterGoal = new ControlClusterGoal();
        FuryGoal furyGoal = new FuryGoal();
        DefendWithShieldGoal defendWithShieldGoal = new DefendWithShieldGoal();
        SummonReinforcementsGoal summonReinforcementsGoal = new SummonReinforcementsGoal();

        List<Goal> allGoals = Arrays.asList(pressureTargetGoal, drainLifeGoal, controlClusterGoal, furyGoal, defendWithShieldGoal);
        List<Goal> fullGoalCatalog = new ArrayList<>(allGoals);
        fullGoalCatalog.add(summonReinforcementsGoal);
        List<Sensor> sensors = Collections.singletonList(new DemonLordCoreSensor());

        Strategy enhancedCombat = new EnhancedCombatStrategy(pressureTargetGoal, drainLifeGoal, controlClusterGoal, furyGoal, defendWithShieldGoal);
        List<Strategy> allStrategies = Collections.singletonList(enhancedCombat);

        List<PhaseDefinition> phases = Arrays.asList(
                new PhaseDefinition(1, 0.6,
                        Arrays.asList(pressureTargetGoal, drainLifeGoal, furyGoal, defendWithShieldGoal, summonReinforcementsGoal),
                        Collections.emptyList(),
                        1.0,
                        "§c§lDemon Lord: \"Come, mortals. Bleed for me.\""),
                new PhaseDefinition(2, 0.3,
                        allGoals,
                        allStrategies,
                        1.15,
                        "§4§lDemon Lord: \"Your blood feeds my hunger!\""),
                new PhaseDefinition(3, 0.0,
                        allGoals,
                        allStrategies,
                        1.35,
                        "§4§lDemon Lord: \"ASCENSION!\"")
        );

        ActionGraph actionGraph = new GraphBuilder(goapSkills).build();

        return new BossType(
                ID,
                EntityType.WITHER_SKELETON,
                "§4Demon Lord",
                500,
                1.0,
                actionGraph,
                allSkills,
                fullGoalCatalog,
                allStrategies,
                sensors,
                phases
        );
    }

    public static void register() {
        BossFactory.registerBossType(create());
    }
}
