package com.mauliwinterevent;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Storage {
    private final JavaPlugin plugin;
    private Connection conn;

    public Storage(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        try {
            File db = new File(plugin.getDataFolder(), plugin.getConfig().getString("storage.sqlite_file", "winterevent.db"));
            db.getParentFile().mkdirs();
            String url = "jdbc:sqlite:" + db.getAbsolutePath();
            conn = DriverManager.getConnection(url);
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE IF NOT EXISTS claims(uuid TEXT, day INTEGER, claimed_at TEXT, PRIMARY KEY(uuid, day))");
                st.execute("CREATE TABLE IF NOT EXISTS daily(uuid TEXT, ymd TEXT, PRIMARY KEY(uuid, ymd))");
                st.execute("CREATE TABLE IF NOT EXISTS cosmetics(uuid TEXT, code TEXT, PRIMARY KEY(uuid, code))");
            }
        } catch (SQLException e) {
            throw new RuntimeException("SQLite init failed", e);
        }
    }

    public boolean hasClaimedDay(UUID uuid, int day) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM claims WHERE uuid=? AND day=?")) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, day);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public Set<Integer> getClaimedDays(UUID uuid) {
        Set<Integer> set = new HashSet<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT day FROM claims WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) set.add(rs.getInt(1));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return set;
    }

    public void addClaim(UUID uuid, int day, LocalDate when) {
        try (PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO claims(uuid, day, claimed_at) VALUES(?,?,?)")) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, day);
            ps.setString(3, when.toString());
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public boolean hasDailyClaim(UUID uuid, LocalDate ymd) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM daily WHERE uuid=? AND ymd=?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, ymd.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public void setDailyClaim(UUID uuid, LocalDate ymd) {
        try (PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO daily(uuid, ymd) VALUES(?,?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, ymd.toString());
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }


    public boolean isCosmeticUnlocked(java.util.UUID uuid, Cosmetic c) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM cosmetics WHERE uuid=? AND code=?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, c.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public void unlockCosmetic(java.util.UUID uuid, Cosmetic c) {
        try (PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO cosmetics(uuid, code) VALUES(?,?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, c.name());
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public void close() throws SQLException {
        if (conn != null) conn.close();
    }
}
