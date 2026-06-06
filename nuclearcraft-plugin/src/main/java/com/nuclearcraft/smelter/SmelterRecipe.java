package com.nuclearcraft.smelter;

import org.bukkit.inventory.ItemStack;

/**
 * Represents a single processing recipe for the Nuclear Smelter.
 * Recipes are matched by custom item ID (PDC tag), not by material.
 */
public class SmelterRecipe {

    private final String id;
    private final String inputItemId;
    private final String outputItemId;
    private final int processingTicks;
    private final int fuelCostPerCycle;

    /**
     * @param id              Unique recipe identifier
     * @param inputItemId     NuclearCraft item ID of the input (matched via PDC)
     * @param outputItemId    NuclearCraft item ID of the output
     * @param processingTicks Total ticks to complete one operation
     * @param fuelCostPerCycle Fuel units consumed per machine tick-cycle
     */
    public SmelterRecipe(String id, String inputItemId, String outputItemId,
                         int processingTicks, int fuelCostPerCycle) {
        this.id = id;
        this.inputItemId = inputItemId;
        this.outputItemId = outputItemId;
        this.processingTicks = processingTicks;
        this.fuelCostPerCycle = fuelCostPerCycle;
    }

    public String getId()              { return id; }
    public String getInputItemId()     { return inputItemId; }
    public String getOutputItemId()    { return outputItemId; }
    public int getProcessingTicks()    { return processingTicks; }
    public int getFuelCostPerCycle()   { return fuelCostPerCycle; }

    @Override
    public String toString() {
        return "SmelterRecipe{" + inputItemId + " -> " + outputItemId
                + ", ticks=" + processingTicks + "}";
    }
}
