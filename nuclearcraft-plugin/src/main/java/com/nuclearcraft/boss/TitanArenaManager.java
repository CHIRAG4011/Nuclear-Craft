package com.nuclearcraft.boss;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.radiation.RadiationManager;
import com.nuclearcraft.radiation.RadiationSource;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.*;
import org.bukkit.entity.Giant;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Manages the Titan arena environment:
 * – Radiation hazard zones around the boss spawn point
 * – Ambient particles during the fight
 * – Ground contamination effects
 *
 * The arena persists after the battle for future content hooks.
 */
public class TitanArenaManager {

    private final JavaPlugin        plugin;
    private final ConfigManager     configManager;
    private final RadiationManager  radiationManager;

    private Location arenaCenter;
    private BukkitTask particleTask;
    private BukkitTask hazardTask;
    private boolean    active = false;

    private static final double HAZARD_RADIUS    = 15.0;
    private static final double HAZARD_RADIATION  = 2.0;
    private static final long   PARTICLE_INTERVAL = 10L;
    private static final long   HAZARD_INTERVAL   = 40L;

    public TitanArenaManager(JavaPlugin plugin, ConfigManager configManager,
                              RadiationManager radiationManager) {
        this.plugin          = plugin;
        this.configManager   = configManager;
        this.radiationManager = radiationManager;
    }

    public void initialize() {
        NCLogger.debug("TitanArenaManager initialized.");
    }

    public void activateArena(Location center) {
        this.arenaCenter = center.clone();
        this.active      = true;
        startParticleTask();
        startHazardTask();
        NCLogger.debug("Titan arena activated at " + formatLoc(center));
    }

    public void deactivateArena() {
        this.active = false;
        stopTasks();
        if (arenaCenter != null) {
            spawnDeathParticles(arenaCenter);
        }
        NCLogger.debug("Titan arena deactivated.");
    }

    public void shutdown() {
        stopTasks();
        active = false;
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private void startParticleTask() {
        if (particleTask != null) particleTask.cancel();
        particleTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!active || arenaCenter == null) return;
            World world = arenaCenter.getWorld();
            if (world == null) return;
            Random rng = new Random();
            for (int i = 0; i < 6; i++) {
                double angle  = rng.nextDouble() * Math.PI * 2;
                double radius = 4 + rng.nextDouble() * 8;
                double x = arenaCenter.getX() + Math.cos(angle) * radius;
                double z = arenaCenter.getZ() + Math.sin(angle) * radius;
                Location particleLoc = new Location(world, x, arenaCenter.getY(), z);
                world.spawnParticle(Particle.ENTITY_EFFECT, particleLoc, 3,
                        0.3, 0.5, 0.3, 0.01);
                world.spawnParticle(Particle.ASH, particleLoc, 2, 0.2, 0.3, 0.2, 0.0);
            }
        }, 0L, PARTICLE_INTERVAL);
    }

    private void startHazardTask() {
        if (hazardTask != null) hazardTask.cancel();
        hazardTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!active || arenaCenter == null) return;
            World world = arenaCenter.getWorld();
            if (world == null) return;
            for (Player player : world.getPlayers()) {
                if (player.getLocation().distanceSquared(arenaCenter) <= HAZARD_RADIUS * HAZARD_RADIUS) {
                    radiationManager.addRadiation(player, (int) HAZARD_RADIATION, RadiationSource.BOSS_ATTACK);
                }
            }
        }, 0L, HAZARD_INTERVAL);
    }

    private void stopTasks() {
        if (particleTask != null) { particleTask.cancel(); particleTask = null; }
        if (hazardTask   != null) { hazardTask.cancel();   hazardTask   = null; }
    }

    private void spawnDeathParticles(Location loc) {
        World world = loc.getWorld();
        if (world == null) return;
        world.spawnParticle(Particle.EXPLOSION_EMITTER, loc, 10, 2.0, 1.0, 2.0, 0.0);
        world.spawnParticle(Particle.ENTITY_EFFECT, loc, 100, 3.0, 2.0, 3.0, 0.05);
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 4.0f, 0.5f);
        world.playSound(loc, Sound.ENTITY_WITHER_DEATH, 4.0f, 0.7f);
    }

    public boolean isActive()          { return active; }
    public Location getArenaCenter()   { return arenaCenter; }

    private String formatLoc(Location l) {
        return String.format("%.1f,%.1f,%.1f in %s",
                l.getX(), l.getY(), l.getZ(),
                l.getWorld() != null ? l.getWorld().getName() : "null");
    }
}
