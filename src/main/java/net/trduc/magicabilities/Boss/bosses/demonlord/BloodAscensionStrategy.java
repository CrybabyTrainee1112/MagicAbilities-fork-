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

public class BloodAscensionStrategy extends AbstractStrategy {

    public BloodAscensionStrategy(PressureTargetGoal pressureTargetGoal, DrainLifeGoal drainLifeGoal) {
        super("blood_ascension", buildRootTask(pressureTargetGoal, drainLifeGoal), 0.9f);
    }

    private static Task buildRootTask(PressureTargetGoal pressureTargetGoal, DrainLifeGoal drainLifeGoal) {
        Method ascendNow = new Method(
                "ascend_with_charge",
                Condition.greaterOrEqual(WorldStateKeys.DEMON_BLOOD_CHARGE, 1.0),
                Arrays.asList(new SkillTask(new DemonicAscensionSkill()), new GoalTask(pressureTargetGoal))
        );

        Method buildChargeFirst = new Method(
                "build_charge_first",
                Condition.lessThan(WorldStateKeys.DEMON_BLOOD_CHARGE, 1.0),
                Arrays.asList(new SkillTask(new BloodSiphonSkill()), new GoalTask(drainLifeGoal))
        );

        return new CompoundTask("blood_ascension_root", Arrays.asList(ascendNow, buildChargeFirst));
    }

    @Override
    public boolean shouldActivate(SkillContext context) {
        return context.hasTarget();
    }
}
