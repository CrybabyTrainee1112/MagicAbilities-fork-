package net.trduc.magicabilitiesfork.intrinsics.player;

import net.trduc.magicabilitiesfork.intrinsics.IntrinsicId;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class PlayerIntrinsicStorage {
    private final JavaPlugin plugin;

    public PlayerIntrinsicStorage(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
    }

    public void init() {
        try (Connection conn = openConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("create table if not exists learned_intrinsics (" +
                    "player_uuid TEXT NOT NULL, " +
                    "intrinsic_id TEXT NOT NULL, " +
                    "active INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY(player_uuid, intrinsic_id));");
        } catch (Exception e) {
            plugin.getLogger().severe("Could not initialize learned_intrinsics table: " + e.getMessage());
        }
    }

    public Set<IntrinsicId> getLearned(UUID playerId) {
        Set<IntrinsicId> out = EnumSet.noneOf(IntrinsicId.class);
        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement("select intrinsic_id from learned_intrinsics where player_uuid=?;")) {
            stmt.setString(1, playerId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    parseId(rs.getString("intrinsic_id")).ifPresent(out::add);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Could not load learned intrinsics for " + playerId + ": " + e.getMessage());
        }
        return out;
    }

    public Set<IntrinsicId> getActive(UUID playerId) {
        Set<IntrinsicId> out = EnumSet.noneOf(IntrinsicId.class);
        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement("select intrinsic_id from learned_intrinsics where player_uuid=? and active=1;")) {
            stmt.setString(1, playerId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    parseId(rs.getString("intrinsic_id")).ifPresent(out::add);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Could not load active intrinsics for " + playerId + ": " + e.getMessage());
        }
        return out;
    }

    public boolean hasLearned(UUID playerId, IntrinsicId id) {
        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement("select 1 from learned_intrinsics where player_uuid=? and intrinsic_id=? limit 1;")) {
            stmt.setString(1, playerId.toString());
            stmt.setString(2, id.name());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Could not check learned intrinsic for " + playerId + ": " + e.getMessage());
            return false;
        }
    }

    public void learn(UUID playerId, IntrinsicId id) {
        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "insert into learned_intrinsics (player_uuid, intrinsic_id, active) values (?, ?, 0) " +
                             "on conflict(player_uuid, intrinsic_id) do nothing;")) {
            stmt.setString(1, playerId.toString());
            stmt.setString(2, id.name());
            stmt.execute();
        } catch (Exception e) {
            plugin.getLogger().severe("Could not record learned intrinsic for " + playerId + ": " + e.getMessage());
        }
    }

    public void setActive(UUID playerId, IntrinsicId id, boolean active) {
        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "update learned_intrinsics set active=? where player_uuid=? and intrinsic_id=?;")) {
            stmt.setInt(1, active ? 1 : 0);
            stmt.setString(2, playerId.toString());
            stmt.setString(3, id.name());
            stmt.execute();
        } catch (Exception e) {
            plugin.getLogger().severe("Could not update active flag for " + playerId + ": " + e.getMessage());
        }
    }

    private java.util.Optional<IntrinsicId> parseId(String raw) {
        try {
            return java.util.Optional.of(IntrinsicId.valueOf(raw));
        } catch (IllegalArgumentException e) {
            return java.util.Optional.empty();
        }
    }

    private Connection openConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found on classpath", e);
        }
        File dbFile = new File(plugin.getDataFolder(), "intrinsics.db");
        return DriverManager.getConnection("jdbc:sqlite:" + dbFile.getPath());
    }
}
