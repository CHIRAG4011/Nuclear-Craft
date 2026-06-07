package com.nuclearcraft.balance;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks per-player progression milestones and provides progression timing targets.
 *
 * Monitors:
 *   - When each player first reaches each progression milestone.
 *   - Whether players are progressing faster than expected (rush detection).
 *   - Provides configurable day-range targets for admin documentation/monitoring.
 *
 * Phase 13 addition.
 */
public class ProgressionBalanceManager {

    /** Represents a progression milestone in the NuclearCraft advancement tree. */
    public enum Milestone {
        FIRST_ZOMBIE_KILL,
        FIRST_RADIOACTIVE_CORE,
        FIRST_PLUTONIUM_ORE,
        FIRST_RADIATION_DRILL_CRAFT,
        FIRST_NUCLEAR_SMELTER_CRAFT,
        FIRST_PLUTONIUM_EQUIPMENT,
        FIRST_RADIOACTIVE_FARM,
        FIRST_MK1_UPGRADE,
        FIRST_TITAN_SUMMON,
        FIRST_TITAN_KILL,
        FIRST_TITAN_EQUIPMENT
    }

    private final NuclearCraftPlugin plugin;
    private final ConfigManager configManager;

    private FileConfiguration cfg;

    /** UUID → (Milestone → timestamp when first reached) */
    private final Map<UUID, Map<Milestone, Instant>> milestoneData = new HashMap<>();

    /** UUID → session start time (used to estimate in-game days). */
    private final Map<UUID, Instant> playerSessionStart = new HashMap<>();

