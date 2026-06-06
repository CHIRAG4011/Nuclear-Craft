package com.nuclearcraft.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a player uses a Radiation Antidote or Radiation Serum to cure radiation.
 * Cancellable — cancel to prevent the cure being applied.
 */
public class RadiationCureEvent extends Event implements Cancellable {

    public enum CureType {
        ANTIDOTE,
        SERUM
    }

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final CureType cureType;
    private final double radiationBefore;
    private boolean cancelled = false;

    public RadiationCureEvent(Player player, CureType cureType, double radiationBefore) {
        this.player        = player;
        this.cureType      = cureType;
        this.radiationBefore = radiationBefore;
    }

    public Player getPlayer()          { return player; }
    public CureType getCureType()      { return cureType; }
    public double getRadiationBefore() { return radiationBefore; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
