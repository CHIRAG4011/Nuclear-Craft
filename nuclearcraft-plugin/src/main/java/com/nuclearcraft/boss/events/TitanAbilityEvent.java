package com.nuclearcraft.boss.events;

import org.bukkit.entity.Giant;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** Fired when the Titan uses a special ability. */
public class TitanAbilityEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    public enum Ability {
        TITAN_SLAM,
        RADIATION_AURA,
        HEAVY_MELEE,
        RADIATION_WAVE,
        MUTANT_SUMMONING,
        REACTOR_OVERLOAD,
        ENERGY_BEAM,
        NUCLEAR_CATASTROPHE,
        FINAL_FRENZY
    }

    private final Giant   titan;
    private final Ability ability;

    public TitanAbilityEvent(Giant titan, Ability ability) {
        this.titan   = titan;
        this.ability = ability;
    }

    public Giant getTitan()   { return titan; }
    public Ability getAbility() { return ability; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList()  { return HANDLERS; }
}
