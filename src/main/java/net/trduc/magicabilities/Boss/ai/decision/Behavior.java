package net.trduc.magicabilitiesfork.Boss.ai.decision;

import net.trduc.magicabilitiesfork.Boss.ai.skill.Skill;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.SkillContext;
import java.util.List;

public interface Behavior {
    String getId();

    boolean shouldActivate(SkillContext context);

    List<Skill> getSkillSequence();

    default float getPriority(SkillContext context) {
        return 0.5f;
    }

    default void onComplete(SkillContext context) {
    }
}
