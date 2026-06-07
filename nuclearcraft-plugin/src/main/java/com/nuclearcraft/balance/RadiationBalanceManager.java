package com.nuclearcraft.balance;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Radiation balance enforcement for Phase 13.
 *
 * Enforces:
 *   1. Maximum radiation gain per event (prevents one-tick spikes).
 *   2. Contagion cooldown per target (prevents tick-rate spam).
 *   3. Post-cure grace period (prevents instant re-infection after curing).
 *   4. Immunity window tracking from Radiation Serum.
 *
 * Phase 13 addition.
 */
public class RadiationBalanceManager {

    private final NuclearCraftPlugin plugin;
    private final ConfigManager configManager;

    private FileConfiguration cfg;

    /** UUID → tick timestamp of last contagion application to that player. */
    private final Map<UUID, Long> lastContagionTick = new HashMap<>();

    /** UUID → server tick when the post-cure grace period expires. */
    private final Map<UUID, Long> postCureGraceUntilTick = new HashMap<>();

    /** Server tick counter — incremented via the radiation task. */
    private long serverTick = 0;

    public RadiationBalanceManager(NuclearCraftPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void initialize() {
        cfg = configManager.getConfig(ConfigManager.ConfigFile.BALANCE);
        NCLogger.info("[RadiationBalanceManager] Radiation balance rules loaded.");
    }

    public void reload() {
        cfg = configManager.getConfig(ConfigManager.ConfigFile.BALANCE);
    }

    public void shutdown() {
        lastContagionTick.clear();
        postCureGraceUntilTick.clear();
    }

    // ── Tick counter (called from RadiationManager task) ─────────────────────

    /** Must be called once per radiation tick to advance the internal tick counter. */
    public void tick() {
        serverTick++;
    }

    // ── Gain cap ─────────────────────────────────────────────────────────────

    /**
     * Clamps a raw radiation gain to the configured per-event maximum.
     *
     * @param rawGain  The radiation amount that would normally be applied.
     * @return         The safe amount to actually apply.
     */
    public double clampGain(double rawGain) {
        double max = getMaxGainPerEvent();
        return Math.min(rawGain, max);
    }

    // ── Contagion gate ────────────────────────────────────────────────────────

    /**
     * Returns true if contagion radiation may be applied to this player right now.
     * Records the tick if allowed.
     */
    public boolean tryContagion(UUID targetId) {
        long cooldown = getContagionCooldownTicks();
        Long last = lastContagionTick.get(targetId);
        if (last != null && (serverTick - last) < cooldown) return false;
        lastContagionTick.put(targetId, serverTick);
        return true;
    }

    // ── Post-cure grace ───────────────────────────────────────────────────────

    /**
     * Records that a player just consumed a cure.
     * During the grace period, radiation rises above 50 pts is blocked.
     */
    public void recordCure(UUID playerId) {
        long graceTicks = getPostCureGraceTicks();
        postCureGraceUntilTick.put(playerId, serverTick + graceTicks);
        NCLogger.debug("[RadiationBalanceManager] Post-cure grace granted to %s for %d ticks", playerId, graceTicks);
    }

    /**
     * Returns true if this player is in a post-cure grace window.
     * During grace, radiation gains that would push beyond 50 pts are blocked.
     */
    public boolean isInPostCureGrace(UUID playerId) {
        Long until = postCureGraceUntilTick.get(playerId);
        if (until == null) return false;
        if (serverTick >= until) {
            postCureGraceUntilTick.remove(playerId);
            return false;
        }
        return true;
    }

    /**
     * If the player is in post-cure grace and the gain would push radiation above
     * the grace threshold, returns the clamped gain. Otherwise returns rawGain.
     */
    public double applyPostCureGrace(UUID playerId, double currentRadiation, double rawGain) {
        if (!isInPostCureGrace(playerId)) return rawGain;
        double threshold = getPostCureGraceThreshold();
        if (currentRadiation >= threshold) return 0;
        return Math.min(rawGain, threshold - currentRadiation);
    }

    // ── Config getters ────────────────────────────────────────────────────────

    /** Maximum radiation that a single event can apply. Prevents one-tick kills. */
    public double getMaxGainPerEvent() {
        return cfg.getDouble("radiation.max-gain-per-event", 100.0);
    }

    /** Minimum ticks between contagion applications to the same player. */
    public long getContagionCooldownTicks() {
        return cfg.getLong("radiation.contagion-cooldown-ticks", 40L);
    }

    /** Maximum distance in blocks that contagion can spread. */
    public double getContagionMaxRange() {
        return cfg.getDouble("radiation.contagion-max-range", 5.0);
    }

    /** Ticks after a cure before radiation can rise above the grace threshold. */
    public long getPostCureGraceTicks() {
        return cfg.getLong("radiation.post-cure-grace-ticks", 600L);
    }

    /**
     * Radiation level (pts) that radiation cannot rise above during the post-cure
     * grace period. Default 50 pts — safe zone.
     */
    public double getPostCureGraceThreshold() {
        return cfg.getDouble("radiation.post-cure-grace-threshold", 50.0);
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    /** Cleans up tracking data for a player who logged out. */
    public void onPlayerQuit(UUID playerId) {
        lastContagionTick.remove(playerId);
        postCureGraceUntilTick.remove(playerId);
    }
}
