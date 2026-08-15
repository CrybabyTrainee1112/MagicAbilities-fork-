package net.trduc.magicabilitiesfork.Boss.event;

import net.trduc.magicabilitiesfork.Boss.core.Boss;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Objects;

public class BossPhaseChangeEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Boss boss;
    private final int oldPhase;
    private final int newPhase;

    public BossPhaseChangeEvent(Boss boss, int oldPhase, int newPhase) {
        this.boss = Objects.requireNonNull(boss, "Boss cannot be null");
        this.oldPhase = oldPhase;
        this.newPhase = newPhase;
    }

    public Boss getBoss() {
        return boss;
    }

    public int getOldPhase() {
        return oldPhase;
    }

    public int getNewPhase() {
        return newPhase;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
