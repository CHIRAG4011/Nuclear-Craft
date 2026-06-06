package com.nuclearcraft.equipment;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.radiation.RadiationManager;
import com.nuclearcraft.radiation.RadiationSource;
import com.nuclearcraft.upgrade.UpgradeManager;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;

/**
 * Handles the Radiation Aura passive for MK-IV equipment.
 *
 * When a player has any MK-IV Plutonium item equipped, a periodic aura
 * applies radiation to nearby hostile mobs and (optionally) nearby players.
 *
 * The aura ticks every {@code aura.tick-period} server ticks (default 40 = 2s).
 */
public class RadiationAuraManager {

    private final NuclearCraftPlugin plugin;
    private final ConfigManager configManager;
    private final RadiationManager radiationManager;
    private final UpgradeManager upgradeManager;

    private BukkitTask tickTask;

    private boolean enabled;
    private double radius;
    private int mobRadiationPerTick;
    private int pvpRadiationPerTick;
    private int tickPeriod;
    private boolean affectAllies;

    public RadiationAuraManager(NuclearCraftPlugin plugin,
                                 ConfigManager configManager,
                                 RadiationManager radiationManager,
                                 UpgradeManager upgradeManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.radiationManager = radiationManager;
        this.upgradeManager = upgradeManager;
    }

    public void initialize() {
        loadConfig();
        if (enabled) {
            scheduleTask();
            NCLogger.info("RadiationAuraManager initialized (radius=" + radius
                    + ", period=" + tickPeriod + " ticks).");
        } else {
            NCLogger.info("RadiationAuraManager initialized — aura disabled in config.");
        }
    }

    public void shutdown() {
        if (tickTask != null && !tickTask.isCancelled()) {
            tickTask.cancel();
            tickTask = null;
        }
    }

    // ──────────────────────────────────────────────────────────────────────────

    private void loadConfig() {
        var forge = configManager.getForge();
        enabled              = forge.getBoolean("aura.enabled", true);
        radius               = forge.getDouble("aura.radius", 3.0);
        mobRadiationPerTick  = forge.getInt("aura.mob-radiation-per-tick", 25);
        pvpRadiationPerTick  = forge.getInt("aura.pvp-radiation-per-tick", 15);
        tickPeriod           = forge.getInt("aura.tick-period", 40);
        affectAllies         = forge.getBoolean("aura.affect-allies", false);
    }

    private void scheduleTask() {
        tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, tickPeriod, tickPeriod);
    }

    // ──────────────────────────────────────────────────────────────────────────

    private void tick() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!hasAnyMkIVEquipped(player)) continue;

            Location loc = player.getLocation();
            World world = loc.getWorld();
            if (world == null) continue;

            // Visual aura effect
            spawnAuraParticles(loc);

            // Apply radiation to nearby hostile mobs
            Collection<Entity> nearby = world.getNearbyEntities(loc, radius, radius, radius,
                    e -> e instanceof Monster && !(e instanceof Player));
            for (Entity entity : nearby) {
                if (entity instanceof LivingEntity living) {
                    living.damage(mobRadiationPerTick / 10.0, player);
                }
            }

            // Apply radiation to nearby players (PvP)
            if (!affectAllies) {
                Collection<Entity> nearbyPlayers = world.getNearbyEntities(loc, radius, radius, radius,
                        e -> e instanceof Player && !e.equals(player));
                for (Entity entity : nearbyPlayers) {
                    if (entity instanceof Player target) {
                        radiationManager.addRadiation(target, pvpRadiationPerTick, RadiationSource.EQUIPMENT_AURA);
                    }
                }
            }
        }
    }

    private boolean hasAnyMkIVEquipped(Player player) {
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (upgradeManager.hasAura(item)) return true;
        }
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off  = player.getInventory().getItemInOffHand();
        return upgradeManager.hasAura(main) || upgradeManager.hasAura(off);
    }

    private void spawnAuraParticles(Location center) {
        World world = center.getWorld();
        if (world == null) return;
        double r = radius;
        for (int i = 0; i < 6; i++) {
            double angle = (2 * Math.PI / 6) * i;
            double x = center.getX() + r * Math.cos(angle);
            double z = center.getZ() + r * Math.sin(angle);
            Location particle = new Location(world, x, center.getY() + 1, z);
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, particle, 1, 0, 0.2, 0, 0.01);
        }
    }
}
