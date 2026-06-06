package com.nuclearcraft.smelter;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.radiation.RadiationManager;
import com.nuclearcraft.radiation.RadiationSource;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;

/**
 * Applies passive radiation to players standing near active Nuclear Smelters.
 *
 * Radiation is emitted only when the machine is in the ACTIVE state.
 * Checks run every N ticks (configurable, default 100 = 5 seconds).
 * Protection: Hazmat armor reduces by 80%, Plutonium armor by 100%.
 * Currently, standard armor reduction from RadiationManager handles this
 * via the armor-reduction system. Future plutonium armor integration will
 * be handled when Phase 6 (armor) is implemented.
 */
public class MachineRadiationManager {

    private final NuclearCraftPlugin plugin;
    private final ConfigManager configManager;
    private final RadiationManager radiationManager;
    private final NuclearSmelterManager smelterManager;

    private BukkitTask radiationTask;

    public MachineRadiationManager(NuclearCraftPlugin plugin, ConfigManager configManager,
                                    RadiationManager radiationManager,
                                    NuclearSmelterManager smelterManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.radiationManager = radiationManager;
        this.smelterManager = smelterManager;
    }

    public void initialize() {
        scheduleRadiationTask();
        NCLogger.info("MachineRadiationManager initialized.");
    }

    public void shutdown() {
        if (radiationTask != null && !radiationTask.isCancelled()) {
            radiationTask.cancel();
            radiationTask = null;
        }
        NCLogger.info("MachineRadiationManager shut down.");
    }

    // ──────────────────────────────────────────────────────────────────────────

    private void scheduleRadiationTask() {
        var cfg = configManager.getSmelter();
        long intervalTicks = cfg.getLong("radiation.check-interval-ticks", 100L);
        int radiationAmount = cfg.getInt("radiation.passive-amount", 8);
        double radius = cfg.getDouble("radiation.passive-radius", 3.0);

        radiationTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            Collection<SmelterData> machines = smelterManager.getAllMachines();
            if (machines.isEmpty()) return;

            for (Player player : plugin.getServer().getOnlinePlayers()) {
                Location playerLoc = player.getLocation();

                for (SmelterData smelter : machines) {
                    if (!smelter.getState().isProcessing()) continue;

                    Location machineLoc = smelter.getLocation();
                    if (!isSameWorld(playerLoc, machineLoc)) continue;

                    double dist = playerLoc.distance(machineLoc);
                    if (dist <= radius) {
                        radiationManager.addRadiation(player, radiationAmount, RadiationSource.NUCLEAR_SMELTER);
                        NCLogger.debug("MachineRadiation: applied %d rad to %s (dist=%.1f from %s)",
                                radiationAmount, player.getName(), dist, smelter.getLocationKey());
                        break; // only apply once per player per tick, even near multiple machines
                    }
                }
            }
        }, intervalTicks, intervalTicks);
    }

    private boolean isSameWorld(Location a, Location b) {
        return a.getWorld() != null && a.getWorld().equals(b.getWorld());
    }
}
