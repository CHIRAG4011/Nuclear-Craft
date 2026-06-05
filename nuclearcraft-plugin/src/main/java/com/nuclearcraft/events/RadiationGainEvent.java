package com.nuclearcraft.events;

import com.nuclearcraft.radiation.RadiationSource;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a player gains radiation points.
 * Cancelling this event prevents the radiation from being applied.
 */
public class RadiationGainEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private int amount;
    private final RadiationSource source;
    private final double radiationBefore;
    private boolean cancelled;

    public RadiationGainEvent(Player player, int amount, RadiationSource source, double radiationBefore) {
        this.player = player;
        this.amount = amount;
        this.source = source;
        this.radiationBefore = radiationBefore;
        this.cancelled = false;
    }

    /** The player gaining radiation. */
    public Player getPlayer() { return player; }

    /** Amount of radiation to be added. Can be modified by listeners. */
    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = Math.max(0, amount); }

    /** The source that caused this radiation gain. */
    public RadiationSource getSource() { return source; }

    /** The player's radiation level before this gain. */
    public double getRadiationBefore() { return radiationBefore; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
