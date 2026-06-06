package com.nuclearcraft.events;

import com.nuclearcraft.smelter.SmelterData;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a Nuclear Smelter exceeds the maximum temperature threshold
 * and enters the OVERHEATED state.
 * Not cancellable — the overheat state is already applied when this fires.
 */
public class NuclearSmelterOverheatEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final SmelterData smelter;
    private final double temperature;

    public NuclearSmelterOverheatEvent(SmelterData smelter, double temperature) {
        this.smelter = smelter;
        this.temperature = temperature;
    }

    public SmelterData getSmelter()  { return smelter; }
    public double getTemperature()   { return temperature; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
