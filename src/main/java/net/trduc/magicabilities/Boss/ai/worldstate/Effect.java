package net.trduc.magicabilitiesfork.Boss.ai.worldstate;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public interface Effect {
    void apply(SkillContext context, WorldState state);

    default String getDescription() {
        return "effect";
    }

    default Collection<WorldKey> getModifiedKeys() {
        return Collections.emptySet();
    }


    static Effect setValue(WorldKey key, double value) {
        return new Effect() {
            @Override
            public void apply(SkillContext context, WorldState state) {
                state.setValue(key, value);
            }

            @Override
            public String getDescription() {
                return "Set " + key + " = " + value;
            }

            @Override
            public Collection<WorldKey> getModifiedKeys() {
                return Collections.singleton(key);
            }
        };
    }

    static Effect modifyValue(WorldKey key, double delta) {
        return new Effect() {
            @Override
            public void apply(SkillContext context, WorldState state) {
                double current = state.getValue(key);
                state.setValue(key, current + delta);
            }

            @Override
            public String getDescription() {
                return "Modify " + key + " by " + delta;
            }

            @Override
            public Collection<WorldKey> getModifiedKeys() {
                return Collections.singleton(key);
            }
        };
    }

    static Effect scaleValue(WorldKey key, double multiplier) {
        return new Effect() {
            @Override
            public void apply(SkillContext context, WorldState state) {
                double current = state.getValue(key);
                state.setValue(key, current * multiplier);
            }

            @Override
            public String getDescription() {
                return "Scale " + key + " by " + multiplier;
            }

            @Override
            public Collection<WorldKey> getModifiedKeys() {
                return Collections.singleton(key);
            }
        };
    }

    static Effect setLocation(TargetKey key, WorldKey locationSource) {
        return new Effect() {
            @Override
            public void apply(SkillContext context, WorldState state) {
            }

            @Override
            public String getDescription() {
                return "Set " + key + " from " + locationSource;
            }
        };
    }

    static Effect composite(Effect... effects) {
        return new Effect() {
            @Override
            public void apply(SkillContext context, WorldState state) {
                for (Effect e : effects) {
                    e.apply(context, state);
                }
            }

            @Override
            public String getDescription() {
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < effects.length; i++) {
                    if (i > 0) sb.append("; ");
                    sb.append(effects[i].getDescription());
                }
                sb.append("]");
                return sb.toString();
            }

            @Override
            public Collection<WorldKey> getModifiedKeys() {
                Set<WorldKey> keys = new LinkedHashSet<>();
                for (Effect e : effects) {
                    keys.addAll(e.getModifiedKeys());
                }
                return keys;
            }
        };
    }
}
