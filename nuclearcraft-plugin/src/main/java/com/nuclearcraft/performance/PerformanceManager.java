package com.nuclearcraft.performance;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Performance monitoring and optimization manager for NuclearCraft.
 *
 * Tracks:
 *  - Server TPS (approximated via scheduled task timing)
 *  - Heap memory usage
 *  - Plugin-level counters (tracked radiation players, active machines, etc.)
 *
 * Exposes control signals:
 *  - allowParticles()  — false under heavy load to throttle visual effects
 *  - allowHeavyTasks() — false when heap is critically high
 *
 * Phase 12 addition.
 */
public class PerformanceManager {

    private static final double TPS_TARGET = 20.0;
    private static final double TPS_THROTTLE_THRESHOLD = 16.0;
    private static final double TPS_CRITICAL_THRESHOLD = 12.0;
    private static final double MEMORY_WARN_PERCENT = 0.85;

    private final NuclearCraftPlugin plugin;
    private final ConfigManager configManager;

    private BukkitTask monitorTask;

    private volatile double estimatedTps = 20.0;
    private volatile long lastTickTime = System.currentTimeMillis();
    private volatile int tickCount = 0;

    private final AtomicLong trackedRadiationPlayers = new AtomicLong(0);
    private final AtomicLong activeMachines          = new AtomicLong(0);
    private final AtomicLong trackedCrops            = new AtomicLong(0);
    private final AtomicLong trackedEntities         = new AtomicLong(0);

    private int particleThrottlePercent;
    private int monitorIntervalSeconds;
    private boolean logPerformanceStats;

    public PerformanceManager(NuclearCraftPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void initialize() {
        FileConfiguration cfg = configManager.getConfig(ConfigManager.ConfigFile.MAIN);
        particleThrottlePercent = cfg.getInt("performance.particle-throttle-percent", 50);
        monitorIntervalSeconds  = cfg.getInt("performance.monitor-interval-seconds", 30);
        logPerformanceStats     = cfg.getBoolean("performance.log-stats", false);

        startMonitor();
        NCLogger.info("[PerformanceManager] Initialized. TPS throttle threshold: " + TPS_THROTTLE_THRESHOLD);
    }

    public void reload() {
        shutdown();
        initialize();
    }

    public void shutdown() {
        if (monitorTask != null) {
            monitorTask.cancel();
            monitorTask = null;
        }
    }

    // ── Control signals ───────────────────────────────────────────────────────

    /**
     * Returns true if particle effects should be spawned.
     * Returns false when the server TPS drops below the throttle threshold.
     */
    public boolean allowParticles() {
        return estimatedTps >= TPS_THROTTLE_THRESHOLD;
    }

    /**
     * Returns true if heavy background tasks (e.g. chunk scans) should run.
     * Returns false under critical TPS or near-full heap.
     */
    public boolean allowHeavyTasks() {
        return estimatedTps >= TPS_CRITICAL_THRESHOLD && heapFreePercent() > (1.0 - MEMORY_WARN_PERCENT);
    }

    /** Current estimated TPS. */
    public double getEstimatedTps() {
        return estimatedTps;
    }

    // ── Counters (updated by other managers) ──────────────────────────────────

    public void setTrackedRadiationPlayers(long count) { trackedRadiationPlayers.set(count); }
    public void setActiveMachines(long count)          { activeMachines.set(count); }
    public void setTrackedCrops(long count)            { trackedCrops.set(count); }
    public void setTrackedEntities(long count)         { trackedEntities.set(count); }

    public long getTrackedRadiationPlayers() { return trackedRadiationPlayers.get(); }
    public long getActiveMachines()          { return activeMachines.get(); }
    public long getTrackedCrops()            { return trackedCrops.get(); }
    public long getTrackedEntities()         { return trackedEntities.get(); }

    // ── Statistics report ─────────────────────────────────────────────────────

    public String buildReport() {
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        long usedMB = heap.getUsed() / 1024 / 1024;
        long maxMB  = heap.getMax()  / 1024 / 1024;
        double heapPct = (double) heap.getUsed() / heap.getMax() * 100;

        return String.format(
                "[PerformanceManager] TPS: %.1f | Heap: %dMB / %dMB (%.0f%%) | "
                + "RadPlayers: %d | Machines: %d | Crops: %d | Entities: %d",
                estimatedTps, usedMB, maxMB, heapPct,
                trackedRadiationPlayers.get(), activeMachines.get(),
                trackedCrops.get(), trackedEntities.get());
    }

    // ── Internal TPS sampler ──────────────────────────────────────────────────

    private void startMonitor() {
        long intervalTicks = (long) monitorIntervalSeconds * 20L;
        monitorTask = new BukkitRunnable() {
            private long lastSampleTime = System.currentTimeMillis();
            private int sampleTicks = 0;

            @Override
            public void run() {
                sampleTicks++;
                long now = System.currentTimeMillis();
                long elapsed = now - lastSampleTime;

                if (sampleTicks >= 20) {
                    double measuredTps = 20.0 * 1000.0 / elapsed * sampleTicks;
                    estimatedTps = Math.min(TPS_TARGET, (estimatedTps * 0.7) + (measuredTps * 0.3));
                    sampleTicks = 0;
                    lastSampleTime = now;
                }

                if (logPerformanceStats && sampleTicks == 0) {
                    NCLogger.info(buildReport());
                    checkMemoryWarning();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void checkMemoryWarning() {
        double usedPercent = 1.0 - heapFreePercent();
        if (usedPercent > MEMORY_WARN_PERCENT) {
            MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
            long usedMB = heap.getUsed() / 1024 / 1024;
            long maxMB  = heap.getMax()  / 1024 / 1024;
            NCLogger.warn("[PerformanceManager] HIGH MEMORY: " + usedMB + "MB / " + maxMB + "MB used ("
                    + String.format("%.0f", usedPercent * 100) + "%). Consider restarting.");
        }
    }

    private double heapFreePercent() {
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        if (heap.getMax() <= 0) return 1.0;
        return 1.0 - ((double) heap.getUsed() / heap.getMax());
    }
}
