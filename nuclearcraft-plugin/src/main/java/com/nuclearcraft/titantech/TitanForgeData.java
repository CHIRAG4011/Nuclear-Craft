package com.nuclearcraft.titantech;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Holds all runtime and persistent data for a single Titan Forge machine.
 */
public class TitanForgeData {

    private final Location location;
    private final String locationKey;

    private TitanForgeState state = TitanForgeState.OFFLINE;
    private double energy = 0.0;

    private ItemStack pendingOutput = null;
    private TitanForgeRecipe activeRecipe = null;
    private UUID crafterUuid = null;
    private UUID lastInteractUuid = null;

    private long craftStartTick = 0;
    private long craftEndTick   = 0;
    private boolean crafting    = false;
    private boolean hasOutput   = false;

    // Statistics
    private int totalCrafts     = 0;
    private int successfulCrafts = 0;
    private int failedCrafts    = 0;
    private int overloads       = 0;

    public TitanForgeData(Location location) {
        this.location = location.clone();
        this.locationKey = serializeLocation(location);
    }

    public static String serializeLocation(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    public Location getLocation()  { return location.clone(); }
    public String getLocationKey() { return locationKey; }

    public TitanForgeState getState()             { return state; }
    public void setState(TitanForgeState state)   { this.state = state; }

    public double getEnergy()                     { return energy; }
    public void setEnergy(double e)               { this.energy = Math.max(0, e); }
    public void consumeEnergy(double amount)      { this.energy = Math.max(0, energy - amount); }
    public double getEnergyPercent(double max)    { return max > 0 ? Math.min(1.0, energy / max) : 0; }

    public boolean isCrafting()                   { return crafting; }
    public void setCrafting(boolean c)            { this.crafting = c; }

    public TitanForgeRecipe getActiveRecipe()     { return activeRecipe; }
    public void setActiveRecipe(TitanForgeRecipe r){ this.activeRecipe = r; }

    public ItemStack getPendingOutput()           { return pendingOutput; }
    public void setPendingOutput(ItemStack item)  { this.pendingOutput = item; }

    public UUID getCrafterUuid()                  { return crafterUuid; }
    public void setCrafterUuid(UUID u)            { this.crafterUuid = u; }

    public UUID getLastInteractUuid()             { return lastInteractUuid; }
    public void setLastInteractUuid(UUID u)       { this.lastInteractUuid = u; }

    public long getCraftStartTick()               { return craftStartTick; }
    public void setCraftStartTick(long t)         { this.craftStartTick = t; }

    public long getCraftEndTick()                 { return craftEndTick; }
    public void setCraftEndTick(long t)           { this.craftEndTick = t; }

    public boolean hasOutput()                    { return hasOutput; }
    public void setHasOutput(boolean h)           { this.hasOutput = h; }

    public void clearOutput() {
        pendingOutput = null;
        hasOutput = false;
    }

    public double getCraftProgress(long currentTick) {
        if (!crafting || craftEndTick <= craftStartTick) return 0.0;
        return Math.min(1.0, (double)(currentTick - craftStartTick) / (craftEndTick - craftStartTick));
    }

    public int getTotalCrafts()     { return totalCrafts; }
    public int getSuccessfulCrafts() { return successfulCrafts; }
    public int getFailedCrafts()    { return failedCrafts; }
    public int getOverloads()       { return overloads; }

    public void incrementTotalCrafts()      { totalCrafts++; }
    public void incrementSuccessfulCrafts() { successfulCrafts++; }
    public void incrementFailedCrafts()     { failedCrafts++; }
    public void incrementOverloads()        { overloads++; }
}
