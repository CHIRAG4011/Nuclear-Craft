package com.nuclearcraft.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a player dies as a direct result of radiation damage.
 * Not cancellable — death is already confirmed when this fires.
 * Use this event to hook into radiation death tracking, achievements, and titles.
 */
public class RadiationDeathEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final double radiationAtDeath;
    private final int stageAtDeath;

    public RadiationDeathEvent(Player player, double radiationAtDeath, int stageAtDeath) {
        this.player = player;
        this.radiationAtDeath = radiationAtDeath;
        this.stageAtDeath = stageAtDeath;
    }

    /** The player who died from radiation. */
    public Player getPlayer() { return player; }

    /** Radiation level at the moment of death. */
    public double getRadiationAtDeath() { return radiationAtDeath; }

    /** Radiation stage at the moment of death. */
    public int getStageAtDeath() { return stageAtDeath; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
