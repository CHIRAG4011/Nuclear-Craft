package com.nuclearcraft.farming;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.data.PlayerData;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.utils.ColorUtil;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Manages temporary radiation immunity granted by the Radiation Serum.
 *
 * <p>Immunity state is stored in {@link PlayerData#getImmunityTimerEndMs()},
 * which is already checked by {@link com.nuclearcraft.radiation.RadiationManager#addRadiation}.
 * The RadiationManager has been updated to only block IMMUNITY_BLOCKED sources
 * (environmental), leaving boss attacks and admin commands unaffected.
 *
 * <p>This manager provides a clean API for granting, revoking, and querying immunity.
 */
public class RadiationImmunityManager {

    private final PlayerDataManager playerDataManager;
    private final ConfigManager configManager;

    public RadiationImmunityManager(PlayerDataManager playerDataManager,
                                     ConfigManager configManager) {
        this.playerDataManager = playerDataManager;
        this.configManager     = configManager;
    }

    public void initialize() {
        NCLogger.info("RadiationImmunityManager initialized.");
    }

    public void shutdown() {}

    // ──────────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Grants radiation immunity to the player for the configured serum duration.
     * If the player is already immune, refreshes (extends) the timer.
     *
     * @param player the player to grant immunity to
     */
    public void grantImmunity(Player player) {
        long durationMinutes = configManager.getFarming()
                .getLong("cure.serum.immunity-duration-minutes", 10L);
        long expiryMs = System.currentTimeMillis()
                + TimeUnit.MINUTES.toMillis(durationMinutes);

        playerDataManager.get(player).ifPresent(pd -> {
            pd.setImmunityTimerEndMs(expiryMs);
            pd.markDirty();
        });

        NCLogger.debug("Immunity granted to %s for %d minutes", player.getName(), durationMinutes);
    }

    /**
     * Revokes radiation immunity from the player immediately.
     */
    public void revokeImmunity(Player player) {
        playerDataManager.get(player).ifPresent(pd -> {
            pd.setImmunityTimerEndMs(0L);
            pd.markDirty();
        });
    }

    /**
     * Returns true if the player currently has radiation immunity.
     */
    public boolean isImmune(Player player) {
        Optional<PlayerData> opt = playerDataManager.get(player);
        return opt.map(PlayerData::isImmune).orElse(false);
    }

    /**
     * Returns the remaining immunity time in seconds, or 0 if not immune.
     */
    public long getRemainingSeconds(Player player) {
        return playerDataManager.get(player)
                .map(pd -> Math.max(0L,
                        (pd.getImmunityTimerEndMs() - System.currentTimeMillis()) / 1000L))
                .orElse(0L);
    }
}
