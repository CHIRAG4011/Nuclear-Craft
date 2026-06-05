package com.nuclearcraft.utils;

/**
 * General math utilities for NuclearCraft calculations.
 */
public final class MathHelper {

    private MathHelper() {}

    /**
     * Clamps a value between min and max (inclusive).
     */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Linearly interpolates between a and b by t (0.0 – 1.0).
     */
    public static double lerp(double a, double b, double t) {
        return a + (b - a) * clamp(t, 0, 1);
    }

    /**
     * Maps a value from one range to another.
     */
    public static double remap(double value, double inMin, double inMax, double outMin, double outMax) {
        if (inMax == inMin) return outMin;
        double t = (value - inMin) / (inMax - inMin);
        return lerp(outMin, outMax, t);
    }

    /**
     * Returns the percentage of value relative to max (0–100, clamped).
     */
    public static double percent(double value, double max) {
        if (max <= 0) return 0;
        return clamp((value / max) * 100.0, 0, 100);
    }

    /**
     * Rounds a double to a given number of decimal places.
     */
    public static double round(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }

    /**
     * Returns true if the value is approximately equal to target within epsilon.
     */
    public static boolean approxEquals(double a, double b, double epsilon) {
        return Math.abs(a - b) < epsilon;
    }

    /**
     * Calculates the distance squared between two 3D points.
     * Use instead of distance() to avoid the sqrt when comparing ranges.
     */
    public static double distanceSquared(double x1, double y1, double z1,
                                         double x2, double y2, double z2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        double dz = z1 - z2;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Converts ticks to seconds.
     */
    public static double ticksToSeconds(long ticks) {
        return ticks / 20.0;
    }

    /**
     * Converts seconds to ticks.
     */
    public static long secondsToTicks(double seconds) {
        return Math.round(seconds * 20.0);
    }

    /**
     * Converts minutes to ticks.
     */
    public static long minutesToTicks(double minutes) {
        return secondsToTicks(minutes * 60.0);
    }
}
