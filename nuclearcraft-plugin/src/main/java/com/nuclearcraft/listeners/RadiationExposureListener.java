package com.nuclearcraft.listeners;

import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.items.CustomItem;
import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.ore.OreMiningManager;
import com.nuclearcraft.radiation.RadiationManager;
import com.nuclearcraft.radiation.RadiationSource;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Optional;

/**
 * Handles radiation exposure from:
 *
 *   1. FRAGMENT CARRYING: Players holding Raw Plutonium Fragments in their inventory
 *      gain 1 radiation every 30 seconds (scales with fragment count).
 *
 *   2. STORAGE LEAKAGE: Players near containers holding plutonium fragments
 *      receive small radiation exposure (configurable; stub for Phase 5).
 *
 * Implementation:
 *   Two recurring tasks — one for inventory (every 600 ticks = 30s),
 *   one for storage leakage (every 200 ticks = 10s, disabled by default).
 *
 * Performance:
 *   Inventory scans iterate only over online players once per 30 seconds.
 *   No chunk or block scanning is performed here.
 */
public class RadiationExposureListener {

    private final NuclearCraftPlugin plugin;
    private final ConfigManager configManager;
    private final RadiationManager radiationManager;
    private final ItemManager itemManager;
    private final OreMiningManager miningManager;

    private BukkitTask fragmentTask;

    public RadiationExposureListener(NuclearCraftPlugin plugin, ConfigManager configManager,
                                      RadiationManager radiationManager, ItemManager itemManager,
                                      OreMiningManager miningManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.radiationManager = radiationManager;
        this.itemManager = itemManager;
        this.miningManager = miningManager;
    }

    public void initialize() {
        startFragmentRadiationTask();
        NCLogger.info("RadiationExposureListener initialized.");
    }

    public void shutdown() {
        if (fragmentTask != null) fragmentTask.cancel();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Fragment carrying radiation (every 30 seconds)
    // ──────────────────────────────────────────────────────────────────────────

    private void startFragmentRadiationTask() {
        double baseRad = configManager.getOre().getDouble("plutonium-ore.fragment.radiation-per-30s", 1.0);
        boolean scaleByCount = configManager.getOre().getBoolean("plutonium-ore.fragment.scale-by-count", false);

        fragmentTask = new BukkitRunnable() {
            @Override
            public void run() {
                Optional<CustomItem> fragmentOpt = itemManager.getItem("raw-plutonium-fragment");
                if (fragmentOpt.isEmpty()) return;
                CustomItem fragmentItem = fragmentOpt.get();

                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    int total = countFragments(player, fragmentItem);
                    if (total == 0) continue;

                    // Future: check for protection gear (hazmat/plutonium armor)
                    double protection = 1.0; // stub

                    double amount;
                    if (scaleByCount) {
                        amount = baseRad * total * protection;
                    } else {
                        amount = baseRad * protection;
                    }

                    if (amount >= 1.0) {
                        radiationManager.addRadiation(player, (int) Math.ceil(amount),
                                RadiationSource.PLUTONIUM_FRAGMENT);
                    }
                }
            }
        }.runTaskTimer(plugin, 600L, 600L);
    }

    // ──────────────────────────────────────────────────────────────────────────

    /** Counts total raw-plutonium-fragment items across the player's full inventory. */
    private int countFragments(Player player, CustomItem fragmentItem) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && fragmentItem.matches(item)) {
                total += item.getAmount();
            }
        }
        return total;
    }
}
