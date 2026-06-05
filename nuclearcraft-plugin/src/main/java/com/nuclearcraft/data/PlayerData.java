package com.nuclearcraft.data;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Holds all persistent data for a single player.
 * Designed for thread-safe reads; writes must be synchronized by the caller.
 */
public class PlayerData {

    private final UUID uuid;

    private volatile double radiationLevel;
    private volatile int radiationStage;
    private volatile long immunityTimerEndMs;
    private volatile double infectionProgress;
    private volatile int bossKills;
    private volatile double totalRadiationExposure;

    private final Set<String> unlockedUpgrades;

    private volatile boolean dirty;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.radiationLevel = 0.0;
        this.radiationStage = 0;
        this.immunityTimerEndMs = 0L;
        this.infectionProgress = 0.0;
        this.bossKills = 0;
        this.totalRadiationExposure = 0.0;
        this.unlockedUpgrades = new HashSet<>();
        this.dirty = false;
    }

    public PlayerData(UUID uuid, double radiationLevel, int radiationStage,
                      long immunityTimerEndMs, double infectionProgress,
                      int bossKills, double totalRadiationExposure,
                      Set<String> unlockedUpgrades) {
        this.uuid = uuid;
        this.radiationLevel = radiationLevel;
        this.radiationStage = radiationStage;
        this.immunityTimerEndMs = immunityTimerEndMs;
        this.infectionProgress = infectionProgress;
        this.bossKills = bossKills;
        this.totalRadiationExposure = totalRadiationExposure;
        this.unlockedUpgrades = new HashSet<>(unlockedUpgrades);
        this.dirty = false;
    }

    public UUID getUuid() { return uuid; }

    public double getRadiationLevel() { return radiationLevel; }
    public void setRadiationLevel(double level) {
        this.radiationLevel = Math.max(0, level);
        this.dirty = true;
    }

    public int getRadiationStage() { return radiationStage; }
    public void setRadiationStage(int stage) {
        this.radiationStage = stage;
        this.dirty = true;
    }

    public long getImmunityTimerEndMs() { return immunityTimerEndMs; }
    public void setImmunityTimerEndMs(long ms) {
        this.immunityTimerEndMs = ms;
        this.dirty = true;
    }

    public boolean isImmune() {
        return System.currentTimeMillis() < immunityTimerEndMs;
    }

    public double getInfectionProgress() { return infectionProgress; }
    public void setInfectionProgress(double progress) {
        this.infectionProgress = Math.max(0.0, Math.min(1.0, progress));
        this.dirty = true;
    }

    public int getBossKills() { return bossKills; }
    public void incrementBossKills() {
        this.bossKills++;
        this.dirty = true;
    }

    public double getTotalRadiationExposure() { return totalRadiationExposure; }
    public void addTotalRadiationExposure(double amount) {
        this.totalRadiationExposure += amount;
        this.dirty = true;
    }

    public Set<String> getUnlockedUpgrades() { return new HashSet<>(unlockedUpgrades); }

    public boolean hasUpgrade(String upgradeId) {
        return unlockedUpgrades.contains(upgradeId);
    }

    public void unlockUpgrade(String upgradeId) {
        unlockedUpgrades.add(upgradeId);
        this.dirty = true;
    }

    public void revokeUpgrade(String upgradeId) {
        unlockedUpgrades.remove(upgradeId);
        this.dirty = true;
    }

    public boolean isDirty() { return dirty; }
    public void markClean() { this.dirty = false; }
    public void markDirty() { this.dirty = true; }

    @Override
    public String toString() {
        return "PlayerData{uuid=" + uuid + ", radiation=" + radiationLevel + ", stage=" + radiationStage + "}";
    }
}
