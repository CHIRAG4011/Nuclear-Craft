package com.nuclearcraft.combat;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.equipment.RadiationResistanceManager;
import com.nuclearcraft.radiation.RadiationSource;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.entity.Player;

/**
 * Centralises all final radiation damage calculations for Phase 9 combat.
 *
 * Applies, in order:
 *  1. Armor resistance (via Phase 6 RadiationResistanceManager when available)
 *  2. Mastery multiplier bonus from the attacker
 *  3. Clamp to 0..1000
 *
 * All combat radiation in Phase 9 passes through {@link #calculate} so that
 * balance tweaks need only happen here.
 */
public class DamageCalculationManager {

    private final ConfigManager configManager;
    /** May be null if Phase 6 has not yet initialized. */
    private RadiationResistanceManager resistanceManager;

    public DamageCalculationManager(ConfigManager configManager,
                                     RadiationResistanceManager resistanceManager) {
        this.configManager     = configManager;
        this.resistanceManager = resistanceManager;
    }

    public void initialize() {
        NCLogger.info("DamageCalculationManager initialized.");
    }

    public void shutdown() {}

    /**
     * Allows late-binding of the resistance manager (wired after Phase 6 init).
     */
    public void setResistanceManager(RadiationResistanceManager mgr) {
        this.resistanceManager = mgr;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Core calculation
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Computes the final radiation amount to apply to {@code victim} given:
     *
     * @param rawAmount      base radiation before any reductions/bonuses
     * @param masteryBonus   fractional bonus from attacker mastery (0.0 = no bonus, 0.10 = +10%)
     * @param victim         the player receiving the radiation
     * @param source         the radiation source (used for armor resistance lookup)
     * @param damageType     the combat damage type (used for future type-specific reductions)
     * @return the final clamped radiation amount (≥ 0)
     */
    public int calculate(int rawAmount,
                         double masteryBonus,
                         Player victim,
                         RadiationSource source,
                         RadiationDamageType damageType) {
        if (rawAmount <= 0) return 0;

        // Apply attacker mastery bonus FIRST (increases effective radiation)
        double withMastery = rawAmount * (1.0 + masteryBonus);

        // Apply victim's armor resistance (reduces effective radiation)
        double afterArmor;
        if (resistanceManager != null) {
            double multiplier = resistanceManager.getMultiplier(victim, source);
            afterArmor = withMastery * multiplier;
        } else {
            afterArmor = withMastery * genericArmorReduction(victim);
        }

        int result = (int) Math.max(0, Math.min(afterArmor, 1000));
        NCLogger.debug("DamageCalc: raw=%d mastery=+%.0f%% armor=%.2f -> %d",
                rawAmount, masteryBonus * 100, (1 - afterArmor / withMastery), result);
        return result;
    }

    /**
     * Simplified overload without mastery (for aura, combo-only calculations).
     */
    public int calculate(int rawAmount, Player victim, RadiationSource source) {
        return calculate(rawAmount, 0.0, victim, source, RadiationDamageType.ENVIRONMENTAL);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Fallback armor reduction (pre-Phase-6 style)
    // ──────────────────────────────────────────────────────────────────────────

    private double genericArmorReduction(Player victim) {
        int pieces = 0;
        var inv = victim.getInventory();
        if (inv.getHelmet()     != null) pieces++;
        if (inv.getChestplate() != null) pieces++;
        if (inv.getLeggings()   != null) pieces++;
        if (inv.getBoots()      != null) pieces++;
        double perPiece = configManager.getRadiation()
                .getDouble("armor-reduction.per-piece-reduction", 0.15);
        double maxRed   = configManager.getRadiation()
                .getDouble("armor-reduction.max-reduction", 0.60);
        double reduction = Math.min(pieces * perPiece, maxRed);
        return 1.0 - reduction;
    }
}
