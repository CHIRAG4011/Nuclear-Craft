package com.nuclearcraft.farming;

import com.nuclearcraft.advancements.AdvancementManager;
import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.equipment.RadioactiveFarmlandManager;
import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.radiation.RadiationManager;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Top-level orchestrator for Phase 7: Radioactive Farming & Cure System.
 *
 * <p>Initialises all sub-managers in dependency order:
 * <ol>
 *   <li>{@link MutatedCropManager}   — no internal deps</li>
 *   <li>{@link ToxicBloomManager}    — needs RadiationManager</li>
 *   <li>{@link MutatedSeedManager}   — needs ItemManager, FarmlandManager, CropManager</li>
 *   <li>{@link PlantGrowthManager}   — needs CropManager, ToxicBloomManager</li>
 *   <li>{@link PlantHarvestManager}  — needs CropManager, ItemManager, PlayerDataManager</li>
 *   <li>{@link MutatedPlantManager}  — orchestrates the above</li>
 *   <li>{@link RadiationImmunityManager} — needs PlayerDataManager</li>
 *   <li>{@link AntidoteManager}      — needs RadiationManager, PlayerDataManager</li>
 *   <li>{@link SerumManager}         — needs RadiationManager, ImmunityManager</li>
 *   <li>{@link RadiationCureManager} — orchestrates the above</li>
 * </ol>
 */
public class FarmingManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final ItemManager itemManager;
    private final RadiationManager radiationManager;
    private final PlayerDataManager playerDataManager;
    private final AdvancementManager advancementManager;
    private final RadioactiveFarmlandManager farmlandManager;

    private MutatedPlantManager plantManager;
    private RadiationCureManager cureManager;

    public FarmingManager(JavaPlugin plugin,
                          ConfigManager configManager,
                          ItemManager itemManager,
                          RadiationManager radiationManager,
                          PlayerDataManager playerDataManager,
                          AdvancementManager advancementManager,
                          RadioactiveFarmlandManager farmlandManager) {
        this.plugin             = plugin;
        this.configManager      = configManager;
        this.itemManager        = itemManager;
        this.radiationManager   = radiationManager;
        this.playerDataManager  = playerDataManager;
        this.advancementManager = advancementManager;
        this.farmlandManager    = farmlandManager;
    }

    public void initialize() {
        NCLogger.info("Initializing Phase 7: Radioactive Farming & Cure System...");

        // ── Plant subsystem ──────────────────────────────────────────────────
        MutatedCropManager cropManager = new MutatedCropManager(plugin, configManager);

        ToxicBloomManager toxicBloomManager = new ToxicBloomManager(
                plugin, configManager, radiationManager, playerDataManager);

        MutatedSeedManager seedManager = new MutatedSeedManager(
                itemManager, configManager, farmlandManager, cropManager);

        PlantGrowthManager growthManager = new PlantGrowthManager(
                plugin, configManager, cropManager, toxicBloomManager);

        PlantHarvestManager harvestManager = new PlantHarvestManager(
                plugin, configManager, itemManager, playerDataManager,
                advancementManager, cropManager);

        plantManager = new MutatedPlantManager(
                cropManager, seedManager, growthManager, harvestManager, toxicBloomManager);
        plantManager.initialize();
        plantManager.initializeSubManagers();

        // ── Cure subsystem ───────────────────────────────────────────────────
        RadiationImmunityManager immunityManager = new RadiationImmunityManager(
                playerDataManager, configManager);

        AntidoteManager antidoteManager = new AntidoteManager(
                plugin, configManager, itemManager, playerDataManager,
                radiationManager, advancementManager);

        SerumManager serumManager = new SerumManager(
                plugin, configManager, itemManager, playerDataManager,
                radiationManager, immunityManager, advancementManager);

        cureManager = new RadiationCureManager(antidoteManager, serumManager, immunityManager);
        cureManager.initialize();

        NCLogger.info("Phase 7 Farming System fully initialized.");
    }

    public void shutdown() {
        if (cureManager  != null) cureManager.shutdown();
        if (plantManager != null) plantManager.shutdown();
        NCLogger.info("Phase 7 Farming System shut down.");
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public MutatedPlantManager getPlantManager()   { return plantManager; }
    public RadiationCureManager getCureManager()   { return cureManager; }

    // Convenience delegate accessors
    public MutatedCropManager getCropManager()     { return plantManager.getCropManager(); }
    public MutatedSeedManager getSeedManager()     { return plantManager.getSeedManager(); }
    public PlantGrowthManager getGrowthManager()   { return plantManager.getGrowthManager(); }
    public PlantHarvestManager getHarvestManager() { return plantManager.getHarvestManager(); }
    public ToxicBloomManager getToxicBloomManager(){ return plantManager.getToxicBloomManager(); }
    public AntidoteManager getAntidoteManager()    { return cureManager.getAntidoteManager(); }
    public SerumManager getSerumManager()          { return cureManager.getSerumManager(); }
    public RadiationImmunityManager getImmunityManager() { return cureManager.getImmunityManager(); }
}
