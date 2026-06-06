package com.nuclearcraft.farming;

import org.bukkit.Location;

import java.util.UUID;

/**
 * Holds runtime state for a single tracked Mutated Healing Plant.
 */
public class MutatedCropData {

    /** Growth stages 0–4. */
    public static final int MAX_STAGE = 4;

    /** Wheat age for each growth stage: index = NuclearCraft stage, value = Bukkit Ageable age. */
    public static final int[] WHEAT_AGES = {0, 1, 3, 5, 7};

    private final Location location;
    private final UUID planterUuid;
    private int stage;
    private final long plantedAt;

    public MutatedCropData(Location location, UUID planterUuid) {
        this.location   = location.clone();
        this.planterUuid = planterUuid;
        this.stage      = 0;
        this.plantedAt  = System.currentTimeMillis();
    }

    /** Restore a crop from persistent storage. */
    public MutatedCropData(Location location, UUID planterUuid, int stage, long plantedAt) {
        this.location    = location.clone();
        this.planterUuid = planterUuid;
        this.stage       = Math.max(0, Math.min(MAX_STAGE, stage));
        this.plantedAt   = plantedAt;
    }

    public Location getLocation()   { return location.clone(); }
    public UUID getPlanterUuid()    { return planterUuid; }
    public int getStage()           { return stage; }
    public long getPlantedAt()      { return plantedAt; }

    public boolean isFullyGrown()   { return stage >= MAX_STAGE; }

    /**
     * Advances the stage by one.
     * @return true if the crop just became fully grown.
     */
    public boolean advanceStage() {
        if (stage >= MAX_STAGE) return false;
        stage++;
        return stage == MAX_STAGE;
    }

    /** Returns the Bukkit Ageable wheat age that corresponds to the current stage. */
    public int getWheatAge() {
        return WHEAT_AGES[Math.min(stage, WHEAT_AGES.length - 1)];
    }
}
