package net.trduc.magicabilitiesfork.Boss.spawn;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.logging.Level;

public class FortressBossStore {
    private final JavaPlugin plugin;

    public FortressBossStore(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        try (Connection conn = openConnection()) {
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("CREATE TABLE IF NOT EXISTS fortress_boss (" +
                        "fortress_key TEXT PRIMARY KEY, " +
                        "spawn_count INTEGER NOT NULL DEFAULT 0, " +
                        "last_defeated_ms INTEGER NOT NULL DEFAULT 0)");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[FortressBossStore] Could not initialize fortress_boss table", e);
        }
    }

    private Connection openConnection() throws Exception {
        Class.forName("org.sqlite.JDBC");
        File dbFile = new File(plugin.getDataFolder(), "fortress_boss.db");
        return DriverManager.getConnection("jdbc:sqlite:" + dbFile.getPath());
    }

    public static final class Record {
        public final int spawnCount;
        public final long lastDefeatedMs;

        public Record(int spawnCount, long lastDefeatedMs) {
            this.spawnCount = spawnCount;
            this.lastDefeatedMs = lastDefeatedMs;
        }
    }

    public Record load(String fortressKey) {
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT spawn_count, last_defeated_ms FROM fortress_boss WHERE fortress_key = ?")) {
            ps.setString(1, fortressKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Record(rs.getInt("spawn_count"), rs.getLong("last_defeated_ms"));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[FortressBossStore] Could not load " + fortressKey, e);
        }
        return new Record(0, 0L);
    }

    public void incrementSpawnCount(String fortressKey) {
        try (Connection conn = openConnection()) {
            int updated;
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE fortress_boss SET spawn_count = spawn_count + 1 WHERE fortress_key = ?")) {
                ps.setString(1, fortressKey);
                updated = ps.executeUpdate();
            }
            if (updated == 0) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO fortress_boss (fortress_key, spawn_count, last_defeated_ms) VALUES (?, 1, 0)")) {
                    ps.setString(1, fortressKey);
                    ps.executeUpdate();
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[FortressBossStore] Could not increment spawn count for " + fortressKey, e);
        }
    }

    public void recordDefeated(String fortressKey, long whenMs) {
        try (Connection conn = openConnection()) {
            int updated;
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE fortress_boss SET last_defeated_ms = ? WHERE fortress_key = ?")) {
                ps.setLong(1, whenMs);
                ps.setString(2, fortressKey);
                updated = ps.executeUpdate();
            }
            if (updated == 0) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO fortress_boss (fortress_key, spawn_count, last_defeated_ms) VALUES (?, 0, ?)")) {
                    ps.setString(1, fortressKey);
                    ps.setLong(2, whenMs);
                    ps.executeUpdate();
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[FortressBossStore] Could not record defeat for " + fortressKey, e);
        }
    }
}
