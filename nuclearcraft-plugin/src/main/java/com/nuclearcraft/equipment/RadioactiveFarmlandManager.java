package com.nuclearcraft.equipment;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.data.PlayerData;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.radiation.RadiationManager;
import com.nuclearcraft.radiation.RadiationSource;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collections;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Radioactive Farmland — a supercharged farmland created by the Plutonium Hoe.
 *
 * <h3>Creation</h3>
 * The Plutonium Hoe right-clicked on Grass Block, Dirt, or Coarse Dirt converts
 * the block to FARMLAND and registers it here (handled by {@link EquipmentListener}).
 * Normal FARMLAND is never created.
 *
 * <h3>Effects</h3>
 * <ul>
 *   <li>Crop growth: +50% random-tick acceleration (via {@link BlockGrowEvent}).</li>
 *   <li>Players standing on or 1 block above receive periodic radiation.</li>
 *   <li>Toxic green particles rise from the surface.</li>
 * </ul>
 */
public class RadioactiveFarmlandManager {

    private static final Set<Material> TILLABLE = Set.of(
            Material.GRASS_BLOCK, Material.DIRT, Material.COARSE_DIRT);

    private static final Random RANDOM = new Random();

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final RadiationManager radiationManager;
    private final PlayerDataManager playerDataManager;

    private final Set<Location> farmlandLocations =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    private BukkitTask radiationTask;
    private BukkitTask particleTask;

    public RadioactiveFarmlandManager(JavaPlugin plugin, ConfigManager configManager,
                                      RadiationManager radiationManager,
                                      PlayerDataManager playerDataManager) {
        this.plugin             = plugin;
        this.configManager      = configManager;
        this.radiationManager   = radiationManager;
        this.playerDataManager  = playerDataManager;
    }

    public void initialize() {
        startRadiationTask();
        startParticleTask();
        NCLogger.info("RadioactiveFarmlandManager initialized.");
    }

    public void shutdown() {
        if (radiationTask != null) radiationTask.cancel();
        if (particleTask  != null) particleTask.cancel();
        farmlandLocations.clear();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Checks whether the given block's material can be tilled into Radioactive Farmland.
     */
    public boolean isTillable(Block block) {
        return TILLABLE.contains(block.getType());
    }

    /**
     * Converts the given block to Radioactive Farmland.
     * Called from {@link EquipmentListener} when the Plutonium Hoe right-clicks a tillable block.
     */
    public void createFarmland(Block block, Player player) {
        block.setType(Material.FARMLAND);
        farmlandLocations.add(block.getLocation().clone());
        playerDataManager.get(player.getUniqueId()).ifPresent(data ->
                data.setFarmlandCreated(data.getFarmlandCreated() + 1));
        NCLogger.debug("Radioactive Farmland created at %s by %s", block.getLocation(), player.getName());
    }

    /**
     * Returns true if the location is a tracked Radioactive Farmland block.
     */
    public boolean isFarmland(Location loc) {
        return farmlandLocations.contains(loc.getBlock().getLocation());
    }

    /**
     * Removes the farmland tracking (called when block is broken or trampled).
     */
    public void removeFarmland(Location loc) {
        farmlandLocations.remove(loc.getBlock().getLocation());
    }

    /**
     * Called from {@link EquipmentListener} on {@link BlockGrowEvent}.
     * If the crop is growing on (or adjacent to) Radioactive Farmland, gives a 50% chance
     * to fire an additional grow event by triggering a second growth immediately.
     */
    public void handleCropGrow(BlockGrowEvent event) {
        Block crop = event.getBlock();
        Block below = crop.getRelative(0, -1, 0);
        if (!isFarmland(below.getLocation())) return;

        double bonus = configManager.getEquipment().getDouble("farmland.crop-growth-bonus", 0.50);
        if (RANDOM.nextDouble() < bonus) {
            // Schedule an extra growth tick after current one processes
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (crop.getType().isAir()) return; // already harvested
                org.bukkit.block.data.Ageable ageable;
                if (crop.getBlockData() instanceof org.bukkit.block.data.Ageable a) {
                    ageable = a;
                    if (ageable.getAge() < ageable.getMaximumAge()) {
                        ageable.setAge(ageable.getAge() + 1);
                        crop.setBlockData(ageable);
                    }
                }
            }, 1L);
        }
    }

    public int getFarmlandCount() {
        return farmlandLocations.size();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Tasks
    // ──────────────────────────────────────────────────────────────────────────

    private void startRadiationTask() {
        long interval = configManager.getEquipment().getLong("farmland.radiation-interval-ticks", 100L);
        int  amount   = configManager.getEquipment().getInt("farmland.radiation-amount", 1);

        radiationTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (farmlandLocations.isEmpty()) return;
            farmlandLocations.removeIf(loc -> {
                Block block = loc.getBlock();
                if (block.getType() != Material.FARMLAND) return true;
                irradiateAbove(loc, amount);
                return false;
            });
        }, interval, interval);
    }

    private void startParticleTask() {
        particleTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Location loc : farmlandLocations) {
                loc.getWorld().spawnParticle(Particle.DUST,
                        loc.clone().add(0.5, 1.0, 0.5),
                        4, 0.5, 0.05, 0.5, 0,
                        new Particle.DustOptions(Color.LIME, 0.8f));
            }
        }, 20L, 20L);
    }

    private void irradiateAbove(Location farmlandLoc, int amount) {
        Location above = farmlandLoc.clone().add(0.5, 1, 0.5);
        for (Player player : farmlandLoc.getWorld().getNearbyPlayers(above, 1.0, 1.5, 1.0)) {
            radiationManager.addRadiation(player, amount, RadiationSource.RADIOACTIVE_FARMLAND);
        }
    }
}
