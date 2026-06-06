package com.nuclearcraft.boss.events;

import org.bukkit.Location;
import org.bukkit.entity.Giant;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** Fired when the Plutonium Titan is summoned and spawned. */
public class TitanSpawnEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Giant titan;
    private final Location spawnLocation;
    private final String summonerName;

    public TitanSpawnEvent(Giant titan, Location spawnLocation, String summonerName) {
        this.titan         = titan;
        this.spawnLocation = spawnLocation;
        this.summonerName  = summonerName;
    }

    public Giant getTitan()             { return titan; }
    public Location getSpawnLocation()  { return spawnLocation; }
    public String getSummonerName()     { return summonerName; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList()  { return HANDLERS; }
}
