package com.nuclearcraft.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a player harvests a fully grown Mutated Healing Plant.
 * Cancellable — cancel to prevent the harvest.
 */
public class PlantHarvestEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Location location;
    private boolean cancelled = false;

    public PlantHarvestEvent(Player player, Location location) {
        this.player   = player;
        this.location = location.clone();
    }

    public Player getPlayer()    { return player; }
    public Location getLocation() { return location.clone(); }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
