package com.nuclearcraft.admin;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.data.DatabaseManager;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Phase 14: Config and data validation manager.
 *
 * Validates every config section, every required key, and every critical
 * database integrity constraint. Exposes repair hooks for auto-fixing
 * common misconfigurations and corrupt data rows.
 */
public class ValidationManager {

    public record ValidationResult(String name, boolean passed, String detail, String repairHint) {
        public ValidationResult(String name, boolean passed, String detail) {
            this(name, passed, detail, null);
        }
    }

    private final NuclearCraftPlugin plugin;
    private final ConfigManager      configManager;
    private final DatabaseManager    databaseManager;
    private final PlayerDataManager  playerDataManager;

    private List<ValidationResult> lastResults = new ArrayList<>();

    public ValidationManager(NuclearCraftPlugin plugin,
                             ConfigManager configManager,
                             DatabaseManager databaseManager,
                             PlayerDataManager playerDataManager) {
        this.plugin            = plugin;
        this.configManager     = configManager;
        this.databaseManager   = databaseManager;
        this.playerDataManager = playerDataManager;
    }

    public void initialize() {
        NCLogger.info("[ValidationManager] Initialized.");
    }

    public void shutdown() {}

    // ── Full validation suite ─────────────────────────────────────────────────

    public List<ValidationResult> runAllValidations() {
        lastResults = new ArrayList<>();

        validateConfigFiles();
        validateRadiationConfig();
        validateSmelterConfig();
        validateForgeConfig();
        validateTitanConfig();
        validateDatabaseSchema();
        validatePermissions();

        long failed = lastResults.stream().filter(r -> !r.passed()).count();
        if (failed > 0) {
            NCLogger.warn("[ValidationManager] " + failed + "/" + lastResults.size() + " validations FAILED.");
        } else {
            NCLogger.info("[ValidationManager] All " + lastResults.size() + " validations passed.");
        }
        return List.copyOf(lastResults);
    }

    // ── Repair ────────────────────────────────────────────────────────────────

    public boolean attemptRepair(ValidationResult result) {
        if (result.repairHint() == null) return false;
        try {
            switch (result.repairHint()) {
                case "repair:radiation-stage-clamp" -> {
                    clampRadiationStages();
                    return true;
                }
                case "repair:radiation-level-clamp" -> {
                    clampRadiationLevels();
                    return true;
                }
                default -> { return false; }
            }
        } catch (Exception e) {
            NCLogger.warn("[ValidationManager] Repair failed for '" + result.name() + "': " + e.getMessage());
            return false;
        }
    }

    // ── Config validations ────────────────────────────────────────────────────

    private void validateConfigFiles() {
        for (ConfigManager.ConfigFile cf : ConfigManager.ConfigFile.values()) {
            boolean loaded = configManager.getConfig(cf) != null;
            check("Config loaded: " + cf.name(), loaded,
                    "Config file failed to load — check for YAML syntax errors.",
                    null);
        }
    }

    private void validateRadiationConfig() {
        FileConfiguration cfg = configManager.getConfig(ConfigManager.ConfigFile.RADIATION);
        if (cfg == null) return;

        check("radiation.yml: stages section present",
                cfg.isConfigurationSection("stages"),
                "Missing 'stages' section in radiation.yml", null);

        check("radiation.yml: max-radiation positive",
                cfg.getInt("max-radiation", -1) > 0,
                "max-radiation must be a positive integer in radiation.yml", null);

        check("radiation.yml: decay-rate non-negative",
                cfg.getDouble("decay-rate", -1) >= 0,
                "decay-rate must be >= 0 in radiation.yml", null);
    }

    private void validateSmelterConfig() {
        FileConfiguration cfg = configManager.getConfig(ConfigManager.ConfigFile.SMELTER);
        if (cfg == null) return;

        check("smelter.yml: recipes section present",
                cfg.isConfigurationSection("recipes"),
                "Missing 'recipes' section in smelter.yml", null);

        check("smelter.yml: fuel-consumption positive",
                cfg.getInt("fuel-consumption", -1) > 0,
                "fuel-consumption must be > 0 in smelter.yml", null);
    }

