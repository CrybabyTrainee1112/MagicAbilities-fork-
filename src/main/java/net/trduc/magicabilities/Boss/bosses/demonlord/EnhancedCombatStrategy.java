package net.trduc.magicabilitiesfork.Boss.bosses.demonlord;

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

public class EnhancedCombatStrategy extends AbstractStrategy {

    public EnhancedCombatStrategy(PressureTargetGoal pressureTargetGoal,
                                  DrainLifeGoal drainLifeGoal,
                                  ControlClusterGoal controlClusterGoal,
                                  FuryGoal furyGoal,
                                  DefendWithShieldGoal defendWithShieldGoal) {
        super("enhanced_combat", buildRootTask(pressureTargetGoal, drainLifeGoal, controlClusterGoal, furyGoal, defendWithShieldGoal), 0.85f);
    }

    private static Task buildRootTask(PressureTargetGoal pressureTargetGoal,
                                      DrainLifeGoal drainLifeGoal,
                                      ControlClusterGoal controlClusterGoal,
                                      FuryGoal furyGoal,
                                      DefendWithShieldGoal defendWithShieldGoal) {
        Method demonicAscensionCombo = new Method(
                "demonic_ascension_combo",
                Condition.and(
                        Condition.lessOrEqual(WorldStateKeys.BOSS_HEALTH_PERCENT, 0.4),
                        Condition.greaterOrEqual(WorldStateKeys.DEMON_BLOOD_CHARGE, 1.0)
                ),
                Arrays.asList(new SkillTask(new DemonicAscensionSkill()), new GoalTask(pressureTargetGoal))
        );

        Method furyCombo = new Method(
                "fury_combo",
                Condition.lessThan(WorldStateKeys.BOSS_HEALTH_PERCENT, 0.35),
                Arrays.asList(new SkillTask(new FurySkill()), new GoalTask(furyGoal))
        );

        Method judgmentCombo = new Method(
                "judgment_combo",
                Condition.greaterOrEqual(WorldStateKeys.DEMON_BLOOD_CHARGE, 1.0),
                Arrays.asList(new SkillTask(new JudgmentSkill()), new GoalTask(pressureTargetGoal))
        );

        Method closeGapCombo = new Method(
                "close_gap_combo",
                Condition.and(
                        Condition.greaterThan(WorldStateKeys.NEAREST_THREAT_DISTANCE, 6.0),
                        Condition.lessOrEqual(WorldStateKeys.NEAREST_THREAT_DISTANCE, 14.0)
                ),
                Arrays.asList(new SkillTask(new ShadowStepSkill()), new GoalTask(pressureTargetGoal))
        );

        Method prisonExecuteCombo = new Method(
                "prison_execute_combo",
                Condition.and(
                        Condition.lessOrEqual(WorldStateKeys.NEAREST_THREAT_DISTANCE, 5.0),
                        Condition.lessOrEqual(WorldStateKeys.TARGET_ROOTED_TICKS, 0)
                ),
                Arrays.asList(
                        new SkillTask(new HellPrisonSkill()),
                        new SkillTask(new HellsTongueSkill()),
                        new GoalTask(pressureTargetGoal)
                )
        );

        Method sanguineChainsCombo = new Method(
                "sanguine_chains_combo",
                Condition.and(
                        Condition.lessOrEqual(WorldStateKeys.NEAREST_THREAT_DISTANCE, 12.0),
                        Condition.lessOrEqual(WorldStateKeys.TARGET_ROOTED_TICKS, 0)
                ),
                Arrays.asList(new SkillTask(new SanguineChainsSkill()), new GoalTask(pressureTargetGoal))
        );

        Method punishClusterCombo = new Method(
                "punish_cluster_combo",
                Condition.equals(WorldStateKeys.PLAYERS_CLUSTERED, 1),
                Arrays.asList(new SkillTask(new HellfireEruptionSkill()), new GoalTask(controlClusterGoal))
        );

        Method punishClusterComboAlt = new Method(
                "punish_cluster_combo_alt",
                Condition.equals(WorldStateKeys.PLAYERS_CLUSTERED, 1),
                Arrays.asList(new SkillTask(new CrimsonNovaSkill()), new GoalTask(controlClusterGoal))
        );

        Method summonCombo = new Method(
                "summon_reinforcements_combo",
                Condition.greaterOrEqual(WorldStateKeys.NEARBY_PLAYER_COUNT, 3),
                Arrays.asList(new SkillTask(new SummonSoulWarriorsSkill()), new GoalTask(pressureTargetGoal))
        );

        Method shieldDefenseCombo = new Method(
                "shield_defense_combo",
                Condition.and(
                        Condition.lessOrEqual(WorldStateKeys.BOSS_HEALTH_PERCENT, 0.6),
                        Condition.greaterThan(WorldStateKeys.BOSS_HEALTH_PERCENT, 0.5)
                ),
                Arrays.asList(new SkillTask(new ShieldDefenseSkill()), new GoalTask(defendWithShieldGoal))
        );

        Method drainToHeal = new Method(
                "drain_to_heal",
                Condition.lessOrEqual(WorldStateKeys.BOSS_HEALTH_PERCENT, 0.5),
                Arrays.asList(new SkillTask(new BloodSiphonSkill()), new GoalTask(drainLifeGoal))
        );

        Method generalPressure = new Method(
                "general_pressure",
                Condition.always(),
                Arrays.asList(new SkillTask(new HellsTongueSkill()), new SkillTask(new HellfireBrandSkill()), new GoalTask(pressureTargetGoal))
        );

        return new CompoundTask("enhanced_combat_root", Arrays.asList(
                demonicAscensionCombo,
                furyCombo,
                judgmentCombo,
                summonCombo,
                punishClusterCombo,
                punishClusterComboAlt,
                closeGapCombo,
                prisonExecuteCombo,
                sanguineChainsCombo,
                shieldDefenseCombo,
                drainToHeal,
                generalPressure
        ));
    }

    @Override
    public boolean shouldActivate(SkillContext context) {
        return context.hasTarget();
    }
}
