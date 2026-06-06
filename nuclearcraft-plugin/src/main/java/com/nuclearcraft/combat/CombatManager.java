package com.nuclearcraft.combat;

import com.nuclearcraft.advancements.AdvancementManager;
import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.equipment.EquipmentManager;
import com.nuclearcraft.equipment.RadiationAuraManager;
import com.nuclearcraft.radiation.RadiationManager;
import com.nuclearcraft.upgrade.UpgradeManager;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Phase 9 top-level orchestrator for all advanced combat systems.
 *
 * <p>Analogous to {@link com.nuclearcraft.equipment.EquipmentManager} for Phase 6,
 * {@code CombatManager} owns and initialises all Phase 9 sub-managers, wires their
 * dependencies, and exposes a single entry-point for {@link com.nuclearcraft.listeners.CombatListener}.
 *
 * <p>On initialisation, the Phase 8 {@link RadiationAuraManager} is shut down and replaced
 * by the team-aware {@link AdvancedAuraManager}.
 */
public class CombatManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final RadiationManager radiationManager;
    private final EquipmentManager equipmentManager;
    private final UpgradeManager upgradeManager;
    private final PlayerDataManager playerDataManager;
    private final AdvancementManager advancementManager;
    /** Phase 8 aura manager — shut down and replaced on Phase 9 init. */
    private RadiationAuraManager legacyAuraManager;

    // ── Sub-managers ──────────────────────────────────────────────────────────
    private CombatVisualManager       visualManager;
    private CombatInfectionManager    comboManager;
    private WeaponMasteryManager      masteryManager;
    private DamageCalculationManager  damageCalcManager;
    private PvPRadiationManager       pvpManager;
    private RadiationKillManager      killManager;
    private CombatStatisticsManager   statsManager;
    private AdvancedAuraManager       auraManager;
    private RadiationCombatManager    radiationCombat;

    public CombatManager(JavaPlugin plugin,
                          ConfigManager configManager,
                          RadiationManager radiationManager,
                          EquipmentManager equipmentManager,
                          UpgradeManager upgradeManager,
                          PlayerDataManager playerDataManager,
                          AdvancementManager advancementManager,
                          RadiationAuraManager legacyAuraManager) {
        this.plugin              = plugin;
        this.configManager       = configManager;
        this.radiationManager    = radiationManager;
        this.equipmentManager    = equipmentManager;
        this.upgradeManager      = upgradeManager;
        this.playerDataManager   = playerDataManager;
        this.advancementManager  = advancementManager;
        this.legacyAuraManager   = legacyAuraManager;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ──────────────────────────────────────────────────────────────────────────

    public void initialize() {
        // Shut down Phase 8 aura — AdvancedAuraManager takes over
        if (legacyAuraManager != null) {
            legacyAuraManager.shutdown();
            NCLogger.info("CombatManager: Phase 8 RadiationAuraManager replaced by AdvancedAuraManager.");
        }

        // Retrieve resistance manager from Phase 6 (may be null if not yet inited)
        var resistanceMgr = equipmentManager != null
                ? equipmentManager.getResistanceManager() : null;

        // Instantiate sub-managers in dependency order
        visualManager    = new CombatVisualManager(configManager);
        comboManager     = new CombatInfectionManager(plugin, configManager);
        masteryManager   = new WeaponMasteryManager(plugin, configManager, playerDataManager);
        damageCalcManager = new DamageCalculationManager(configManager, resistanceMgr);
        pvpManager       = new PvPRadiationManager(configManager);
        statsManager     = new CombatStatisticsManager(playerDataManager, advancementManager);
        killManager      = new RadiationKillManager(plugin, configManager, playerDataManager,
                advancementManager, pvpManager, statsManager);
        auraManager      = new AdvancedAuraManager(plugin, configManager, radiationManager,
                upgradeManager, pvpManager, visualManager, statsManager);
        radiationCombat  = new RadiationCombatManager(plugin, configManager, radiationManager,
                playerDataManager, comboManager, masteryManager, damageCalcManager, pvpManager,
                visualManager, killManager, statsManager);

        // Initialise each
        visualManager.initialize();
        comboManager.initialize();
        masteryManager.initialize();
        damageCalcManager.initialize();
        pvpManager.initialize();
        statsManager.initialize();
        killManager.initialize();
        auraManager.initialize();
        radiationCombat.initialize();

        NCLogger.info("CombatManager initialized — Phase 9 Advanced Combat active.");
    }

    public void shutdown() {
        if (auraManager      != null) auraManager.shutdown();
        if (comboManager     != null) comboManager.shutdown();
        if (pvpManager       != null) pvpManager.shutdown();
        if (killManager      != null) killManager.shutdown();
        if (damageCalcManager != null) damageCalcManager.shutdown();
        if (masteryManager   != null) masteryManager.shutdown();
        if (visualManager    != null) visualManager.shutdown();
        if (radiationCombat  != null) radiationCombat.shutdown();
        NCLogger.info("CombatManager shutdown.");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Accessors (used by CombatListener and commands)
    // ──────────────────────────────────────────────────────────────────────────

    public RadiationCombatManager   getRadiationCombat()  { return radiationCombat; }
    public CombatInfectionManager   getComboManager()     { return comboManager; }
    public WeaponMasteryManager     getMasteryManager()   { return masteryManager; }
    public PvPRadiationManager      getPvpManager()       { return pvpManager; }
    public RadiationKillManager     getKillManager()      { return killManager; }
    public CombatStatisticsManager  getStatsManager()     { return statsManager; }
    public AdvancedAuraManager      getAuraManager()      { return auraManager; }
    public CombatVisualManager      getVisualManager()    { return visualManager; }

    // ──────────────────────────────────────────────────────────────────────────
    // Convenience delegators used by CombatListener
    // ──────────────────────────────────────────────────────────────────────────

    public void onMeleeHit(Player attacker, Player victim, String weaponId, boolean isCritical) {
        radiationCombat.processMeleeHit(attacker, victim, weaponId, isCritical);
    }

    public void onArrowHit(Player attacker, Player victim, int baseAmount, boolean isCritical) {
        radiationCombat.processArrowHit(attacker, victim, baseAmount, isCritical);
    }

    public void onKill(Player killer, String weaponId) {
        WeaponMasteryManager.WeaponType type =
                weaponId.contains("axe") ? WeaponMasteryManager.WeaponType.AXE
              : weaponId.contains("bow") || weaponId.contains("arrow") ? WeaponMasteryManager.WeaponType.BOW
              : WeaponMasteryManager.WeaponType.SWORD;
        radiationCombat.onMasteryKill(killer, type);
    }
}
