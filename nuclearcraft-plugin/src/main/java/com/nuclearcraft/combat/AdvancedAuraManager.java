package com.nuclearcraft.combat;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.radiation.RadiationManager;
import com.nuclearcraft.radiation.RadiationSource;
import com.nuclearcraft.upgrade.UpgradeManager;
import com.nuclearcraft.upgrade.UpgradeTier;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;

/**
 * Phase 9 Advanced Radiation Aura — supersedes Phase 8 {@link com.nuclearcraft.equipment.RadiationAuraManager}.
 *
 * Improvements over Phase 8:
 * <ul>
 *   <li>Team-aware: respects Bukkit scoreboard team membership (won't irradiate allies).</li>
 *   <li>Statistics tracking via {@link CombatStatisticsManager}.</li>
 *   <li>Visual effects via {@link CombatVisualManager}.</li>
 *   <li>Separate mob and PvP radiation amounts.</li>
 * </ul>
 *
 * The aura ticks every {@code radiation.aura.tick-period} ticks.
 * Only players with at least one MK-IV item equipped emit the aura.
 */
public class AdvancedAuraManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final RadiationManager radiationManager;
    private final UpgradeManager upgradeManager;
    private final PvPRadiationManager pvpManager;
    private final CombatVisualManager visualManager;
    private final CombatStatisticsManager statsManager;

    private boolean enabled;
    private double radius;
    private int mobRadPerTick;
    private int pvpRadPerTick;
    private int tickPeriod;

    private BukkitTask tickTask;

    public AdvancedAuraManager(JavaPlugin plugin,
                                ConfigManager configManager,
                                RadiationManager radiationManager,
                                UpgradeManager upgradeManager,
                                PvPRadiationManager pvpManager,
                                CombatVisualManager visualManager,
                                CombatStatisticsManager statsManager) {
        this.plugin          = plugin;
        this.configManager   = configManager;
        this.radiationManager = radiationManager;
        this.upgradeManager  = upgradeManager;
        this.pvpManager      = pvpManager;
        this.visualManager   = visualManager;
        this.statsManager    = statsManager;
    }

    public void initialize() {
        loadConfig();
        if (enabled) {
            scheduleTask();
            NCLogger.info("AdvancedAuraManager initialized (radius=" + radius
                    + ", period=" + tickPeriod + " ticks, mob=" + mobRadPerTick
                    + " pvp=" + pvpRadPerTick + ").");
        } else {
            NCLogger.info("AdvancedAuraManager initialized — aura disabled in config.");
        }
    }

    public void shutdown() {
        if (tickTask != null && !tickTask.isCancelled()) {
            tickTask.cancel();
            tickTask = null;
        }
    }

    private void loadConfig() {
        var cfg = configManager.getCombat();
        enabled      = cfg.getBoolean("radiation.aura.enabled", true);
        radius       = cfg.getDouble("radiation.aura.radius", 3.0);
        mobRadPerTick= cfg.getInt("radiation.aura.mob-per-tick", 25);
        pvpRadPerTick= cfg.getInt("radiation.aura.pvp-per-tick", 15);
        tickPeriod   = cfg.getInt("radiation.aura.tick-period", 40);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Scheduling
    // ──────────────────────────────────────────────────────────────────────────

    private void scheduleTask() {
        tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick,
                tickPeriod, tickPeriod);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Tick
    // ──────────────────────────────────────────────────────────────────────────

    private void tick() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!hasMkIVEquipped(player)) continue;
            applyAura(player);
        }
    }

    private void applyAura(Player source) {
        Collection<Entity> nearby = source.getWorld().getNearbyEntities(
                source.getLocation(), radius, radius, radius,
                e -> e != source && e instanceof LivingEntity);

        if (nearby.isEmpty()) return;

        visualManager.spawnAuraEffect(source.getLocation());

        for (Entity e : nearby) {
            if (e instanceof Player target) {
                if (pvpManager.isValidPvPTarget(source, target)) {
                    radiationManager.addRadiation(target, pvpRadPerTick, RadiationSource.EQUIPMENT_AURA);
                    pvpManager.recordRadiationAttacker(source, target);
                    statsManager.recordAuraDamage(source, pvpRadPerTick);
                    visualManager.spawnAuraEffect(target.getLocation());
                }
            } else if (e instanceof Mob mob) {
                mob.damage(mobRadPerTick * 0.1, source);
                visualManager.spawnAuraEffect(mob.getLocation());
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // MK-IV detection
    // ──────────────────────────────────────────────────────────────────────────

    private boolean hasMkIVEquipped(Player player) {
        var inv = player.getInventory();
        ItemStack[] slots = {
                inv.getHelmet(), inv.getChestplate(),
                inv.getLeggings(), inv.getBoots(),
                inv.getItemInMainHand(), inv.getItemInOffHand()
        };
        for (ItemStack item : slots) {
            if (item == null || item.getType().isAir()) continue;
            if (upgradeManager.getTier(item) == UpgradeTier.MK_IV) return true;
        }
        return false;
    }
}
