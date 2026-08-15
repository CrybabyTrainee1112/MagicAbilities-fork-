package net.trduc.magicabilitiesfork.Boss.core;

import net.trduc.magicabilitiesfork.Boss.ai.executor.Brain;
import net.trduc.magicabilitiesfork.Boss.ai.skill.SkillExecutor;
import net.trduc.magicabilitiesfork.Boss.damage.DamageAPI;
import net.trduc.magicabilitiesfork.Boss.event.BossDeathEvent;
import net.trduc.magicabilitiesfork.Boss.event.BossPhaseChangeEvent;
import net.trduc.magicabilitiesfork.Boss.phase.PhaseDefinition;
import net.trduc.magicabilitiesfork.Boss.threat.ThreatTable;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Boss {
    private static final double PHASE_ANNOUNCE_RADIUS = 40.0;
    private static final double BOSSBAR_VISIBILITY_RADIUS = 64.0;

    private final UUID uuid;
    private final Mob entity;
    private final String bossType;
    private final Brain brain;
    private final SkillExecutor skillExecutor;
    private final ThreatTable threatTable;
    private final List<PhaseDefinition> phasesAscending;

    private int currentPhase;
    private boolean alive;
    private final long spawnTimeMs;
    private double damageMultiplier = 1.0;
    private BossBar bossBar;
    private UUID lastLoggedTargetUuid;

    private static boolean debugEnabled = false;

    public static void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
    }

    public Boss(Mob entity, String bossType, Brain brain, SkillExecutor skillExecutor,
                ThreatTable threatTable, List<PhaseDefinition> phases) {
        this.uuid = entity.getUniqueId();
        this.entity = Objects.requireNonNull(entity, "Entity cannot be null");
        this.bossType = Objects.requireNonNull(bossType, "Boss type cannot be null");
        this.brain = Objects.requireNonNull(brain, "Brain cannot be null");
        this.skillExecutor = Objects.requireNonNull(skillExecutor, "SkillExecutor cannot be null");
        this.threatTable = Objects.requireNonNull(threatTable, "ThreatTable cannot be null");

        List<PhaseDefinition> sorted = new ArrayList<>(Objects.requireNonNull(phases, "Phases cannot be null"));
        sorted.sort(Comparator.comparingDouble(PhaseDefinition::getHealthThresholdPercent));
        this.phasesAscending = Collections.unmodifiableList(sorted);

        this.alive = true;
        this.spawnTimeMs = System.currentTimeMillis();

        if (phasesAscending.isEmpty()) {
            this.currentPhase = 1;
        } else {
            PhaseDefinition initialPhase = resolvePhase(1.0);
            this.currentPhase = initialPhase.getPhaseNumber();
            writePhaseDamageMultiplier(initialPhase);
        }
    }

    public void tick() {
        if (!isAlive()) {
            return;
        }

        LivingEntity currentTarget = threatTable.getHighestThreatTarget();

        if (debugEnabled) {
            UUID targetUuid = currentTarget != null ? currentTarget.getUniqueId() : null;
            if (!Objects.equals(targetUuid, lastLoggedTargetUuid)) {
                lastLoggedTargetUuid = targetUuid;
                Bukkit.getLogger().info("[Boss] " + entity.getName() + " target changed -> "
                        + (currentTarget != null ? currentTarget.getName() : "none (no threat)"));
            }
        }

        brain.tick(currentTarget, currentPhase);

        checkPhaseTransition();

        updateBossBar();
    }

    private void updateBossBar() {
        if (bossBar == null) {
            return;
        }

        AttributeInstance maxHealthAttr = entity.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = maxHealthAttr != null ? maxHealthAttr.getValue() : 1.0;
        double percent = maxHealth > 0 ? entity.getHealth() / maxHealth : 0.0;
        bossBar.setProgress(Math.max(0.0, Math.min(1.0, percent)));

        List<Player> nearby = entity.getWorld()
                .getNearbyEntities(entity.getLocation(), BOSSBAR_VISIBILITY_RADIUS, BOSSBAR_VISIBILITY_RADIUS, BOSSBAR_VISIBILITY_RADIUS)
                .stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .collect(java.util.stream.Collectors.toList());

        for (Player viewer : new ArrayList<>(bossBar.getPlayers())) {
            if (!nearby.contains(viewer)) {
                bossBar.removePlayer(viewer);
            }
        }
        for (Player player : nearby) {
            bossBar.addPlayer(player);
        }
    }

    public void takeDamage(double amount, LivingEntity damager) {
        if (damager != null) {
            threatTable.addThreat(damager, (float) amount);
        }
    }

    private PhaseDefinition resolvePhase(double healthPercent) {
        PhaseDefinition best = phasesAscending.get(0);
        for (PhaseDefinition p : phasesAscending) {
            if (p.getHealthThresholdPercent() <= healthPercent) {
                best = p;
            } else {
                break;
            }
        }
        return best;
    }

    private void checkPhaseTransition() {
        if (phasesAscending.isEmpty()) {
            return;
        }

        AttributeInstance maxHealthAttr = entity.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = maxHealthAttr != null ? maxHealthAttr.getValue() : 0.0;
        if (maxHealth <= 0.0) {
            return;
        }

        double healthPercent = entity.getHealth() / maxHealth;
        PhaseDefinition resolved = resolvePhase(healthPercent);
        if (resolved.getPhaseNumber() != currentPhase) {
            transitionTo(resolved);
        }
    }

    public void updatePhase(int newPhaseNumber) {
        if (newPhaseNumber == currentPhase) {
            return;
        }
        for (PhaseDefinition def : phasesAscending) {
            if (def.getPhaseNumber() == newPhaseNumber) {
                transitionTo(def);
                return;
            }
        }
    }

    private void transitionTo(PhaseDefinition newPhase) {
        int oldPhase = currentPhase;
        currentPhase = newPhase.getPhaseNumber();
        writePhaseDamageMultiplier(newPhase);

        brain.cancelPlan();

        Bukkit.getPluginManager().callEvent(new BossPhaseChangeEvent(this, oldPhase, currentPhase));
        announcePhaseTransition(newPhase);
    }

    private void writePhaseDamageMultiplier(PhaseDefinition phase) {
        entity.getPersistentDataContainer().set(DamageAPI.PHASE_DAMAGE_KEY, PersistentDataType.DOUBLE,
                phase.getDamageMultiplier());
    }

    private void announcePhaseTransition(PhaseDefinition phase) {
        String message = phase.getPhaseTransitionMessage();
        if (message == null || message.isEmpty()) {
            return;
        }
        double radiusSquared = PHASE_ANNOUNCE_RADIUS * PHASE_ANNOUNCE_RADIUS;
        for (Player player : entity.getWorld().getPlayers()) {
            if (player.getWorld().equals(entity.getWorld())
                    && player.getLocation().distanceSquared(entity.getLocation()) <= radiusSquared) {
                player.sendMessage(message);
            }
        }
    }

    public void die(LivingEntity killer) {
        if (!alive) {
            return;
        }
        alive = false;
        if (bossBar != null) {
            bossBar.removeAll();
        }
        Bukkit.getPluginManager().callEvent(new BossDeathEvent(this, killer));
    }

    public void die() {
        die(null);
    }


    public UUID getUUID() {
        return uuid;
    }

    public Mob getEntity() {
        return entity;
    }

    public String getBossType() {
        return bossType;
    }

    public Brain getBrain() {
        return brain;
    }

    public SkillExecutor getSkillExecutor() {
        return skillExecutor;
    }

    public ThreatTable getThreatTable() {
        return threatTable;
    }

    public int getCurrentPhase() {
        return currentPhase;
    }

    public long getSpawnTimeMs() {
        return spawnTimeMs;
    }

    public double getDamageMultiplier() {
        return damageMultiplier;
    }

    public void setDamageMultiplier(double damageMultiplier) {
        this.damageMultiplier = damageMultiplier;
    }

    public BossBar getBossBar() {
        return bossBar;
    }

    public void setBossBar(BossBar bossBar) {
        this.bossBar = bossBar;
    }

    public boolean isAlive() {
        return alive && !entity.isDead();
    }

    @Override
    public String toString() {
        return "Boss{" +
                "type='" + bossType + '\'' +
                ", phase=" + currentPhase +
                ", health=" + (int) entity.getHealth() + "/" + (int) entity.getMaxHealth() +
                ", dmgMult=" + damageMultiplier +
                '}';
    }
}
