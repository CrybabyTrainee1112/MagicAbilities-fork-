package net.trduc.magicabilitiesfork.Boss.threat;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import java.util.*;

public class ThreatTable {
    private static final long DECAY_INTERVAL_MS = 5000;
    private static final float DECAY_RATE = 0.95f;
    private static final int THREAT_TIMEOUT_MS = 60000;

    private final Map<UUID, ThreatEntry> threats;
    private long lastDecayTime;

    public ThreatTable() {
        this.threats = new HashMap<>();
        this.lastDecayTime = System.currentTimeMillis();
    }

    public void addThreat(LivingEntity attacker, float amount) {
        Objects.requireNonNull(attacker, "Attacker cannot be null");
        if (amount <= 0) return;

        UUID uuid = attacker.getUniqueId();
        ThreatEntry entry = threats.get(uuid);

        if (entry == null) {
            entry = new ThreatEntry(attacker, amount);
            threats.put(uuid, entry);
        } else {
            entry.addThreat(amount);
        }
    }

    public LivingEntity getHighestThreatTarget() {
        cleanup();

        if (threats.isEmpty()) {
            return null;
        }

        ThreatEntry highest = null;
        float maxThreat = 0;

        for (ThreatEntry entry : threats.values()) {
            if (entry.getThreat() > maxThreat) {
                maxThreat = entry.getThreat();
                highest = entry;
            }
        }

        return highest != null ? highest.getEntity() : null;
    }

    public float getThreat(LivingEntity entity) {
        Objects.requireNonNull(entity, "Entity cannot be null");
        ThreatEntry entry = threats.get(entity.getUniqueId());
        return entry != null ? entry.getThreat() : 0f;
    }

    public void removeThreat(LivingEntity entity) {
        Objects.requireNonNull(entity, "Entity cannot be null");
        threats.remove(entity.getUniqueId());
    }

    public void clear() {
        threats.clear();
    }

    public void cleanup() {
        long now = System.currentTimeMillis();

        if (now - lastDecayTime >= DECAY_INTERVAL_MS) {
            for (ThreatEntry entry : threats.values()) {
                entry.decay(DECAY_RATE);
            }
            lastDecayTime = now;
        }

        threats.entrySet().removeIf(entry -> {
            LivingEntity entity = entry.getValue().getEntity();

            if (entity.isDead()) {
                return true;
            }

            if (entity instanceof Player) {
                Player player = (Player) entity;
                if (!player.isOnline()) {
                    return true;
                }
            }

            if (now - entry.getValue().getLastUpdateTime() >= THREAT_TIMEOUT_MS) {
                return true;
            }

            return false;
        });
    }

    public Map<String, Float> getDebugInfo() {
        Map<String, Float> info = new LinkedHashMap<>();
        threats.forEach((uuid, entry) ->
                info.put(entry.getEntity().getName(), entry.getThreat())
        );
        return info;
    }

    private static class ThreatEntry {
        private final LivingEntity entity;
        private float threat;
        private long lastUpdateTime;

        ThreatEntry(LivingEntity entity, float initialThreat) {
            this.entity = entity;
            this.threat = initialThreat;
            this.lastUpdateTime = System.currentTimeMillis();
        }

        void addThreat(float amount) {
            this.threat += amount;
            this.lastUpdateTime = System.currentTimeMillis();
        }

        void decay(float rate) {
            this.threat *= rate;
        }

        LivingEntity getEntity() {
            return entity;
        }

        float getThreat() {
            return threat;
        }

        long getLastUpdateTime() {
            return lastUpdateTime;
        }
    }
}
