package net.trduc.magicabilitiesfork.Boss.ai.sensor;

import net.trduc.magicabilitiesfork.Boss.ai.worldstate.SkillContext;
import net.trduc.magicabilitiesfork.Boss.ai.worldstate.WorldState;

public interface Sensor {
    String getId();

    SensorScope getScope();

    int getThrottleTicks();

    void sense(SkillContext context, WorldState worldState);

    default boolean isActive(SkillContext context) {
        return true;
    }
}
