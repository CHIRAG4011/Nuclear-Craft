package com.nuclearcraft.combat;

import com.nuclearcraft.advancements.AdvancementManager;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.entity.Player;

/**
 * Centralises all Phase 9 combat statistics writes.
 *
 * Rather than scattering {@code playerDataManager.get(...).ifPresent(...)} calls
 * across every combat manager, this class owns all stat updates and checks
 * advancement thresholds after each update.
 */
public class CombatStatisticsManager {

    private final PlayerDataManager playerDataManager;
    private final AdvancementManager advancementManager;

    public CombatStatisticsManager(PlayerDataManager playerDataManager,
                                    AdvancementManager advancementManager) {
        this.playerDataManager  = playerDataManager;
        this.advancementManager = advancementManager;
    }

    public void initialize() {
        NCLogger.info("CombatStatisticsManager initialized.");
    }

    public void shutdown() {}

    // ──────────────────────────────────────────────────────────────────────────
    // PvP radiation inflicted
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Records radiation inflicted on a victim by an attacker in PvP.
     * Awards the Radioactive Warrior advancement on first record.
     */
    public void recordPvPRadiation(Player attacker, int amount) {
        playerDataManager.get(attacker.getUniqueId()).ifPresent(data -> {
            data.addPvPRadiationInflicted(amount);
            data.setDirty(true);

            if (data.getTotalPvPRadiationInflicted() >= 1 &&
                    !advancementManager.hasEarned(attacker, AdvancementManager.Advancement.RADIOACTIVE_WARRIOR)) {
                advancementManager.award(attacker, AdvancementManager.Advancement.RADIOACTIVE_WARRIOR);
            }
            if (data.getTotalPvPRadiationInflicted() >= 10_000 &&
                    !advancementManager.hasEarned(attacker, AdvancementManager.Advancement.RADIATION_MASTER)) {
                advancementManager.award(attacker, AdvancementManager.Advancement.RADIATION_MASTER);
            }
        });
    }

    /**
     * Records that {@code attacker} infected {@code victim} (raised to stage ≥ 1 via PvP).
     */
    public void recordPvPInfection(Player attacker, Player victim) {
        advancementManager.award(attacker, AdvancementManager.Advancement.CONTAMINATOR);
        NCLogger.debug("%s infected %s via PvP radiation.", attacker.getName(), victim.getName());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Arrow hits
    // ──────────────────────────────────────────────────────────────────────────

    public void recordArrowHit(Player attacker, int radiationApplied) {
        playerDataManager.get(attacker.getUniqueId()).ifPresent(data -> {
            data.incrementPvPArrowHits();
            data.addPvPRadiationInflicted(radiationApplied);
            data.setDirty(true);
        });
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Aura damage
    // ──────────────────────────────────────────────────────────────────────────

    public void recordAuraDamage(Player auraOwner, int amount) {
        playerDataManager.get(auraOwner.getUniqueId()).ifPresent(data -> {
            data.addAuraDamageDealt(amount);
            data.setDirty(true);
        });
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Kill tracking
    // ──────────────────────────────────────────────────────────────────────────

    public void recordPvPKill(Player killer, KillType killType) {
        playerDataManager.get(killer.getUniqueId()).ifPresent(data -> {
            data.incrementPvPKills();
            switch (killType) {
                case RADIATION -> data.incrementPvPRadiationKills();
                case ARROW     -> {
                    data.incrementPvPRadiationKills();
                    data.incrementPvPArrowKills();
                    advancementManager.award(killer, AdvancementManager.Advancement.NUCLEAR_ARCHER);
                }
                case AURA      -> {
                    data.incrementPvPRadiationKills();
                    data.incrementPvPAuraKills();
                }
            }
            data.setDirty(true);
        });
    }

    public enum KillType { RADIATION, ARROW, AURA, MELEE }
}
