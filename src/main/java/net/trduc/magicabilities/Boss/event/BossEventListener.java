package net.trduc.magicabilitiesfork.Boss.event;

import net.trduc.magicabilitiesfork.Boss.core.Boss;
import net.trduc.magicabilitiesfork.Boss.core.BossManager;
import net.trduc.magicabilitiesfork.Boss.mastery.BossMastery;
import net.trduc.magicabilitiesfork.Boss.mastery.BossMasteryStore;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Objects;
import java.util.Optional;

public class BossEventListener implements Listener {

    private final BossManager bossManager;
    private final BossMasteryStore masteryStore;
    private final FileConfiguration config;

    public BossEventListener(BossManager bossManager, BossMasteryStore masteryStore, FileConfiguration config) {
        this.bossManager = Objects.requireNonNull(bossManager, "BossManager cannot be null");
        this.masteryStore = Objects.requireNonNull(masteryStore, "BossMasteryStore cannot be null");
        this.config = Objects.requireNonNull(config, "Config cannot be null");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBossDamaged(EntityDamageByEntityEvent event) {
        if (!bossManager.isManagedBoss(event.getEntity())) {
            return;
        }

        LivingEntity attacker = resolveAttacker(event);
        if (attacker == null) {
            return;
        }

        bossManager.notifyBossDamaged(event.getEntity(), event.getFinalDamage(), attacker);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBossDeath(EntityDeathEvent event) {
        Optional<Boss> boss = bossManager.getBoss(event.getEntity());
        if (!boss.isPresent()) {
            return;
        }

        Boss b = boss.get();
        b.die(event.getEntity().getKiller());
        bossManager.unregisterBoss(b);
        updateMastery(b);
    }

    private void updateMastery(Boss boss) {
        long fightSeconds = (System.currentTimeMillis() - boss.getSpawnTimeMs()) / 1000L;
        int minTier = config.getInt("boss-mastery.min-tier", 0);
        int maxTier = config.getInt("boss-mastery.max-tier", 5);
        int quickKillSeconds = config.getInt("boss-mastery.quick-kill-seconds", 60);
        int slowKillSeconds = config.getInt("boss-mastery.slow-kill-seconds", 240);

        BossMastery mastery = masteryStore.load(boss.getBossType());
        mastery.incrementWins();

        if (fightSeconds <= quickKillSeconds) {
            mastery.setTier(Math.min(maxTier, mastery.getTier() + 1));
        } else if (fightSeconds >= slowKillSeconds) {
            mastery.setTier(Math.max(minTier, mastery.getTier() - 1));
        }

        masteryStore.save(mastery);
    }

    private LivingEntity resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof LivingEntity) {
            return (LivingEntity) event.getDamager();
        }
        if (event.getDamager() instanceof Projectile) {
            Projectile proj = (Projectile) event.getDamager();
            if (proj.getShooter() instanceof LivingEntity) {
                return (LivingEntity) proj.getShooter();
            }
        }
        return null;
    }
}

