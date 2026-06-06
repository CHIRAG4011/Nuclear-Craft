package com.nuclearcraft.listeners;

import com.nuclearcraft.advancements.AdvancementManager;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.farming.FarmingManager;
import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.radiation.RadiationManager;
import com.nuclearcraft.utils.ColorUtil;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Event listener for Phase 7: Radioactive Farming & Cure System.
 *
 * <p>Handles:
 * <ul>
 *   <li>Mutated Seed planting (right-click Radioactive Farmland)</li>
 *   <li>Mutated Crop growth interception</li>
 *   <li>Mature plant harvesting (right-click)</li>
 *   <li>Toxic Bloom block break cleanup</li>
 *   <li>Farmland physics (trampling cleanup)</li>
 *   <li>Antidote / Serum consumption (right-click)</li>
 *   <li>Crafting statistics tracking</li>
 * </ul>
 */
public class FarmingListener implements Listener {

    private final JavaPlugin plugin;
    private final ItemManager itemManager;
    private final PlayerDataManager playerDataManager;
    private final RadiationManager radiationManager;
    private final AdvancementManager advancementManager;
    private final FarmingManager farmingManager;

    public FarmingListener(JavaPlugin plugin,
                            ItemManager itemManager,
                            PlayerDataManager playerDataManager,
                            RadiationManager radiationManager,
                            AdvancementManager advancementManager,
                            FarmingManager farmingManager) {
        this.plugin             = plugin;
        this.itemManager        = itemManager;
        this.playerDataManager  = playerDataManager;
        this.radiationManager   = radiationManager;
        this.advancementManager = advancementManager;
        this.farmingManager     = farmingManager;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Planting & Harvesting
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Handles right-click interactions:
     * 1. Planting mutated seeds on radioactive farmland.
     * 2. Harvesting fully grown mutated plants.
     * 3. Using Radiation Antidote or Serum.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only handle main-hand interactions to prevent double-firing
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();

        // ── Antidote / Serum consumption ─────────────────────────────────────
        if (event.getAction() == Action.RIGHT_CLICK_AIR
                || event.getAction() == Action.RIGHT_CLICK_BLOCK) {

            if (farmingManager.getAntidoteManager().isAntidote(held)) {
                event.setCancelled(true);
                farmingManager.getAntidoteManager().applyAntidote(player, held);
                return;
            }

            if (farmingManager.getSerumManager().isSerum(held)) {
                event.setCancelled(true);
                farmingManager.getSerumManager().applySerum(player, held);
                return;
            }
        }

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        // ── Seed planting on farmland ─────────────────────────────────────────
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK
                && clicked.getType() == Material.FARMLAND
                && farmingManager.getSeedManager().isMutatedSeed(held)) {
            event.setCancelled(true);
            boolean planted = farmingManager.getSeedManager()
                    .attemptPlant(player, clicked, held);
            if (planted) {
                playerDataManager.get(player).ifPresent(pd -> {
                    pd.incrementSeedsPlanted();
                    pd.markDirty();
                });
                // Nuclear Farmer advancement: first farm created (first seed planted)
                advancementManager.award(player, AdvancementManager.Advancement.NUCLEAR_FARMER);
                NCLogger.debug("Mutated seed planted by %s", player.getName());
            }
            return;
        }

        // ── Harvesting fully grown mutated crop ───────────────────────────────
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK
                && clicked.getType() == Material.WHEAT) {
            if (farmingManager.getCropManager().isMutatedCrop(clicked.getLocation())) {
                event.setCancelled(true);
                farmingManager.getHarvestManager().attemptHarvest(player, clicked.getLocation());
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Growth
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Intercepts BlockGrowEvent for mutated crops and applies custom growth logic.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockGrow(BlockGrowEvent event) {
        if (event.getBlock().getType() != Material.WHEAT) return;
        boolean handled = farmingManager.getGrowthManager().handleGrowth(event);
        if (handled) {
            event.setCancelled(true);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Crop / Bloom break cleanup
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Cleans up tracking when a player breaks a mutated crop or toxic bloom.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        // Mutated crop break — drop nothing, clean tracking
        if (block.getType() == Material.WHEAT
                && farmingManager.getCropManager().isMutatedCrop(block.getLocation())) {
            event.setDropItems(false);
            farmingManager.getCropManager().untrackCrop(block.getLocation());
            NCLogger.debug("Mutated crop broken by %s at %s", event.getPlayer().getName(), block.getLocation());
            return;
        }

        // Toxic bloom break
        if (farmingManager.getToxicBloomManager().isBloom(block.getLocation())) {
            farmingManager.getToxicBloomManager().removeBloom(block.getLocation());
            NCLogger.debug("Toxic Bloom broken at %s", block.getLocation());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Physics (farmland trampling)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Handles block physics: when farmland is trampled, the crop above it dies.
     * Also cleans up if the block above radioactive farmland was a mutated crop.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        Block block = event.getBlock();
        // If a wheat block loses support (farmland beneath is trampled/replaced)
        if (block.getType() == Material.WHEAT) {
            Block below = block.getRelative(0, -1, 0);
            if (below.getType() != Material.FARMLAND) {
                farmingManager.getCropManager().untrackCrop(block.getLocation());
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Crafting statistics
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Tracks crafting statistics for antidotes and serums.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack result = event.getRecipe().getResult();

        if (farmingManager.getAntidoteManager().isAntidote(result)) {
            playerDataManager.get(player).ifPresent(pd -> {
                pd.incrementAntidotesCrafted();
                pd.markDirty();
            });
            NCLogger.debug("%s crafted a Radiation Antidote", player.getName());
        } else if (farmingManager.getSerumManager().isSerum(result)) {
            playerDataManager.get(player).ifPresent(pd -> {
                pd.incrementSerumsCrafted();
                pd.markDirty();
            });
            NCLogger.debug("%s crafted a Radiation Serum", player.getName());
        }
    }
}
