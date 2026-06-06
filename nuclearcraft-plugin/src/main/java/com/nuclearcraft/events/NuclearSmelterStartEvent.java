package com.nuclearcraft.events;

import com.nuclearcraft.smelter.SmelterData;
import com.nuclearcraft.smelter.SmelterRecipe;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a Nuclear Smelter begins processing a new recipe.
 * Not cancellable — use {@link com.nuclearcraft.events.NuclearSmelterCompleteEvent}
 * for post-processing hooks.
 */
public class NuclearSmelterStartEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final SmelterData smelter;
    private final SmelterRecipe recipe;

    public NuclearSmelterStartEvent(SmelterData smelter, SmelterRecipe recipe) {
        this.smelter = smelter;
        this.recipe = recipe;
    }

    public SmelterData getSmelter()   { return smelter; }
    public SmelterRecipe getRecipe()  { return recipe; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
