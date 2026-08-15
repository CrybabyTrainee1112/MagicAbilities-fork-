package net.trduc.magicabilitiesfork.Boss.ai.htn;

import net.trduc.magicabilitiesfork.Boss.ai.worldstate.SkillContext;

public interface Strategy {
    String getId();

    boolean shouldActivate(SkillContext context);

    Task getRootTask();

    default float getPriority(SkillContext context) {
        return 0.5f;
    }

    default void onComplete(SkillContext context) {
    }
}
