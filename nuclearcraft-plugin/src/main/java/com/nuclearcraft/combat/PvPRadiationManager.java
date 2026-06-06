package com.nuclearcraft.combat;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PvP-specific radiation routing and validity checks for Phase 9.
 *
 * Responsibilities:
 * <ul>
 *   <li>Determine whether an attacker→victim pair is a valid PvP radiation target
 *       (prevents team-friendly fire, self-damage, or spectator hits).</li>
 *   <li>Record the most recent PvP radiation attacker per victim for radiation kill attribution.</li>
 *   <li>Manage the radiation surge cooldown between player pairs.</li>
 * </ul>
 */
public class PvPRadiationManager {

    private final ConfigManager configManager;

    private boolean surgeEnabled;
    private int surgeMinStage;
    private double surgeTriggerChance;
    private int surgeBothAmount;
    private int surgeNearbyAmount;
    private double surgeNearbyRadius;
    private long surgeCooldownMs;
    private long killAttributionWindowMs;

    /**
     * Maps victim UUID → the last PvP radiation attacker's UUID + timestamp.
     */
    private final Map<UUID, AttackerRecord> lastAttackerMap = new ConcurrentHashMap<>();

    /**
     * Maps "attackerUUID-victimUUID" pair key → last surge timestamp.
     */
    private final Map<String, Long> surgeCooldowns = new ConcurrentHashMap<>();

    public PvPRadiationManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void initialize() {
        loadConfig();
        NCLogger.info("PvPRadiationManager initialized.");
    }

    public void shutdown() {
        lastAttackerMap.clear();
        surgeCooldowns.clear();
    }

    private void loadConfig() {
        var cfg = configManager.getCombat();
        surgeEnabled         = cfg.getBoolean("surge.enabled", true);
        surgeMinStage        = cfg.getInt("surge.min-stage-both", 2);
        surgeTriggerChance   = cfg.getDouble("surge.trigger-chance", 0.05);
        surgeBothAmount      = cfg.getInt("surge.both-player-amount", 30);
        surgeNearbyAmount    = cfg.getInt("surge.nearby-amount", 15);
        surgeNearbyRadius    = cfg.getDouble("surge.nearby-radius", 8.0);
        surgeCooldownMs      = cfg.getLong("surge.cooldown-ms", 15000L);
        killAttributionWindowMs = cfg.getLong("kills.attribution-window-ms", 30000L);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Target validity
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Returns true if this attacker → victim pair should have PvP radiation applied.
     * <ul>
     *   <li>Both players must be online.</li>
     *   <li>They must not be the same player.</li>
     *   <li>Neither may be in spectator mode.</li>
     *   <li>Team-check: future team system hook (always true for now).</li>
     * </ul>
     */
    public boolean isValidPvPTarget(Player attacker, Player victim) {
        if (attacker == null || victim == null)       return false;
        if (attacker.getUniqueId().equals(victim.getUniqueId())) return false;
        if (!attacker.isOnline() || !victim.isOnline()) return false;
        if (attacker.getGameMode() == org.bukkit.GameMode.SPECTATOR) return false;
        if (victim.getGameMode()   == org.bukkit.GameMode.SPECTATOR) return false;
        return !isSameTeam(attacker, victim);
    }

    /**
     * Naive team-check using Bukkit scoreboard teams.
     * Returns true if both players are on the same non-empty team.
     */
    private boolean isSameTeam(Player a, Player b) {
        var scoreboard = a.getServer().getScoreboardManager().getMainScoreboard();
        var teamA = scoreboard.getEntryTeam(a.getName());
        var teamB = scoreboard.getEntryTeam(b.getName());
        if (teamA == null || teamB == null) return false;
        return teamA.equals(teamB);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Attacker attribution
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Records that {@code attacker} applied radiation to {@code victim}.
     * Used later for kill attribution.
     */
    public void recordRadiationAttacker(Player attacker, Player victim) {
        lastAttackerMap.put(victim.getUniqueId(),
                new AttackerRecord(attacker.getUniqueId(), System.currentTimeMillis()));
    }

    /**
     * Returns the UUID of the player who last applied PvP radiation to {@code victim},
     * or null if no such attacker is recorded or the attribution window has expired.
     */
    public UUID getLastAttacker(Player victim) {
        AttackerRecord rec = lastAttackerMap.get(victim.getUniqueId());
        if (rec == null) return null;
        if (System.currentTimeMillis() - rec.timestamp > killAttributionWindowMs) {
            lastAttackerMap.remove(victim.getUniqueId());
            return null;
        }
        return rec.attackerUuid;
    }

    public void clearAttacker(Player victim) {
        lastAttackerMap.remove(victim.getUniqueId());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Radiation surge
    // ──────────────────────────────────────────────────────────────────────────

    public boolean isSurgeEnabled()          { return surgeEnabled; }
    public int getSurgeMinStage()            { return surgeMinStage; }
    public double getSurgeTriggerChance()    { return surgeTriggerChance; }
    public int getSurgeBothAmount()          { return surgeBothAmount; }
    public int getSurgeNearbyAmount()        { return surgeNearbyAmount; }
    public double getSurgeNearbyRadius()     { return surgeNearbyRadius; }

    /**
     * Returns true if a surge between this pair is not on cooldown.
     * Records the cooldown if the check passes.
     */
    public boolean checkAndRecordSurgeCooldown(Player a, Player b) {
        String key = surgePairKey(a, b);
        Long last = surgeCooldowns.get(key);
        long now = System.currentTimeMillis();
        if (last != null && now - last < surgeCooldownMs) return false;
        surgeCooldowns.put(key, now);
        return true;
    }

    private String surgePairKey(Player a, Player b) {
        // Canonical order so A↔B and B↔A share the same key
        String u1 = a.getUniqueId().toString();
        String u2 = b.getUniqueId().toString();
        return u1.compareTo(u2) < 0 ? u1 + "-" + u2 : u2 + "-" + u1;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Data
    // ──────────────────────────────────────────────────────────────────────────

    private static final class AttackerRecord {
        final UUID attackerUuid;
        final long timestamp;

        AttackerRecord(UUID attackerUuid, long timestamp) {
            this.attackerUuid = attackerUuid;
            this.timestamp    = timestamp;
        }
    }
}
