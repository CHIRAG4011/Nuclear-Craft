package com.nuclearcraft.equipment;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.radiation.RadiationManager;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Top-level orchestrator for Phase 6: Plutonium Equipment System.
 *
 * <p>Owns and initialises every Phase 6 sub-manager in dependency order:
 * <ol>
 *   <li>{@link RadiationResistanceManager} — no deps</li>
 *   <li>{@link PlutoniumToolManager}  — needs ItemManager</li>
 *   <li>{@link PlutoniumArmorManager} — needs ItemManager</li>
 *   <li>{@link WeaponEffectManager}   — needs RadiationManager, PlayerDataManager</li>
 *   <li>{@link ArmorEffectManager}    — needs ItemManager</li>
 *   <li>{@link RadioactiveDebrisManager}   — needs RadiationManager</li>
 *   <li>{@link RadioactiveSoilManager}     — needs RadiationManager</li>
 *   <li>{@link RadioactiveFarmlandManager} — needs RadiationManager, PlayerDataManager</li>
 *   <li>{@link PlutoniumArrowManager}      — needs ItemManager, RadiationManager</li>
 *   <li>{@link EquipmentRepairManager}     — needs ItemManager</li>
 * </ol>
 *
 * After initialisation, wires {@link RadiationResistanceManager} into
 * {@link RadiationManager} so that all subsequent radiation applications
 * go through the new armour-resistance logic.
 */
public class EquipmentManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final ItemManager itemManager;
    private final RadiationManager radiationManager;
    private final PlayerDataManager playerDataManager;

    private RadiationResistanceManager resistanceManager;
    private PlutoniumToolManager        toolManager;
    private PlutoniumArmorManager       armorManager;
    private WeaponEffectManager         weaponEffectManager;
    private ArmorEffectManager          armorEffectManager;
    private RadioactiveDebrisManager    debrisManager;
    private RadioactiveSoilManager      soilManager;
    private RadioactiveFarmlandManager  farmlandManager;
    private PlutoniumArrowManager       arrowManager;
    private EquipmentRepairManager      repairManager;

    public EquipmentManager(JavaPlugin plugin,
                            ConfigManager configManager,
                            ItemManager itemManager,
                            RadiationManager radiationManager,
                            PlayerDataManager playerDataManager) {
        this.plugin             = plugin;
        this.configManager      = configManager;
        this.itemManager        = itemManager;
        this.radiationManager   = radiationManager;
        this.playerDataManager  = playerDataManager;
    }

    public void initialize() {
        NCLogger.info("Initializing Phase 6: Equipment System...");

        resistanceManager   = new RadiationResistanceManager(itemManager, configManager);
        toolManager         = new PlutoniumToolManager(plugin, itemManager, configManager);
        armorManager        = new PlutoniumArmorManager(plugin, itemManager, configManager);
        weaponEffectManager = new WeaponEffectManager(plugin, itemManager, configManager, radiationManager, playerDataManager);
        armorEffectManager  = new ArmorEffectManager(plugin, itemManager, configManager);
        debrisManager       = new RadioactiveDebrisManager(plugin, configManager, radiationManager);
        soilManager         = new RadioactiveSoilManager(plugin, configManager, radiationManager);
        farmlandManager     = new RadioactiveFarmlandManager(plugin, configManager, radiationManager, playerDataManager);
        arrowManager        = new PlutoniumArrowManager(plugin, itemManager, configManager, radiationManager, playerDataManager);
        repairManager       = new EquipmentRepairManager(itemManager, configManager);

        resistanceManager.initialize();
        toolManager.initialize();        // must come after resistanceManager (items need to exist)
        armorManager.initialize();
        weaponEffectManager.initialize();
        armorEffectManager.initialize();
        debrisManager.initialize();
        soilManager.initialize();
        farmlandManager.initialize();
        arrowManager.initialize();
        repairManager.initialize();

        // Wire resistance into the existing radiation pipeline
        radiationManager.setResistanceManager(resistanceManager);

        NCLogger.info("Phase 6 Equipment System fully initialized.");
    }

    public void shutdown() {
        if (arrowManager        != null) arrowManager.shutdown();
        if (farmlandManager     != null) farmlandManager.shutdown();
        if (soilManager         != null) soilManager.shutdown();
        if (debrisManager       != null) debrisManager.shutdown();
        if (armorEffectManager  != null) armorEffectManager.shutdown();
        if (weaponEffectManager != null) weaponEffectManager.shutdown();
        if (repairManager       != null) repairManager.shutdown();
        if (resistanceManager   != null) resistanceManager.shutdown();
        NCLogger.info("Phase 6 Equipment System shut down.");
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public RadiationResistanceManager  getResistanceManager()   { return resistanceManager; }
    public PlutoniumToolManager        getToolManager()         { return toolManager; }
    public PlutoniumArmorManager       getArmorManager()        { return armorManager; }
    public WeaponEffectManager         getWeaponEffectManager() { return weaponEffectManager; }
    public ArmorEffectManager          getArmorEffectManager()  { return armorEffectManager; }
    public RadioactiveDebrisManager    getDebrisManager()       { return debrisManager; }
    public RadioactiveSoilManager      getSoilManager()         { return soilManager; }
    public RadioactiveFarmlandManager  getFarmlandManager()     { return farmlandManager; }
    public PlutoniumArrowManager       getArrowManager()        { return arrowManager; }
    public EquipmentRepairManager      getRepairManager()       { return repairManager; }
}
