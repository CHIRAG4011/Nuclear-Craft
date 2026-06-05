package com.nuclearcraft.gui;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages open GUI sessions per player.
 * Routes inventory click and close events to the correct GuiMenu instance.
 */
public class GUIManager {

    private final NuclearCraftPlugin plugin;
    private final ConfigManager configManager;

    private final Map<UUID, GuiMenu> openMenus = new ConcurrentHashMap<>();

    public GUIManager(NuclearCraftPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void initialize() {
        NCLogger.info("GUIManager initialized.");
    }

    public void reload() {
        closeAll();
        NCLogger.info("GUIManager reloaded.");
    }

    public void shutdown() {
        closeAll();
    }

    /**
     * Opens a GuiMenu for a player and tracks the session.
     */
    public void open(Player player, GuiMenu menu) {
        openMenus.put(player.getUniqueId(), menu);
        menu.open(player);
    }

    /**
     * Called by the listener on InventoryClickEvent.
     * Returns true if the event was consumed by a NuclearCraft GUI.
     */
    public boolean handleClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof GuiMenu menu)) return false;
        menu.handleClick(event);
        return true;
    }

    /**
     * Called by the listener on InventoryCloseEvent.
     */
    public void handleClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof GuiMenu menu)) return;
        UUID uuid = event.getPlayer().getUniqueId();
        menu.handleClose(event);
        openMenus.remove(uuid);
    }

    public boolean hasOpenMenu(Player player) {
        return openMenus.containsKey(player.getUniqueId());
    }

    public GuiMenu getOpenMenu(Player player) {
        return openMenus.get(player.getUniqueId());
    }

    private void closeAll() {
        for (UUID uuid : openMenus.keySet()) {
            var player = plugin.getServer().getPlayer(uuid);
            if (player != null) {
                player.closeInventory();
            }
        }
        openMenus.clear();
    }
}
