package com.nuclearcraft.release;

import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.utils.NCLogger;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Manages version information, build metadata, and startup health diagnostics
 * for NuclearCraft v1.0 release.
 *
 * On {@link #initialize()} this prints a full startup banner to the console
 * including version, platform, and system stats.
 *
 * Phase 12 addition.
 */
public class ReleaseManager {

    private static final String PLUGIN_NAME    = "NuclearCraft: Plutonium Age";
    private static final String RELEASE_STAGE  = "v1.0.0 Production";
    private static final int    PHASE_COUNT    = 14;

    private final NuclearCraftPlugin plugin;

    private final String buildTimestamp;

    public ReleaseManager(NuclearCraftPlugin plugin) {
        this.plugin         = plugin;
        this.buildTimestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());
    }

    public void initialize() {
        printBanner();
        runHealthChecks();
    }

    public void shutdown() {}

    // ── Version info ──────────────────────────────────────────────────────────

    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    public String getFullVersion() {
        return PLUGIN_NAME + " v" + getVersion() + " [" + RELEASE_STAGE + "]";
    }

    public String getBuildLine() {
        return "Build: " + getFullVersion() + " | Phases: 1-" + PHASE_COUNT + " | Built: " + buildTimestamp;
    }

    public String getPlatformLine() {
        return "Platform: " + plugin.getServer().getName()
                + " " + plugin.getServer().getBukkitVersion()
                + " | Java " + System.getProperty("java.version");
    }

    public int getPhaseCount() {
        return PHASE_COUNT;
    }

    // ── Startup banner ────────────────────────────────────────────────────────

    private void printBanner() {
        NCLogger.info("§a╔══════════════════════════════════════════╗");
        NCLogger.info("§a║        NuclearCraft: Plutonium Age       ║");
        NCLogger.info("§a╠══════════════════════════════════════════╣");
        NCLogger.info("§a║  Version : §f" + padRight(getVersion(), 30) + "§a║");
        NCLogger.info("§a║  Stage   : §f" + padRight(RELEASE_STAGE, 30) + "§a║");
        NCLogger.info("§a║  Phases  : §f" + padRight("1 – " + PHASE_COUNT + " (complete)", 30) + "§a║");
        NCLogger.info("§a║  Built   : §f" + padRight(buildTimestamp, 30) + "§a║");
        NCLogger.info("§a╠══════════════════════════════════════════╣");
        NCLogger.info("§a║  " + padRight(getPlatformLine(), 40) + "║");
        NCLogger.info("§a╚══════════════════════════════════════════╝");
    }

    // ── Health checks ─────────────────────────────────────────────────────────

    private void runHealthChecks() {
        checkJavaVersion();
        checkMemory();
        checkCompatibility();
    }

    private void checkJavaVersion() {
        int javaVersion = Runtime.version().feature();
        if (javaVersion < 21) {
            NCLogger.warn("[ReleaseManager] Java " + javaVersion + " detected. NuclearCraft requires Java 21+.");
        } else {
            NCLogger.info("[ReleaseManager] Java version: " + javaVersion + " ✔");
        }
    }

    private void checkMemory() {
        Runtime rt = Runtime.getRuntime();
        long maxMB = rt.maxMemory() / 1024 / 1024;
        if (maxMB < 1024) {
            NCLogger.warn("[ReleaseManager] Low max heap (" + maxMB + " MB). " +
                    "Recommend at least 2 GB for large SMP servers with NuclearCraft.");
        } else {
            NCLogger.info("[ReleaseManager] Max heap: " + maxMB + " MB ✔");
        }
    }

    private void checkCompatibility() {
        String version = plugin.getServer().getBukkitVersion();
        if (version == null || (!version.contains("1.21") && !version.contains("1.22"))) {
            NCLogger.warn("[ReleaseManager] Unexpected server version: " + version +
                    ". NuclearCraft targets Paper 1.21+.");
        } else {
            NCLogger.info("[ReleaseManager] Server version: " + version + " ✔");
        }
    }

    private String padRight(String text, int width) {
        if (text == null) text = "";
        if (text.length() >= width) return text;
        return text + " ".repeat(width - text.length());
    }
}
