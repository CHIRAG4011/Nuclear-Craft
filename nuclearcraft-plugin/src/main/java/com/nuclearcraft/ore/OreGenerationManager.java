package com.nuclearcraft.ore;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.utils.NCLogger;
import com.nuclearcraft.utils.RandomUtil;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * Handles Plutonium Ore placement during world generation.
 *
 * Called from {@link com.nuclearcraft.listeners.OreListener} on {@code ChunkLoadEvent}
 * when {@code isNewChunk()} returns true.
 *
 * Strategy:
 *   - Only overworld (Environment.NORMAL).
 *   - Y range: -64 to -58 (deepest deepslate layer, spec-mandated).
 *   - Vein size: 1-2 blocks.
 *   - Rarity: ~40% rarer than diamonds. Diamond averages ~3.7 blocks per chunk in this layer;
 *     plutonium targets ~0.6 veins per chunk (chance 0.60, avg vein 1.5 blocks = 0.9 blocks/chunk).
 *   - Host rock: DEEPSLATE, DEEPSLATE_BRICKS, or any stone variant.
 *   - Replaces host block only — never air, lava, or water.
 */
public class OreGenerationManager {

    /** Block material used as the visual placeholder for Plutonium Ore. */
    public static final Material ORE_MATERIAL = Material.DEEPSLATE_EMERALD_ORE;

    private final ConfigManager configManager;
    private final PlutoniumOreManager oreManager;

    public OreGenerationManager(ConfigManager configManager, PlutoniumOreManager oreManager) {
        this.configManager = configManager;
        this.oreManager = oreManager;
    }

    public void initialize() {
        NCLogger.info("OreGenerationManager initialized.");
    }

    public void shutdown() {}

    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Attempts to generate plutonium ore in the given newly-created chunk.
     * Called on the main thread from OreListener.
     */
    public void generateInChunk(Chunk chunk) {
        World world = chunk.getWorld();
        if (world.getEnvironment() != World.Environment.NORMAL) return;

        boolean enabled = configManager.getOre().getBoolean("plutonium-ore.enabled", true);
        if (!enabled) return;

        double spawnChance  = configManager.getOre().getDouble("plutonium-ore.generation.spawn-chance-per-chunk", 0.60);
        int minY            = configManager.getOre().getInt("plutonium-ore.generation.min-y", -64);
        int maxY            = configManager.getOre().getInt("plutonium-ore.generation.max-y", -58);
        int minVein         = configManager.getOre().getInt("plutonium-ore.generation.vein-size-min", 1);
        int maxVein         = configManager.getOre().getInt("plutonium-ore.generation.vein-size-max", 2);

        if (!RandomUtil.chance(spawnChance)) return;

        int veinSize = RandomUtil.nextInt(minVein, maxVein);
        int placed = 0;
        int attempts = 0;
        int maxAttempts = veinSize * 8; // avoid infinite loops

        while (placed < veinSize && attempts++ < maxAttempts) {
            int lx = RandomUtil.nextInt(0, 15);
            int lz = RandomUtil.nextInt(0, 15);
            int y  = RandomUtil.nextInt(minY, maxY);

            Block block = chunk.getBlock(lx, y, lz);
            if (isValidHost(block)) {
                block.setType(ORE_MATERIAL, false); // false = skip physics update for performance
                oreManager.registerOre(block.getLocation());
                placed++;
                NCLogger.debug("Placed plutonium ore at %d,%d,%d (chunk %d,%d)",
                        block.getX(), block.getY(), block.getZ(), chunk.getX(), chunk.getZ());
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────

    private boolean isValidHost(Block block) {
        Material m = block.getType();
        return m == Material.DEEPSLATE
                || m == Material.STONE
                || m == Material.COBBLED_DEEPSLATE
                || m == Material.TUFF;
    }
}
