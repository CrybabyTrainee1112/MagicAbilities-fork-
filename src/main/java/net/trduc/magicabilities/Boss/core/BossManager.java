package net.trduc.magicabilitiesfork.Boss.core;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

import java.util.*;
import java.util.stream.Collectors;

public class BossManager {
    private final Map<UUID, Boss> bosses;

    public BossManager() {
        this.bosses = Collections.synchronizedMap(new HashMap<>());
    }

    public void registerBoss(Boss boss) {
        Objects.requireNonNull(boss, "Boss cannot be null");
        bosses.put(boss.getUUID(), boss);
    }

    public void unregisterBoss(Boss boss) {
        Objects.requireNonNull(boss, "Boss cannot be null");
        bosses.remove(boss.getUUID());
    }

    public Optional<Boss> getBoss(UUID uuid) {
        Objects.requireNonNull(uuid, "UUID cannot be null");
        return Optional.ofNullable(bosses.get(uuid));
    }

    public Optional<Boss> getBoss(Entity entity) {
        Objects.requireNonNull(entity, "Entity cannot be null");
        return getBoss(entity.getUniqueId());
    }

    public Collection<Boss> getAllBosses() {
        return bosses.values();
    }

    public Collection<Boss> getBossesByType(String bossType) {
        Objects.requireNonNull(bossType, "Boss type cannot be null");
        return bosses.values().stream()
                .filter(b -> b.getBossType().equals(bossType))
                .collect(Collectors.toList());
    }

    public void tickAll() {
        List<Boss> bosstoCopy = new ArrayList<>(bosses.values());

        for (Boss boss : bosstoCopy) {
            if (boss.isAlive()) {
                try {
                    boss.tick();
                } catch (Exception e) {
                    System.err.println("Error ticking boss " + boss.getBossType() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                unregisterBoss(boss);
            }
        }
    }

    public int getActiveBossCount() {
        return bosses.size();
    }

    public boolean isManagedBoss(Entity entity) {
        return bosses.containsKey(entity.getUniqueId());
    }

    public void notifyBossDamaged(Entity entity, double amount, LivingEntity damager) {
        Optional<Boss> boss = getBoss(entity);
        if (boss.isPresent()) {
            boss.get().takeDamage(amount, damager);
        }
    }

    public void clearAll() {
        for (Boss boss : bosses.values()) {
            if (boss.getBossBar() != null) {
                boss.getBossBar().removeAll();
            }
        }
        bosses.clear();
    }

    public String getDebugInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Active bosses: ").append(bosses.size()).append("\n");
        for (Boss boss : bosses.values()) {
            sb.append("  - ").append(boss).append("\n");
        }
        return sb.toString();
    }
}
