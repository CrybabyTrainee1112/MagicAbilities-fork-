package net.trduc.magicabilitiesfork.Boss.ai.htn;

import net.trduc.magicabilitiesfork.Boss.ai.skill.Skill;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Condition;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Effect;

import java.util.Objects;

public class SkillTask implements PrimitiveTask {
    private final Skill skill;

    public SkillTask(Skill skill) {
        this.skill = Objects.requireNonNull(skill, "Skill cannot be null");
    }

    public Skill getSkill() {
        return skill;
    }

    @Override
    public String getId() {
        return "skill:" + skill.getId();
    }

    @Override
    public Condition getPrecondition() {
        return skill.getPrecondition();
    }

    @Override
    public Effect getEffect() {
        return skill.getEffect();
    }

    @Override
    public String toString() {
        return "SkillTask{" + skill.getId() + "}";
    }
}
