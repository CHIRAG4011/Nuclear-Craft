package com.nuclearcraft.farming;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks all active Mutated Healing Plants across the server.
 *
 * <p>Data is persisted to {@code mutated_crops.yml} in the plugin data folder
 * so that crop state survives server restarts.
 *
 * <p>Growth and harvesting are handled by event-driven listeners — no per-tick scanning.
 */
public class MutatedCropManager {

    private static final String FILE_NAME = "mutated_crops.yml";

    private final JavaPlugin plugin;
    private final ConfigManager configManager;

    /** location-key → crop data */
    private final Map<String, MutatedCropData> activeCrops = new ConcurrentHashMap<>();

    private File dataFile;

    public MutatedCropManager(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin        = plugin;
        this.configManager = configManager;
    }

    public void initialize() {
        dataFile = new File(plugin.getDataFolder(), FILE_NAME);
        loadFromDisk();
        NCLogger.info("MutatedCropManager initialized — " + activeCrops.size() + " crops loaded.");
    }

    public void shutdown() {
        saveToDisk();
        activeCrops.clear();
        NCLogger.info("MutatedCropManager shut down. Crops saved.");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────────

    /** Returns true if the given location contains a tracked mutated crop. */
    public boolean isMutatedCrop(Location loc) {
        return activeCrops.containsKey(key(loc));
    }

    /** Returns the crop data for the location, or empty if not tracked. */
    public Optional<MutatedCropData> getCrop(Location loc) {
        return Optional.ofNullable(activeCrops.get(key(loc)));
    }

    /**
     * Registers a newly planted mutated crop.
     * Sets the block to WHEAT at age 0 and stores the crop data.
     */
    public void registerCrop(Location loc, UUID planterUuid) {
        Block block = loc.getBlock();
        block.setType(Material.WHEAT);
        if (block.getBlockData() instanceof Ageable ageable) {
            ageable.setAge(0);
            block.setBlockData(ageable);
        }
        activeCrops.put(key(loc), new MutatedCropData(loc, planterUuid));
        NCLogger.debug("MutatedCrop registered at %s by %s", loc, planterUuid);
    }

    /**
     * Advances the crop at the given location by one stage.
     * Updates the wheat block age accordingly.
     *
     * @return the new crop data, or empty if not tracked
     */
    public Optional<MutatedCropData> advanceStage(Location loc) {
        MutatedCropData data = activeCrops.get(key(loc));
        if (data == null) return Optional.empty();
        data.advanceStage();
        applyStageToBlock(loc, data);
        NCLogger.debug("MutatedCrop at %s advanced to stage %d", loc, data.getStage());
        return Optional.of(data);
    }

    /**
     * Removes the crop from tracking and clears the block (sets to AIR).
     */
    public void removeCrop(Location loc) {
        activeCrops.remove(key(loc));
        loc.getBlock().setType(Material.AIR);
        NCLogger.debug("MutatedCrop removed at %s", loc);
    }

    /**
     * Removes the crop from tracking WITHOUT clearing the block.
     * Used when the block is already gone (e.g. broken by player or physics).
     */
    public void untrackCrop(Location loc) {
        activeCrops.remove(key(loc));
    }

    /** Returns the number of currently tracked crops. */
    public int getCropCount() {
        return activeCrops.size();
    }

    /** Returns a snapshot of all active crop locations. */
    public Set<String> getAllKeys() {
        return Collections.unmodifiableSet(activeCrops.keySet());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ──────────────────────────────────────────────────────────────────────────

    private void applyStageToBlock(Location loc, MutatedCropData data) {
        Block block = loc.getBlock();
        if (block.getType() != Material.WHEAT) {
            block.setType(Material.WHEAT);
        }
        if (block.getBlockData() instanceof Ageable ageable) {
            ageable.setAge(data.getWheatAge());
            block.setBlockData(ageable);
        }
    }

    private String key(Location loc) {
        return loc.getWorld().getName()
                + ":" + loc.getBlockX()
                + ":" + loc.getBlockY()
                + ":" + loc.getBlockZ();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Persistence
    // ──────────────────────────────────────────────────────────────────────────

    private void saveToDisk() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, MutatedCropData> entry : activeCrops.entrySet()) {
            MutatedCropData data = entry.getValue();
            String base = "crops." + entry.getKey().replace(":", "_");
            yaml.set(base + ".world",      data.getLocation().getWorld().getName());
            yaml.set(base + ".x",          data.getLocation().getBlockX());
            yaml.set(base + ".y",          data.getLocation().getBlockY());
            yaml.set(base + ".z",          data.getLocation().getBlockZ());
            yaml.set(base + ".stage",      data.getStage());
            yaml.set(base + ".planter",    data.getPlanterUuid().toString());
            yaml.set(base + ".planted-at", data.getPlantedAt());
        }
        try {
            yaml.save(dataFile);
        } catch (IOException e) {
            NCLogger.severe("Failed to save mutated_crops.yml", e);
        }
    }

    private void loadFromDisk() {
        if (!dataFile.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
        var cropsSection = yaml.getConfigurationSection("crops");
        if (cropsSection == null) return;

        for (String key : cropsSection.getKeys(false)) {
            String base = "crops." + key;
            String worldName = yaml.getString(base + ".world");
            int x = yaml.getInt(base + ".x");
            int y = yaml.getInt(base + ".y");
            int z = yaml.getInt(base + ".z");
            int stage = yaml.getInt(base + ".stage", 0);
            String planterStr = yaml.getString(base + ".planter", UUID.randomUUID().toString());
            long plantedAt = yaml.getLong(base + ".planted-at", System.currentTimeMillis());

            var world = plugin.getServer().getWorld(worldName);
            if (world == null) continue;

            Location loc = new Location(world, x, y, z);
            // Validate the block is still wheat
            if (loc.getBlock().getType() != Material.WHEAT) continue;

            UUID planterUuid;
            try { planterUuid = UUID.fromString(planterStr); }
            catch (IllegalArgumentException e) { planterUuid = UUID.randomUUID(); }

            MutatedCropData data = new MutatedCropData(loc, planterUuid, stage, plantedAt);
            activeCrops.put(key(loc), data);
            applyStageToBlock(loc, data);
        }
    }
}