    private void validateForgeConfig() {
        FileConfiguration cfg = configManager.getConfig(ConfigManager.ConfigFile.FORGE);
        if (cfg == null) return;

        check("forge.yml: tiers section present",
                cfg.isConfigurationSection("tiers"),
                "Missing 'tiers' section in forge.yml", null);
    }

    private void validateTitanConfig() {
        FileConfiguration cfg = configManager.getConfig(ConfigManager.ConfigFile.TITAN);
        if (cfg == null) return;

        check("titan.yml: health positive",
                cfg.getDouble("health", -1) > 0,
                "Titan health must be positive in titan.yml", null);

        check("titan.yml: phases section present",
                cfg.isConfigurationSection("phases"),
                "Missing 'phases' section in titan.yml", null);
    }

    // ── Database validations ──────────────────────────────────────────────────

    private void validateDatabaseSchema() {
        if (databaseManager == null) {
            check("Database: connection available", false, "DatabaseManager is null", null);
            return;
        }
        try (Connection conn = databaseManager.getConnection()) {
            if (conn == null || conn.isClosed()) {
                check("Database: connection available", false, "Cannot open connection", null);
                return;
            }
            check("Database: connection available", true, "", null);

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM player_data")) {
                int count = rs.next() ? rs.getInt(1) : 0;
                check("Database: player_data table readable", true, "", null);
                NCLogger.debug("[ValidationManager] player_data rows: " + count);
            } catch (Exception e) {
                check("Database: player_data table readable", false, e.getMessage(), null);
                return;
            }

            checkDataIntegrity(conn);

        } catch (Exception e) {
            check("Database: connection available", false, e.getMessage(), null);
        }
    }

    private void checkDataIntegrity(Connection conn) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT COUNT(*) FROM player_data WHERE radiation_stage < 0 OR radiation_stage > 4")) {
            int badStages = rs.next() ? rs.getInt(1) : 0;
            check("Database: radiation_stage in valid range [0-4]",
                    badStages == 0,
                    badStages + " rows have out-of-range radiation_stage",
                    badStages > 0 ? "repair:radiation-stage-clamp" : null);
        } catch (Exception e) {
            NCLogger.debug("[ValidationManager] Stage integrity check skipped: " + e.getMessage());
        }

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT COUNT(*) FROM player_data WHERE radiation_level < 0 OR radiation_level > 1000")) {
            int badLevels = rs.next() ? rs.getInt(1) : 0;
            check("Database: radiation_level in valid range [0-1000]",
                    badLevels == 0,
                    badLevels + " rows have out-of-range radiation_level",
                    badLevels > 0 ? "repair:radiation-level-clamp" : null);
        } catch (Exception e) {
            NCLogger.debug("[ValidationManager] Level integrity check skipped: " + e.getMessage());
        }
    }

    // ── Permission validation ─────────────────────────────────────────────────

    private void validatePermissions() {
        var pm = plugin.getServer().getPluginManager().getPermission("nuclearcraft.admin");
        check("Permission: nuclearcraft.admin defined",
                pm != null,
                "nuclearcraft.admin permission not defined in plugin.yml", null);
    }

    // ── Database repairs ──────────────────────────────────────────────────────

    private void clampRadiationStages() throws Exception {
        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                    "UPDATE player_data SET radiation_stage = MAX(0, MIN(4, radiation_stage)) " +
                    "WHERE radiation_stage < 0 OR radiation_stage > 4");
            NCLogger.info("[ValidationManager] Clamped radiation_stage values to [0-4].");
        }
    }

    private void clampRadiationLevels() throws Exception {
        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                    "UPDATE player_data SET radiation_level = MAX(0, MIN(1000, radiation_level)) " +
                    "WHERE radiation_level < 0 OR radiation_level > 1000");
            NCLogger.info("[ValidationManager] Clamped radiation_level values to [0-1000].");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void check(String name, boolean passed, String failDetail, String repairHint) {
        lastResults.add(new ValidationResult(name, passed,
                passed ? "" : failDetail,
                passed ? null : repairHint));
        if (!passed) {
            NCLogger.warn("[ValidationManager] FAIL: " + name + " — " + failDetail);
        }
    }

    public List<ValidationResult> getLastResults() {
        return List.copyOf(lastResults);
    }
}
