package com.nuclearcraft.balance;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * Titan boss balance for Phase 13.
 *
 * Handles:
 *   1. Dynamic Titan health scaling by participant count.
 *   2. Contribution tier classification for reward purposes.
 *   3. Loot multipliers per contribution tier.
 *   4. Solo vs. group fight detection.
 *
 * Phase 13 addition.
 */
public class BossBalanceManager {

    private final NuclearCraftPlugin plugin;
    private final ConfigManager configManager;

    private FileConfiguration cfg;

    public BossBalanceManager(NuclearCraftPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void initialize() {
        cfg = configManager.getConfig(ConfigManager.ConfigFile.BALANCE);
        validate();
        NCLogger.info("[BossBalanceManager] Boss balance rules loaded.");
    }

    public void reload() {
        cfg = configManager.getConfig(ConfigManager.ConfigFile.BALANCE);
        validate();
    }

    public void shutdown() {}

    // ── Health scaling ────────────────────────────────────────────────────────

    /**
     * Calculates scaled Titan health for the given number of nearby participants.
     *
     * Formula: baseHealth + (extraPlayers * healthPerExtraPlayer)
     * Capped at the max player scaling count.
     *
     * @param playerCount  Number of players in the arena at spawn
     * @return             Scaled health value
     */
    public double calculateTitanHealth(int playerCount) {
        double base = getTitanBaseHealth();
        int extra = Math.max(0, Math.min(playerCount - 1, getMaxPlayersForScaling() - 1));
        double scaled = base + (extra * getHealthPerExtraPlayer());
        NCLogger.debug("[BossBalanceManager] Titan health for %d players: %.0f", playerCount, scaled);
        return scaled;
    }

    /**
     * Counts players within the scan radius of the given spawn location.
     *
     * @param spawnLoc  Titan spawn location
     * @return          Player count for health scaling
     */
    public int countNearbyPlayers(Location spawnLoc) {
        int radius = getPlayerScanRadius();
        Collection<Player> nearby = spawnLoc.getWorld().getNearbyPlayers(spawnLoc, radius);
        int count = Math.min(nearby.size(), getMaxPlayersForScaling());
        NCLogger.debug("[BossBalanceManager] %d player(s) in scan radius (%d blocks)", count, radius);
        return count;
    }

    /**
     * Returns true if there are enough players for a group fight (>= 3 participants).
     */
    public boolean isGroupFight(int playerCount) {
        return playerCount >= cfg.getInt("boss.titan.group-fight-min-players", 3);
    }

    // ── Contribution tiers ────────────────────────────────────────────────────

    /**
     * Returns the contribution tier (1 = top, 2 = mid, 3 = minor) based on the
     * player's share of total damage dealt.
     *
     * Tier 1: dealt ≥ tier1Percent of total damage
     * Tier 2: dealt ≥ tier2Percent of total damage
     * Tier 3: participated (dealt any damage)
     *
     * @param contributionPercent  0.0–1.0, player's fraction of total damage
     * @return                     Tier (1, 2, or 3), or 0 if no contribution
     */
    public int getContributionTier(double contributionPercent) {
        if (contributionPercent <= 0) return 0;
        if (contributionPercent >= getTier1Threshold()) return 1;
        if (contributionPercent >= getTier2Threshold()) return 2;
        return 3;
    }

    /**
     * Returns a loot quantity multiplier for a given contribution tier.
     * Tier 1 (top contributor) receives full bonus loot.
     *
     * @param tier  1, 2, or 3
     * @return      Multiplier (e.g. 1.5 for tier 1 = 50% more loot)
     */
    public double getLootMultiplierForTier(int tier) {
        return switch (tier) {
            case 1  -> cfg.getDouble("boss.titan.loot-multiplier.tier-1", 1.5);
            case 2  -> cfg.getDouble("boss.titan.loot-multiplier.tier-2", 1.2);
            case 3  -> cfg.getDouble("boss.titan.loot-multiplier.tier-3", 1.0);
            default -> 1.0;
        };
    }

    /**
     * Applies a loot multiplier to a base fragment count for the given tier.
     */
    public int applyLootMultiplier(int baseCount, int tier) {
        return (int) Math.round(baseCount * getLootMultiplierForTier(tier));
    }

    // ── Config getters ────────────────────────────────────────────────────────

    public double getTitanBaseHealth() {
        return cfg.getDouble("boss.titan.base-health", 800.0);
    }

    public double getHealthPerExtraPlayer() {
        return cfg.getDouble("boss.titan.health-per-extra-player", 100.0);
    }

    public int getMaxPlayersForScaling() {
        return cfg.getInt("boss.titan.max-players-for-scaling", 10);
    }

    public int getPlayerScanRadius() {
        return cfg.getInt("boss.titan.player-scan-radius", 80);
    }

    public double getTier1Threshold() {
        return cfg.getDouble("boss.titan.contribution.tier-1-percent", 0.40);
    }

    public double getTier2Threshold() {
        return cfg.getDouble("boss.titan.contribution.tier-2-percent", 0.20);
    }

    public double getTier3Threshold() {
        return cfg.getDouble("boss.titan.contribution.tier-3-percent", 0.05);
    }

    public int getTitanAbilityCooldownTicks() {
        return cfg.getInt("boss.titan.ability-cooldown-ticks", 100);
    }

    public int getTitanMaxSummons() {
        return cfg.getInt("boss.titan.max-summons", 6);
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private void validate() {
        if (getHealthPerExtraPlayer() < 0)
            NCLogger.warn("[BossBalanceManager] health-per-extra-player < 0; Titan will be weaker in groups.");
        if (getTier1Threshold() <= getTier2Threshold())
            NCLogger.warn("[BossBalanceManager] contribution tier-1-percent <= tier-2-percent. Check config.");
        if (getTitanBaseHealth() < 200)
            NCLogger.warn("[BossBalanceManager] Titan base health very low (" + getTitanBaseHealth() + "). Intended?");
    }
}
