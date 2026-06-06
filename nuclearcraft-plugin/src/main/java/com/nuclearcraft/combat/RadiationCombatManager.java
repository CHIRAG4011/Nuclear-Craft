package com.nuclearcraft.combat;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.radiation.RadiationManager;
import com.nuclearcraft.radiation.RadiationSource;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

/**
 * Central radiation combat router for Phase 9.
 *
 * All PvP radiation application passes through this class so that combo,
 * mastery, damage calculation, visual effects, statistics, and kill attribution
 * are applied consistently from a single code path.
 *
 * <p><strong>Call order per hit:</strong>
 * <ol>
 *   <li>Validate PvP target ({@link PvPRadiationManager}).</li>
 *   <li>Compute combo bonus ({@link CombatInfectionManager}).</li>
 *   <li>Compute mastery bonus ({@link WeaponMasteryManager}).</li>
 *   <li>Apply damage calculation / armor resistance ({@link DamageCalculationManager}).</li>
 *   <li>Apply radiation via {@link RadiationManager}.</li>
 *   <li>Spawn visual effects ({@link CombatVisualManager}).</li>
 *   <li>Update statistics ({@link CombatStatisticsManager}).</li>
 *   <li>Check and potentially trigger a radiation surge.</li>
 * </ol>
 */
public class RadiationCombatManager {

    private static final Random RANDOM = new Random();

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final RadiationManager radiationManager;
    private final PlayerDataManager playerDataManager;
    private final CombatInfectionManager comboManager;
    private final WeaponMasteryManager masteryManager;
    private final DamageCalculationManager damageCalcManager;
    private final PvPRadiationManager pvpManager;
    private final CombatVisualManager visualManager;
    private final RadiationKillManager killManager;
    private final CombatStatisticsManager statsManager;

    private int swordBaseBonus;
    private int criticalBonus;
    private int highStageBonus;
    private int axeBaseBonus;
    private int axeShockwaveExtra;

    public RadiationCombatManager(JavaPlugin plugin,
                                   ConfigManager configManager,
                                   RadiationManager radiationManager,
                                   PlayerDataManager playerDataManager,
                                   CombatInfectionManager comboManager,
                                   WeaponMasteryManager masteryManager,
                                   DamageCalculationManager damageCalcManager,
                                   PvPRadiationManager pvpManager,
                                   CombatVisualManager visualManager,
                                   RadiationKillManager killManager,
                                   CombatStatisticsManager statsManager) {
        this.plugin             = plugin;
        this.configManager      = configManager;
        this.radiationManager   = radiationManager;
        this.playerDataManager  = playerDataManager;
        this.comboManager       = comboManager;
        this.masteryManager   = masteryManager;
        this.damageCalcManager = damageCalcManager;
        this.pvpManager       = pvpManager;
        this.visualManager    = visualManager;
        this.killManager      = killManager;
        this.statsManager     = statsManager;
    }

    public void initialize() {
        loadConfig();
        NCLogger.info("RadiationCombatManager initialized.");
    }

    public void shutdown() {}

