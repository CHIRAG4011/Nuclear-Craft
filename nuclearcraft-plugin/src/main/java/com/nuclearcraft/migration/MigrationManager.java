package com.nuclearcraft.migration;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages config and data migrations between NuclearCraft versions.
 *
 * On startup, compares the stored config version against the current plugin
 * version. If they differ, applies the appropriate migration steps and
 * saves the updated version tag.
 *
 * Config version is stored in config.yml under plugin.config-version.
 * Missing keys are added with their defaults; no existing values are lost.
 *
 * Phase 12 addition.
 */
public class MigrationManager {

    private static final String VERSION_KEY = "plugin.config-version";
    private static final String CURRENT_CONFIG_VERSION = "12";

    private final NuclearCraftPlugin plugin;
    private final ConfigManager configManager;

    private final List<String> migrationLog = new ArrayList<>();

    public MigrationManager(NuclearCraftPlugin plugin, ConfigManager configManager) {
        this.plugin        = plugin;
        this.configManager = configManager;
    }

    public void initialize() {
        String stored = configManager.getConfig(ConfigManager.ConfigFile.MAIN)
                .getString(VERSION_KEY, "0");

        if (stored.equals(CURRENT_CONFIG_VERSION)) {
            NCLogger.info("[MigrationManager] Config is up to date (v" + CURRENT_CONFIG_VERSION + ").");
            return;
        }

        NCLogger.info("[MigrationManager] Config version mismatch: found v" + stored
                + ", current v" + CURRENT_CONFIG_VERSION + ". Running migrations...");
        runMigrations(stored);
        bumpConfigVersion();
        NCLogger.info("[MigrationManager] Migration complete. " + migrationLog.size() + " step(s) applied.");
    }

    public void shutdown() {}

    // ── Migration chain ───────────────────────────────────────────────────────

    private void runMigrations(String fromVersion) {
        int from = parseVersion(fromVersion);

        if (from < 12) {
            migrateToV12();
        }
    }

    /**
     * Migration to config version 12 (Phase 12).
     *
     * Adds missing keys introduced in Phase 12:
     *  - performance.monitor-interval-seconds
     *  - performance.log-stats
     *  - performance.particle-throttle-percent
     */
    private void migrateToV12() {
        FileConfiguration main = configManager.getConfig(ConfigManager.ConfigFile.MAIN);

        addIfMissing(main, "performance.monitor-interval-seconds", 30);
        addIfMissing(main, "performance.log-stats",                false);
        addIfMissing(main, "performance.particle-throttle-percent", 50);

        saveConfig(main, new File(plugin.getDataFolder(), "config.yml"));
        log("Migrated config.yml to v12 (added performance monitoring keys).");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void addIfMissing(FileConfiguration cfg, String key, Object defaultValue) {
        if (!cfg.isSet(key)) {
            cfg.set(key, defaultValue);
            log("Added missing key: " + key + " = " + defaultValue);
        }
    }

    private void bumpConfigVersion() {
        FileConfiguration main = configManager.getConfig(ConfigManager.ConfigFile.MAIN);
        main.set(VERSION_KEY, CURRENT_CONFIG_VERSION);
        saveConfig(main, new File(plugin.getDataFolder(), "config.yml"));
        log("Set " + VERSION_KEY + " = " + CURRENT_CONFIG_VERSION);
    }

    private void saveConfig(FileConfiguration cfg, File file) {
        try {
            cfg.save(file);
        } catch (IOException e) {
            NCLogger.warn("[MigrationManager] Failed to save config after migration: " + e.getMessage());
        }
    }

    private int parseVersion(String version) {
        try {
            return Integer.parseInt(version.split("\\.")[0]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void log(String message) {
        migrationLog.add(message);
        NCLogger.info("[MigrationManager] " + message);
    }

    public List<String> getMigrationLog() {
        return List.copyOf(migrationLog);
    }
}
