package net.trduc.magicabilitiesfork.Boss.ai.skill;

import net.trduc.magicabilitiesfork.Boss.ai.worldstate.*;
import java.util.Optional;
import java.util.Objects;

public abstract class AbstractSkill implements Skill {
    protected final String id;
    protected final double cost;
    protected final Condition precondition;
    protected final Effect effect;
    protected final Optional<TargetKey> targetKey;
    protected final int cooldownTicks;

    protected AbstractSkill(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "Skill ID cannot be null");
        this.cost = builder.cost;
        this.precondition = builder.precondition != null ? builder.precondition : ctx -> true;
        this.effect = builder.effect != null ? builder.effect : (ctx, state) -> {};
        this.targetKey = Optional.ofNullable(builder.targetKey);
        this.cooldownTicks = builder.cooldownTicks;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public double getCost() {
        return cost;
    }

    @Override
    public Condition getPrecondition() {
        return precondition;
    }

    @Override
    public Effect getEffect() {
        return effect;
    }

    @Override
    public Optional<TargetKey> getTargetKey() {
        return targetKey;
    }

    @Override
    public int getCooldownTicks() {
        return cooldownTicks;
    }

    @Override
    public abstract void execute(SkillContext context);

    @Override
    public String toString() {
        return "Skill{" +
                "id='" + id + '\'' +
                ", cost=" + cost +
                ", cooldown=" + cooldownTicks + "ticks" +
                '}';
    }

    public static class Builder {
        private String id;
        private double cost = 1.0;
        private Condition precondition;
        private Effect effect;
        private TargetKey targetKey;
        private int cooldownTicks = 0;

        public Builder(String id) {
            this.id = Objects.requireNonNull(id, "Skill ID cannot be null");
        }

        public Builder cost(double cost) {
            this.cost = cost;
            return this;
        }

        public Builder precondition(Condition precondition) {
            this.precondition = precondition;
            return this;
        }

        public Builder effect(Effect effect) {
            this.effect = effect;
            return this;
        }

        public Builder targetKey(TargetKey targetKey) {
            this.targetKey = targetKey;
            return this;
        }

        public Builder cooldownTicks(int ticks) {
            this.cooldownTicks = ticks;
            return this;
        }

        public Builder copy(AbstractSkill skill) {
            this.cost = skill.cost;
            this.precondition = skill.precondition;
            this.effect = skill.effect;
            this.targetKey = skill.targetKey.orElse(null);
            this.cooldownTicks = skill.cooldownTicks;
            return this;
        }
    }
}
