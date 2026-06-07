package com.nuclearcraft.core;

import com.nuclearcraft.advancements.AdvancementManager;
import com.nuclearcraft.titantech.TitanTechManager;
import com.nuclearcraft.balance.BalanceManager;
import com.nuclearcraft.balance.BossBalanceManager;
import com.nuclearcraft.balance.CombatBalanceManager;
import com.nuclearcraft.balance.EconomyBalanceManager;
import com.nuclearcraft.balance.LootBalanceManager;
import com.nuclearcraft.balance.ProgressionBalanceManager;
import com.nuclearcraft.balance.RadiationBalanceManager;
import com.nuclearcraft.blocks.BlockManager;
import com.nuclearcraft.boss.TitanManager;
import com.nuclearcraft.combat.CombatManager;
import com.nuclearcraft.commands.NuclearCraftCommand;
import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.data.DatabaseManager;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.debug.DebugManager;
import com.nuclearcraft.debug.TestingManager;
import com.nuclearcraft.equipment.EquipmentManager;
import com.nuclearcraft.equipment.RadiationAuraManager;
import com.nuclearcraft.farming.FarmingManager;
import com.nuclearcraft.forge.ForgeRecipeManager;
import com.nuclearcraft.forge.NuclearForgeManager;
import com.nuclearcraft.gui.GUIManager;
import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.listeners.*;
import com.nuclearcraft.migration.MigrationManager;
import com.nuclearcraft.ore.*;
import com.nuclearcraft.particles.ParticleManager;
import com.nuclearcraft.performance.PerformanceManager;
import com.nuclearcraft.radiation.ContagionManager;
import com.nuclearcraft.radiation.RadiationManager;
import com.nuclearcraft.radiation.RadiationVisualManager;
import com.nuclearcraft.recipes.RecipeManager;
import com.nuclearcraft.release.ReleaseManager;
import com.nuclearcraft.resourcepack.AssetRegistry;
import com.nuclearcraft.resourcepack.ResourcePackManager;
import com.nuclearcraft.smelter.MachineRadiationManager;
import com.nuclearcraft.smelter.NuclearSmelterManager;
import com.nuclearcraft.smelter.NuclearSmelterRecipeManager;
import com.nuclearcraft.sound.SoundManager;
import com.nuclearcraft.tasks.TaskManager;
import com.nuclearcraft.upgrade.UpgradeManager;
import com.nuclearcraft.utils.NCLogger;
import com.nuclearcraft.zombies.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

