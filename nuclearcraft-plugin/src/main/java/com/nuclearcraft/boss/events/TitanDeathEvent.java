package com.nuclearcraft.boss.events;

import org.bukkit.Location;
import org.bukkit.entity.Giant;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

/** Fired when the Plutonium Titan is defeated. */
public class TitanDeathEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Giant               titan;
    private final Location            deathLocation;
    private final Map<UUID, Double>   damageContributions;
    private final @Nullable Player    killCredit;

    public TitanDeathEvent(Giant titan, Location deathLocation,
                           Map<UUID, Double> damageContributions,
                           @Nullable Player killCredit) {
        this.titan               = titan;
        this.deathLocation       = deathLocation;
        this.damageContributions = damageContributions;
        this.killCredit          = killCredit;
    }

    public Giant getTitan()                           { return titan; }
    public Location getDeathLocation()                { return deathLocation; }
    public Map<UUID, Double> getDamageContributions() { return damageContributions; }
    public @Nullable Player getKillCredit()           { return killCredit; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList()  { return HANDLERS; }
}
