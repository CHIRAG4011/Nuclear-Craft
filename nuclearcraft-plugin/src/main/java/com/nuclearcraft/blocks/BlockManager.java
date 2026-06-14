package com.nuclearcraft.blocks;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.Material;

/**
 * Manages registration and lifecycle of all NuclearCraft custom block types.
 */
public class BlockManager {

    private final NuclearCraftPlugin plugin;
    private final ConfigManager configManager;
    private final BlockRegistry registry;

    public BlockManager(NuclearCraftPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.registry = new BlockRegistry(plugin);
    }

    public void initialize() {
        registerCoreBlocks();
        NCLogger.info("BlockManager initialized — " + registry.size() + " block types registered.");
    }

    public void reload() {
        registry.clear();
        registerCoreBlocks();
        NCLogger.info("BlockManager reloaded — " + registry.size() + " block types registered.");
    }

    public void shutdown() {
        registry.clear();
    }

    private void registerCoreBlocks() {
        registry.register(new CustomBlock(plugin,
                "plutonium-ore",
                "Plutonium Ore",
                Material.STONE,
                true,
                true));

        registry.register(new CustomBlock(plugin,
                "radioactive-farmland",
                "Radioactive Farmland",
                Material.FARMLAND,
                false,
                true));

        registry.register(new CustomBlock(plugin,
                "radioactive-debris",
                "Radioactive Debris",
                Material.ANCIENT_DEBRIS,
                true,
                true));

        registry.register(new CustomBlock(plugin,
                "nuclear-smelter",
                "Nuclear Smelter",
                Material.SMOKER,
                true,
                true));

        registry.register(new CustomBlock(plugin,
                "nuclear-forge",
                "Nuclear Forge",
                Material.CARTOGRAPHY_TABLE,
                true,
                true));
    }

    public BlockRegistry getRegistry() {
        return registry;
    }
}
