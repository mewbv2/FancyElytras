package io.mewb.fancyElytras.data;

import io.mewb.fancyElytras.FancyElytras;

import java.io.File;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class DatabaseManager {

    private final FancyElytras plugin;
    private Connection connection;
    private final String databasePath;

    public DatabaseManager(FancyElytras plugin) {
        this.plugin = plugin;
        this.databasePath = plugin.getDataFolder() + File.separator + "database.db";
    }

    public boolean initialize() {
        try {
            // Create data folder if it doesn't exist
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }

            // Load SQLite driver
            Class.forName("org.sqlite.JDBC");

            // Connect to database
            connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);

            // Create tables
            createTables();

            plugin.getLogger().info("Database connection established successfully!");
            return true;

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database", e);
            return false;
        }
    }

    private void createTables() throws SQLException {
        // Player data table
        String createPlayerTable = """
            CREATE TABLE IF NOT EXISTS player_data (
                uuid TEXT PRIMARY KEY,
                default_particle TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                last_login INTEGER NOT NULL
            )
        """;

        // Player statistics table
        String createStatsTable = """
            CREATE TABLE IF NOT EXISTS player_statistics (
                uuid TEXT NOT NULL,
                stat_type TEXT NOT NULL,
                value INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY (uuid, stat_type),
                FOREIGN KEY (uuid) REFERENCES player_data(uuid)
            )
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createPlayerTable);
            stmt.execute(createStatsTable);

            // Create indexes for better performance
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_player_last_login ON player_data(last_login)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_stats_type ON player_statistics(stat_type)");

            plugin.getLogger().info("Database tables created/verified successfully!");
        }
    }

    public CompletableFuture<PlayerData> loadPlayerData(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM player_data WHERE uuid = ?";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return new PlayerData(
                                playerUuid,
                                rs.getString("default_particle"),
                                rs.getLong("created_at"),
                                rs.getLong("last_login")
                        );
                    } else {
                        // Create new player data
                        PlayerData newData = new PlayerData(
                                playerUuid,
                                plugin.getParticleManager().getDefaultParticle(),
                                System.currentTimeMillis(),
                                System.currentTimeMillis()
                        );

                        // Insert into database
                        insertPlayerData(newData);
                        return newData;
                    }
                }

            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load player data for " + playerUuid, e);
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Boolean> savePlayerData(PlayerData playerData) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                INSERT OR REPLACE INTO player_data (uuid, default_particle, created_at, last_login)
                VALUES (?, ?, ?, ?)
            """;

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerData.getUuid().toString());
                stmt.setString(2, playerData.getDefaultParticle());
                stmt.setLong(3, playerData.getCreatedAt());
                stmt.setLong(4, playerData.getLastLogin());

                int rowsAffected = stmt.executeUpdate();
                return rowsAffected > 0;

            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to save player data for " + playerData.getUuid(), e);
                return false;
            }
        });
    }

    private void insertPlayerData(PlayerData playerData) throws SQLException {
        String sql = """
            INSERT INTO player_data (uuid, default_particle, created_at, last_login)
            VALUES (?, ?, ?, ?)
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerData.getUuid().toString());
            stmt.setString(2, playerData.getDefaultParticle());
            stmt.setLong(3, playerData.getCreatedAt());
            stmt.setLong(4, playerData.getLastLogin());

            stmt.executeUpdate();
        }
    }

    public CompletableFuture<Boolean> updatePlayerStatistic(UUID playerUuid, String statType, int value) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                INSERT OR REPLACE INTO player_statistics (uuid, stat_type, value)
                VALUES (?, ?, ?)
            """;

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());
                stmt.setString(2, statType);
                stmt.setInt(3, value);

                int rowsAffected = stmt.executeUpdate();
                return rowsAffected > 0;

            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to update statistic for " + playerUuid, e);
                return false;
            }
        });
    }

    public CompletableFuture<Integer> getPlayerStatistic(UUID playerUuid, String statType) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT value FROM player_statistics WHERE uuid = ? AND stat_type = ?";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());
                stmt.setString(2, statType);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("value");
                    } else {
                        return 0;
                    }
                }

            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to get statistic for " + playerUuid, e);
                return 0;
            }
        });
    }

    public CompletableFuture<Boolean> incrementPlayerStatistic(UUID playerUuid, String statType, int increment) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                INSERT INTO player_statistics (uuid, stat_type, value)
                VALUES (?, ?, ?)
                ON CONFLICT(uuid, stat_type) DO UPDATE SET
                value = value + ?
            """;

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());
                stmt.setString(2, statType);
                stmt.setInt(3, increment);
                stmt.setInt(4, increment);

                int rowsAffected = stmt.executeUpdate();
                return rowsAffected > 0;

            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to increment statistic for " + playerUuid, e);
                return false;
            }
        });
    }

    public CompletableFuture<Integer> getTotalPlayers() {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) FROM player_data";

            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;

            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to get total players", e);
                return 0;
            }
        });
    }

    public CompletableFuture<Void> cleanupOldData(long cutoffTime) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM player_data WHERE last_login < ?";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, cutoffTime);

                int rowsDeleted = stmt.executeUpdate();
                if (rowsDeleted > 0) {
                    plugin.getLogger().info("Cleaned up " + rowsDeleted + " old player records");
                }

            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to cleanup old data", e);
            }
        });
    }

    public void optimizeDatabase() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("VACUUM");
            stmt.execute("ANALYZE");
            plugin.getLogger().info("Database optimization completed");

        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to optimize database", e);
        }
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Database connection closed");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error closing database connection", e);
        }
    }

    // Test database connection
    public boolean testConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                return false;
            }

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT 1")) {
                return rs.next();
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Database connection test failed", e);
            return false;
        }
    }

    // Get database file size
    public long getDatabaseSize() {
        File dbFile = new File(databasePath);
        return dbFile.exists() ? dbFile.length() : 0;
    }

    // Backup database
    public CompletableFuture<Boolean> backupDatabase() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                File sourceFile = new File(databasePath);
                File backupFile = new File(plugin.getDataFolder(), "database_backup_" + System.currentTimeMillis() + ".db");

                java.nio.file.Files.copy(sourceFile.toPath(), backupFile.toPath());

                plugin.getLogger().info("Database backup created: " + backupFile.getName());
                return true;

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to backup database", e);
                return false;
            }
        });
    }
}