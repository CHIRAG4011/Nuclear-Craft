package com.nuclearcraft.forge;

import com.nuclearcraft.upgrade.UpgradeTier;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Persistent runtime state for a single Nuclear Forge machine.
 *
 * Forge energy is stored here and persisted across restarts via forge-machines.yml.
 * The pending upgrade item is stored in memory only; if the server restarts mid-upgrade,
 * the item is safely returned to the upgraderUuid player on their next login.
 */
public class ForgeData {

    // ── Identity ─────────────────────────────────────────────────────────────

    private final Location location;
    private final String locationKey;

    // ── Machine state ─────────────────────────────────────────────────────────

    private ForgeState state = ForgeState.OFFLINE;
    private double energy = 0.0;

    // ── Active upgrade tracking ───────────────────────────────────────────────

    private UUID lastInteractingPlayerUuid;
    private UUID upgraderUuid;
    private ItemStack pendingEquipment;    // item being upgraded (held during UPGRADING)
    private UpgradeTier pendingTargetTier; // tier we are upgrading toward
    private long upgradeStartTick;
    private long upgradeEndTick;

    /** Set when upgrade completes; displayed in the output slot of the GUI. */
    private ItemStack outputItem;

    // ── Forge statistics (persisted) ──────────────────────────────────────────

    private int totalForgeUses;
    private int successfulUpgrades;
    private int failedUpgrades;
    private int mk4Creations;
    private int overloadCount;
    private long totalEnergyConsumed;
    private long totalMaterialsConsumed;

    // ──────────────────────────────────────────────────────────────────────────

    public ForgeData(Location location) {
        this.location = location.clone();
        this.locationKey = serializeLocation(location);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public static String serializeLocation(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    public boolean isUpgrading() {
        return state == ForgeState.UPGRADING;
    }

    public boolean isReady() {
        return state == ForgeState.READY;
    }

    public boolean hasOutput() {
        return outputItem != null && !outputItem.getType().isAir();
    }

    public double getEnergyPercent(double maxEnergy) {
        return maxEnergy <= 0 ? 0 : Math.min(1.0, energy / maxEnergy);
    }

    // ── Upgrade progress ──────────────────────────────────────────────────────

    public double getUpgradeProgress(long currentTick) {
        if (!isUpgrading() || upgradeEndTick <= upgradeStartTick) return 0.0;
        double progress = (double)(currentTick - upgradeStartTick) / (upgradeEndTick - upgradeStartTick);
        return Math.max(0, Math.min(1.0, progress));
    }

    // ── Getters & setters ─────────────────────────────────────────────────────

    public Location getLocation()                       { return location.clone(); }
    public String getLocationKey()                      { return locationKey; }

    public ForgeState getState()                        { return state; }
    public void setState(ForgeState state)              { this.state = state; }

    public double getEnergy()                           { return energy; }
    public void setEnergy(double energy)                { this.energy = Math.max(0, energy); }
    public void addEnergy(double amount)                { this.energy = Math.max(0, this.energy + amount); }
    public void consumeEnergy(double amount)            { this.energy = Math.max(0, this.energy - amount); }

    public UUID getLastInteractingPlayerUuid()          { return lastInteractingPlayerUuid; }
    public void setLastInteractingPlayerUuid(UUID uuid) { this.lastInteractingPlayerUuid = uuid; }

    public UUID getUpgraderUuid()                       { return upgraderUuid; }
    public void setUpgraderUuid(UUID uuid)              { this.upgraderUuid = uuid; }

    public ItemStack getPendingEquipment()              { return pendingEquipment; }
    public void setPendingEquipment(ItemStack item)     { this.pendingEquipment = item == null ? null : item.clone(); }

    public UpgradeTier getPendingTargetTier()           { return pendingTargetTier; }
    public void setPendingTargetTier(UpgradeTier tier)  { this.pendingTargetTier = tier; }

    public long getUpgradeStartTick()                   { return upgradeStartTick; }
    public void setUpgradeStartTick(long tick)          { this.upgradeStartTick = tick; }

    public long getUpgradeEndTick()                     { return upgradeEndTick; }
    public void setUpgradeEndTick(long tick)            { this.upgradeEndTick = tick; }

    public ItemStack getOutputItem()                    { return outputItem; }
    public void setOutputItem(ItemStack item)           { this.outputItem = item == null ? null : item.clone(); }
    public void clearOutputItem()                       { this.outputItem = null; }

    // Stats
    public int getTotalForgeUses()                      { return totalForgeUses; }
    public void incrementForgeUses()                    { totalForgeUses++; }
    public void setTotalForgeUses(int v)                { this.totalForgeUses = v; }

    public int getSuccessfulUpgrades()                  { return successfulUpgrades; }
    public void incrementSuccessfulUpgrades()           { successfulUpgrades++; }
    public void setSuccessfulUpgrades(int v)            { this.successfulUpgrades = v; }

    public int getFailedUpgrades()                      { return failedUpgrades; }
    public void incrementFailedUpgrades()               { failedUpgrades++; }
    public void setFailedUpgrades(int v)                { this.failedUpgrades = v; }

    public int getMk4Creations()                        { return mk4Creations; }
    public void incrementMk4Creations()                 { mk4Creations++; }
    public void setMk4Creations(int v)                  { this.mk4Creations = v; }

    public int getOverloadCount()                       { return overloadCount; }
    public void incrementOverloadCount()                { overloadCount++; }
    public void setOverloadCount(int v)                 { this.overloadCount = v; }

    public long getTotalEnergyConsumed()                { return totalEnergyConsumed; }
    public void addEnergyConsumed(long v)               { this.totalEnergyConsumed += v; }
    public void setTotalEnergyConsumed(long v)          { this.totalEnergyConsumed = v; }

    public long getTotalMaterialsConsumed()             { return totalMaterialsConsumed; }
    public void addMaterialsConsumed(long v)            { this.totalMaterialsConsumed += v; }
    public void setTotalMaterialsConsumed(long v)       { this.totalMaterialsConsumed = v; }
}
