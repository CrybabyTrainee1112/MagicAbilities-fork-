package net.trduc.magicabilitiesfork.Boss.ai.worldstate;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public interface Condition {
    boolean isSatisfied(WorldState state);

    default String getDescription() {
        return "condition";
    }

    default Collection<WorldKey> getRelevantKeys() {
        return Collections.emptySet();
    }


    static Condition equals(WorldKey key, double value) {
        return new Condition() {
            @Override
            public boolean isSatisfied(WorldState state) {
                return state.getValue(key) == value;
            }

            @Override
            public String getDescription() {
                return key + " == " + value;
            }

            @Override
            public Collection<WorldKey> getRelevantKeys() {
                return Collections.singleton(key);
            }
        };
    }

    static Condition greaterThan(WorldKey key, double threshold) {
        return new Condition() {
            @Override
            public boolean isSatisfied(WorldState state) {
                return state.getValue(key) > threshold;
            }

            @Override
            public String getDescription() {
                return key + " > " + threshold;
            }

            @Override
            public Collection<WorldKey> getRelevantKeys() {
                return Collections.singleton(key);
            }
        };
    }

    static Condition greaterOrEqual(WorldKey key, double threshold) {
        return new Condition() {
            @Override
            public boolean isSatisfied(WorldState state) {
                return state.getValue(key) >= threshold;
            }

            @Override
            public String getDescription() {
                return key + " >= " + threshold;
            }

            @Override
            public Collection<WorldKey> getRelevantKeys() {
                return Collections.singleton(key);
            }
        };
    }

    static Condition lessThan(WorldKey key, double threshold) {
        return new Condition() {
            @Override
            public boolean isSatisfied(WorldState state) {
                return state.getValue(key) < threshold;
            }

            @Override
            public String getDescription() {
                return key + " < " + threshold;
            }

            @Override
            public Collection<WorldKey> getRelevantKeys() {
                return Collections.singleton(key);
            }
        };
    }

    static Condition lessOrEqual(WorldKey key, double threshold) {
        return new Condition() {
            @Override
            public boolean isSatisfied(WorldState state) {
                return state.getValue(key) <= threshold;
            }

            @Override
            public String getDescription() {
                return key + " <= " + threshold;
            }

            @Override
            public Collection<WorldKey> getRelevantKeys() {
                return Collections.singleton(key);
            }
        };
    }

    static Condition and(Condition... conditions) {
        return new Condition() {
            @Override
            public boolean isSatisfied(WorldState state) {
                for (Condition c : conditions) {
                    if (!c.isSatisfied(state)) return false;
                }
                return true;
            }

            @Override
            public String getDescription() {
                StringBuilder sb = new StringBuilder("(");
                for (int i = 0; i < conditions.length; i++) {
                    if (i > 0) sb.append(" AND ");
                    sb.append(conditions[i].getDescription());
                }
                sb.append(")");
                return sb.toString();
            }

            @Override
            public Collection<WorldKey> getRelevantKeys() {
                Set<WorldKey> keys = new LinkedHashSet<>();
                for (Condition c : conditions) {
                    keys.addAll(c.getRelevantKeys());
                }
                return keys;
            }
        };
    }

    static Condition or(Condition... conditions) {
        return new Condition() {
            @Override
            public boolean isSatisfied(WorldState state) {
                for (Condition c : conditions) {
                    if (c.isSatisfied(state)) return true;
                }
                return false;
            }

            @Override
            public String getDescription() {
                StringBuilder sb = new StringBuilder("(");
                for (int i = 0; i < conditions.length; i++) {
                    if (i > 0) sb.append(" OR ");
                    sb.append(conditions[i].getDescription());
                }
                sb.append(")");
                return sb.toString();
            }

            @Override
            public Collection<WorldKey> getRelevantKeys() {
                Set<WorldKey> keys = new LinkedHashSet<>();
                for (Condition c : conditions) {
                    keys.addAll(c.getRelevantKeys());
                }
                return keys;
            }
        };
    }

    static Condition always() {
        return new Condition() {
            @Override
            public boolean isSatisfied(WorldState state) {
                return true;
            }

            @Override
            public String getDescription() {
                return "always";
            }
        };
    }
}
