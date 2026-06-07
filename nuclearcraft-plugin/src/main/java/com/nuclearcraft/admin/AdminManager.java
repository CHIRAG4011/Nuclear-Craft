package com.nuclearcraft.admin;

import com.nuclearcraft.boss.TitanManager;
import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.data.DatabaseManager;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.debug.TestingManager;
import com.nuclearcraft.farming.FarmingManager;
import com.nuclearcraft.forge.NuclearForgeManager;
import com.nuclearcraft.performance.PerformanceManager;
import com.nuclearcraft.radiation.RadiationManager;
import com.nuclearcraft.release.ReleaseManager;
import com.nuclearcraft.smelter.NuclearSmelterManager;
import com.nuclearcraft.utils.NCLogger;
import com.nuclearcraft.zombies.IrradiatedZombieManager;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Phase 14: Admin management system.
 *
 * Handles all server administration and operational commands:
 *   /nc health         — system health overview
 *   /nc diagnostics    — runs startup check suite and reports
 *   /nc performance    — detailed performance metrics
 *   /nc cleanup        — triggers resource and cache cleanup
 *   /nc fixdata        — runs data validation and repairs
 *   /nc dumpdata       — writes full debug dump to disk
 *   /nc serverreport   — generates full production server report
 */
public class AdminManager {

    public static final List<String> ADMIN_SUBCOMMANDS =
            List.of("health", "diagnostics", "performance", "cleanup", "fixdata", "dumpdata", "serverreport");

    private final NuclearCraftPlugin      plugin;
    private final ConfigManager           configManager;
    private final DatabaseManager         databaseManager;
    private final PlayerDataManager       playerDataManager;
    private final RadiationManager        radiationManager;
    private final NuclearSmelterManager   smelterManager;
    private final NuclearForgeManager     forgeManager;
    private final FarmingManager          farmingManager;
    private final TitanManager            titanManager;
    private final IrradiatedZombieManager zombieManager;
    private final PerformanceManager      performanceManager;
    private final TestingManager          testingManager;
    private final ReleaseManager          releaseManager;
    private final ValidationManager       validationManager;
    private final MemoryManager           memoryManager;
    private final RecoveryManager         recoveryManager;

    public AdminManager(NuclearCraftPlugin plugin,
                        ConfigManager configManager,
                        DatabaseManager databaseManager,
                        PlayerDataManager playerDataManager,
                        RadiationManager radiationManager,
                        NuclearSmelterManager smelterManager,
                        NuclearForgeManager forgeManager,
                        FarmingManager farmingManager,
                        TitanManager titanManager,
                        IrradiatedZombieManager zombieManager,
                        PerformanceManager performanceManager,
                        TestingManager testingManager,
                        ReleaseManager releaseManager,
                        ValidationManager validationManager,
                        MemoryManager memoryManager,
                        RecoveryManager recoveryManager) {
        this.plugin            = plugin;
        this.configManager     = configManager;
        this.databaseManager   = databaseManager;
        this.playerDataManager = playerDataManager;
        this.radiationManager  = radiationManager;
        this.smelterManager    = smelterManager;
        this.forgeManager      = forgeManager;
        this.farmingManager    = farmingManager;
        this.titanManager      = titanManager;
        this.zombieManager     = zombieManager;
        this.performanceManager = performanceManager;
        this.testingManager    = testingManager;
        this.releaseManager    = releaseManager;
        this.validationManager = validationManager;
        this.memoryManager     = memoryManager;
        this.recoveryManager   = recoveryManager;
    }

    public void initialize() {
        NCLogger.info("[AdminManager] Initialized. Admin subsystem ready.");
    }

    public void shutdown() {}

    // ── Command dispatch ─────────────────────────────────────────────────────

