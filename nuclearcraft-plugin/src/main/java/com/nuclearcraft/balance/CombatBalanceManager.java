package com.nuclearcraft.balance;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * PvP and combat balance enforcement for Phase 13.
 *
 * Enforces:
 *   1. Max radiation bonus damage per hit (prevents radiation one-shots).
 *   2. Infection stack limit per fight (prevents infinite radiation stacking).
 *   3. Contagion range validation (ensures range stays reasonable).
 *   4. Mastery-related combat caps.
 *
 * Phase 13 addition.
 */
public class CombatBalanceManager implements Listener {

    private final NuclearCraftPlugin plugin;
    private final ConfigManager configManager;

    private FileConfiguration cfg;

    /**
     * UUID (victim) → infections applied in the current fight.
     * Resets on the victim's death.
     */
    private final Map<UUID, Integer> infectionsThisFight = new HashMap<>();

    public CombatBalanceManager(NuclearCraftPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void initialize() {
        cfg = configManager.getConfig(ConfigManager.ConfigFile.BALANCE);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        NCLogger.info("[CombatBalanceManager] Combat balance rules active.");
    }

    public void reload() {
        cfg = configManager.getConfig(ConfigManager.ConfigFile.BALANCE);
    }

    public void shutdown() {
        infectionsThisFight.clear();
    }

    // ── Radiation damage cap ──────────────────────────────────────────────────

    /**
     * Clamps the radiation-derived bonus damage to the configured maximum.
     *
     * @param rawBonus  Bonus damage calculated from radiation level (e.g. rad * multiplier)
     * @return          Safe bonus damage amount
     */
    public double clampRadiationBonus(double rawBonus) {
        return Math.min(rawBonus, getMaxRadiationDamageBonus());
    }

    /**
     * Calculates and clamps the full radiation bonus damage for a combat hit.
     *
     * @param attackerRadiation  Attacker's current radiation level (0–1000)
     * @param multiplier         radiation-damage-multiplier from balance config
     * @return                   Capped bonus damage
     */
    public double calculateRadiationDamageBonus(double attackerRadiation, double multiplier) {
        double raw = attackerRadiation * multiplier;
        return clampRadiationBonus(raw);
    }

    // ── Infection stack limit ─────────────────────────────────────────────────

    /**
     * Returns true if the attacker can infect the victim in this fight.
     * Increments the infection counter if allowed.
     */
    public boolean tryApplyInfection(UUID victimId) {
        int current = infectionsThisFight.getOrDefault(victimId, 0);
        if (current >= getInfectionStackLimit()) {
            NCLogger.debug("[CombatBalanceManager] Infection blocked for %s (stack limit %d reached)",
                    victimId, getInfectionStackLimit());
            return false;
        }
        infectionsThisFight.put(victimId, current + 1);
        return true;
    }

    /**
     * Returns how many times the given player has been infected this fight.
     */
    public int getInfectionCount(UUID victimId) {
        return infectionsThisFight.getOrDefault(victimId, 0);
    }

    // ── Aura pulse cap ────────────────────────────────────────────────────────

    /**
     * Returns the capped per-pulse radiation gain for radiation aura weapons.
     * Prevents aura effects from stacking beyond a sane threshold per tick.
     */
    public double clampAuraPulse(double rawGain) {
        return Math.min(rawGain, getMaxAuraPulseGain());
    }

    // ── Config getters ────────────────────────────────────────────────────────

    /** Maximum bonus damage from radiation per hit. */
    public double getMaxRadiationDamageBonus() {
        return cfg.getDouble("combat.max-radiation-damage-bonus", 15.0);
    }

    /** Max infection applications per fight per victim before the cap is hit. */
    public int getInfectionStackLimit() {
        return cfg.getInt("combat.infection-stack-limit", 5);
    }

    /** Maximum contagion spread range in blocks. */
    public double getContagionRange() {
        return cfg.getDouble("combat.contagion-range", 5.0);
    }

    /** Minimum ticks between contagion applications from aura/melee to same target. */
    public int getContagionIntervalTicks() {
        return cfg.getInt("combat.contagion-interval-ticks", 40);
    }

    /** Maximum radiation gain per aura pulse to a single target. */
    public double getMaxAuraPulseGain() {
        return cfg.getDouble("combat.max-aura-pulse-gain", 20.0);
    }

    /** Mastery kills per level (how many kills to advance weapon mastery). */
    public int getMasteryKillsPerLevel() {
        return cfg.getInt("combat.mastery-kills-per-level", 50);
    }

    /** Maximum weapon mastery level. */
    public int getMasteryMaxLevel() {
        return cfg.getInt("combat.mastery-max-level", 10);
    }

    // ── Listener ──────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        // Reset infection stacks for the dead player — fight is over
        infectionsThisFight.remove(event.getEntity().getUniqueId());
    }
}
