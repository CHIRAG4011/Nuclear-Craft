package com.nuclearcraft.titantech;

/**
 * Represents a single Titan Forge crafting recipe.
 * All costs/durations configurable via titan_items.yml.
 */
public class TitanForgeRecipe {

    private final String id;
    private final String displayName;
    private final String outputItemId;
    private final String material1Id;
    private final int material1Amount;
    private final String material2Id;
    private final int material2Amount;
    private final int coresRequired;
    private final int durationTicks;
    private final double successChance;

    public TitanForgeRecipe(String id, String displayName,
                             String outputItemId,
                             String material1Id, int material1Amount,
                             String material2Id, int material2Amount,
                             int coresRequired, int durationTicks, double successChance) {
        this.id = id;
        this.displayName = displayName;
        this.outputItemId = outputItemId;
        this.material1Id = material1Id;
        this.material1Amount = material1Amount;
        this.material2Id = material2Id;
        this.material2Amount = material2Amount;
        this.coresRequired = coresRequired;
        this.durationTicks = durationTicks;
        this.successChance = successChance;
    }

    public String getId()              { return id; }
    public String getDisplayName()     { return displayName; }
    public String getOutputItemId()    { return outputItemId; }
    public String getMaterial1Id()     { return material1Id; }
    public int getMaterial1Amount()    { return material1Amount; }
    public String getMaterial2Id()     { return material2Id; }
    public int getMaterial2Amount()    { return material2Amount; }
    public int getCoresRequired()      { return coresRequired; }
    public int getDurationTicks()      { return durationTicks; }
    public double getSuccessChance()   { return successChance; }
}
