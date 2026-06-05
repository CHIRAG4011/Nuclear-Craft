package com.nuclearcraft.utils;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility for random operations used across NuclearCraft systems.
 * Uses ThreadLocalRandom for thread safety without lock contention.
 */
public final class RandomUtil {

    private RandomUtil() {}

    /**
     * Returns the shared ThreadLocalRandom instance.
     */
    public static Random random() {
        return ThreadLocalRandom.current();
    }

    /**
     * Returns a random integer between min (inclusive) and max (inclusive).
     */
    public static int nextInt(int min, int max) {
        if (min >= max) return min;
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    /**
     * Returns a random double between min (inclusive) and max (exclusive).
     */
    public static double nextDouble(double min, double max) {
        if (min >= max) return min;
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    /**
     * Returns true with the given probability (0.0 – 1.0).
     * e.g., chance(0.15) has a 15% chance of returning true.
     */
    public static boolean chance(double probability) {
        return ThreadLocalRandom.current().nextDouble() < probability;
    }

    /**
     * Returns true with a percent probability (0 – 100).
     * e.g., percentChance(15) has a 15% chance of returning true.
     */
    public static boolean percentChance(double percent) {
        return chance(percent / 100.0);
    }

    /**
     * Returns a random element from the list, or null if empty.
     */
    public static <T> T pick(List<T> list) {
        if (list == null || list.isEmpty()) return null;
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    /**
     * Returns a random element from the collection, or null if empty.
     */
    public static <T> T pick(Collection<T> collection) {
        if (collection == null || collection.isEmpty()) return null;
        int index = ThreadLocalRandom.current().nextInt(collection.size());
        int i = 0;
        for (T element : collection) {
            if (i++ == index) return element;
        }
        return null;
    }

    /**
     * Returns a random double between -spread and +spread.
     */
    public static double spread(double spread) {
        return ThreadLocalRandom.current().nextDouble(-spread, spread);
    }

    /**
     * Rolls a random integer drop count between min and max (both inclusive).
     */
    public static int rollAmount(int min, int max) {
        return nextInt(min, max);
    }

    /**
     * Rolls with Fortune bonus — each extra level adds an independent re-roll,
     * keeping the higher result.
     */
    public static int rollWithFortune(int min, int max, int fortuneLevel) {
        int result = rollAmount(min, max);
        for (int i = 0; i < fortuneLevel; i++) {
            result = Math.max(result, rollAmount(min, max));
        }
        return result;
    }
}
