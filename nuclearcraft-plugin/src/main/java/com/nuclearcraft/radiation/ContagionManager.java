package com.nuclearcraft.radiation;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.events.RadiationSpreadEvent;
import com.nuclearcraft.utils.NCLogger;
import com.nuclearcraft.utils.RandomUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;

import java.util.Collection;

/**
 * Manages the spread of radiation between players.
 *
 * Three spread vectors:
 *   1. Proximity  — checked every 5 sec; stage-dependent chance in 3-block radius.
 *   2. Contact    — triggered on hit via {@link #handlePhysicalContact(Player, Player)}.
 *   3. Vehicle    — checked every 15 sec; shared boat/minecart passengers.
 */
public class ContagionManager {

    private final NuclearCraftPlugin plugin;
    private final ConfigManager configManager;
    private final RadiationManager radiationManager;

    public ContagionManager(NuclearCraftPlugin plugin, ConfigManager configManager,
                             RadiationManager radiationManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.radiationManager = radiationManager;
    }

    public void initialize() {
        scheduleProximityTask();
        scheduleVehicleTask();
        NCLogger.info("ContagionManager initialized.");
    }

    public void shutdown() {
        NCLogger.info("ContagionManager shut down.");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Spread vectors
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Called by {@link com.nuclearcraft.listeners.RadiationListener} on EntityDamageByEntityEvent.
     * Applies 50 radiation to the victim with a 35% chance.
     *
     * @param attacker the infected attacker
     * @param victim   the player who was hit
     */
    public void handlePhysicalContact(Player attacker, Player victim) {
        if (!radiationManager.isContagious(attacker)) return;
        double chance = configManager.getRadiation().getDouble("contagion.physical-contact-chance", 0.35);
        if (!RandomUtil.chance(chance)) return;
        int amount = configManager.getRadiation().getInt("contagion.physical-contact-amount", 50);
        fireSpreadEvent(attacker, victim, amount, RadiationSpreadEvent.SpreadType.PHYSICAL_CONTACT);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Scheduled tasks
    // ──────────────────────────────────────────────────────────────────────────

    /** Every 5 seconds: scan nearby players around each contagious player. */
    private void scheduleProximityTask() {
        long interval = configManager.getRadiation().getLong("contagion.proximity-interval-ticks", 100L);
        plugin.getTaskManager().scheduleSync(() -> {
            for (Player infected : plugin.getServer().getOnlinePlayers()) {
                if (!radiationManager.isContagious(infected)) continue;
                int stage = radiationManager.getStage(infected);
                double spreadChance = getProximityChance(stage);
                double range = configManager.getRadiation().getDouble("contagion.proximity-range", 3.0);
                int amount = configManager.getRadiation().getInt("contagion.proximity-amount", 25);

                Collection<Entity> nearby = infected.getWorld().getNearbyEntities(
                        infected.getLocation(), range, range, range,
                        e -> e instanceof Player && !e.equals(infected));

                for (Entity entity : nearby) {
                    if (!RandomUtil.chance(spreadChance)) continue;
                    fireSpreadEvent(infected, (Player) entity, amount, RadiationSpreadEvent.SpreadType.PROXIMITY);
                }
            }
        }, interval, interval);
    }

    /** Every 15 seconds: check passengers sharing a vehicle with an infected player. */
    private void scheduleVehicleTask() {
        long interval = configManager.getRadiation().getLong("contagion.vehicle-interval-ticks", 300L);
        plugin.getTaskManager().scheduleSync(() -> {
            double vehicleChance = configManager.getRadiation().getDouble("contagion.vehicle-chance", 0.20);
            int vehicleAmount = configManager.getRadiation().getInt("contagion.vehicle-amount", 25);

            for (Player infected : plugin.getServer().getOnlinePlayers()) {
                if (!radiationManager.isContagious(infected)) continue;
                Entity vehicle = infected.getVehicle();
                if (!(vehicle instanceof Vehicle)) continue;

                for (Entity passenger : vehicle.getPassengers()) {
                    if (!(passenger instanceof Player target)) continue;
                    if (target.equals(infected)) continue;
                    if (!RandomUtil.chance(vehicleChance)) continue;
                    fireSpreadEvent(infected, target, vehicleAmount, RadiationSpreadEvent.SpreadType.VEHICLE);
                }
            }
        }, interval, interval);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private double getProximityChance(int stage) {
        return switch (stage) {
            case 2 -> configManager.getRadiation().getDouble("contagion.proximity-chance-stage-2", 0.05);
            case 3 -> configManager.getRadiation().getDouble("contagion.proximity-chance-stage-3", 0.15);
            case 4 -> configManager.getRadiation().getDouble("contagion.proximity-chance-stage-4", 0.25);
            default -> 0.0;
        };
    }

    private void fireSpreadEvent(Player source, Player target, int amount, RadiationSpreadEvent.SpreadType type) {
        RadiationSpreadEvent event = new RadiationSpreadEvent(source, target, amount, type);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return;
        radiationManager.addRadiation(target, event.getAmount(), RadiationSource.RADIATED_PLAYER);
        NCLogger.debug("Contagion [%s]: %s -> %s +%d", type, source.getName(), target.getName(), event.getAmount());
    }
}
