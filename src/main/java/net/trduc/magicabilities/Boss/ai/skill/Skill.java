package net.trduc.magicabilitiesfork.Boss.ai.skill;

import net.trduc.magicabilitiesfork.Boss.ai.worldstate.*;
import java.util.Optional;

public interface Skill {
    String getId();

    double getCost();

    Condition getPrecondition();

    Effect getEffect();

    Optional<TargetKey> getTargetKey();

    int getCooldownTicks();

    default boolean canExecute(SkillContext context) {
        return getPrecondition().isSatisfied(context.getWorldState());
    }

    void execute(SkillContext context);

    default void applyEffect(SkillContext context) {
        getEffect().apply(context, context.getWorldState());
    }
}
