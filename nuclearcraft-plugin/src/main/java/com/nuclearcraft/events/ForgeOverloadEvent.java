package com.nuclearcraft.events;

import com.nuclearcraft.forge.ForgeData;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a Nuclear Forge enters the OVERLOADED state.
 *
 * Other systems listen to trigger green explosions, radiation bursts,
 * and temporary shutdown effects.
 */
public class ForgeOverloadEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final ForgeData forgeData;
    private final Location location;
    private final double energyAtOverload;

    public ForgeOverloadEvent(ForgeData forgeData, Location location, double energyAtOverload) {
        this.forgeData = forgeData;
        this.location = location;
        this.energyAtOverload = energyAtOverload;
    }

    public ForgeData getForgeData()         { return forgeData; }
    public Location getLocation()           { return location; }
    public double getEnergyAtOverload()     { return energyAtOverload; }

    @Override public HandlerList getHandlers()  { return HANDLERS; }
    public static HandlerList getHandlerList()  { return HANDLERS; }
}
