package com.nuclearcraft.zombies;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.radiation.RadiationManager;
import com.nuclearcraft.radiation.RadiationSource;
import com.nuclearcraft.utils.NCLogger;
import com.nuclearcraft.utils.RandomUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Radiation Clouds spawned on Irradiated Zombie death.
 *
 * A cloud:
 *   - Spawns with 20% probability on irradiated zombie death.
 *   - Lasts 10 seconds (configurable).
 *   - Has a 3-block radius (configurable).
 *   - Applies radiation every second to players inside it.
 *   - Emits green particle effects every tick.
 *
 * One task ticks all active clouds every 20 ticks (1 second).
 * Clouds are stored in a UUID-keyed map for fast removal.
 */
public class RadiationCloudManager {

    private record RadiationCloud(UUID id, Location center, long expiryMs, double radius, int radiationPerSecond) {}

    private final NuclearCraftPlugin plugin;
    private final ConfigManager configManager;
    private final RadiationManager radiationManager;
    private final IrradiatedZombieManager zombieManager;

    private final Map<UUID, RadiationCloud> activeClouds = new ConcurrentHashMap<>();
    private BukkitTask cloudTask;

    // Stat counter
    private final Map<UUID, Integer> playerCloudSurvivals = new ConcurrentHashMap<>();

    public RadiationCloudManager(NuclearCraftPlugin plugin, ConfigManager configManager,
                                  RadiationManager radiationManager, IrradiatedZombieManager zombieManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.radiationManager = radiationManager;
        this.zombieManager = zombieManager;
    }

    public void initialize() {
        startCloudTask();
        NCLogger.info("RadiationCloudManager initialized.");
    }

    public void shutdown() {
        if (cloudTask != null) cloudTask.cancel();
        activeClouds.clear();
        NCLogger.info("RadiationCloudManager shut down.");
    }

    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Called from ZombieSpawnListener on EntityDeathEvent for irradiated zombies.
     */
    public void handleZombieDeath(Zombie zombie) {
        double spawnChance = configManager.getZombies().getDouble("cloud.spawn-chance", 0.20);
        if (!RandomUtil.chance(spawnChance)) return;

        int duration = configManager.getZombies().getInt("cloud.duration-seconds", 10);
        double radius = configManager.getZombies().getDouble("cloud.radius", 3.0);
        int radPerSecond = configManager.getZombies().getInt("cloud.radiation-per-second", 5);

        Location loc = zombie.getLocation();
        UUID cloudId = UUID.randomUUID();
        long expiryMs = System.currentTimeMillis() + (duration * 1000L);

        activeClouds.put(cloudId, new RadiationCloud(cloudId, loc.clone(), expiryMs, radius, radPerSecond));
        NCLogger.debug("Spawned radiation cloud at %s (duration=%ds, radius=%.1f)", loc.toVector(), duration, radius);
    }

    /**
     * Returns how many times the player has been inside a cloud this session.
     */
    public int getPlayerCloudSurvivals(UUID playerId) {
        return playerCloudSurvivals.getOrDefault(playerId, 0);
    }

    public int getActiveCount() { return activeClouds.size(); }

    // ──────────────────────────────────────────────────────────────────────────

    private void startCloudTask() {
        cloudTask = new BukkitRunnable() {
            // Track which players are currently inside any cloud for "survived" counting
            private final Set<UUID> playersInCloud = new HashSet<>();

            @Override
            public void run() {
                long now = System.currentTimeMillis();
                Set<UUID> expired = new HashSet<>();

                for (RadiationCloud cloud : activeClouds.values()) {
                    if (now >= cloud.expiryMs()) {
                        expired.add(cloud.id());
                        continue;
                    }
                    tickCloud(cloud, playersInCloud);
                }

                // Remove expired
                expired.forEach(id -> {
                    activeClouds.remove(id);
                    NCLogger.debug("Radiation cloud %s expired.", id);
                });
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void tickCloud(RadiationCloud cloud, Set<UUID> previouslyInCloud) {
        Location center = cloud.center();
        double radiusSq = cloud.radius() * cloud.radius();
        boolean emitParticles = configManager.getZombies().getBoolean("cloud.particles", true);

        // Emit particles (at cloud center, visible to all nearby)
        if (emitParticles && center.getWorld() != null) {
            center.getWorld().spawnParticle(
                    Particle.SPORE_BLOSSOM_AIR,
                    center.clone().add(0, 1, 0),
                    30,         // count
                    cloud.radius() * 0.8, 1.0, cloud.radius() * 0.8,
                    0.0
            );
        }

        // Apply radiation to players inside
        if (center.getWorld() == null) return;
        for (Player player : center.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(center) <= radiusSq) {
                radiationManager.addRadiation(player, cloud.radiationPerSecond(), RadiationSource.RADIATION_CLOUD);
                previouslyInCloud.add(player.getUniqueId());
            } else {
                // Player just left — count as survived if they were inside before
                if (previouslyInCloud.remove(player.getUniqueId())) {
                    playerCloudSurvivals.merge(player.getUniqueId(), 1, Integer::sum);
                }
            }
        }
    }
}
