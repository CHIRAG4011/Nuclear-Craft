package com.nuclearcraft.farming;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.radiation.RadiationManager;
import com.nuclearcraft.radiation.RadiationSource;
import com.nuclearcraft.utils.NCLogger;
import com.nuclearcraft.utils.RandomUtil;
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
 * Manages Toxic Bloom — a rare mutation of the Mutated Healing Plant.
 *
 * <h3>Behaviour</h3>
 * <ul>
 *   <li>1% chance when a plant tries to advance from stage 3 → 4.</li>
 *   <li>Represented visually by an ALLIUM block (purple flower).</li>
 *   <li>Irradiates all players within 4 blocks every 2 seconds.</li>
 *   <li>Emits heavy dark-purple particle effects.</li>
 *   <li>Attracts Irradiated Zombies at night (handled in FarmingListener via BloomLocations).</li>
 * </ul>
 */
public class ToxicBloomManager {

    private static final Material BLOOM_MATERIAL = Material.ALLIUM;

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final RadiationManager radiationManager;
    private final PlayerDataManager playerDataManager;

    /** Locations of active Toxic Blooms. */
    private final Set<Location> bloomLocations =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    private BukkitTask radiationTask;
    private BukkitTask particleTask;

    public ToxicBloomManager(JavaPlugin plugin,
                              ConfigManager configManager,
                              RadiationManager radiationManager,
                              PlayerDataManager playerDataManager) {
        this.plugin            = plugin;
        this.configManager     = configManager;
        this.radiationManager  = radiationManager;
        this.playerDataManager = playerDataManager;
    }

    public void initialize() {
        startRadiationTask();
        startParticleTask();
        NCLogger.info("ToxicBloomManager initialized.");
    }

    public void shutdown() {
        if (radiationTask != null) radiationTask.cancel();
        if (particleTask  != null) particleTask.cancel();
        bloomLocations.clear();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Spawns a Toxic Bloom at the given location.
     * Replaces whatever block is there with an ALLIUM and registers it.
     */
    public void spawnBloom(Location loc) {
        loc.getBlock().setType(BLOOM_MATERIAL);
        bloomLocations.add(loc.clone());
        NCLogger.debug("Toxic Bloom spawned at %s", loc);
    }

    /**
     * Returns true if the given location is a tracked Toxic Bloom.
     */
    public boolean isBloom(Location loc) {
        return bloomLocations.contains(loc.getBlock().getLocation());
    }

    /**
     * Removes the Toxic Bloom at the given location.
     */
    public void removeBloom(Location loc) {
        bloomLocations.remove(loc.getBlock().getLocation());
    }

    /** Returns a snapshot of all active bloom locations. */
    public Set<Location> getBloomLocations() {
        return Collections.unmodifiableSet(bloomLocations);
    }

    public int getBloomCount() {
        return bloomLocations.size();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Tasks
    // ──────────────────────────────────────────────────────────────────────────

    private void startRadiationTask() {
        long interval = configManager.getFarming()
                .getLong("toxic-bloom.radiation-interval-ticks", 40L);
        int  amount   = configManager.getFarming()
                .getInt("toxic-bloom.radiation-amount", 8);
        double radius = configManager.getFarming()
                .getDouble("toxic-bloom.radius", 4.0);

        radiationTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            bloomLocations.removeIf(loc -> {
                Block block = loc.getBlock();
                if (block.getType() != BLOOM_MATERIAL) return true; // bloom was removed
                irradiateNearby(loc, amount, radius);
                return false;
            });
        }, interval, interval);
    }

    private void startParticleTask() {
        particleTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Location loc : bloomLocations) {
                if (loc.getBlock().getType() != BLOOM_MATERIAL) continue;
                Location center = loc.clone().add(0.5, 1.0, 0.5);
                // Dark purple poison particles
                loc.getWorld().spawnParticle(Particle.DUST,
                        center, 12, 0.6, 0.6, 0.6, 0,
                        new Particle.DustOptions(Color.fromRGB(0x6600CC), 1.5f));
                // Green energy particles
                loc.getWorld().spawnParticle(Particle.DUST,
                        center, 6, 0.3, 0.4, 0.3, 0,
                        new Particle.DustOptions(Color.fromRGB(0x39FF14), 1.0f));
                // Spell particles for eerie effect
                loc.getWorld().spawnParticle(Particle.WITCH,
                        center, 4, 0.4, 0.3, 0.4, 0.05);
            }
        }, 10L, 10L);
    }

    private void irradiateNearby(Location loc, int amount, double radius) {
        Location center = loc.clone().add(0.5, 1.0, 0.5);
        for (Player player : loc.getWorld().getNearbyPlayers(center, radius)) {
            radiationManager.addRadiation(player, amount, RadiationSource.TOXIC_BLOOM);
            playerDataManager.get(player).ifPresent(pd -> {
                // Stat tracked for awareness (not per-tick hit, but per interval hit)
            });
        }
    }
}
