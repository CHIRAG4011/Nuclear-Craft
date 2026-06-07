package com.nuclearcraft.admin;

import com.nuclearcraft.boss.TitanManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.data.DatabaseManager;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.farming.FarmingManager;
import com.nuclearcraft.forge.NuclearForgeManager;
import com.nuclearcraft.smelter.NuclearSmelterManager;
import com.nuclearcraft.utils.NCLogger;

import java.sql.Connection;
import java.sql.Statement;

/**
 * Phase 14: Crash recovery manager.
 *
 * On unexpected shutdown (power loss, OOM kill, server crash) machines and
 * boss state may be left in an inconsistent state in the database.
 *
 * RecoveryManager sweeps orphaned transient flags on startup and resets
 * them to safe idle values so players can interact with systems normally.
 *
 * Also provides a runtime recovery method callable via /nc fixdata to
 * reconcile divergence that builds up during a live session.
 */
public class RecoveryManager {

    private final NuclearCraftPlugin    plugin;
    private final DatabaseManager       databaseManager;
    private final PlayerDataManager     playerDataManager;
    private final NuclearSmelterManager smelterManager;
    private final NuclearForgeManager   forgeManager;
    private final FarmingManager        farmingManager;
    private final TitanManager          titanManager;

    private int startupRecoveredCount = 0;

    public RecoveryManager(NuclearCraftPlugin plugin,
                           DatabaseManager databaseManager,
                           PlayerDataManager playerDataManager,
                           NuclearSmelterManager smelterManager,
                           NuclearForgeManager forgeManager,
                           FarmingManager farmingManager,
                           TitanManager titanManager) {
        this.plugin            = plugin;
        this.databaseManager   = databaseManager;
        this.playerDataManager = playerDataManager;
        this.smelterManager    = smelterManager;
        this.forgeManager      = forgeManager;
        this.farmingManager    = farmingManager;
        this.titanManager      = titanManager;
    }

    public void initialize() {
        startupRecoveredCount = runStartupRecovery();
        if (startupRecoveredCount > 0) {
            NCLogger.warn("[RecoveryManager] Startup recovery: " + startupRecoveredCount
                    + " orphaned record(s) reset. A previous shutdown may have been unclean.");
        } else {
            NCLogger.info("[RecoveryManager] No orphaned records detected. Clean shutdown confirmed.");
        }
    }

    public void shutdown() {}

    // ── Startup recovery ──────────────────────────────────────────────────────

    private int runStartupRecovery() {
        int total = 0;
        total += recoverOrphanedPlayerFlags();
        total += validateMachineState();
        return total;
    }

    /**
     * Clears transient in-fight flags from player_data that are left set when
     * the server crashes mid-combat (e.g. titan_in_combat).
     */
    private int recoverOrphanedPlayerFlags() {
        if (databaseManager == null) return 0;
        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            int rows = 0;
            try {
                rows += stmt.executeUpdate(
                        "UPDATE player_data SET titan_in_combat = 0 WHERE titan_in_combat = 1");
                if (rows > 0) {
                    NCLogger.info("[RecoveryManager] Reset titan_in_combat flag on " + rows + " player row(s).");
                }
            } catch (Exception ignored) {
                // Column may not exist in older schema — silently skip
            }
            return rows;

        } catch (Exception e) {
            NCLogger.warn("[RecoveryManager] Could not recover player flags: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Logs the current machine state for diagnostics. Machine data is loaded
     * from disk by each manager's own initialize() — this simply audits counts.
     */
    private int validateMachineState() {
        int issues = 0;
        try {
            int smelters = smelterManager.getMachineCount();
            int forges   = forgeManager.getMachineCount();
            NCLogger.info("[RecoveryManager] Loaded machines — Smelters: " + smelters + ", Forges: " + forges);
        } catch (Exception e) {
            NCLogger.warn("[RecoveryManager] Machine state validation failed: " + e.getMessage());
            issues++;
        }
        return issues;
    }

    // ── Runtime recovery (called by /nc fixdata) ──────────────────────────────

    /**
     * Re-runs orphan recovery checks during a live session.
     *
     * @return number of records corrected
     */
    public int recoverOrphanedData() {
        int total = 0;
        total += recoverOrphanedPlayerFlags();

        if (total > 0) {
            NCLogger.info("[RecoveryManager] Live recovery corrected " + total + " record(s).");
        }
        return total;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public int getStartupRecoveredCount() {
        return startupRecoveredCount;
    }
}
