package com.github.joaoalberis.treasurehunt.database;

import com.github.joaoalberis.treasurehunt.models.TreasureModel;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.Plugin;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public class DatabaseManager {

    private final Plugin plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void connect() throws SQLException {
        String host = plugin.getConfig().getString("database.host", "localhost");
        int port = plugin.getConfig().getInt("database.port", 3306);
        String database = plugin.getConfig().getString("database.database", "treasurehunt");
        String user = plugin.getConfig().getString("database.user", "root");
        String pass = plugin.getConfig().getString("database.password", "");
        int poolSize = plugin.getConfig().getInt("database.pool-size", 10);
        boolean useSsl = plugin.getConfig().getBoolean("database.useSsl", false);

        String jdbcUrl = String.format(
                "jdbc:mysql://%s:%d/%s?useSSL=%b&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                host, port, database, useSsl);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(user);
        config.setPassword(pass);
        config.setMaximumPoolSize(poolSize);
        config.setMinimumIdle(Math.max(1, poolSize / 2));
        config.setPoolName("TreasureHuntPool");

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(config);

        try (Connection conn = getConnection()) {
            plugin.getLogger().info("Connected to MySQL successfully.");
            createTables(conn);
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) throw new SQLException("DataSource is not initialized.");
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("HikariCP pool closed.");
        }
    }

    private void createTables(Connection conn) throws SQLException {
        String createTreasures = """
            CREATE TABLE IF NOT EXISTS treasures (
                id VARCHAR(128) NOT NULL PRIMARY KEY,
                world VARCHAR(128) NOT NULL,
                x INT NOT NULL,
                y INT NOT NULL,
                z INT NOT NULL,
                command TEXT NOT NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """;

        String createClaims = """
            CREATE TABLE IF NOT EXISTS treasure_claims (
                player_uuid CHAR(36) NOT NULL,
                treasure_id VARCHAR(128) NOT NULL,
                claimed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (player_uuid, treasure_id),
                INDEX idx_treasure_id (treasure_id),
                CONSTRAINT fk_treasure_claims_treasure FOREIGN KEY (treasure_id) REFERENCES treasures(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """;

        try (Statement st = conn.createStatement()) {
            st.executeUpdate(createTreasures);
            st.executeUpdate(createClaims);
        }

        plugin.getLogger().info("Database tables ensured.");
    }


    public void upsertTreasure(String id, String world, int x, int y, int z, String command) throws SQLException {
        String sql = """
            INSERT INTO treasures (id, world, x, y, z, command)
            VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE world = VALUES(world), x = VALUES(x), y = VALUES(y), z = VALUES(z), command = VALUES(command)
            """;

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, world);
            ps.setInt(3, x);
            ps.setInt(4, y);
            ps.setInt(5, z);
            ps.setString(6, command);
            ps.executeUpdate();
        }
    }

    public boolean deleteById(String id) throws SQLException{
        String sql = """
                DELETE FROM treasures
                WHERE id = (?);
                """;
        try (
            Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
                ps.setString(1, id);
                int affected = ps.executeUpdate();
                return affected > 0;
        }

    }

    public boolean insertClaimIfNotExists(UUID playerUuid, String treasureId) throws SQLException {
        String sql = """
            INSERT INTO treasure_claims (player_uuid, treasure_id) VALUES (?, ?)
            """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, treasureId);
            int changed = ps.executeUpdate();
            return changed > 0;
        } catch (SQLException ex) {
            String sqlState = ex.getSQLState();
            plugin.getLogger().log(Level.FINE, "Insert claim failed: {0}", ex.getMessage());
            return false;
        }
    }

    public boolean hasClaimed(UUID playerUuid, String treasureId) throws SQLException {
        String sql = "SELECT 1 FROM treasure_claims WHERE player_uuid = ? AND treasure_id = ? LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, treasureId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }


    public Map<String, TreasureModel> loadAllTreasures() throws SQLException {
        String sql = "SELECT id, world, x, y, z, command FROM treasures";
        Map<String, TreasureModel> map = new HashMap<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String id = rs.getString("id");
                String world = rs.getString("world");
                int x = rs.getInt("x");
                int y = rs.getInt("y");
                int z = rs.getInt("z");
                String command = rs.getString("command");
                String key = posKey(world, x, y, z);
                map.put(key, new TreasureModel(id, command, world, x, y, z));
            }
        }
        return map;
    }

    public Map<UUID, Set<String>> loadAllClaims() throws SQLException {
        String sql = "SELECT player_uuid, treasure_id FROM treasure_claims";
        Map<UUID, Set<String>> map = new HashMap<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("player_uuid"));
                String treasureId = rs.getString("treasure_id");
                map.computeIfAbsent(uuid, k -> new HashSet<>()).add(treasureId);
            }
        }
        return map;
    }

    public Set<UUID> loadClaimsForTreasure(String treasureId) throws SQLException {
        String sql = "SELECT player_uuid FROM treasure_claims WHERE treasure_id = ?";
        Set<UUID> players = new LinkedHashSet<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, treasureId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String s = rs.getString("player_uuid");
                    players.add(UUID.fromString(s));
                }
            }
        }
        return players;
    }

    public Map<UUID, Timestamp> loadClaimsWithDateForTreasure(String treasureId) throws SQLException {
        String sql = "SELECT player_uuid, claimed_at FROM treasure_claims WHERE treasure_id = ? ORDER BY claimed_at ASC";
        Map<UUID, Timestamp> claims = new LinkedHashMap<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, treasureId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("player_uuid"));
                    Timestamp date = rs.getTimestamp("claimed_at");
                    claims.put(uuid, date);
                }
            }
        }
        return claims;
    }


    public static String posKey(String world, int x, int y, int z) {
        return world + ":" + x + ":" + y + ":" + z;
    }
}
