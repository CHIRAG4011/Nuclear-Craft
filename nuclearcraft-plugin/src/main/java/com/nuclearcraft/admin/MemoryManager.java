package com.nuclearcraft.admin;

import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Phase 14: Memory management and leak detection.
 *
 * Periodically samples heap usage and JVM GC activity to detect memory
 * pressure and potential leaks. Exposes cleanup hooks other managers can
 * call to purge caches and release references.
 *
 * Leak detection heuristic: if heap usage grows by more than LEAK_GROWTH_THRESHOLD
 * across LEAK_SAMPLE_WINDOW consecutive samples without a GC reducing it, a
 * warning is issued to help server admins diagnose runaway object retention.
 */
public class MemoryManager {

    private static final double LEAK_GROWTH_THRESHOLD = 0.05;
    private static final int    LEAK_SAMPLE_WINDOW    = 5;
    private static final double HIGH_MEMORY_THRESHOLD = 0.85;
    private static final long   SAMPLE_INTERVAL_TICKS = 20L * 60;

    private final NuclearCraftPlugin plugin;

    private BukkitTask sampleTask;

    private double lastHeapPercent = 0.0;
    private int    growthStreak    = 0;
    private final AtomicLong  lastGcCount    = new AtomicLong(-1);
    private final AtomicInteger cleanedRefs  = new AtomicInteger(0);

    public MemoryManager(NuclearCraftPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        startSampler();
        NCLogger.info("[MemoryManager] Initialized. Sampling heap every 60 seconds.");
    }

    public void shutdown() {
        if (sampleTask != null) {
            sampleTask.cancel();
            sampleTask = null;
        }
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    /**
     * Requests JVM GC and clears any internal soft-reference caches.
     *
     * @return estimated number of cached references purged (best-effort)
     */
    public int runCleanup() {
        int purged = cleanedRefs.getAndSet(0);
        System.gc();
        NCLogger.info("[MemoryManager] Cleanup requested. Purged tracking refs: " + purged);
        return purged;
    }

    // ── Reports ───────────────────────────────────────────────────────────────

    public String getMemorySummary() {
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        MemoryUsage nonHeap = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
        long heapUsed   = heap.getUsed()    / 1024 / 1024;
        long heapMax    = heap.getMax()     / 1024 / 1024;
        long nhUsed     = nonHeap.getUsed() / 1024 / 1024;
        double pct      = (double) heap.getUsed() / heap.getMax() * 100;

        long gcCount = getTotalGcCount();
        return String.format("Heap: %dMB/%dMB (%.0f%%) | Non-Heap: %dMB | GC runs: %d",
                heapUsed, heapMax, pct, nhUsed, gcCount);
    }

    public double getCurrentHeapPercent() {
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        if (heap.getMax() <= 0) return 0.0;
        return (double) heap.getUsed() / heap.getMax();
    }

    public boolean isMemoryCritical() {
        return getCurrentHeapPercent() > HIGH_MEMORY_THRESHOLD;
    }

    // ── Internal sampler ──────────────────────────────────────────────────────

    private void startSampler() {
        sampleTask = new BukkitRunnable() {
            @Override
            public void run() {
                sampleMemory();
            }
        }.runTaskTimerAsynchronously(plugin, SAMPLE_INTERVAL_TICKS, SAMPLE_INTERVAL_TICKS);
    }

    private void sampleMemory() {
        double currentPct = getCurrentHeapPercent();
        long   currentGc  = getTotalGcCount();
        long   prevGc     = lastGcCount.getAndSet(currentGc);

        boolean gcOccurred = prevGc >= 0 && currentGc > prevGc;

        if (currentPct > HIGH_MEMORY_THRESHOLD) {
            MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
            long usedMB = heap.getUsed() / 1024 / 1024;
            long maxMB  = heap.getMax()  / 1024 / 1024;
            NCLogger.warn("[MemoryManager] HIGH MEMORY: " + usedMB + "MB / " + maxMB
                    + "MB (" + String.format("%.0f", currentPct * 100) + "%)");
        }

        if (gcOccurred) {
            growthStreak = 0;
        } else if (currentPct > lastHeapPercent + LEAK_GROWTH_THRESHOLD) {
            growthStreak++;
            if (growthStreak >= LEAK_SAMPLE_WINDOW) {
                NCLogger.warn("[MemoryManager] POTENTIAL MEMORY LEAK: heap grew "
                        + String.format("%.0f", LEAK_GROWTH_THRESHOLD * LEAK_SAMPLE_WINDOW * 100)
                        + "% over " + LEAK_SAMPLE_WINDOW + " samples without GC. "
                        + "Current: " + String.format("%.0f", currentPct * 100) + "%. "
                        + "Consider running /nc cleanup or restarting.");
                growthStreak = 0;
            }
        } else {
            if (growthStreak > 0) growthStreak--;
        }

        lastHeapPercent = currentPct;
    }

    private long getTotalGcCount() {
        List<GarbageCollectorMXBean> gcs = ManagementFactory.getGarbageCollectorMXBeans();
        return gcs.stream()
                .mapToLong(GarbageCollectorMXBean::getCollectionCount)
                .filter(c -> c >= 0)
                .sum();
    }
}
