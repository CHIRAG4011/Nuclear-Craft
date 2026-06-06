package com.nuclearcraft.events;

import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a Mutated Healing Plant advances to the next growth stage.
 * Cancellable — cancel to prevent the growth.
 */
public class PlantGrowthEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Location location;
    private final int oldStage;
    private final int newStage;
    private boolean cancelled = false;

    public PlantGrowthEvent(Location location, int oldStage, int newStage) {
        this.location = location.clone();
        this.oldStage = oldStage;
        this.newStage = newStage;
    }

    public Location getLocation() { return location.clone(); }
    public int getOldStage()      { return oldStage; }
    public int getNewStage()      { return newStage; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
