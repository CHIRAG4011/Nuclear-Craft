package com.nuclearcraft.events;

import com.nuclearcraft.smelter.SmelterData;
import com.nuclearcraft.smelter.SmelterRecipe;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a Nuclear Smelter completes processing a recipe and produces output.
 * Not cancellable — output has already been placed.
 * Hook into this event for advancement tracking, stats, and future systems.
 */
public class NuclearSmelterCompleteEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final SmelterData smelter;
    private final SmelterRecipe recipe;
    private final int outputAmount;

    public NuclearSmelterCompleteEvent(SmelterData smelter, SmelterRecipe recipe, int outputAmount) {
        this.smelter = smelter;
        this.recipe = recipe;
        this.outputAmount = outputAmount;
    }

    public SmelterData getSmelter()  { return smelter; }
    public SmelterRecipe getRecipe() { return recipe; }
    public int getOutputAmount()     { return outputAmount; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
