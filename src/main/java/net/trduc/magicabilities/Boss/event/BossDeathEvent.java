package net.trduc.magicabilitiesfork.Boss.event;

import net.trduc.magicabilitiesfork.Boss.core.Boss;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.entity.LivingEntity;

import java.util.Objects;

public class BossDeathEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Boss boss;
    private final LivingEntity killer;

    public BossDeathEvent(Boss boss, LivingEntity killer) {
        this.boss = Objects.requireNonNull(boss, "Boss cannot be null");
        this.killer = killer;
    }

    public Boss getBoss() {
        return boss;
    }

    public LivingEntity getKiller() {
        return killer;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
