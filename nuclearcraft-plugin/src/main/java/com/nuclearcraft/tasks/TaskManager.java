package com.nuclearcraft.tasks;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.utils.NCLogger;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages all repeating and scheduled tasks for NuclearCraft.
 * All task handles are tracked for clean cancellation on shutdown.
 */
public class TaskManager {

    private final NuclearCraftPlugin plugin;
    private final ConfigManager configManager;
    private final PlayerDataManager playerDataManager;

    private final List<BukkitTask> tasks = new ArrayList<>();

    public TaskManager(NuclearCraftPlugin plugin, ConfigManager configManager,
                       PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.playerDataManager = playerDataManager;
    }

    public void initialize() {
        scheduleDataFlush();
        NCLogger.info("TaskManager initialized — " + tasks.size() + " tasks scheduled.");
    }

    public void shutdown() {
        for (BukkitTask task : tasks) {
            if (!task.isCancelled()) {
                task.cancel();
            }
        }
        tasks.clear();
        NCLogger.info("TaskManager shut down — all tasks cancelled.");
    }

    /**
     * Schedules periodic flushing of dirty player data to the database.
     */
    private void scheduleDataFlush() {
        long intervalTicks = configManager.getDataSaveIntervalTicks();
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
                plugin,
                () -> {
                    NCLogger.debug("Running scheduled data flush...");
                    playerDataManager.flushDirty();
                },
                intervalTicks,
                intervalTicks
        );
        tasks.add(task);
        NCLogger.debug("Data flush scheduled every %d ticks.", intervalTicks);
    }

    /**
     * Schedules a repeating sync task.
     * Use for tasks that must run on the main thread (entity interactions, block updates).
     */
    public BukkitTask scheduleSync(Runnable runnable, long delayTicks, long periodTicks) {
        BukkitTask task = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, runnable, delayTicks, periodTicks);
        tasks.add(task);
        return task;
    }

    /**
     * Schedules a repeating async task.
     * Use for I/O, heavy computation, or database access.
     */
    public BukkitTask scheduleAsync(Runnable runnable, long delayTicks, long periodTicks) {
        BukkitTask task = plugin.getServer().getScheduler()
                .runTaskTimerAsynchronously(plugin, runnable, delayTicks, periodTicks);
        tasks.add(task);
        return task;
    }

    /**
     * Schedules a one-shot sync task after a delay.
     */
    public BukkitTask scheduleSyncDelayed(Runnable runnable, long delayTicks) {
        BukkitTask task = plugin.getServer().getScheduler()
                .runTaskLater(plugin, runnable, delayTicks);
        tasks.add(task);
        return task;
    }

    /**
     * Runs a task on the main thread (immediate, no delay).
     */
    public BukkitTask runSync(Runnable runnable) {
        BukkitTask task = plugin.getServer().getScheduler()
                .runTask(plugin, runnable);
        tasks.add(task);
        return task;
    }

    public int getActiveTaskCount() {
        return (int) tasks.stream().filter(t -> !t.isCancelled()).count();
    }
}