    private void loadConfig() {
        var cfg = configManager.getCombat();
        swordBaseBonus   = cfg.getInt("radiation.sword.base-hit", 0);
        criticalBonus    = cfg.getInt("radiation.sword.critical-bonus", 5);
        highStageBonus   = cfg.getInt("radiation.sword.high-stage-bonus", 5);
        axeBaseBonus     = cfg.getInt("radiation.axe.base-hit", 0);
        axeShockwaveExtra = cfg.getInt("radiation.axe.shockwave-extra-radiation", 5);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Melee weapon hit (sword / axe)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Processes a PvP melee hit with a Plutonium weapon.
     *
     * The base radiation is already applied by Phase 6's {@link com.nuclearcraft.equipment.WeaponEffectManager}.
     * This method applies the Phase 9 BONUS radiation (combo + mastery + stage vulnerability).
     *
     * @param attacker   the attacking player
     * @param victim     the defending player
     * @param weaponId   the custom item ID (e.g. "plutonium-sword")
     * @param isCritical whether the hit was a critical (falling + not sprinting)
     */
    public void processMeleeHit(Player attacker, Player victim,
                                 String weaponId, boolean isCritical) {
        if (!pvpManager.isValidPvPTarget(attacker, victim)) return;

        WeaponMasteryManager.WeaponType weaponType =
                weaponId.contains("axe") ? WeaponMasteryManager.WeaponType.AXE
                                         : WeaponMasteryManager.WeaponType.SWORD;

        // ── Combo bonus ───────────────────────────────────────────────────────
        int comboBonus = comboManager.onHit(attacker, victim);
        int comboCount = comboManager.getComboCount(attacker, victim);

        // ── Extra bonus factors ───────────────────────────────────────────────
        int baseBonus   = weaponType == WeaponMasteryManager.WeaponType.AXE ? axeBaseBonus : swordBaseBonus;
        int critBonus   = isCritical ? criticalBonus : 0;
        int stageBonus  = radiationManager.getStage(victim) >= 3 ? highStageBonus : 0;
        int rawExtra    = baseBonus + comboBonus + critBonus + stageBonus;

        if (rawExtra <= 0 && weaponType == WeaponMasteryManager.WeaponType.SWORD) {
            // Even with no raw bonus we still need to go through stats/effects
        }

        // ── Mastery multiplier ────────────────────────────────────────────────
        double masteryBonus = masteryManager.getMasteryBonus(attacker, weaponType);

        // ── Damage calculation ────────────────────────────────────────────────
        int finalAmount = damageCalcManager.calculate(
                Math.max(rawExtra, 0), masteryBonus, victim,
                RadiationSource.PLUTONIUM_WEAPON, RadiationDamageType.WEAPON);

        // ── Apply radiation ───────────────────────────────────────────────────
        if (finalAmount > 0) {
            killManager.trackLastDamageType(victim, RadiationDamageType.WEAPON);
            pvpManager.recordRadiationAttacker(attacker, victim);
            radiationManager.addRadiation(victim, finalAmount, RadiationSource.PLUTONIUM_WEAPON);
        }

        // ── Visual effects ────────────────────────────────────────────────────
        if (comboCount >= 2) {
            visualManager.spawnComboEffect(victim.getLocation(), comboCount);
        } else {
            visualManager.spawnWeaponHitEffect(victim.getLocation());
        }
        if (isCritical && finalAmount > 0) {
            visualManager.spawnCriticalRadiationEffect(victim.getLocation());
        }

        // ── Statistics ────────────────────────────────────────────────────────
        masteryManager.recordHit(attacker, weaponType);
        if (finalAmount > 0) {
            statsManager.recordPvPRadiation(attacker, finalAmount);
        }

        // ── Axe shockwave extra radiation ─────────────────────────────────────
        if (weaponType == WeaponMasteryManager.WeaponType.AXE) {
            applyAxeShockwave(attacker, victim);
        }

        // ── Surge check ───────────────────────────────────────────────────────
        checkAndTriggerSurge(attacker, victim);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Arrow hit
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Processes a Plutonium Arrow hit on a player victim.
     * Applies the Phase 9 arrow radiation bonuses on top of Phase 6 base.
     *
     * @param attacker      the player who shot the arrow
     * @param victim        the player struck by the arrow
     * @param baseAmount    the base radiation configured for the arrow
     * @param isCritical    true if the bow was fully charged
     */
    public void processArrowHit(Player attacker, Player victim,
                                 int baseAmount, boolean isCritical) {
        if (!pvpManager.isValidPvPTarget(attacker, victim)) return;

        double masteryBonus = masteryManager.getMasteryBonus(attacker, WeaponMasteryManager.WeaponType.BOW);
        int critExtra = isCritical
                ? configManager.getCombat().getInt("radiation.arrow.critical-bonus", 20)
                : 0;
        int raw = baseAmount + critExtra;

        int finalAmount = damageCalcManager.calculate(raw, masteryBonus, victim,
                RadiationSource.PLUTONIUM_ARROW, RadiationDamageType.ARROW);

        if (finalAmount > 0) {
            killManager.trackLastDamageType(victim, RadiationDamageType.ARROW);
            pvpManager.recordRadiationAttacker(attacker, victim);
            radiationManager.addRadiation(victim, finalAmount, RadiationSource.PLUTONIUM_ARROW);
        }

        visualManager.spawnArrowImpactEffect(victim.getLocation());
        masteryManager.recordHit(attacker, WeaponMasteryManager.WeaponType.BOW);
        statsManager.recordArrowHit(attacker, finalAmount);
        checkAndTriggerSurge(attacker, victim);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Axe shockwave
    // ──────────────────────────────────────────────────────────────────────────

    private void applyAxeShockwave(Player attacker, Player primaryVictim) {
        double baseRadius = 3.0;
        double bonusRadius = masteryManager.isMaster(attacker, WeaponMasteryManager.WeaponType.AXE)
                ? configManager.getCombat().getDouble("mastery.bonuses.axe-shockwave-radius-bonus", 1.5)
                : 0.0;
        double radius = baseRadius + bonusRadius;

        primaryVictim.getWorld().getNearbyEntities(
                primaryVictim.getLocation(), radius, radius, radius,
                e -> e instanceof Player p && !p.equals(attacker) && !p.equals(primaryVictim)
        ).forEach(e -> {
            Player nearby = (Player) e;
            if (!pvpManager.isValidPvPTarget(attacker, nearby)) return;
            int shockwaveRad = damageCalcManager.calculate(
                    axeShockwaveExtra, nearby, RadiationSource.PLUTONIUM_WEAPON);
            if (shockwaveRad > 0) {
                radiationManager.addRadiation(nearby, shockwaveRad, RadiationSource.PLUTONIUM_WEAPON);
                visualManager.spawnWeaponHitEffect(nearby.getLocation());
            }
        });
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Radiation surge
    // ──────────────────────────────────────────────────────────────────────────

    private void checkAndTriggerSurge(Player attacker, Player victim) {
        if (!pvpManager.isSurgeEnabled()) return;

        int attackerStage = playerDataManager.get(attacker.getUniqueId())
                .map(d -> d.getRadiationStage()).orElse(0);
        int victimStage = playerDataManager.get(victim.getUniqueId())
                .map(d -> d.getRadiationStage()).orElse(0);

        if (attackerStage < pvpManager.getSurgeMinStage() ||
                victimStage < pvpManager.getSurgeMinStage()) return;

        if (RANDOM.nextDouble() >= pvpManager.getSurgeTriggerChance()) return;

        if (!pvpManager.checkAndRecordSurgeCooldown(attacker, victim)) return;

        triggerSurge(attacker, victim);
    }

    private void triggerSurge(Player attacker, Player victim) {
        NCLogger.debug("Radiation surge between %s and %s!", attacker.getName(), victim.getName());

        int amount = pvpManager.getSurgeBothAmount();
        radiationManager.addRadiation(attacker, amount, RadiationSource.RADIATED_PLAYER);
        radiationManager.addRadiation(victim,   amount, RadiationSource.RADIATED_PLAYER);

        visualManager.spawnSurgeEffect(victim.getLocation());

        attacker.sendMessage("§c§l☢ RADIATION SURGE! §r§cThe combat radiation between you spikes!");
        victim.sendMessage("§c§l☢ RADIATION SURGE! §r§cCombined irradiation overload!");

        int nearbyAmount = pvpManager.getSurgeNearbyAmount();
        double nearbyRadius = pvpManager.getSurgeNearbyRadius();

        victim.getWorld().getNearbyEntities(victim.getLocation(),
                nearbyRadius, nearbyRadius, nearbyRadius,
                e -> e instanceof Player p && !p.equals(attacker) && !p.equals(victim)
        ).forEach(e -> {
            Player bystander = (Player) e;
            radiationManager.addRadiation(bystander, nearbyAmount, RadiationSource.RADIATED_PLAYER);
            bystander.sendMessage("§6§l☢ RADIATION SURGE NEARBY! §r§6You were caught in the surge!");
        });
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Kill notification hook (called from CombatListener on weapon kill)
    // ──────────────────────────────────────────────────────────────────────────

    public void onMasteryKill(Player killer, WeaponMasteryManager.WeaponType type) {
        masteryManager.recordKill(killer, type);
    }
}
