package com.nuclearcraft.config;

import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages all plugin configuration files.
 * Handles loading, saving, and hot-reloading of YAML configs.
 */
public class ConfigManager {

    public enum ConfigFile {
        MAIN("config.yml"),
        MESSAGES("messages.yml"),
        ORE("ore.yml"),
        RADIATION("radiation.yml"),
        ZOMBIES("zombies.yml"),
        FARMING("farming.yml"),
        BOSS("boss.yml"),
        TOOLSTATS("toolstats.yml"),
        ARMORS("armors.yml"),
        SMELTER("smelter.yml"),
        EQUIPMENT("equipment.yml"),
        FORGE("forge.yml"),
        COMBAT("combat.yml"),
        TITAN("titan.yml"),
        TITAN_ITEMS("titan_items.yml"),
        RESOURCEPACK("resourcepack.yml"),
        BALANCE("balance.yml");

        private final String fileName;

        ConfigFile(String fileName) {
            this.fileName = fileName;
        }

        public String getFileName() {
            return fileName;
        }
    }

    private final NuclearCraftPlugin plugin;
    private final Map<ConfigFile, FileConfiguration> configs = new HashMap<>();
    private final Map<ConfigFile, File> files = new HashMap<>();

    public ConfigManager(NuclearCraftPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        for (ConfigFile cf : ConfigFile.values()) {
            loadConfig(cf);
        }
        NCLogger.info("Loaded " + ConfigFile.values().length + " configuration files.");
    }

    public void reload() {
        configs.clear();
        for (ConfigFile cf : ConfigFile.values()) {
            loadConfig(cf);
        }
        NCLogger.info("Configuration reloaded.");
    }

    public void shutdown() {
        configs.clear();
        files.clear();
    }

    private void loadConfig(ConfigFile cf) {
        plugin.saveResource(cf.getFileName(), false);
        File file = new File(plugin.getDataFolder(), cf.getFileName());
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        configs.put(cf, config);
        files.put(cf, file);
        NCLogger.debug("Loaded config: %s", cf.getFileName());
    }

    public FileConfiguration get(ConfigFile cf) {
        return configs.getOrDefault(cf, plugin.getConfig());
    }

    /** Alias for {@link #get(ConfigFile)} — used by Phase 12 managers. */
    public FileConfiguration getConfig(ConfigFile cf) {
        return get(cf);
    }

    public FileConfiguration getMain() {
        return get(ConfigFile.MAIN);
    }

    public FileConfiguration getMessages() {
        return get(ConfigFile.MESSAGES);
    }

    public FileConfiguration getOre() {
        return get(ConfigFile.ORE);
    }

    public FileConfiguration getRadiation() {
        return get(ConfigFile.RADIATION);
    }

    public FileConfiguration getZombies() {
        return get(ConfigFile.ZOMBIES);
    }

    public FileConfiguration getFarming() {
        return get(ConfigFile.FARMING);
    }

    public FileConfiguration getBoss() {
        return get(ConfigFile.BOSS);
    }

    public FileConfiguration getToolStats() {
        return get(ConfigFile.TOOLSTATS);
    }

    public FileConfiguration getArmors() {
        return get(ConfigFile.ARMORS);
    }

    public FileConfiguration getSmelter() {
        return get(ConfigFile.SMELTER);
    }

    public FileConfiguration getEquipment() {
        return get(ConfigFile.EQUIPMENT);
    }

    public FileConfiguration getForge() {
        return get(ConfigFile.FORGE);
    }

    public FileConfiguration getCombat() {
        return get(ConfigFile.COMBAT);
    }

    public FileConfiguration getTitan() {
        return get(ConfigFile.TITAN);
    }

    public FileConfiguration getTitanItems() {
        return get(ConfigFile.TITAN_ITEMS);
    }

    public FileConfiguration getResourcePack() {
        return get(ConfigFile.RESOURCEPACK);
    }

    public FileConfiguration getBalance() {
        return get(ConfigFile.BALANCE);
    }

    public void save(ConfigFile cf) {
        try {
            configs.get(cf).save(files.get(cf));
        } catch (IOException e) {
            NCLogger.severe("Failed to save config: " + cf.getFileName(), e);
        }
    }

    public boolean isDebugMode() {
        return getMain().getBoolean("plugin.debug", false);
    }

    public long getDataSaveIntervalTicks() {
        return getMain().getLong("performance.data-save-interval-minutes", 5) * 60 * 20L;
    }

    public long getTaskIntervalTicks() {
        return getMain().getLong("performance.task-interval-ticks", 20);
    }

    public boolean isAsyncDataOperations() {
        return getMain().getBoolean("performance.async-data-operations", true);
    }

    public String getDatabaseType() {
        return getMain().getString("database.type", "SQLITE").toUpperCase();
    }

    public String getMessage(String path) {
        String prefix = getMessages().getString("prefix", "[NuclearCraft] ");
        String msg = getMessages().getString(path, "<red>Missing message: " + path + "</red>");
        return prefix + msg;
    }

    public String getRawMessage(String path) {
        return getMessages().getString(path, "<red>Missing message: " + path + "</red>");
    }
}
