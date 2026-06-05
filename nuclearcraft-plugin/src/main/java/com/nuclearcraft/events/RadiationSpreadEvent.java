package com.nuclearcraft.events;

import com.nuclearcraft.radiation.RadiationSource;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when radiation spreads from one player to another.
 * Cancelling prevents the spread from occurring.
 */
public class RadiationSpreadEvent extends Event implements Cancellable {

    public enum SpreadType {
        PROXIMITY,
        PHYSICAL_CONTACT,
        VEHICLE
    }

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player source;
    private final Player target;
    private int amount;
    private final SpreadType spreadType;
    private boolean cancelled;

    public RadiationSpreadEvent(Player source, Player target, int amount, SpreadType spreadType) {
        this.source = source;
        this.target = target;
        this.amount = amount;
        this.spreadType = spreadType;
        this.cancelled = false;
    }

    /** The infected player who is spreading radiation. */
    public Player getSource() { return source; }

    /** The player receiving the radiation. */
    public Player getTarget() { return target; }

    /** Radiation amount to be applied to the target. Modifiable. */
    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = Math.max(0, amount); }

    /** How the spread is occurring. */
    public SpreadType getSpreadType() { return spreadType; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
