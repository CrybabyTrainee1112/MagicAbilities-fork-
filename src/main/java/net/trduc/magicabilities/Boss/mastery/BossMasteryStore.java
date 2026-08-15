package net.trduc.magicabilitiesfork.Boss.mastery;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Objects;

public class BossMasteryStore {
    private final JavaPlugin plugin;

    public BossMasteryStore(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
    }

    public void init() {
        try (Connection conn = openConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("create table if not exists boss_mastery (" +
                    "boss_type TEXT PRIMARY KEY NOT NULL, " +
                    "tier INTEGER NOT NULL DEFAULT 0, " +
                    "wins INTEGER NOT NULL DEFAULT 0, " +
                    "losses INTEGER NOT NULL DEFAULT 0);");
        } catch (Exception e) {
            plugin.getLogger().severe("Could not initialize boss_mastery table: " + e.getMessage());
        }
    }

    public BossMastery load(String bossTypeId) {
        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement("select * from boss_mastery where boss_type=?;")) {
            stmt.setString(1, bossTypeId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new BossMastery(bossTypeId, rs.getInt("tier"), rs.getInt("wins"), rs.getInt("losses"));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Could not load boss_mastery for " + bossTypeId + ": " + e.getMessage());
        }
        return BossMastery.initial(bossTypeId);
    }

    public void save(BossMastery mastery) {
        String sql = "insert into boss_mastery (boss_type, tier, wins, losses) values (?, ?, ?, ?) " +
                "on conflict(boss_type) do update set tier=excluded.tier, wins=excluded.wins, losses=excluded.losses;";
        try (Connection conn = openConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, mastery.getBossTypeId());
            stmt.setInt(2, mastery.getTier());
            stmt.setInt(3, mastery.getWins());
            stmt.setInt(4, mastery.getLosses());
            stmt.execute();
        } catch (Exception e) {
            plugin.getLogger().severe("Could not save boss_mastery for " + mastery.getBossTypeId() + ": " + e.getMessage());
        }
    }

    private Connection openConnection() throws Exception {
        Class.forName("org.sqlite.JDBC");
        File dbFile = new File(plugin.getDataFolder(), "boss.db");
        return DriverManager.getConnection("jdbc:sqlite:" + dbFile.getPath());
    }
}
