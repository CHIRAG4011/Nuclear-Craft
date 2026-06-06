package com.nuclearcraft.equipment;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.radiation.RadiationManager;
import com.nuclearcraft.radiation.RadiationSource;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks Radioactive Soil blocks and irradiates players who walk on them.
 *
 * <p>Radioactive Soil is created by the Plutonium Shovel (15% chance when digging).
 * The dug block is replaced with COARSE_DIRT and its location is tracked here.
 *
 * <h3>Effects</h3>
 * <ul>
 *   <li>Players standing directly on the soil (1 block above) receive periodic radiation.</li>
 *   <li>Green dust particles rise from the surface.</li>
 * </ul>
 */
public class RadioactiveSoilManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final RadiationManager radiationManager;

    private final Set<Location> soilLocations =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    private BukkitTask radiationTask;
    private BukkitTask particleTask;

    public RadioactiveSoilManager(JavaPlugin plugin, ConfigManager configManager,
                                  RadiationManager radiationManager) {
        this.plugin           = plugin;
        this.configManager    = configManager;
        this.radiationManager = radiationManager;
    }

    public void initialize() {
        startRadiationTask();
        startParticleTask();
        NCLogger.info("RadioactiveSoilManager initialized.");
    }

    public void shutdown() {
        if (radiationTask != null) radiationTask.cancel();
        if (particleTask  != null) particleTask.cancel();
        soilLocations.clear();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Creates a Radioactive Soil block at the given location.
     */
    public void createSoil(Location loc) {
        loc = loc.getBlock().getLocation();
        loc.getBlock().setType(Material.COARSE_DIRT);
        soilLocations.add(loc.clone());
        NCLogger.debug("Radioactive Soil created at %s", loc);
    }

    public boolean isSoil(Location loc) {
        return soilLocations.contains(loc.getBlock().getLocation());
    }

    public void removeSoil(Location loc) {
        soilLocations.remove(loc.getBlock().getLocation());
    }

    public int getSoilCount() {
        return soilLocations.size();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Tasks
    // ──────────────────────────────────────────────────────────────────────────

    private void startRadiationTask() {
        long interval = configManager.getEquipment().getLong("soil.radiation-interval-ticks", 100L);
        int  amount   = configManager.getEquipment().getInt("soil.radiation-amount", 2);

        radiationTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (soilLocations.isEmpty()) return;
            soilLocations.removeIf(loc -> {
                Block block = loc.getBlock();
                // If block was replaced naturally, drop tracking
                if (block.getType() != Material.COARSE_DIRT) return true;
                checkPlayersOnSoil(loc, amount);
                return false;
            });
        }, interval, interval);
    }

    private void startParticleTask() {
        particleTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Location loc : soilLocations) {
                loc.getWorld().spawnParticle(Particle.DUST,
                        loc.clone().add(0.5, 1.0, 0.5),
                        3, 0.4, 0.1, 0.4, 0,
                        new Particle.DustOptions(Color.fromRGB(0, 200, 50), 1.0f));
            }
        }, 20L, 20L);
    }

    private void checkPlayersOnSoil(Location soilLoc, int amount) {
        // Irradiate players standing exactly 1 block above
        Location above = soilLoc.clone().add(0.5, 1, 0.5);
        for (Player player : soilLoc.getWorld().getNearbyPlayers(above, 0.8, 1.2, 0.8)) {
            radiationManager.addRadiation(player, amount, RadiationSource.RADIOACTIVE_SOIL);
        }
    }
}
