package com.nuclearcraft.utils;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Utility for spawning particles cleanly and safely.
 */
public final class ParticleUtil {

    private ParticleUtil() {}

    /**
     * Spawns a simple particle at a location for all nearby players.
     */
    public static void spawn(Location location, Particle particle, int count) {
        World world = location.getWorld();
        if (world == null) return;
        world.spawnParticle(particle, location, count, 0.3, 0.3, 0.3, 0.01);
    }

    /**
     * Spawns a particle with custom spread.
     */
    public static void spawn(Location location, Particle particle, int count,
                             double spreadX, double spreadY, double spreadZ) {
        World world = location.getWorld();
        if (world == null) return;
        world.spawnParticle(particle, location, count, spreadX, spreadY, spreadZ, 0.01);
    }

    /**
     * Spawns a colored dust particle (DUST type).
     */
    public static void spawnDust(Location location, Color color, float size, int count) {
        World world = location.getWorld();
        if (world == null) return;
        Particle.DustOptions options = new Particle.DustOptions(color, size);
        world.spawnParticle(Particle.DUST, location, count, 0.3, 0.3, 0.3, 0, options);
    }

    /**
     * Spawns a particle only visible to a specific player.
     */
    public static void spawnForPlayer(Player player, Location location, Particle particle, int count) {
        player.spawnParticle(particle, location, count, 0.3, 0.3, 0.3, 0.01);
    }

    /**
     * Draws a sphere of particles around a location.
     */
    public static void spawnSphere(Location center, double radius, Particle particle, int pointsPerRing) {
        World world = center.getWorld();
        if (world == null) return;
        for (double phi = 0; phi < Math.PI; phi += Math.PI / pointsPerRing) {
            for (double theta = 0; theta < 2 * Math.PI; theta += Math.PI / pointsPerRing) {
                double x = radius * Math.sin(phi) * Math.cos(theta);
                double y = radius * Math.cos(phi);
                double z = radius * Math.sin(phi) * Math.sin(theta);
                world.spawnParticle(particle, center.clone().add(x, y, z), 1, 0, 0, 0, 0);
            }
        }
    }

    /**
     * Parses an RGB string like "57,255,20" into a Bukkit Color.
     */
    public static Color parseColor(String rgb) {
        try {
            String[] parts = rgb.split(",");
            int r = Integer.parseInt(parts[0].trim());
            int g = Integer.parseInt(parts[1].trim());
            int b = Integer.parseInt(parts[2].trim());
            return Color.fromRGB(r, g, b);
        } catch (Exception e) {
            NCLogger.warn("Invalid color string: '" + rgb + "'. Using green.");
            return Color.fromRGB(57, 255, 20);
        }
    }
}
