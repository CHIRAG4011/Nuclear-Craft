package com.nuclearcraft.events;

import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a mutated crop mutates into a Toxic Bloom.
 * Cancellable — cancel to prevent the mutation.
 */
public class CropMutationEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Location location;
    private boolean cancelled = false;

    public CropMutationEvent(Location location) {
        this.location = location.clone();
    }

    /** The location of the crop that is mutating. */
    public Location getLocation() {
        return location.clone();
    }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