/**
 * Main entry point for the NuclearCraft plugin.
 * Manages the lifecycle of all subsystems via dependency injection.
 *
 * Phase 7 additions: FarmingManager (orchestrates all Phase 7 sub-managers),
 *                    FarmingListener registration.
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

    // ── Phase 5 ──
    private NuclearSmelterRecipeManager smelterRecipeManager;
    private NuclearSmelterManager nuclearSmelterManager;
    private MachineRadiationManager machineRadiationManager;
    private SmelterListener smelterListener;

    // ── Phase 6 ──
    private EquipmentManager equipmentManager;
    private EquipmentListener equipmentListener;

    // ── Phase 7 ──
    private FarmingManager farmingManager;
    private FarmingListener farmingListener;

    // ── Phase 8 ──
    private UpgradeManager upgradeManager;
    private ForgeRecipeManager forgeRecipeManager;
    private NuclearForgeManager nuclearForgeManager;
    private RadiationAuraManager radiationAuraManager;
    private ForgeListener forgeListener;

    // ── Phase 9 ──
    private CombatManager combatManager;

    // ── Phase 10 ──
    private TitanManager titanManager;

    // ── Phase 11 ──
    private TitanTechManager titanTechManager;

    // ── Phase 12 ──
    private ReleaseManager      releaseManager;
    private MigrationManager    migrationManager;
    private BalanceManager      balanceManager;
    private PerformanceManager  performanceManager;
    private ResourcePackManager resourcePackManager;
    private SoundManager        soundManager;
    private ParticleManager     particleManager;
    private DebugManager        debugManager;
    private TestingManager      testingManager;

    // ── Phase 13 ──
    private LootBalanceManager        lootBalanceManager;
    private EconomyBalanceManager     economyBalanceManager;
    private RadiationBalanceManager   radiationBalanceManager;
    private CombatBalanceManager      combatBalanceManager;
    private BossBalanceManager        bossBalanceManager;
    private ProgressionBalanceManager progressionBalanceManager;

    private NuclearCraftCommand nuclearCraftCommandHandler;

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

        // Phase 13
        if (progressionBalanceManager != null) progressionBalanceManager.shutdown();
        if (bossBalanceManager        != null) bossBalanceManager.shutdown();
        if (combatBalanceManager      != null) combatBalanceManager.shutdown();
        if (radiationBalanceManager   != null) radiationBalanceManager.shutdown();
        if (economyBalanceManager     != null) economyBalanceManager.shutdown();
        if (lootBalanceManager        != null) lootBalanceManager.shutdown();

        // Phase 12
        if (testingManager      != null) testingManager.shutdown();
        if (debugManager        != null) debugManager.shutdown();
        if (particleManager     != null) particleManager.shutdown();
        if (soundManager        != null) soundManager.shutdown();
        if (resourcePackManager != null) resourcePackManager.shutdown();
        if (performanceManager  != null) performanceManager.shutdown();
        if (balanceManager      != null) balanceManager.shutdown();
        if (migrationManager    != null) migrationManager.shutdown();
        if (releaseManager      != null) releaseManager.shutdown();

        // Phase 11 — shut down titan tech first
        if (titanTechManager != null) titanTechManager.shutdown();

        // Phase 10 — shut down titan before combat
        if (titanManager != null) titanManager.shutdown();

        // Phase 9 — shut down combat systems first
        if (combatManager != null) combatManager.shutdown();

        // Phase 8 — shut down forge systems before Phase 7
        // Note: radiationAuraManager may already be shut down by CombatManager
        if (radiationAuraManager != null) radiationAuraManager.shutdown();
        if (nuclearForgeManager  != null) nuclearForgeManager.shutdown();
        if (forgeRecipeManager   != null) forgeRecipeManager.shutdown();
        if (upgradeManager       != null) upgradeManager.shutdown();

        // Phase 7 — shut down first so crops are saved before DB closes
        if (farmingManager != null) farmingManager.shutdown();

        // Phase 6
        if (equipmentManager != null) equipmentManager.shutdown();

        // Phase 5
        if (machineRadiationManager != null) machineRadiationManager.shutdown();
        if (nuclearSmelterManager  != null)  nuclearSmelterManager.shutdown();
        if (smelterRecipeManager   != null)  smelterRecipeManager.shutdown();

        // Phase 4
        if (radiationExposureListener != null) radiationExposureListener.shutdown();
        if (oreRadiationManager != null)       oreRadiationManager.shutdown();
        if (oreMiningManager != null)          oreMiningManager.shutdown();
        if (radiationDrillManager != null)     radiationDrillManager.shutdown();
        if (oreGenerationManager != null)      oreGenerationManager.shutdown();
        if (plutoniumOreManager != null)       plutoniumOreManager.shutdown();

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

        // ── Phase 5 ──
        smelterRecipeManager = new NuclearSmelterRecipeManager(configManager);
        smelterRecipeManager.initialize();

        nuclearSmelterManager = new NuclearSmelterManager(
                this, configManager, itemManager, playerDataManager,
                advancementManager, smelterRecipeManager);
        nuclearSmelterManager.initialize();

        machineRadiationManager = new MachineRadiationManager(
                this, configManager, radiationManager, nuclearSmelterManager);
        machineRadiationManager.initialize();

        // ── Phase 6 ──
        equipmentManager = new EquipmentManager(
                this, configManager, itemManager, radiationManager, playerDataManager);
        equipmentManager.initialize(); // also wires resistanceManager into radiationManager

        // ── Phase 7 ──
        farmingManager = new FarmingManager(
                this, configManager, itemManager, radiationManager,
                playerDataManager, advancementManager,
                equipmentManager.getFarmlandManager());
        farmingManager.initialize();

        // ── Phase 8 ──
        upgradeManager = new UpgradeManager(this);
        upgradeManager.initialize();

        forgeRecipeManager = new ForgeRecipeManager(this, itemManager);
        forgeRecipeManager.initialize();

        nuclearForgeManager = new NuclearForgeManager(
                this, configManager, itemManager, upgradeManager,
                playerDataManager, advancementManager, radiationManager);
        nuclearForgeManager.initialize();

        radiationAuraManager = new RadiationAuraManager(
                this, configManager, radiationManager, upgradeManager);
        radiationAuraManager.initialize();

        // ── Phase 9 ──
        combatManager = new CombatManager(
                this, configManager, radiationManager, equipmentManager,
                upgradeManager, playerDataManager, advancementManager,
                radiationAuraManager);
        combatManager.initialize();

        // ── Phase 10 ──
        titanManager = new TitanManager(
                this, configManager, radiationManager, playerDataManager,
                itemManager, advancementManager,
                irradiatedZombieManager, zombieSpawnManager);
        titanManager.initialize();

        // ── Phase 11 ──
        titanTechManager = new TitanTechManager(
                this, configManager, itemManager, radiationManager,
                playerDataManager, advancementManager);
        titanTechManager.initialize();

        // ── Phase 12 ──
        releaseManager = new ReleaseManager(this);
        releaseManager.initialize();

        migrationManager = new MigrationManager(this, configManager);
        migrationManager.initialize();

        balanceManager = new BalanceManager(this, configManager);
        balanceManager.initialize();

        performanceManager = new PerformanceManager(this, configManager);
        performanceManager.initialize();

        resourcePackManager = new ResourcePackManager(this, configManager);
        resourcePackManager.initialize();

        soundManager = new SoundManager(this, configManager);
        soundManager.initialize();

        particleManager = new ParticleManager(this, configManager);
        particleManager.initialize();
        particleManager.setPerformanceManager(performanceManager);

        debugManager = new DebugManager(
                this, configManager, radiationManager,
                irradiatedZombieManager, zombieSpawnManager,
                plutoniumOreManager, oreGenerationManager,
                nuclearSmelterManager, nuclearForgeManager,
                farmingManager, combatManager, titanManager,
                performanceManager, releaseManager);
        debugManager.initialize();

        testingManager = new TestingManager(this, configManager, databaseManager, itemManager);
        testingManager.initialize();

        AssetRegistry.validate();

        // ── Phase 13 ──
        lootBalanceManager = new LootBalanceManager(this, configManager);
        lootBalanceManager.initialize();

        economyBalanceManager = new EconomyBalanceManager(this, configManager);
        economyBalanceManager.initialize();

        radiationBalanceManager = new RadiationBalanceManager(this, configManager);
        radiationBalanceManager.initialize();

        combatBalanceManager = new CombatBalanceManager(this, configManager);
        combatBalanceManager.initialize();

        bossBalanceManager = new BossBalanceManager(this, configManager);
        bossBalanceManager.initialize();

        progressionBalanceManager = new ProgressionBalanceManager(this, configManager);
        progressionBalanceManager.initialize();

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

        // Phase 5
        smelterListener = new SmelterListener(
                this, configManager, itemManager, playerDataManager,
                advancementManager, nuclearSmelterManager);
        pm.registerEvents(smelterListener, this);

        // Phase 6
        equipmentListener = new EquipmentListener(
                this, itemManager, playerDataManager, advancementManager, equipmentManager);
        pm.registerEvents(equipmentListener, this);

        // Phase 7
        farmingListener = new FarmingListener(
                this, itemManager, playerDataManager, radiationManager,
                advancementManager, farmingManager);
        pm.registerEvents(farmingListener, this);

        // Phase 8
        forgeListener = new ForgeListener(nuclearForgeManager, itemManager, this);
        pm.registerEvents(forgeListener, this);

        // Phase 9
        pm.registerEvents(
                new com.nuclearcraft.listeners.CombatListener(this, combatManager, configManager),
                this);

        // Phase 10
        pm.registerEvents(
                new com.nuclearcraft.listeners.TitanListener(this, titanManager, itemManager),
                this);

        // Phase 11
        pm.registerEvents(
                new com.nuclearcraft.listeners.TitanForgeListener(
                        titanTechManager.getForgeManager(), itemManager, this),
                this);
        pm.registerEvents(
                new com.nuclearcraft.listeners.TitanItemListener(titanTechManager, this),
                this);

        NCLogger.debug("Event listeners registered.");
    }

    private void registerCommands() {
        var cmd = Objects.requireNonNull(getCommand("nuclearcraft"),
                "Command 'nuclearcraft' not found in plugin.yml");
        nuclearCraftCommandHandler = new NuclearCraftCommand(
                this, configManager, playerDataManager, itemManager,
                radiationManager, irradiatedZombieManager, zombieSpawnManager,
                radiationCloudManager, radiationNightManager, advancementManager,
                plutoniumOreManager, oreMiningManager, nuclearSmelterManager,
                equipmentManager, farmingManager, nuclearForgeManager, upgradeManager);
        nuclearCraftCommandHandler.setCombatManager(combatManager);
        nuclearCraftCommandHandler.setTitanManager(titanManager);
        nuclearCraftCommandHandler.setTitanTechManager(titanTechManager);
        nuclearCraftCommandHandler.setDebugManager(debugManager);
        cmd.setExecutor(nuclearCraftCommandHandler);
        cmd.setTabCompleter(nuclearCraftCommandHandler);
        NCLogger.debug("Commands registered.");
    }

    public void reload() throws Exception {
        // Phase 7
        if (farmingManager != null) farmingManager.shutdown();

        // Shutdown Phase 6 tasks
        if (equipmentManager != null) equipmentManager.shutdown();

        // Shutdown Phase 5 tasks
        if (machineRadiationManager != null) machineRadiationManager.shutdown();
        if (nuclearSmelterManager   != null) nuclearSmelterManager.shutdown();

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

        // Restart Phase 4 tasks
        radiationDrillManager.initialize();
        oreRadiationManager.initialize();
        radiationExposureListener.initialize();

        // Restart Phase 5 tasks
        smelterRecipeManager.reload();
        nuclearSmelterManager.initialize();
        machineRadiationManager.initialize();

        // Restart Phase 6
        equipmentManager.initialize();

        // Restart Phase 7
        farmingManager.initialize();

        // Restart Phase 8
        upgradeManager.initialize();
        forgeRecipeManager.reload();
        nuclearForgeManager.initialize();
        radiationAuraManager.initialize();

        // Reload Phase 13
        if (lootBalanceManager        != null) lootBalanceManager.reload();
        if (economyBalanceManager     != null) economyBalanceManager.reload();
        if (radiationBalanceManager   != null) radiationBalanceManager.reload();
        if (combatBalanceManager      != null) combatBalanceManager.reload();
        if (bossBalanceManager        != null) bossBalanceManager.reload();
        if (progressionBalanceManager != null) progressionBalanceManager.reload();

        // Reload Phase 12
        if (balanceManager      != null) balanceManager.reload();
        if (performanceManager  != null) performanceManager.reload();
        if (resourcePackManager != null) resourcePackManager.reload();
        if (soundManager        != null) soundManager.reload();
        if (particleManager     != null) particleManager.reload();

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
    public NuclearSmelterRecipeManager getSmelterRecipeManager()  { return smelterRecipeManager; }
    public NuclearSmelterManager getNuclearSmelterManager()       { return nuclearSmelterManager; }
    public MachineRadiationManager getMachineRadiationManager()   { return machineRadiationManager; }
    public SmelterListener getSmelterListener()                   { return smelterListener; }
    public EquipmentManager getEquipmentManager()                 { return equipmentManager; }
    public EquipmentListener getEquipmentListener()               { return equipmentListener; }
    public FarmingManager getFarmingManager()                     { return farmingManager; }
    public FarmingListener getFarmingListener()                   { return farmingListener; }

    // Phase 8
    public UpgradeManager getUpgradeManager()                     { return upgradeManager; }
    public ForgeRecipeManager getForgeRecipeManager()             { return forgeRecipeManager; }
    public NuclearForgeManager getNuclearForgeManager()           { return nuclearForgeManager; }
    public RadiationAuraManager getRadiationAuraManager()         { return radiationAuraManager; }
    public ForgeListener getForgeListener()                       { return forgeListener; }

    // Phase 9
    public CombatManager getCombatManager()                       { return combatManager; }

    // Phase 10
    public TitanManager getTitanManager()                         { return titanManager; }

    // Phase 11
    public TitanTechManager getTitanTechManager()                 { return titanTechManager; }

    // Phase 12
    public ReleaseManager getReleaseManager()                     { return releaseManager; }
    public MigrationManager getMigrationManager()                 { return migrationManager; }
    public BalanceManager getBalanceManager()                     { return balanceManager; }
    public PerformanceManager getPerformanceManager()             { return performanceManager; }
    public ResourcePackManager getResourcePackManager()           { return resourcePackManager; }
    public SoundManager getSoundManager()                         { return soundManager; }
    public ParticleManager getParticleManager()                   { return particleManager; }
    public DebugManager getDebugManager()                         { return debugManager; }
    public TestingManager getTestingManager()                     { return testingManager; }

    // Phase 13
    public LootBalanceManager getLootBalanceManager()                   { return lootBalanceManager; }
    public EconomyBalanceManager getEconomyBalanceManager()             { return economyBalanceManager; }
    public RadiationBalanceManager getRadiationBalanceManager()         { return radiationBalanceManager; }
    public CombatBalanceManager getCombatBalanceManager()               { return combatBalanceManager; }
    public BossBalanceManager getBossBalanceManager()                   { return bossBalanceManager; }
    public ProgressionBalanceManager getProgressionBalanceManager()     { return progressionBalanceManager; }
}
