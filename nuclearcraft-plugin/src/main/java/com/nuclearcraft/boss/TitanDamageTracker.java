package com.nuclearcraft.boss;

import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks damage dealt to the Titan by each player.
 * Used to determine kill credit and scale contribution rewards.
 * Thread-safe via ConcurrentHashMap (damage events may fire off-thread).
 */
public class TitanDamageTracker {

    private final Map<UUID, Double> damageMap   = new ConcurrentHashMap<>();
    private final Map<UUID, Long>   lastHitTime = new ConcurrentHashMap<>();

    public void recordDamage(Player player, double amount) {
        if (amount <= 0) return;
        damageMap.merge(player.getUniqueId(), amount, Double::sum);
        lastHitTime.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public double getDamageBy(UUID uuid) {
        return damageMap.getOrDefault(uuid, 0.0);
    }

    public double getTotalDamage() {
        return damageMap.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    public double getContributionPercent(UUID uuid) {
        double total = getTotalDamage();
        if (total <= 0) return 0.0;
        return getDamageBy(uuid) / total;
    }

    /** @return the player UUID with the highest damage dealt, or empty if no one attacked. */
    public Optional<UUID> getTopDamager() {
        return damageMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);
    }

    /** @return unmodifiable snapshot of damage contributions. */
    public Map<UUID, Double> snapshot() {
        return Collections.unmodifiableMap(new HashMap<>(damageMap));
    }

    /** Returns UUIDs of players who have dealt damage (i.e., contributed). */
    public Set<UUID> getParticipants() {
        return Collections.unmodifiableSet(damageMap.keySet());
    }

    /** Removes players who haven't hit the titan in {@code timeoutMs} milliseconds. */
    public void purgeInactive(long timeoutMs) {
        long now = System.currentTimeMillis();
        lastHitTime.entrySet().removeIf(e -> (now - e.getValue()) > timeoutMs);
        damageMap.keySet().retainAll(lastHitTime.keySet());
    }

    public void reset() {
        damageMap.clear();
        lastHitTime.clear();
    }
}