    public ProgressionBalanceManager(NuclearCraftPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void initialize() {
        cfg = configManager.getConfig(ConfigManager.ConfigFile.BALANCE);
        NCLogger.info("[ProgressionBalanceManager] Progression tracking active.");
    }

    public void reload() {
        cfg = configManager.getConfig(ConfigManager.ConfigFile.BALANCE);
    }

    public void shutdown() {
        milestoneData.clear();
        playerSessionStart.clear();
    }

    // ── Milestone tracking ────────────────────────────────────────────────────

    /**
     * Records that a player has reached a milestone for the first time.
     * If the milestone was already recorded this session, does nothing.
     *
     * @param player     The player
     * @param milestone  The milestone reached
     */
    public void recordMilestone(Player player, Milestone milestone) {
        UUID id = player.getUniqueId();
        Map<Milestone, Instant> playerMilestones =
                milestoneData.computeIfAbsent(id, k -> new EnumMap<>(Milestone.class));

        if (playerMilestones.containsKey(milestone)) return; // already recorded

        Instant now = Instant.now();
        playerMilestones.put(milestone, now);

        if (isLoggingEnabled()) {
            long hoursPlayed = getHoursPlayed(id, now);
            NCLogger.info(String.format("[ProgressionBalanceManager] %s reached milestone %s after %dh played.",
                    player.getName(), milestone.name(), hoursPlayed));
        }

        if (isRushDetectionEnabled()) {
            checkForRush(player, milestone, now);
        }
    }

    /**
     * Returns true if the player has reached the given milestone this session.
     */
    public boolean hasMilestone(UUID playerId, Milestone milestone) {
        var map = milestoneData.get(playerId);
        return map != null && map.containsKey(milestone);
    }

    /**
     * Returns the timestamp when the player first reached a milestone, or null.
     */
    public Instant getMilestoneTime(UUID playerId, Milestone milestone) {
        var map = milestoneData.get(playerId);
        if (map == null) return null;
        return map.get(milestone);
    }

    /**
     * Records that a player joined (or re-joined) a session.
     * Used to calculate hours-played estimates for milestone timing.
     */
    public void recordSessionStart(UUID playerId) {
        playerSessionStart.put(playerId, Instant.now());
    }

    /** Cleans up session data when a player quits. */
    public void onPlayerQuit(UUID playerId) {
        playerSessionStart.remove(playerId);
        // Keep milestoneData — it's session-scoped but useful for debug commands
    }

    // ── Progression targets ───────────────────────────────────────────────────

    /**
     * Returns the minimum expected real-hours to reach a given milestone on a
     * standard survival server. Based on balance.yml progression targets.
     * (Converts configured day targets to hours at 8 active hours/day.)
     */
    public int getExpectedMinHours(Milestone milestone) {
        int minDays = getExpectedMinDays(milestone);
        return minDays * 8;
    }

    public int getExpectedMaxHours(Milestone milestone) {
        int maxDays = getExpectedMaxDays(milestone);
        return maxDays * 8;
    }

    public int getExpectedMinDays(Milestone milestone) {
        String key = milestoneConfigKey(milestone) + ".target-day-min";
        return cfg.getInt(key, defaultMinDays(milestone));
    }

    public int getExpectedMaxDays(Milestone milestone) {
        String key = milestoneConfigKey(milestone) + ".target-day-max";
        return cfg.getInt(key, defaultMaxDays(milestone));
    }

    // ── Rush detection ────────────────────────────────────────────────────────

    private void checkForRush(Player player, Milestone milestone, Instant reachedAt) {
        long hoursPlayed = getHoursPlayed(player.getUniqueId(), reachedAt);
        int expectedMinHours = getExpectedMinHours(milestone);
        double threshold = getRushThresholdPercent();

        if (expectedMinHours > 0 && hoursPlayed < expectedMinHours * threshold) {
            NCLogger.warn(String.format(
                    "[ProgressionBalanceManager] RUSH DETECTED: %s reached %s in %dh (expected min %dh). Possible exploit.",
                    player.getName(), milestone.name(), hoursPlayed, expectedMinHours));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private long getHoursPlayed(UUID playerId, Instant now) {
        Instant start = playerSessionStart.get(playerId);
        if (start == null) return 0;
        return ChronoUnit.HOURS.between(start, now);
    }

    private String milestoneConfigKey(Milestone milestone) {
        return "progression.milestones." + milestone.name().toLowerCase().replace('_', '-');
    }

    private boolean isLoggingEnabled() {
        return cfg.getBoolean("progression.log-milestones", true);
    }

    private boolean isRushDetectionEnabled() {
        return cfg.getBoolean("progression.rush-detection", true);
    }

    private double getRushThresholdPercent() {
        return cfg.getDouble("progression.rush-threshold-percent", 0.50);
    }

    private int defaultMinDays(Milestone m) {
        return switch (m) {
            case FIRST_ZOMBIE_KILL          -> 1;
            case FIRST_RADIOACTIVE_CORE     -> 2;
            case FIRST_PLUTONIUM_ORE        -> 4;
            case FIRST_RADIATION_DRILL_CRAFT -> 4;
            case FIRST_NUCLEAR_SMELTER_CRAFT -> 7;
            case FIRST_PLUTONIUM_EQUIPMENT  -> 10;
            case FIRST_RADIOACTIVE_FARM     -> 12;
            case FIRST_MK1_UPGRADE          -> 15;
            case FIRST_TITAN_SUMMON         -> 20;
            case FIRST_TITAN_KILL           -> 25;
            case FIRST_TITAN_EQUIPMENT      -> 35;
        };
    }

    private int defaultMaxDays(Milestone m) {
        return switch (m) {
            case FIRST_ZOMBIE_KILL          -> 1;
            case FIRST_RADIOACTIVE_CORE     -> 3;
            case FIRST_PLUTONIUM_ORE        -> 7;
            case FIRST_RADIATION_DRILL_CRAFT -> 7;
            case FIRST_NUCLEAR_SMELTER_CRAFT -> 14;
            case FIRST_PLUTONIUM_EQUIPMENT  -> 18;
            case FIRST_RADIOACTIVE_FARM     -> 20;
            case FIRST_MK1_UPGRADE          -> 25;
            case FIRST_TITAN_SUMMON         -> 35;
            case FIRST_TITAN_KILL           -> 40;
            case FIRST_TITAN_EQUIPMENT      -> 50;
        };
    }
}