    public void handleAdminCommand(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sendUsage(sender);
            return;
        }
        switch (args[0].toLowerCase()) {
            case "health"       -> handleHealth(sender);
            case "diagnostics"  -> handleDiagnostics(sender);
            case "performance"  -> handlePerformance(sender);
            case "cleanup"      -> handleCleanup(sender);
            case "fixdata"      -> handleFixData(sender);
            case "dumpdata"     -> handleDumpData(sender);
            case "serverreport" -> handleServerReport(sender);
            default             -> sendUsage(sender);
        }
    }

    // ── /nc health ───────────────────────────────────────────────────────────

    private void handleHealth(CommandSender sender) {
        header(sender, "SERVER HEALTH");

        double tps = performanceManager.getEstimatedTps();
        String tpsColor = tps >= 18.0 ? "§a" : tps >= 14.0 ? "§e" : "§c";
        line(sender, "TPS: " + tpsColor + String.format("%.1f", tps));

        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        long usedMB = heap.getUsed() / 1024 / 1024;
        long maxMB  = heap.getMax()  / 1024 / 1024;
        double heapPct = (double) heap.getUsed() / heap.getMax() * 100;
        String memColor = heapPct < 70 ? "§a" : heapPct < 85 ? "§e" : "§c";
        line(sender, "Heap: " + memColor + usedMB + "MB / " + maxMB + "MB (" + String.format("%.0f", heapPct) + "%)");

        line(sender, "Online Players: §f" + plugin.getServer().getOnlinePlayers().size());
        line(sender, "Active Smelters: §f" + smelterManager.getMachineCount());
        line(sender, "Active Forges: §f" + forgeManager.getMachineCount());
        line(sender, "Irradiated Zombies: §f" + zombieManager.getActiveCount());
        line(sender, "Titan Alive: §f" + (titanManager != null && titanManager.isTitanAlive() ? "§cYES" : "§aNO"));
        line(sender, "Database: §f" + (isDatabaseHealthy() ? "§aONLINE" : "§cOFFLINE"));
        line(sender, "Particles Allowed: §f" + (performanceManager.allowParticles() ? "§aYES" : "§eNO (throttled)"));
        line(sender, "Heavy Tasks Allowed: §f" + (performanceManager.allowHeavyTasks() ? "§aYES" : "§cNO (critical load)"));

        if (releaseManager != null) {
            line(sender, releaseManager.getFullVersion());
        }
    }

    // ── /nc diagnostics ──────────────────────────────────────────────────────

    private void handleDiagnostics(CommandSender sender) {
        header(sender, "DIAGNOSTICS");
        sender.sendMessage("§7Running startup check suite...");
        var results = testingManager.runStartupChecks();
        testingManager.reportTo(sender);

        var valResults = validationManager.runAllValidations();
        sender.sendMessage("§2§l[Config Validation]");
        for (ValidationManager.ValidationResult r : valResults) {
            String icon = r.passed() ? "§a✔" : "§c✘";
            sender.sendMessage(icon + " §7" + r.name() + (r.passed() ? "" : " §c— " + r.detail()));
        }
        long passed = valResults.stream().filter(ValidationManager.ValidationResult::passed).count();
        sender.sendMessage("§e" + passed + " / " + valResults.size() + " validations passed.");
    }

    // ── /nc performance ──────────────────────────────────────────────────────

    private void handlePerformance(CommandSender sender) {
        header(sender, "PERFORMANCE");
        line(sender, performanceManager.buildReport());
        line(sender, "Radiation players tracked: §f" + performanceManager.getTrackedRadiationPlayers());
        line(sender, "Active machines: §f" + performanceManager.getActiveMachines());
        line(sender, "Tracked crops: §f" + performanceManager.getTrackedCrops());
        line(sender, "Tracked entities: §f" + performanceManager.getTrackedEntities());
        line(sender, memoryManager.getMemorySummary());
        line(sender, "GC collections: §f" + getGcCount());
    }

    // ── /nc cleanup ──────────────────────────────────────────────────────────

    private void handleCleanup(CommandSender sender) {
        header(sender, "CLEANUP");
        sender.sendMessage("§7Running resource cleanup...");

        int result = memoryManager.runCleanup();
        line(sender, "Memory cleanup freed: §a" + result + " cached references");

        int farmCrops = 0;
        try {
            farmCrops = farmingManager.getCropManager().getCropCount();
        } catch (Exception e) {
            NCLogger.warn("[AdminManager] Farming check error: " + e.getMessage());
        }
        line(sender, "Farming active crops: §a" + farmCrops + " (tracking healthy)");

        System.gc();
        line(sender, "JVM GC: §aRequested");
        sender.sendMessage("§aCleanup complete.");
        NCLogger.info("[AdminManager] Cleanup performed by " + sender.getName());
    }

    // ── /nc fixdata ──────────────────────────────────────────────────────────

    private void handleFixData(CommandSender sender) {
        header(sender, "FIX DATA");
        sender.sendMessage("§7Running data validation and repair...");

        var results = validationManager.runAllValidations();
        int repaired = 0;
        for (ValidationManager.ValidationResult r : results) {
            if (!r.passed()) {
                sender.sendMessage("§c✘ §7" + r.name() + " §c— " + r.detail());
                boolean fixed = validationManager.attemptRepair(r);
                if (fixed) {
                    sender.sendMessage("  §a→ Repaired automatically.");
                    repaired++;
                } else {
                    sender.sendMessage("  §e→ Could not auto-repair. Manual intervention may be needed.");
                }
            }
        }

        int recovered = recoveryManager.recoverOrphanedData();
        line(sender, "Orphaned machine/boss data recovered: §a" + recovered);
        sender.sendMessage("§aData fix complete. §f" + repaired + " issues auto-repaired.");
        NCLogger.info("[AdminManager] fixdata performed by " + sender.getName() + " — repaired: " + repaired);
    }

    // ── /nc dumpdata ─────────────────────────────────────────────────────────

    private void handleDumpData(CommandSender sender) {
        header(sender, "DUMP DATA");
        sender.sendMessage("§7Writing debug dump to disk...");

        File dumpDir = new File(plugin.getDataFolder(), "dumps");
        if (!dumpDir.exists()) dumpDir.mkdirs();

        String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());
        File dumpFile = new File(dumpDir, "nuclearcraft-dump-" + timestamp + ".txt");

        try (FileWriter fw = new FileWriter(dumpFile)) {
            fw.write("=== NuclearCraft Debug Dump ===\n");
            fw.write("Generated: " + timestamp + " UTC\n");
            fw.write("Requested by: " + sender.getName() + "\n\n");

            if (releaseManager != null) {
                fw.write("[Build]\n");
                fw.write(releaseManager.getBuildLine() + "\n");
                fw.write(releaseManager.getPlatformLine() + "\n\n");
            }

            fw.write("[Performance]\n");
            fw.write(performanceManager.buildReport() + "\n");
            fw.write(memoryManager.getMemorySummary() + "\n\n");

            fw.write("[Systems]\n");
            fw.write("Online Players: " + plugin.getServer().getOnlinePlayers().size() + "\n");
            fw.write("Smelters: " + smelterManager.getMachineCount() + "\n");
            fw.write("Forges: " + forgeManager.getMachineCount() + "\n");
            fw.write("Zombies: " + zombieManager.getActiveCount() + "\n");
            fw.write("Titan Alive: " + (titanManager != null && titanManager.isTitanAlive()) + "\n\n");

            fw.write("[Startup Checks]\n");
            for (TestingManager.TestResult r : testingManager.getLastResults()) {
                fw.write((r.passed() ? "PASS" : "FAIL") + " — " + r.name()
                        + (r.passed() ? "" : " — " + r.detail()) + "\n");
            }
            fw.write("\n");

            fw.write("[Validation]\n");
            for (ValidationManager.ValidationResult r : validationManager.runAllValidations()) {
                fw.write((r.passed() ? "PASS" : "FAIL") + " — " + r.name()
                        + (r.passed() ? "" : " — " + r.detail()) + "\n");
            }

        } catch (IOException e) {
            sender.sendMessage("§cFailed to write dump: " + e.getMessage());
            NCLogger.warn("[AdminManager] Dump write failed: " + e.getMessage());
            return;
        }

        line(sender, "Dump written to: §f" + dumpFile.getPath());
        sender.sendMessage("§aDump complete.");
        NCLogger.info("[AdminManager] dumpdata performed by " + sender.getName() + " → " + dumpFile.getName());
    }

    // ── /nc serverreport ─────────────────────────────────────────────────────

    private void handleServerReport(CommandSender sender) {
        header(sender, "SERVER REPORT");

        if (releaseManager != null) {
            line(sender, releaseManager.getFullVersion());
            line(sender, releaseManager.getPlatformLine());
        }

        line(sender, "Uptime: §f" + getUptimeString());

        double tps = performanceManager.getEstimatedTps();
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        long usedMB = heap.getUsed() / 1024 / 1024;
        long maxMB  = heap.getMax()  / 1024 / 1024;

        line(sender, "TPS: §f" + String.format("%.1f", tps) + "  Heap: §f" + usedMB + "/" + maxMB + "MB");
        line(sender, "Players: §f" + plugin.getServer().getOnlinePlayers().size());
        line(sender, "Smelters: §f" + smelterManager.getMachineCount()
                + "  Forges: §f" + forgeManager.getMachineCount());
        line(sender, "Zombies: §f" + zombieManager.getActiveCount()
                + "  Titan: §f" + (titanManager != null && titanManager.isTitanAlive() ? "ACTIVE" : "dormant"));

        var valResults = validationManager.runAllValidations();
        long passedVal = valResults.stream().filter(ValidationManager.ValidationResult::passed).count();
        line(sender, "Config Validations: §f" + passedVal + "/" + valResults.size() + " passed");

        var testResults = testingManager.getLastResults();
        long passedTest = testResults.stream().filter(TestingManager.TestResult::passed).count();
        line(sender, "Startup Checks: §f" + passedTest + "/" + testResults.size() + " passed");

        line(sender, memoryManager.getMemorySummary());

        sender.sendMessage("§7Use §f/nc dumpdata §7to save full report to disk.");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§e[NuclearCraft Admin] Usage: /nc <"
                + String.join("|", ADMIN_SUBCOMMANDS) + ">");
    }

    private void header(CommandSender sender, String title) {
        sender.sendMessage("§2§l[NuclearCraft: " + title + "]");
    }

    private void line(CommandSender sender, String text) {
        sender.sendMessage("§7  " + text);
    }

    private boolean isDatabaseHealthy() {
        try (var conn = databaseManager.getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (Exception e) {
            return false;
        }
    }

    private long getGcCount() {
        return ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(gc -> gc.getCollectionCount())
                .filter(c -> c >= 0)
                .sum();
    }

    private String getUptimeString() {
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        long seconds = uptimeMs / 1000;
        long hours   = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs    = seconds % 60;
        return String.format("%dh %dm %ds", hours, minutes, secs);
    }
}
