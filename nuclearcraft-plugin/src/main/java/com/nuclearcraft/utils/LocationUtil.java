package com.nuclearcraft.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Utility for serializing and deserializing Bukkit Locations.
 */
public final class LocationUtil {

    private LocationUtil() {}

    /**
     * Serializes a Location to a compact string: "world:x:y:z:yaw:pitch"
     */
    public static String serialize(Location loc) {
        if (loc == null || loc.getWorld() == null) return "";
        return loc.getWorld().getName() + ":"
                + loc.getBlockX() + ":"
                + loc.getBlockY() + ":"
                + loc.getBlockZ() + ":"
                + loc.getYaw() + ":"
                + loc.getPitch();
    }

    /**
     * Deserializes a Location from the compact string format.
     * Returns null if the string is malformed or the world is not loaded.
     */
    public static Location deserialize(String s) {
        if (s == null || s.isBlank()) return null;
        String[] parts = s.split(":");
        if (parts.length < 4) return null;
        try {
            World world = Bukkit.getWorld(parts[0]);
            if (world == null) {
                NCLogger.warn("Cannot deserialize location — world '" + parts[0] + "' not loaded.");
                return null;
            }
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            float yaw = parts.length > 4 ? Float.parseFloat(parts[4]) : 0f;
            float pitch = parts.length > 5 ? Float.parseFloat(parts[5]) : 0f;
            return new Location(world, x, y, z, yaw, pitch);
        } catch (NumberFormatException e) {
            NCLogger.warn("Failed to deserialize location from string: " + s);
            return null;
        }
    }

    /**
     * Returns the block-level center of a location.
     */
    public static Location blockCenter(Location loc) {
        return new Location(loc.getWorld(),
                loc.getBlockX() + 0.5,
                loc.getBlockY() + 0.5,
                loc.getBlockZ() + 0.5,
                loc.getYaw(), loc.getPitch());
    }

    /**
     * Returns true if the two locations are in the same block.
     */
    public static boolean sameBlock(Location a, Location b) {
        return a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ()
                && a.getWorld() != null
                && a.getWorld().equals(b.getWorld());
    }
}
