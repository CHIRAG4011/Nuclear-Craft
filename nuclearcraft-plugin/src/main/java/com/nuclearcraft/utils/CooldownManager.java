package com.nuclearcraft.utils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe per-player cooldown tracker.
 * Cooldowns are keyed by a string ID, allowing multiple independent cooldowns per player.
 */
public class CooldownManager {

    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    private String makeKey(UUID uuid, String cooldownId) {
        return uuid + ":" + cooldownId;
    }

    /**
     * Sets a cooldown for the given player and cooldown ID.
     *
     * @param uuid       The player UUID
     * @param cooldownId A unique identifier for the cooldown type
     * @param durationMs Duration in milliseconds
     */
    public void setCooldown(UUID uuid, String cooldownId, long durationMs) {
        cooldowns.put(makeKey(uuid, cooldownId), System.currentTimeMillis() + durationMs);
    }

    /**
     * Returns true if the player is currently on cooldown.
     */
    public boolean isOnCooldown(UUID uuid, String cooldownId) {
        Long expiry = cooldowns.get(makeKey(uuid, cooldownId));
        if (expiry == null) return false;
        if (System.currentTimeMillis() >= expiry) {
            cooldowns.remove(makeKey(uuid, cooldownId));
            return false;
        }
        return true;
    }

    /**
     * Returns remaining cooldown in milliseconds, or 0 if not on cooldown.
     */
    public long getRemainingMs(UUID uuid, String cooldownId) {
        Long expiry = cooldowns.get(makeKey(uuid, cooldownId));
        if (expiry == null) return 0L;
        long remaining = expiry - System.currentTimeMillis();
        return Math.max(0L, remaining);
    }

    /**
     * Returns remaining cooldown in seconds (rounded up), or 0.
     */
    public long getRemainingSeconds(UUID uuid, String cooldownId) {
        return (long) Math.ceil(getRemainingMs(uuid, cooldownId) / 1000.0);
    }

    /**
     * Clears a specific cooldown for a player.
     */
    public void clearCooldown(UUID uuid, String cooldownId) {
        cooldowns.remove(makeKey(uuid, cooldownId));
    }

    /**
     * Clears all cooldowns for a player.
     */
    public void clearAllCooldowns(UUID uuid) {
        String prefix = uuid + ":";
        cooldowns.keySet().removeIf(k -> k.startsWith(prefix));
    }

    /**
     * Removes all expired entries. Call periodically to prevent memory leaks.
     */
    public void purgeExpired() {
        long now = System.currentTimeMillis();
        cooldowns.entrySet().removeIf(e -> e.getValue() <= now);
    }

    public int size() {
        return cooldowns.size();
    }
}
