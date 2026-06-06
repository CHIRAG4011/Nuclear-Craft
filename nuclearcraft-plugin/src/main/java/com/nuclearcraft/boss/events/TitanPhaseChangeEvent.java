package com.nuclearcraft.boss.events;

import com.nuclearcraft.boss.TitanPhase;
import org.bukkit.entity.Giant;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** Fired when the Plutonium Titan transitions to a new combat phase. */
public class TitanPhaseChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Giant     titan;
    private final TitanPhase previousPhase;
    private final TitanPhase newPhase;

    public TitanPhaseChangeEvent(Giant titan, TitanPhase previousPhase, TitanPhase newPhase) {
        this.titan         = titan;
        this.previousPhase = previousPhase;
        this.newPhase      = newPhase;
    }

    public Giant getTitan()              { return titan; }
    public TitanPhase getPreviousPhase() { return previousPhase; }
    public TitanPhase getNewPhase()      { return newPhase; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList()  { return HANDLERS; }
}
