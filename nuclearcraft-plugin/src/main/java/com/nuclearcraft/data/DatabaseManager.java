package com.nuclearcraft.data;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.utils.NCLogger;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.*;
import java.util.*;

/**
 * Manages database connectivity and schema (Phase 1 + Phase 2 extended schema).
 * Supports SQLite (default) and MySQL via HikariCP.
 * Migration-safe: uses ALTER TABLE IF NOT COLUMN EXISTS approach for new columns.
 */
public class DatabaseManager {

    private final NuclearCraftPlugin plugin;
    private final ConfigManager configManager;
    private HikariDataSource dataSource;

    public DatabaseManager(NuclearCraftPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void initialize() throws Exception {
        String dbType = configManager.getDatabaseType();
        NCLogger.info("Initializing database (" + dbType + ")...");
        HikariConfig hikariConfig = new HikariConfig();
        if ("MYSQL".equals(dbType)) {
            configureMySql(hikariConfig);
        } else {
            configureSQLite(hikariConfig);
        }
        dataSource = new HikariDataSource(hikariConfig);
        createTables();
        runMigrations();
        NCLogger.info("Database initialized successfully.");
    }

    private void configureSQLite(HikariConfig config) {
        File dbFile = new File(plugin.getDataFolder(),
                configManager.getMain().getString("database.sqlite.file", "nuclearcraft.db"));
        config.setDriverClassName("org.sqlite.JDBC");
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setMaximumPoolSize(1);
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("NuclearCraft-SQLite");
        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("synchronous", "NORMAL");
    }

    private void configureMySql(HikariConfig config) {
        var mysql = configManager.getMain().getConfigurationSection("database.mysql");
        if (mysql == null) throw new IllegalStateException("MySQL configuration section missing.");
        String host = mysql.getString("host", "localhost");
        int port = mysql.getInt("port", 3306);
        String database = mysql.getString("database", "nuclearcraft");
        String username = mysql.getString("username", "root");
        String password = mysql.getString("password", "");
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&autoReconnect=true&characterEncoding=utf-8");
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(mysql.getInt("pool-size", 10));
        config.setMaxLifetime(mysql.getLong("max-lifetime", 1800000));
        config.setConnectionTimeout(mysql.getLong("connection-timeout", 30000));
        config.setPoolName("NuclearCraft-MySQL");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Schema
    // ──────────────────────────────────────────────────────────────────────────

    private void createTables() throws SQLException {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Phase 1 + 2 combined schema
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_data (
                    uuid                    TEXT PRIMARY KEY,
                    radiation_level         REAL    NOT NULL DEFAULT 0.0,
                    radiation_stage         INTEGER NOT NULL DEFAULT 0,
                    immunity_timer_end      INTEGER NOT NULL DEFAULT 0,
                    infection_progress      REAL    NOT NULL DEFAULT 0.0,
                    last_radiation_recv_ms  INTEGER NOT NULL DEFAULT 0,
                    last_radiation_source   TEXT    NOT NULL DEFAULT 'UNKNOWN',
                    times_infected          INTEGER NOT NULL DEFAULT 0,
                    boss_kills              INTEGER NOT NULL DEFAULT 0,
                    total_exposure          REAL    NOT NULL DEFAULT 0.0,
                    total_cured             REAL    NOT NULL DEFAULT 0.0,
                    radiation_deaths        INTEGER NOT NULL DEFAULT 0
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_upgrades (
                    uuid       TEXT NOT NULL,
                    upgrade_id TEXT NOT NULL,
                    PRIMARY KEY (uuid, upgrade_id)
                )
            """);
        }
        NCLogger.debug("Database tables verified.");
    }

    /**
     * Applies migrations for databases created with Phase 1 schema.
     * Adds Phase 2 columns if they don't already exist.
     */
    private void runMigrations() throws SQLException {
        try (Connection conn = getConnection()) {
            addColumnIfMissing(conn, "player_data", "last_radiation_recv_ms", "INTEGER NOT NULL DEFAULT 0");
            addColumnIfMissing(conn, "player_data", "last_radiation_source",  "TEXT NOT NULL DEFAULT 'UNKNOWN'");
            addColumnIfMissing(conn, "player_data", "times_infected",         "INTEGER NOT NULL DEFAULT 0");
            addColumnIfMissing(conn, "player_data", "total_cured",            "REAL NOT NULL DEFAULT 0.0");
            addColumnIfMissing(conn, "player_data", "radiation_deaths",       "INTEGER NOT NULL DEFAULT 0");
        }
        NCLogger.debug("Database migrations complete.");
    }

    private void addColumnIfMissing(Connection conn, String table, String column, String definition)
            throws SQLException {
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, table, column)) {
            if (!rs.next()) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
                    NCLogger.info("Migration: added column '" + column + "' to table '" + table + "'.");
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // CRUD
    // ──────────────────────────────────────────────────────────────────────────

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public Optional<PlayerData> loadPlayerData(UUID uuid) {
        String sql = "SELECT * FROM player_data WHERE uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Set<String> upgrades = loadUpgrades(conn, uuid);
                    return Optional.of(new PlayerData(
                            uuid,
                            rs.getDouble("radiation_level"),
                            rs.getInt("radiation_stage"),
                            rs.getLong("immunity_timer_end"),
                            rs.getDouble("infection_progress"),
                            rs.getLong("last_radiation_recv_ms"),
                            rs.getString("last_radiation_source"),
                            rs.getInt("times_infected"),
                            rs.getDouble("total_exposure"),
                            rs.getDouble("total_cured"),
                            rs.getInt("radiation_deaths"),
                            rs.getInt("boss_kills"),
                            upgrades
                    ));
                }
            }
        } catch (SQLException e) {
            NCLogger.severe("Failed to load player data for " + uuid, e);
        }
        return Optional.empty();
    }

    public void savePlayerData(PlayerData data) {
        String sql = """
            INSERT INTO player_data (
                uuid, radiation_level, radiation_stage, immunity_timer_end,
                infection_progress, last_radiation_recv_ms, last_radiation_source,
                times_infected, boss_kills, total_exposure, total_cured, radiation_deaths
            ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT(uuid) DO UPDATE SET
                radiation_level        = excluded.radiation_level,
                radiation_stage        = excluded.radiation_stage,
                immunity_timer_end     = excluded.immunity_timer_end,
                infection_progress     = excluded.infection_progress,
                last_radiation_recv_ms = excluded.last_radiation_recv_ms,
                last_radiation_source  = excluded.last_radiation_source,
                times_infected         = excluded.times_infected,
                boss_kills             = excluded.boss_kills,
                total_exposure         = excluded.total_exposure,
                total_cured            = excluded.total_cured,
                radiation_deaths       = excluded.radiation_deaths
        """;
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1,  data.getUuid().toString());
            ps.setDouble(2,  data.getRadiationLevel());
            ps.setInt(3,     data.getRadiationStage());
            ps.setLong(4,    data.getImmunityTimerEndMs());
            ps.setDouble(5,  data.getInfectionProgress());
            ps.setLong(6,    data.getLastRadiationReceivedMs());
            ps.setString(7,  data.getLastRadiationSource());
            ps.setInt(8,     data.getTimesInfected());
            ps.setInt(9,     data.getBossKills());
            ps.setDouble(10, data.getTotalRadiationExposure());
            ps.setDouble(11, data.getTotalRadiationCured());
            ps.setInt(12,    data.getRadiationDeaths());
            ps.executeUpdate();
            saveUpgrades(conn, data);
            data.markClean();
        } catch (SQLException e) {
            NCLogger.severe("Failed to save player data for " + data.getUuid(), e);
        }
    }

    private Set<String> loadUpgrades(Connection conn, UUID uuid) throws SQLException {
        Set<String> upgrades = new HashSet<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT upgrade_id FROM player_upgrades WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) upgrades.add(rs.getString("upgrade_id"));
            }
        }
        return upgrades;
    }

    private void saveUpgrades(Connection conn, PlayerData data) throws SQLException {
        try (PreparedStatement del = conn.prepareStatement(
                "DELETE FROM player_upgrades WHERE uuid = ?")) {
            del.setString(1, data.getUuid().toString());
            del.executeUpdate();
        }
        if (data.getUnlockedUpgrades().isEmpty()) return;
        try (PreparedStatement ins = conn.prepareStatement(
                "INSERT OR IGNORE INTO player_upgrades (uuid, upgrade_id) VALUES (?, ?)")) {
            for (String id : data.getUnlockedUpgrades()) {
                ins.setString(1, data.getUuid().toString());
                ins.setString(2, id);
                ins.addBatch();
            }
            ins.executeBatch();
        }
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            NCLogger.info("Database connection pool closed.");
        }
    }
}
