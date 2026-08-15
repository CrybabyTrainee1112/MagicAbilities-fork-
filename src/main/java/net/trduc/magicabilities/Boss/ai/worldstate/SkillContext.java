package net.trduc.magicabilitiesfork.Boss.ai.worldstate;

import org.bukkit.entity.Mob;
import org.bukkit.entity.LivingEntity;
import java.util.Objects;

public class SkillContext {
    private final Mob boss;
    private final LivingEntity target;
    private final int currentPhase;
    private final WorldState worldState;

    public SkillContext(Mob boss, LivingEntity target, int currentPhase, WorldState worldState) {
        this.boss = Objects.requireNonNull(boss, "Boss mob cannot be null");
        this.target = target;
        this.currentPhase = currentPhase;
        this.worldState = Objects.requireNonNull(worldState, "WorldState cannot be null");
    }

    public Mob getBoss() {
        return boss;
    }

    public LivingEntity getTarget() {
        return target;
    }

    public boolean hasTarget() {
        return target != null && !target.isDead();
    }

    public int getCurrentPhase() {
        return currentPhase;
    }

    public WorldState getWorldState() {
        return worldState;
    }

    @Override
    public String toString() {
        return "SkillContext{" +
                "boss=" + boss.getName() +
                ", target=" + (target != null ? target.getName() : "null") +
                ", currentPhase=" + currentPhase +
                '}';
    }
}
