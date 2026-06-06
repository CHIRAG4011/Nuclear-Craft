package com.nuclearcraft.core;

import com.nuclearcraft.advancements.AdvancementManager;
import com.nuclearcraft.blocks.BlockManager;
import com.nuclearcraft.commands.NuclearCraftCommand;
import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.data.DatabaseManager;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.gui.GUIManager;
import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.listeners.*;
import com.nuclearcraft.ore.*;
import com.nuclearcraft.radiation.ContagionManager;
import com.nuclearcraft.radiation.RadiationManager;
import com.nuclearcraft.radiation.RadiationVisualManager;
import com.nuclearcraft.recipes.RecipeManager;
import com.nuclearcraft.tasks.TaskManager;
import com.nuclearcraft.utils.NCLogger;
import com.nuclearcraft.zombies.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

/**
 * Main entry point for the NuclearCraft plugin.
 * Manages the lifecycle of all subsystems via dependency injection.
 *
 * Phase 2 additions: RadiationManager, ContagionManager, RadiationVisualManager,
 *                    RadiationListener registration.
 * Phase 3 additions: IrradiatedZombieManager, ZombieSpawnManager, ZombieCombatManager,
 *                    ZombieLootManager, RadiationCloudManager, RadiationNightManager,
 *                    AdvancementManager, ZombieSpawnListener registration.
 * Phase 4 additions: PlutoniumOreManager, OreGenerationManager, OreMiningManager,
 *                    OreRadiationManager, RadiationDrillManager,
 *                    OreListener, RadiationExposureListener.
 */
public final class NuclearCraftPlugin extends JavaPlugin {

    // ── Phase 1 ──
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private PlayerDataManager playerDataManager;
    private ItemManager itemManager;
    private BlockManager blockManager;
    private RecipeManager recipeManager;
    private GUIManager guiManager;
    private TaskManager taskManager;

    // ── Phase 2 ──
    private RadiationManager radiationManager;
    private ContagionManager contagionManager;
    private RadiationVisualManager radiationVisualManager;
    private RadiationListener radiationListener;

    // ── Phase 3 ──
    private IrradiatedZombieManager irradiatedZombieManager;
    private ZombieSpawnManager zombieSpawnManager;
    private ZombieCombatManager zombieCombatManager;
    private ZombieLootManager zombieLootManager;
    private RadiationCloudManager radiationCloudManager;
    private RadiationNightManager radiationNightManager;
    private AdvancementManager advancementManager;
    private ZombieSpawnListener zombieSpawnListener;

    // ── Phase 4 ──
    private PlutoniumOreManager plutoniumOreManager;
    private OreGenerationManager oreGenerationManager;
    private OreMiningManager oreMiningManager;
    private OreRadiationManager oreRadiationManager;
    private RadiationDrillManager radiationDrillManager;
    private OreListener oreListener;
    private RadiationExposureListener radiationExposureListener;

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();
        NCLogger.init(this);
        NCLogger.info("Starting NuclearCraft v" + getDescription().getVersion() + "...");

