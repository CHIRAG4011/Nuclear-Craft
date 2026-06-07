package com.nuclearcraft.listeners;

import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.titantech.TitanForgeData;
import com.nuclearcraft.titantech.TitanForgeGUI;
import com.nuclearcraft.titantech.TitanForgeManager;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Optional;

/**
 * Handles Titan Reactor Forge block-level events:
 *  - Place: register machine with TitanForgeManager
 *  - Break: unregister and drop the custom forge item
 *  - Right-click: open GUI (via TitanForgeManager.openGUI)
 *  - Inventory close: notify manager to clean up GUI reference
 */
public class TitanForgeListener implements Listener {

    private final TitanForgeManager forgeManager;
    private final ItemManager itemManager;
    private final NamespacedKey itemIdKey;

    public TitanForgeListener(TitanForgeManager forgeManager, ItemManager itemManager, Plugin plugin) {
        this.forgeManager = forgeManager;
        this.itemManager  = itemManager;
        this.itemIdKey    = new NamespacedKey(plugin, "nuclearcraft_item_id");
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack inHand = event.getItemInHand();
        if (!isTitanForgeItem(inHand)) return;

        Block placed = event.getBlockPlaced();
        forgeManager.registerMachine(placed, event.getPlayer().getUniqueId());
        event.getPlayer().sendMessage("§5☢ Titan Reactor Forge placed! Right-click to open.");
        NCLogger.debug("Titan Forge placed by " + event.getPlayer().getName() + " at " + placed.getLocation());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!forgeManager.isTitanForge(block)) return;

        event.setDropItems(false);
        itemManager.getItem("titan-reactor-forge")
                .ifPresent(ci -> block.getWorld().dropItemNaturally(
                        block.getLocation().add(0.5, 0.5, 0.5), ci.build(1)));

        forgeManager.unregisterMachine(block);
        event.getPlayer().sendMessage("§5☢ Titan Reactor Forge dismantled.");
        NCLogger.debug("Titan Forge broken by " + event.getPlayer().getName() + " at " + block.getLocation());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getAction().isRightClick()) return;
        Block clicked = event.getClickedBlock();
        if (clicked == null || clicked.getType() != Material.CRYING_OBSIDIAN) return;
        if (!forgeManager.isTitanForge(clicked)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.SPECTATOR) return;

        forgeManager.getForgeAt(clicked).ifPresent(forge -> forgeManager.openGUI(player, forge));
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof TitanForgeGUI)) return;
        forgeManager.onGUIClosed(event.getPlayer().getUniqueId());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private boolean isTitanForgeItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        String id = item.getItemMeta().getPersistentDataContainer()
                .get(itemIdKey, PersistentDataType.STRING);
        return "titan-reactor-forge".equals(id);
    }
}
