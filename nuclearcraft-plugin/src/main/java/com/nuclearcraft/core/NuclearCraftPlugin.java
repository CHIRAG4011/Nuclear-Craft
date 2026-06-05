package com.nuclearcraft.core;

import com.nuclearcraft.blocks.BlockManager;
import com.nuclearcraft.commands.NuclearCraftCommand;
import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.data.DatabaseManager;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.gui.GUIManager;
import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.listeners.CoreListener;
import com.nuclearcraft.recipes.RecipeManager;
import com.nuclearcraft.tasks.TaskManager;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

/**
 * Main entry point for the NuclearCraft plugin.
 * Manages the lifecycle of all subsystems via dependency injection.
 */
public final class NuclearCraftPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private PlayerDataManager playerDataManager;
    private ItemManager itemManager;
    private BlockManager blockManager;
    private RecipeManager recipeManager;
    private GUIManager guiManager;
    private TaskManager taskManager;

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

        if (taskManager != null) taskManager.shutdown();
        if (playerDataManager != null) playerDataManager.shutdown();
        if (databaseManager != null) databaseManager.shutdown();
        if (guiManager != null) guiManager.shutdown();
        if (recipeManager != null) recipeManager.shutdown();
        if (blockManager != null) blockManager.shutdown();
        if (itemManager != null) itemManager.shutdown();
        if (configManager != null) configManager.shutdown();

        NCLogger.info("NuclearCraft has been disabled.");
    }

    private void initializeManagers() throws Exception {
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

        NCLogger.debug("All managers initialized successfully.");
    }

    private void registerListeners() {
        var pm = getServer().getPluginManager();
        pm.registerEvents(new CoreListener(this, playerDataManager), this);
        NCLogger.debug("Event listeners registered.");
    }

    private void registerCommands() {
        var cmd = Objects.requireNonNull(getCommand("nuclearcraft"),
                "Command 'nuclearcraft' not found in plugin.yml");
        var handler = new NuclearCraftCommand(this, configManager, playerDataManager, itemManager);
        cmd.setExecutor(handler);
        cmd.setTabCompleter(handler);
        NCLogger.debug("Commands registered.");
    }

    public void reload() throws Exception {
        if (taskManager != null) taskManager.shutdown();

        configManager.reload();
        NCLogger.setDebugMode(configManager.isDebugMode());
        itemManager.reload();
        blockManager.reload();
        recipeManager.reload();
        guiManager.reload();
        taskManager.initialize();

        NCLogger.info("NuclearCraft reloaded successfully.");
    }

    public ConfigManager getConfigManager() { return configManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
    public ItemManager getItemManager() { return itemManager; }
    public BlockManager getBlockManager() { return blockManager; }
    public RecipeManager getRecipeManager() { return recipeManager; }
    public GUIManager getGuiManager() { return guiManager; }
    public TaskManager getTaskManager() { return taskManager; }
}