        try {
            initializeManagers();
            registerListeners();
            registerCommands();
        } catch (Exception e) {
            NCLogger.severe("FATAL: Failed to initialize NuclearCraft. Disabling plugin.", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        long elapsed = System.currentTimeMillis() - start;
        NCLogger.info("NuclearCraft enabled successfully in " + elapsed + "ms.");
    }

    @Override
    public void onDisable() {
        NCLogger.info("Shutting down NuclearCraft...");

        // Phase 4 — shut down first (ore radiation task, exposure task, save ore locations)
        if (radiationExposureListener != null) radiationExposureListener.shutdown();
        if (oreRadiationManager != null)       oreRadiationManager.shutdown();
        if (oreMiningManager != null)          oreMiningManager.shutdown();
        if (radiationDrillManager != null)     radiationDrillManager.shutdown();
        if (oreGenerationManager != null)      oreGenerationManager.shutdown();
        if (plutoniumOreManager != null)       plutoniumOreManager.shutdown(); // saves ore_data.yml

        // Phase 3
        if (radiationNightManager != null)     radiationNightManager.shutdown();
        if (radiationCloudManager != null)     radiationCloudManager.shutdown();
        if (zombieLootManager != null)         zombieLootManager.shutdown();
        if (zombieCombatManager != null)       zombieCombatManager.shutdown();
        if (zombieSpawnManager != null)        zombieSpawnManager.shutdown();
        if (irradiatedZombieManager != null)   irradiatedZombieManager.shutdown();
        if (advancementManager != null)        advancementManager.shutdown();

        // Phase 2
        if (taskManager != null)              taskManager.shutdown();
        if (radiationVisualManager != null)   radiationVisualManager.shutdown();
        if (contagionManager != null)         contagionManager.shutdown();
        if (radiationManager != null)         radiationManager.shutdown();

        // Phase 1
        if (playerDataManager != null)        playerDataManager.shutdown();
        if (databaseManager != null)          databaseManager.shutdown();
        if (guiManager != null)               guiManager.shutdown();
        if (recipeManager != null)            recipeManager.shutdown();
        if (blockManager != null)             blockManager.shutdown();
        if (itemManager != null)              itemManager.shutdown();
        if (configManager != null)            configManager.shutdown();

        NCLogger.info("NuclearCraft has been disabled.");
    }

    // ──────────────────────────────────────────────────────────────────────────

    private void initializeManagers() throws Exception {
        // ── Phase 1 ──
        configManager = new ConfigManager(this);
        configManager.initialize();

        NCLogger.setDebugMode(configManager.isDebugMode());

        databaseManager = new DatabaseManager(this, configManager);
        databaseManager.initialize();

        playerDataManager = new PlayerDataManager(this, databaseManager, configManager);
        playerDataManager.initialize();

        itemManager = new ItemManager(this, configManager);
        itemManager.initialize();

        blockManager = new BlockManager(this, configManager);
        blockManager.initialize();

        recipeManager = new RecipeManager(this, itemManager);
        recipeManager.initialize();

        guiManager = new GUIManager(this, configManager);
        guiManager.initialize();

        taskManager = new TaskManager(this, configManager, playerDataManager);
        taskManager.initialize();

        // ── Phase 2 ──
        radiationManager = new RadiationManager(this, configManager, playerDataManager);
        radiationManager.initialize();

        contagionManager = new ContagionManager(this, configManager, radiationManager);
        contagionManager.initialize();

        radiationVisualManager = new RadiationVisualManager(
                this, configManager, playerDataManager, radiationManager);
        radiationVisualManager.initialize();

        // ── Phase 3 ──
        irradiatedZombieManager = new IrradiatedZombieManager();
        irradiatedZombieManager.initialize();

        zombieSpawnManager = new ZombieSpawnManager(this, configManager, irradiatedZombieManager);
        zombieSpawnManager.initialize();

        zombieCombatManager = new ZombieCombatManager(
                this, configManager, irradiatedZombieManager, radiationManager, zombieSpawnManager);
        zombieCombatManager.initialize();

        zombieLootManager = new ZombieLootManager(
                this, configManager, irradiatedZombieManager, itemManager, zombieSpawnManager);
        zombieLootManager.initialize();

        radiationCloudManager = new RadiationCloudManager(
                this, configManager, radiationManager, irradiatedZombieManager);
        radiationCloudManager.initialize();

        radiationNightManager = new RadiationNightManager(this, configManager, zombieSpawnManager);
        radiationNightManager.initialize();

        advancementManager = new AdvancementManager(this, playerDataManager);
        advancementManager.initialize();

        // ── Phase 4 ──
        plutoniumOreManager = new PlutoniumOreManager(this, playerDataManager);
        plutoniumOreManager.initialize();

        oreGenerationManager = new OreGenerationManager(configManager, plutoniumOreManager);
        oreGenerationManager.initialize();

        radiationDrillManager = new RadiationDrillManager(this, itemManager);
        radiationDrillManager.initialize();

        oreMiningManager = new OreMiningManager(
                this, configManager, plutoniumOreManager, radiationDrillManager,
                radiationManager, playerDataManager, itemManager, advancementManager);
        oreMiningManager.initialize();

        oreRadiationManager = new OreRadiationManager(
                this, configManager, radiationManager, plutoniumOreManager);
        oreRadiationManager.initialize();

        NCLogger.debug("All managers initialized successfully.");
    }

    private void registerListeners() {
        var pm = getServer().getPluginManager();

        pm.registerEvents(new CoreListener(this, playerDataManager), this);

        radiationListener = new RadiationListener(this, radiationManager, contagionManager);
        pm.registerEvents(radiationListener, this);

        zombieSpawnListener = new ZombieSpawnListener(
                zombieSpawnManager, zombieCombatManager, zombieLootManager,
                radiationCloudManager, radiationNightManager, irradiatedZombieManager,
                playerDataManager, advancementManager);
        pm.registerEvents(zombieSpawnListener, this);

        // Phase 4
        oreListener = new OreListener(
                oreGenerationManager, oreMiningManager, plutoniumOreManager,
                radiationDrillManager, advancementManager);
        pm.registerEvents(oreListener, this);

        radiationExposureListener = new RadiationExposureListener(
                this, configManager, radiationManager, itemManager, oreMiningManager);
        radiationExposureListener.initialize();

        NCLogger.debug("Event listeners registered.");
    }

    private void registerCommands() {
        var cmd = Objects.requireNonNull(getCommand("nuclearcraft"),
                "Command 'nuclearcraft' not found in plugin.yml");
        var handler = new NuclearCraftCommand(
                this, configManager, playerDataManager, itemManager,
                radiationManager, irradiatedZombieManager, zombieSpawnManager,
                radiationCloudManager, radiationNightManager, advancementManager,
                plutoniumOreManager, oreMiningManager);
        cmd.setExecutor(handler);
        cmd.setTabCompleter(handler);
        NCLogger.debug("Commands registered.");
    }

    public void reload() throws Exception {
        // Shutdown Phase 4 tasks
        if (radiationExposureListener != null) radiationExposureListener.shutdown();
        if (oreRadiationManager != null)       oreRadiationManager.shutdown();

        // Shutdown Phase 3 tasks
        if (radiationNightManager != null)  radiationNightManager.shutdown();
        if (radiationCloudManager != null)  radiationCloudManager.shutdown();

        // Shutdown Phase 2 tasks
        if (taskManager != null)            taskManager.shutdown();
        if (radiationVisualManager != null) radiationVisualManager.shutdown();
        if (contagionManager != null)       contagionManager.shutdown();
        if (radiationManager != null)       radiationManager.shutdown();

        configManager.reload();
        NCLogger.setDebugMode(configManager.isDebugMode());
        itemManager.reload();
        blockManager.reload();
        recipeManager.reload();
        guiManager.reload();

        // Restart Phase 2
        taskManager.initialize();
        radiationManager.initialize();
        contagionManager.initialize();
        radiationVisualManager.initialize();

        // Restart Phase 3 tasks
        radiationCloudManager.initialize();
        radiationNightManager.initialize();

        // Restart Phase 4 tasks (drill recipe re-registered by RadiationDrillManager)
        radiationDrillManager.initialize();
        oreRadiationManager.initialize();
        radiationExposureListener.initialize();

        NCLogger.info("NuclearCraft reloaded successfully.");
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public ConfigManager getConfigManager()                       { return configManager; }
    public DatabaseManager getDatabaseManager()                   { return databaseManager; }
    public PlayerDataManager getPlayerDataManager()               { return playerDataManager; }
    public ItemManager getItemManager()                           { return itemManager; }
    public BlockManager getBlockManager()                         { return blockManager; }
    public RecipeManager getRecipeManager()                       { return recipeManager; }
    public GUIManager getGuiManager()                             { return guiManager; }
    public TaskManager getTaskManager()                           { return taskManager; }
    public RadiationManager getRadiationManager()                 { return radiationManager; }
    public ContagionManager getContagionManager()                 { return contagionManager; }
    public RadiationVisualManager getRadiationVisualManager()     { return radiationVisualManager; }
    public RadiationListener getRadiationListener()               { return radiationListener; }
    public IrradiatedZombieManager getIrradiatedZombieManager()   { return irradiatedZombieManager; }
    public ZombieSpawnManager getZombieSpawnManager()             { return zombieSpawnManager; }
    public ZombieCombatManager getZombieCombatManager()           { return zombieCombatManager; }
    public ZombieLootManager getZombieLootManager()               { return zombieLootManager; }
    public RadiationCloudManager getRadiationCloudManager()       { return radiationCloudManager; }
    public RadiationNightManager getRadiationNightManager()       { return radiationNightManager; }
    public AdvancementManager getAdvancementManager()             { return advancementManager; }
    public ZombieSpawnListener getZombieSpawnListener()           { return zombieSpawnListener; }
    public PlutoniumOreManager getPlutoniumOreManager()           { return plutoniumOreManager; }
    public OreGenerationManager getOreGenerationManager()         { return oreGenerationManager; }
    public OreMiningManager getOreMiningManager()                 { return oreMiningManager; }
    public OreRadiationManager getOreRadiationManager()           { return oreRadiationManager; }
    public RadiationDrillManager getRadiationDrillManager()       { return radiationDrillManager; }
    public OreListener getOreListener()                           { return oreListener; }
    public RadiationExposureListener getRadiationExposureListener() { return radiationExposureListener; }
}
