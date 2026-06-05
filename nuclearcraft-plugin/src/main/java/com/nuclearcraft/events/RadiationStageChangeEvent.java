package com.nuclearcraft.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a player's radiation stage changes (either up or down).
 * Cancelling this event prevents the stage from being updated and blocks
 * any stage-specific effect triggers.
 */
public class RadiationStageChangeEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final int previousStage;
    private final int newStage;
    private final double currentRadiation;
    private boolean cancelled;

    public RadiationStageChangeEvent(Player player, int previousStage, int newStage, double currentRadiation) {
        this.player = player;
        this.previousStage = previousStage;
        this.newStage = newStage;
        this.currentRadiation = currentRadiation;
        this.cancelled = false;
    }

    public Player getPlayer() { return player; }
    public int getPreviousStage() { return previousStage; }
    public int getNewStage() { return newStage; }
    public double getCurrentRadiation() { return currentRadiation; }

    /** True if the stage is increasing (getting worse). */
    public boolean isEscalating() { return newStage > previousStage; }

    /** True if the stage is decreasing (recovering). */
    public boolean isRecovering() { return newStage < previousStage; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
