package com.nuclearcraft.listeners;

import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Core event listener handling player sessions and GUI routing.
 * Phase-specific listeners (radiation, zombies, ore, etc.) will be added in later phases.
 */
public class CoreListener implements Listener {

    private final NuclearCraftPlugin plugin;
    private final PlayerDataManager playerDataManager;

    public CoreListener(NuclearCraftPlugin plugin, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        playerDataManager.loadAsync(event.getPlayer()).thenAccept(data -> {
            NCLogger.debug("Loaded data for player %s: %s", event.getPlayer().getName(), data);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerDataManager.unloadAndSave(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        plugin.getGuiManager().handleClick(event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        plugin.getGuiManager().handleClose(event);
    }
}
