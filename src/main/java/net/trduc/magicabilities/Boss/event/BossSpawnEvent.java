package net.trduc.magicabilitiesfork.Boss.event;

import net.trduc.magicabilitiesfork.Boss.core.Boss;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.entity.LivingEntity;

import java.util.Objects;

public class BossSpawnEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Boss boss;

    public BossSpawnEvent(Boss boss) {
        this.boss = Objects.requireNonNull(boss, "Boss cannot be null");
    }

    public Boss getBoss() {
        return boss;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
