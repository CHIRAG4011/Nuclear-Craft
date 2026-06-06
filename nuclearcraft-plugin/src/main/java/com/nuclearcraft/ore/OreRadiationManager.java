package com.nuclearcraft.ore;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.radiation.RadiationManager;
import com.nuclearcraft.radiation.RadiationSource;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * Applies passive radiation to players standing near Plutonium Ore blocks.
 *
 * Radiation tiers (configurable):
 *   ≤ 1 block → 5 radiation / 5 seconds
 *   ≤ 2 blocks → 2 radiation / 5 seconds
 *   ≤ 3 blocks → 1 radiation / 5 seconds
 *
 * Implementation:
 *   One task runs every 100 ticks (5 seconds).
 *   For each online player in the Overworld:
 *     Check a 7×7×7 bounding box around the player against the ore tracking set.
 *   This keeps the per-tick cost O(players × ~343 lookups) regardless of world size.
 *
 * Future hook: hazmat/plutonium armor can multiply down the exposure.
 */
public class OreRadiationManager {

    private final NuclearCraftPlugin plugin;
    private final ConfigManager configManager;
    private final RadiationManager radiationManager;
    private final PlutoniumOreManager oreManager;

    private BukkitTask proximityTask;

    public OreRadiationManager(NuclearCraftPlugin plugin, ConfigManager configManager,
                                RadiationManager radiationManager, PlutoniumOreManager oreManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.radiationManager = radiationManager;
        this.oreManager = oreManager;
    }

    public void initialize() {
        startProximityTask();
        NCLogger.info("OreRadiationManager initialized.");
    }

    public void shutdown() {
        if (proximityTask != null) proximityTask.cancel();
        NCLogger.info("OreRadiationManager shut down.");
    }

    // ──────────────────────────────────────────────────────────────────────────

    private void startProximityTask() {
        proximityTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (player.getWorld().getEnvironment() != World.Environment.NORMAL) continue;
                    applyOreProximityRadiation(player);
                }
            }
        }.runTaskTimer(plugin, 100L, 100L);
    }

    private void applyOreProximityRadiation(Player player) {
        int rad1 = configManager.getOre().getInt("plutonium-ore.radiation.tier1-radiation", 5);  // ≤1 block
        int rad2 = configManager.getOre().getInt("plutonium-ore.radiation.tier2-radiation", 2);  // ≤2 blocks
        int rad3 = configManager.getOre().getInt("plutonium-ore.radiation.tier3-radiation", 1);  // ≤3 blocks
        int maxRadius = 3;

        Location pLoc = player.getLocation();
        int px = pLoc.getBlockX();
        int py = pLoc.getBlockY();
        int pz = pLoc.getBlockZ();
        World world = player.getWorld();

        int maxRadiation = 0;

        for (int dx = -maxRadius; dx <= maxRadius; dx++) {
            for (int dy = -maxRadius; dy <= maxRadius; dy++) {
                for (int dz = -maxRadius; dz <= maxRadius; dz++) {
                    Location candidate = new Location(world, px + dx, py + dy, pz + dz);
                    if (!oreManager.isOre(candidate)) continue;

                    // Chebyshev distance (block distance)
                    int dist = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
                    int tierRad;
                    if (dist <= 1)      tierRad = rad1;
                    else if (dist <= 2) tierRad = rad2;
                    else                tierRad = rad3;

                    if (tierRad > maxRadiation) maxRadiation = tierRad;
                }
            }
        }

        if (maxRadiation > 0) {
            // Armor protection (future hook: check for hazmat/plutonium armor)
            double protectionMultiplier = getProtectionMultiplier(player);
            int finalRad = (int) Math.ceil(maxRadiation * protectionMultiplier);
            if (finalRad > 0) {
                radiationManager.addRadiation(player, finalRad, RadiationSource.PLUTONIUM_ORE);
            }
        }
    }

    /**
     * Returns the fraction of radiation that passes through armor protection.
     * 1.0 = no protection, 0.2 = 80% reduction (hazmat), 0.0 = 100% (plutonium armor).
     * Future armor integration will read item PDC here.
     */
    private double getProtectionMultiplier(Player player) {
        // Stub — Phase 5+ will check armor PDC for hazmat/plutonium bonuses
        return 1.0;
    }
}
