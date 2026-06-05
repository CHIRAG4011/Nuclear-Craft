package com.nuclearcraft.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a player loses radiation points (cure, decay, or command).
 * Cancelling prevents the reduction.
 */
public class RadiationLossEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private int amount;
    private final double radiationBefore;
    private boolean cancelled;

    public RadiationLossEvent(Player player, int amount, double radiationBefore) {
        this.player = player;
        this.amount = amount;
        this.radiationBefore = radiationBefore;
        this.cancelled = false;
    }

    public Player getPlayer() { return player; }
    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = Math.max(0, amount); }
    public double getRadiationBefore() { return radiationBefore; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
