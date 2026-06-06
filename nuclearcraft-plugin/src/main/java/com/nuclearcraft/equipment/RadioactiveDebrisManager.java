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
 * Tracks Radioactive Debris blocks and applies radiation to nearby players.
 *
 * <p>Radioactive Debris is created by the Plutonium Pickaxe (10% chance when mining).
 * The mined block is replaced with GRAVEL and its location is tracked here.
 *
 * <h3>Effects</h3>
 * <ul>
 *   <li>Nearby players (within configurable radius) receive periodic radiation.</li>
 *   <li>Green dust particles emit from the block periodically.</li>
 *   <li>Block can be removed by breaking it (tracking cleanup happens in {@link EquipmentListener}).</li>
 * </ul>
 */
public class RadioactiveDebrisManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final RadiationManager radiationManager;

    /** Set of block locations currently tracked as Radioactive Debris. */
    private final Set<Location> debrisLocations =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    private BukkitTask radiationTask;
    private BukkitTask particleTask;

    public RadioactiveDebrisManager(JavaPlugin plugin, ConfigManager configManager,
                                    RadiationManager radiationManager) {
        this.plugin           = plugin;
        this.configManager    = configManager;
        this.radiationManager = radiationManager;
    }

    public void initialize() {
        startRadiationTask();
        startParticleTask();
        NCLogger.info("RadioactiveDebrisManager initialized.");
    }

    public void shutdown() {
        if (radiationTask != null) radiationTask.cancel();
        if (particleTask  != null) particleTask.cancel();
        debrisLocations.clear();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Registers a block location as Radioactive Debris and places a GRAVEL block.
     */
    public void createDebris(Location loc) {
        loc = loc.getBlock().getLocation(); // normalize to block coords
        loc.getBlock().setType(Material.GRAVEL);
        debrisLocations.add(loc.clone());
        NCLogger.debug("Radioactive Debris created at %s", loc);
    }

    /**
     * Returns true if the given location is a tracked Radioactive Debris block.
     */
    public boolean isDebris(Location loc) {
        return debrisLocations.contains(loc.getBlock().getLocation());
    }

    /**
     * Removes the debris tracking for a location (called when block is broken).
     */
    public void removeDebris(Location loc) {
        debrisLocations.remove(loc.getBlock().getLocation());
    }

    public int getDebrisCount() {
        return debrisLocations.size();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Tasks
    // ──────────────────────────────────────────────────────────────────────────

    private void startRadiationTask() {
        long interval = configManager.getEquipment().getLong("debris.radiation-interval-ticks", 100L);
        int  amount   = configManager.getEquipment().getInt("debris.radiation-amount", 3);
        double radius = configManager.getEquipment().getDouble("debris.radiation-radius", 2.0);

        radiationTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (debrisLocations.isEmpty()) return;
            debrisLocations.removeIf(loc -> {
                Block block = loc.getBlock();
                if (block.getType() != Material.GRAVEL) return true; // block changed — drop tracking
                irradiateNearby(block.getLocation().add(0.5, 0.5, 0.5), radius, amount);
                return false;
            });
        }, interval, interval);
    }

    private void startParticleTask() {
        if (!configManager.getEquipment().getBoolean("debris.particles-enabled", true)) return;
        long interval = configManager.getEquipment().getLong("debris.particle-interval-ticks", 20L);

        particleTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Location loc : debrisLocations) {
                Location center = loc.clone().add(0.5, 0.5, 0.5);
                center.getWorld().spawnParticle(Particle.DUST,
                        center, 5, 0.3, 0.3, 0.3, 0,
                        new Particle.DustOptions(Color.LIME, 1.0f));
            }
        }, interval, interval);
    }

    private void irradiateNearby(Location center, double radius, int amount) {
        for (Player player : center.getWorld().getNearbyPlayers(center, radius)) {
            radiationManager.addRadiation(player, amount, RadiationSource.RADIOACTIVE_DEBRIS);
        }
    }
}
