package net.trduc.magicabilitiesfork.Boss.spawn;

import net.trduc.magicabilitiesfork.Boss.core.Boss;
import net.trduc.magicabilitiesfork.Boss.core.BossFactory;
import net.trduc.magicabilitiesfork.Boss.core.BossRegistry;
import net.trduc.magicabilitiesfork.Boss.core.BossType;
import net.trduc.magicabilitiesfork.Boss.event.BossDeathEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.generator.structure.Structure;
import org.bukkit.util.StructureSearchResult;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class FortressBossSpawner implements Listener {

    private final JavaPlugin plugin;
    private final BossFactory bossFactory;
    private final FortressBossStore store;

    private final Map<String, UUID> deployedBoss = new HashMap<>();
    private final Map<UUID, String> bossFortress = new HashMap<>();

    private BukkitRunnable scanTask;
    private boolean loggedStructureLookupFailure = false;

    public FortressBossSpawner(JavaPlugin plugin, BossFactory bossFactory) {
        this.plugin = plugin;
        this.bossFactory = bossFactory;
        this.store = new FortressBossStore(plugin);
    }

    public void start() {
        store.init();

        if (!plugin.getConfig().getBoolean("fortress-boss-spawn.enabled", true)) {
            plugin.getLogger().info("[FortressBossSpawner] Disabled via config.");
            return;
        }

        long interval = Math.max(20L, plugin.getConfig().getLong("fortress-boss-spawn.scan-interval-ticks", 100L));
        scanTask = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    scanOnlinePlayers();
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "[FortressBossSpawner] Scan tick failed", e);
                }
            }
        };
        scanTask.runTaskTimer(plugin, interval, interval);
    }

    public void stop() {
        if (scanTask != null) {
            try { scanTask.cancel(); } catch (Exception ignored) {}
            scanTask = null;
        }
    }

    private void scanOnlinePlayers() {
        FileConfiguration cfg = plugin.getConfig();
        String bossTypeId   = cfg.getString("fortress-boss-spawn.boss-type", "demon_lord");
        int searchChunks     = cfg.getInt("fortress-boss-spawn.search-radius-chunks", 8);
        double proximity     = cfg.getInt("fortress-boss-spawn.proximity-radius", 48);
        int maxSpawns         = cfg.getInt("fortress-boss-spawn.max-spawns-per-fortress", 3);
        long cooldownMs      = cfg.getLong("fortress-boss-spawn.respawn-cooldown-minutes", 30) * 60_000L;

        for (Player p : Bukkit.getOnlinePlayers()) {
            World world = p.getWorld();
            if (world.getEnvironment() != World.Environment.NETHER) continue;

            Location structureLoc;
            try {
                StructureSearchResult result = world.locateNearestStructure(p.getLocation(), Structure.FORTRESS, searchChunks, false);
                structureLoc = (result != null) ? result.getLocation() : null;
            } catch (Exception e) {
                if (!loggedStructureLookupFailure) {
                    loggedStructureLookupFailure = true;
                    plugin.getLogger().log(Level.WARNING,
                            "[FortressBossSpawner] locateNearestStructure(Structure.FORTRESS) failed - " +
                            "automatic fortress boss spawning will not trigger until this is resolved. " +
                            "If the message below is 'Cannot convert direct holder to bukkit representation', " +
                            "this is a known Paper-side bug (see PaperMC/Paper#13431), not a bug in this plugin.",
                            e);
                }
                continue;
            }
            if (structureLoc == null) continue;
            if (structureLoc.distance(p.getLocation()) > proximity) continue;

            String fortressKey = fortressKey(structureLoc);
            if (deployedBoss.containsKey(fortressKey)) continue;

            FortressBossStore.Record record = store.load(fortressKey);
            if (record.spawnCount >= maxSpawns) continue;
            if (record.lastDefeatedMs > 0 && System.currentTimeMillis() - record.lastDefeatedMs < cooldownMs) continue;

            spawnAt(bossTypeId, fortressKey, p);
        }
    }

    private void spawnAt(String bossTypeId, String fortressKey, Player nearPlayer) {
        double angle = Math.random() * Math.PI * 2;
        Location spawnLoc = nearPlayer.getLocation().clone().add(Math.cos(angle) * 6, 0, Math.sin(angle) * 6);

        Boss boss;
        try {
            boss = bossFactory.spawn(bossTypeId, spawnLoc);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[FortressBossSpawner] Failed to spawn '" + bossTypeId + "' at fortress " + fortressKey, e);
            return;
        }
        if (boss == null) return;

        deployedBoss.put(fortressKey, boss.getUUID());
        bossFortress.put(boss.getUUID(), fortressKey);
        store.incrementSpawnCount(fortressKey);

        String displayName = BossRegistry.get(bossTypeId)
                .map(BossType::getDisplayName)
                .map(ChatColor::stripColor)
                .orElse("A boss");
        for (Player nearby : spawnLoc.getWorld().getPlayers()) {
            if (nearby.getLocation().distanceSquared(spawnLoc) <= 64.0 * 64.0) {
                nearby.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + displayName + " has awoken within the fortress!");
            }
        }
    }

    @EventHandler
    public void onBossDeath(BossDeathEvent event) {
        UUID bossUuid = event.getBoss().getUUID();
        String fortressKey = bossFortress.remove(bossUuid);
        if (fortressKey == null) return;

        deployedBoss.remove(fortressKey);
        store.recordDefeated(fortressKey, System.currentTimeMillis());
    }

    private static String fortressKey(Location loc) {
        int gx = Math.floorDiv(loc.getBlockX(), 128);
        int gz = Math.floorDiv(loc.getBlockZ(), 128);
        return loc.getWorld().getName() + ":" + gx + ":" + gz;
    }
}
