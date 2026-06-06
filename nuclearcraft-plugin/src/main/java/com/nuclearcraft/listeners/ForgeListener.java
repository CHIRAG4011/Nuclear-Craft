package com.nuclearcraft.listeners;

import com.nuclearcraft.forge.ForgeData;
import com.nuclearcraft.forge.NuclearForgeManager;
import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.utils.NCLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * Handles all block-level and GUI interaction events for the Nuclear Forge.
 *
 * Block detection: the Forge item (SMITHING_TABLE + CMD 1401 + PDC id = "nuclear-forge")
 * is recognized by its {@code nuclearcraft_item_id} PDC key both in hand and once placed.
 * Since a placed SMITHING_TABLE doesn't carry item PDC, we track placed forges by location
 * via {@link NuclearForgeManager#isForge(Block)}.
 */
public class ForgeListener implements Listener {

    private final NuclearForgeManager forgeManager;
    private final ItemManager itemManager;
    private final NamespacedKey itemIdKey;

    public ForgeListener(NuclearForgeManager forgeManager, ItemManager itemManager,
                          org.bukkit.plugin.Plugin plugin) {
        this.forgeManager = forgeManager;
        this.itemManager  = itemManager;
        this.itemIdKey    = new NamespacedKey(plugin, "nuclearcraft_item_id");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Block place: register the forge
    // ──────────────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack inHand = event.getItemInHand();
        if (!isForgeItem(inHand)) return;

        Block placed = event.getBlockPlaced();
        ForgeData forge = forgeManager.registerMachine(placed, event.getPlayer().getUniqueId());

        event.getPlayer().sendMessage(Component.text(
                "☢ Nuclear Forge placed! Right-click to open.", NamedTextColor.GREEN));
        NCLogger.debug("Forge placed by " + event.getPlayer().getName() + " at " + placed.getLocation());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Block break: unregister and drop the forge item
    // ──────────────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!forgeManager.isForge(block)) return;

        event.setDropItems(false); // We'll drop the custom forge item instead

        // Drop the custom nuclear-forge item
        itemManager.getItem("nuclear-forge").ifPresent(ci -> {
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), ci.build(1));
        });

        forgeManager.unregisterMachine(block);

        event.getPlayer().sendMessage(Component.text("☢ Nuclear Forge dismantled.", NamedTextColor.YELLOW));
        NCLogger.debug("Forge broken by " + event.getPlayer().getName() + " at " + block.getLocation());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Right-click: open GUI
    // ──────────────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null || clicked.getType() != Material.SMITHING_TABLE) return;
        if (!forgeManager.isForge(clicked)) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.SPECTATOR) return;

        forgeManager.getForgeAt(clicked).ifPresent(forge -> {
            forgeManager.openGUI(player, forge);
        });
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GUI click routing
    // ──────────────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof com.nuclearcraft.forge.NuclearForgeGUI)) return;
        // Delegated to GuiMenu.handleClick via GUIManager — this fires automatically
        // through the existing GUIManager's InventoryClickEvent handler.
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof com.nuclearcraft.forge.NuclearForgeGUI)) return;
        forgeManager.onGUIClosed(event.getPlayer().getUniqueId());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private boolean isForgeItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        String id = item.getItemMeta().getPersistentDataContainer()
                .get(itemIdKey, PersistentDataType.STRING);
        return "nuclear-forge".equals(id);
    }
}
