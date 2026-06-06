package com.nuclearcraft.listeners;

import com.nuclearcraft.advancements.AdvancementManager;
import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.items.CustomItem;
import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.smelter.SmelterData;
import com.nuclearcraft.smelter.NuclearSmelterManager;
import com.nuclearcraft.utils.ColorUtil;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Handles all events related to the Nuclear Smelter block/machine.
 *
 * Covers:
 *  - Block placement detection and machine registration
 *  - Block breaking — unregistration and item drops
 *  - GUI opening on right-click
 *  - Inventory click/drag routing (functional vs display slots)
 *  - Vanilla furnace blocking for radioactive items
 *  - Advancement triggers on placement
 */
public class SmelterListener implements Listener {

    private static final String SMELTER_ITEM_ID = "nuclear-smelter";

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final ItemManager itemManager;
    private final PlayerDataManager playerDataManager;
    private final AdvancementManager advancementManager;
    private final NuclearSmelterManager smelterManager;

    public SmelterListener(JavaPlugin plugin, ConfigManager configManager,
                            ItemManager itemManager, PlayerDataManager playerDataManager,
                            AdvancementManager advancementManager,
                            NuclearSmelterManager smelterManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.itemManager = itemManager;
        this.playerDataManager = playerDataManager;
        this.advancementManager = advancementManager;
        this.smelterManager = smelterManager;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Block Place
    // ──────────────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();

        String itemId = CustomItem.getId(plugin, item);
        if (!SMELTER_ITEM_ID.equals(itemId)) return;

        Block placed = event.getBlockPlaced();

        smelterManager.registerMachine(placed, player.getUniqueId());

        playerDataManager.get(player).ifPresent(data -> {
            data.incrementMachinesBuilt();
            advancementManager.award(player, AdvancementManager.Advancement.INDUSTRIAL_AGE);
        });

        player.sendMessage(ColorUtil.parse(
                "<dark_gray>[</dark_gray><gradient:#39ff14:#00bfff>Nuclear Smelter</gradient><dark_gray>]</dark_gray> "
                + "<gray>Machine placed. Right-click to open.</gray>"));

        NCLogger.debug("Nuclear Smelter placed by %s at %s",
                player.getName(), SmelterData.serializeLocation(placed.getLocation()));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Block Break
    // ──────────────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!smelterManager.isMachine(block)) return;

        event.setDropItems(false);

        List<ItemStack> contents = smelterManager.unregisterMachine(block);
        for (ItemStack drop : contents) {
            block.getWorld().dropItemNaturally(block.getLocation(), drop);
        }

        itemManager.getItem(SMELTER_ITEM_ID).ifPresent(ci ->
                block.getWorld().dropItemNaturally(block.getLocation(), ci.build()));

        NCLogger.debug("Nuclear Smelter broken at %s",
                SmelterData.serializeLocation(block.getLocation()));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GUI Open
    // ──────────────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        if (!smelterManager.isMachine(block)) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        SmelterData machine = smelterManager.getMachine(block);
        if (machine == null) return;

        machine.setLastInteractingPlayerUuid(player.getUniqueId());
        machine.refreshDisplay();

        player.openInventory(machine.getInventory());
        NCLogger.debug("%s opened smelter at %s", player.getName(), machine.getLocationKey());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Inventory Click Routing
    // ──────────────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof SmelterData machine)) return;

        int rawSlot = event.getRawSlot();
        int invSize = machine.getInventory().getSize();

        // Player inventory slots — allow freely
        if (rawSlot >= invSize) return;

        // Track interacting player
        if (event.getWhoClicked() instanceof Player p) {
            machine.setLastInteractingPlayerUuid(p.getUniqueId());
        }

        if (SmelterData.isFunctional(rawSlot)) {
            // Output slot: prevent placing items in (only take)
            if (rawSlot == SmelterData.OUTPUT_SLOT) {
                ItemStack cursor = event.getCursor();
                if (cursor != null && !cursor.getType().isAir()) {
                    event.setCancelled(true);
                    return;
                }
            }

            // Fuel slot: only allow valid fuels
            if (rawSlot == SmelterData.FUEL_SLOT) {
                ItemStack cursor = event.getCursor();
                if (cursor != null && !cursor.getType().isAir()) {
                    if (!smelterManager.isValidFuel(cursor.getType())) {
                        event.setCancelled(true);
                        if (event.getWhoClicked() instanceof Player p) {
                            p.sendMessage(ColorUtil.parse(
                                    "<red>That item cannot be used as fuel in the Nuclear Smelter.</red>"));
                        }
                        return;
                    }
                }
            }
            return; // allow the interaction
        }

        // All non-functional slots: cancel
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof SmelterData machine)) return;

        int invSize = machine.getInventory().getSize();
        for (int slot : event.getRawSlots()) {
            if (slot < invSize && !SmelterData.isFunctional(slot)) {
                event.setCancelled(true);
                return;
            }
        }

        // Validate fuel if dragging into the fuel slot
        if (event.getRawSlots().contains(SmelterData.FUEL_SLOT)) {
            ItemStack dragged = event.getOldCursor();
            if (dragged != null && !dragged.getType().isAir()
                    && !smelterManager.isValidFuel(dragged.getType())) {
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player p) {
                    p.sendMessage(ColorUtil.parse(
                            "<red>That item cannot be used as fuel in the Nuclear Smelter.</red>"));
                }
            }
        }

        if (event.getWhoClicked() instanceof Player p) {
            machine.setLastInteractingPlayerUuid(p.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof SmelterData machine)) return;
        machine.refreshDisplay();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Block vanilla furnace smelting of radioactive items
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Prevents Raw Plutonium Fragments from being smelted in vanilla furnaces.
     * The Nuclear Smelter is the only valid refining machine.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        String id = CustomItem.getId(plugin, event.getSource());
        if ("raw-plutonium-fragment".equals(id)) {
            event.setCancelled(true);
            NCLogger.debug("Blocked vanilla furnace smelting of raw-plutonium-fragment.");
        }
    }
}
