package com.nuclearcraft.admin;

import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.command.CommandSender;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Phase 14: Build information and version management.
 *
 * Centralises all version/build metadata for display in startup banners,
 * admin commands, and server reports. Tracks the Phase 14 release target
 * as v1.0.0 production.
 */
public class BuildManager {

    private static final String PLUGIN_NAME    = "NuclearCraft: Plutonium Age";
    private static final String RELEASE_STAGE  = "v1.0.0 Production";
    private static final int    TOTAL_PHASES   = 14;
    private static final String TARGET_PLATFORM = "Paper / Purpur 1.21+";

    private final NuclearCraftPlugin plugin;
    private final String buildTimestamp;

    public BuildManager(NuclearCraftPlugin plugin) {
        this.plugin         = plugin;
        this.buildTimestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());
    }

    public void initialize() {
        NCLogger.info("[BuildManager] " + getFullBuildLine());
    }

    public void shutdown() {}

    // ── Version info ──────────────────────────────────────────────────────────

    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    public String getFullBuildLine() {
        return PLUGIN_NAME + " " + RELEASE_STAGE
                + " | Phases 1–" + TOTAL_PHASES
                + " | Built: " + buildTimestamp;
    }

    public String getPlatformLine() {
        return "Platform: " + plugin.getServer().getName()
                + " " + plugin.getServer().getBukkitVersion()
                + " | Java " + System.getProperty("java.version")
                + " | Target: " + TARGET_PLATFORM;
    }

    public String getCompatibilityStatus() {
        String serverVersion = plugin.getServer().getBukkitVersion();
        if (serverVersion != null && (serverVersion.contains("1.21") || serverVersion.contains("1.22"))) {
            return "§aCompatible";
        }
        return "§eUnknown version — expected 1.21.x";
    }

    public int getTotalPhases() {
        return TOTAL_PHASES;
    }

    // ── Commands ──────────────────────────────────────────────────────────────

    public void sendBuildInfo(CommandSender sender) {
        sender.sendMessage("§2§l[NuclearCraft Build Info]");
        sender.sendMessage("§7  Name:     §f" + PLUGIN_NAME);
        sender.sendMessage("§7  Version:  §f" + getVersion() + " §7(" + RELEASE_STAGE + ")");
        sender.sendMessage("§7  Phases:   §f1 – " + TOTAL_PHASES + " (complete)");
        sender.sendMessage("§7  Built:    §f" + buildTimestamp);
        sender.sendMessage("§7  Target:   §f" + TARGET_PLATFORM);
        sender.sendMessage("§7  Server:   §f" + plugin.getServer().getName()
                + " " + plugin.getServer().getBukkitVersion());
        sender.sendMessage("§7  Java:     §f" + System.getProperty("java.version"));
        sender.sendMessage("§7  Status:   " + getCompatibilityStatus());
    }

    // ── Startup banner ────────────────────────────────────────────────────────

    private void printBanner() {
        String version = getVersion();
        NCLogger.info("§a╔══════════════════════════════════════════════╗");
        NCLogger.info("§a║      NuclearCraft: Plutonium Age v1.0.0      ║");
        NCLogger.info("§a╠══════════════════════════════════════════════╣");
        NCLogger.info("§a║  Release  : §f" + pad(RELEASE_STAGE, 32) + "§a║");
        NCLogger.info("§a║  Phases   : §f" + pad("1 – " + TOTAL_PHASES + " (all complete)", 32) + "§a║");
        NCLogger.info("§a║  Built    : §f" + pad(buildTimestamp, 32) + "§a║");
        NCLogger.info("§a║  Platform : §f" + pad(TARGET_PLATFORM, 32) + "§a║");
        NCLogger.info("§a╚══════════════════════════════════════════════╝");
    }

    private String pad(String s, int width) {
        if (s == null) s = "";
        return s.length() >= width ? s : s + " ".repeat(width - s.length());
    }
}
