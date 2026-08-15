package net.trduc.magicabilitiesfork.Boss.ai.htn;

import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Condition;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.Effect;

public interface PrimitiveTask extends Task {

    Condition getPrecondition();

    Effect getEffect();
}
