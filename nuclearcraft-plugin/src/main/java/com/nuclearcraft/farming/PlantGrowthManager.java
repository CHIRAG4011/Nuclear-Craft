package com.nuclearcraft.farming;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.events.CropMutationEvent;
import com.nuclearcraft.events.PlantGrowthEvent;
import com.nuclearcraft.utils.NCLogger;
import com.nuclearcraft.utils.RandomUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Handles Mutated Healing Plant growth logic.
 *
 * <p>Growth is purely event-driven via {@link BlockGrowEvent} — no per-tick scanning.
 *
 * <ul>
 *   <li>Fires {@link PlantGrowthEvent} before advancing a stage.</li>
 *   <li>Radioactive Farmland bonus: +50% extra growth chance on each tick.</li>
 *   <li>1% chance of Toxic Bloom mutation when growing from stage 3 → 4.</li>
 * </ul>
 */
public class PlantGrowthManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final MutatedCropManager cropManager;
    private final ToxicBloomManager toxicBloomManager;

    public PlantGrowthManager(JavaPlugin plugin,
                               ConfigManager configManager,
                               MutatedCropManager cropManager,
                               ToxicBloomManager toxicBloomManager) {
        this.plugin           = plugin;
        this.configManager    = configManager;
        this.cropManager      = cropManager;
        this.toxicBloomManager = toxicBloomManager;
    }

    public void initialize() {
        NCLogger.info("PlantGrowthManager initialized.");
    }

    public void shutdown() {}

    // ──────────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Called from FarmingListener on BlockGrowEvent.
     * Intercepts wheat growth for tracked mutated crops and applies custom stage logic.
     *
     * @param event the Bukkit BlockGrowEvent
     * @return true if the event was consumed (caller should cancel it)
     */
    public boolean handleGrowth(BlockGrowEvent event) {
        Block block = event.getBlock();
        Location loc = block.getLocation();

        if (!cropManager.isMutatedCrop(loc)) return false;

        var optData = cropManager.getCrop(loc);
        if (optData.isEmpty()) return false;

        MutatedCropData data = optData.get();

        if (data.isFullyGrown()) {
            // Already fully grown; prevent further Bukkit growth
            event.setCancelled(true);
            return true;
        }

        int oldStage = data.getStage();
        int newStage = oldStage + 1;

        // Fire growth event — third-party systems can cancel it
        PlantGrowthEvent growthEvent = new PlantGrowthEvent(loc, oldStage, newStage);
        plugin.getServer().getPluginManager().callEvent(growthEvent);
        if (growthEvent.isCancelled()) {
            event.setCancelled(true);
            return true;
        }

        // Check Toxic Bloom mutation at stage 3 → 4
        double mutationChance = configManager.getFarming()
                .getDouble("plant.toxic-bloom-chance", 0.01);
        if (oldStage == 3 && RandomUtil.chance(mutationChance)) {
            CropMutationEvent mutationEvent = new CropMutationEvent(loc);
            plugin.getServer().getPluginManager().callEvent(mutationEvent);
            if (!mutationEvent.isCancelled()) {
                event.setCancelled(true);
                cropManager.untrackCrop(loc);
                toxicBloomManager.spawnBloom(loc);
                NCLogger.debug("Toxic Bloom spawned at %s!", loc);
                return true;
            }
        }

        // Advance the stage
        event.setCancelled(true);
        cropManager.advanceStage(loc);

        // Radioactive Farmland bonus: 50% chance of an additional growth tick
        double bonus = configManager.getFarming()
                .getDouble("plant.farmland-growth-bonus", 0.50);
        if (newStage < MutatedCropData.MAX_STAGE && RandomUtil.chance(bonus)) {
            // Schedule an extra growth after a short delay
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!cropManager.isMutatedCrop(loc)) return;
                cropManager.getCrop(loc).ifPresent(d -> {
                    if (!d.isFullyGrown()) {
                        cropManager.advanceStage(loc);
                        NCLogger.debug("Bonus growth applied at %s", loc);
                    }
                });
            }, 2L);
        }

        NCLogger.debug("MutatedCrop grew: stage %d → %d at %s", oldStage, newStage, loc);
        return true;
    }

    /**
     * Forces all mutated crops within radius of the given location to grow by one stage.
     * Used by the admin command: /nc farming growall
     */
    public void forceGrowNearby(Location center, int radius) {
        int grown = 0;
        for (String key : cropManager.getAllKeys()) {
            var optData = cropManager.getCrop(parseLocation(key));
            if (optData.isEmpty()) continue;
            MutatedCropData data = optData.get();
            if (data.getLocation().distanceSquared(center) <= radius * radius) {
                if (!data.isFullyGrown()) {
                    cropManager.advanceStage(data.getLocation());
                    grown++;
                }
            }
        }
        NCLogger.debug("forceGrowNearby: grew %d crops near %s", grown, center);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private Location parseLocation(String key) {
        String[] parts = key.split(":");
        var world = plugin.getServer().getWorld(parts[0]);
        if (world == null) return null;
        return new Location(world,
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2]),
                Integer.parseInt(parts[3]));
    }
}
