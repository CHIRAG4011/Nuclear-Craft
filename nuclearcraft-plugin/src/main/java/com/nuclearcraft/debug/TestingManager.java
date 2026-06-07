package com.nuclearcraft.debug;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.data.DatabaseManager;
import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.resourcepack.ModelRegistry;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

/**
 * Diagnostic and validation toolset for NuclearCraft.
 *
 * On startup, runs a series of lightweight checks to catch common
 * configuration and integration errors before they cause runtime bugs.
 *
 * Can also be triggered via /nc debug all by an admin.
 *
 * Phase 12 addition.
 */
public class TestingManager {

    public record TestResult(String name, boolean passed, String detail) {}

    private final NuclearCraftPlugin plugin;
    private final ConfigManager      configManager;
    private final DatabaseManager    databaseManager;
    private final ItemManager        itemManager;

    private final List<TestResult> lastResults = new ArrayList<>();

    public TestingManager(NuclearCraftPlugin plugin,
                          ConfigManager configManager,
                          DatabaseManager databaseManager,
                          ItemManager itemManager) {
        this.plugin          = plugin;
        this.configManager   = configManager;
        this.databaseManager = databaseManager;
        this.itemManager     = itemManager;
    }

    public void initialize() {
        runStartupChecks();
    }

    public void shutdown() {}

    // ── Startup check suite ───────────────────────────────────────────────────

    public List<TestResult> runStartupChecks() {
        lastResults.clear();
        NCLogger.info("[TestingManager] Running startup diagnostics...");

        check("ConfigManager loaded",
                configManager != null,
                "ConfigManager is null — this should never happen.");

        check("Database reachable",
                isDatabaseReachable(),
                "Database connection failed. Check config.yml database settings.");

        check("ItemManager has items",
                itemManager != null && itemManager.getRegistry().size() > 0,
                "ItemManager registry is empty — no items registered.");

        check("ModelRegistry has no duplicates",
                validateModelRegistry(),
                "ModelRegistry contains duplicate CustomModelData IDs. Check ModelRegistry.java.");

        check("Config files present",
                validateConfigFiles(),
                "One or more expected config files failed to load.");

        check("Paper API version compatible",
                validateServerVersion(),
                "Server version may be incompatible. NuclearCraft requires Paper 1.21+.");

        int passed = (int) lastResults.stream().filter(TestResult::passed).count();
        int total  = lastResults.size();
        if (passed == total) {
            NCLogger.info("[TestingManager] All " + total + " startup checks PASSED.");
        } else {
            NCLogger.warn("[TestingManager] " + (total - passed) + "/" + total + " startup checks FAILED:");
            for (TestResult r : lastResults) {
                if (!r.passed()) {
                    NCLogger.warn("  FAIL: " + r.name() + " — " + r.detail());
                }
            }
        }
        return List.copyOf(lastResults);
    }

    /** Print the last check results to a command sender. */
    public void reportTo(CommandSender sender) {
        sender.sendMessage("§2§l[NuclearCraft Diagnostics]");
        for (TestResult r : lastResults) {
            String icon = r.passed() ? "§a✔" : "§c✘";
            sender.sendMessage(icon + " §7" + r.name() + (r.passed() ? "" : " §c— " + r.detail()));
        }
        long passed = lastResults.stream().filter(TestResult::passed).count();
        sender.sendMessage("§e" + passed + " / " + lastResults.size() + " checks passed.");
    }

    // ── Individual checks ─────────────────────────────────────────────────────

    private boolean isDatabaseReachable() {
        if (databaseManager == null) return false;
        try (var conn = databaseManager.getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean validateModelRegistry() {
        try {
            var mappings = ModelRegistry.getAllMappings();
            var seen = new java.util.HashSet<Integer>();
            for (int id : mappings.values()) {
                if (!seen.add(id)) return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean validateConfigFiles() {
        try {
            for (ConfigManager.ConfigFile cf : ConfigManager.ConfigFile.values()) {
                if (configManager.getConfig(cf) == null) return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean validateServerVersion() {
        try {
            String version = plugin.getServer().getBukkitVersion();
            return version != null && (version.contains("1.21") || version.contains("1.22"));
        } catch (Exception e) {
            return false;
        }
    }

    private void check(String name, boolean passed, String failDetail) {
        lastResults.add(new TestResult(name, passed, passed ? "" : failDetail));
    }

    public List<TestResult> getLastResults() {
        return List.copyOf(lastResults);
    }
}
