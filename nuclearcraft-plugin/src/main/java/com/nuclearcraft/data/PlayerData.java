package com.nuclearcraft.data;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Holds all persistent data for a single player.
 *
 * Phase 2 additions: timesInfected, totalRadiationCured, radiationDeaths,
 *                    lastRadiationSource, lastRadiationReceivedMs.
 *
 * Phase 3 additions: irradiatedZombiesKilled, alphaZombiesKilled,
 *                    radiationCloudsSurvived, radioactiveCoresCollected,
 *                    mutatedSeedsCollected, irradiatedHeartsCollected.
 *
 * Thread-safety: volatile for primitive fields read across threads;
 * writes must be done on the main thread or with external synchronization.
 */
public class PlayerData {

    private final UUID uuid;

    // Radiation core
    private volatile double radiationLevel;
    private volatile int radiationStage;
    private volatile long immunityTimerEndMs;
    private volatile double infectionProgress;

    // Radiation statistics (Phase 2)
    private volatile long lastRadiationReceivedMs;
    private volatile String lastRadiationSource;
    private volatile int timesInfected;
    private volatile double totalRadiationExposure;
    private volatile double totalRadiationCured;
    private volatile int radiationDeaths;

    // Zombie statistics (Phase 3)
    private volatile int irradiatedZombiesKilled;
    private volatile int alphaZombiesKilled;
    private volatile int radiationCloudsSurvived;
    private volatile int radioactiveCoresCollected;
    private volatile int mutatedSeedsCollected;
    private volatile int irradiatedHeartsCollected;

    // Progression
    private volatile int bossKills;
    private final Set<String> unlockedUpgrades;

    private volatile boolean dirty;

    // ──────────────────────────────────────────────────────────────────────────
    // Constructors
    // ──────────────────────────────────────────────────────────────────────────

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.radiationLevel = 0.0;
        this.radiationStage = 0;
        this.immunityTimerEndMs = 0L;
        this.infectionProgress = 0.0;
        this.lastRadiationReceivedMs = 0L;
        this.lastRadiationSource = "UNKNOWN";
        this.timesInfected = 0;
        this.totalRadiationExposure = 0.0;
        this.totalRadiationCured = 0.0;
        this.radiationDeaths = 0;
        this.irradiatedZombiesKilled = 0;
        this.alphaZombiesKilled = 0;
        this.radiationCloudsSurvived = 0;
        this.radioactiveCoresCollected = 0;
        this.mutatedSeedsCollected = 0;
        this.irradiatedHeartsCollected = 0;
        this.bossKills = 0;
        this.unlockedUpgrades = new HashSet<>();
        this.dirty = false;
    }

    /** Full constructor used when loading from the database. */
    public PlayerData(UUID uuid,
                      double radiationLevel, int radiationStage,
                      long immunityTimerEndMs, double infectionProgress,
                      long lastRadiationReceivedMs, String lastRadiationSource,
                      int timesInfected,
                      double totalRadiationExposure, double totalRadiationCured,
                      int radiationDeaths,
                      int irradiatedZombiesKilled, int alphaZombiesKilled,
                      int radiationCloudsSurvived,
                      int radioactiveCoresCollected, int mutatedSeedsCollected,
                      int irradiatedHeartsCollected,
                      int bossKills,
                      Set<String> unlockedUpgrades) {
        this.uuid = uuid;
        this.radiationLevel = radiationLevel;
        this.radiationStage = radiationStage;
        this.immunityTimerEndMs = immunityTimerEndMs;
        this.infectionProgress = infectionProgress;
        this.lastRadiationReceivedMs = lastRadiationReceivedMs;
        this.lastRadiationSource = lastRadiationSource != null ? lastRadiationSource : "UNKNOWN";
        this.timesInfected = timesInfected;
        this.totalRadiationExposure = totalRadiationExposure;
        this.totalRadiationCured = totalRadiationCured;
        this.radiationDeaths = radiationDeaths;
        this.irradiatedZombiesKilled = irradiatedZombiesKilled;
        this.alphaZombiesKilled = alphaZombiesKilled;
        this.radiationCloudsSurvived = radiationCloudsSurvived;
        this.radioactiveCoresCollected = radioactiveCoresCollected;
        this.mutatedSeedsCollected = mutatedSeedsCollected;
        this.irradiatedHeartsCollected = irradiatedHeartsCollected;
        this.bossKills = bossKills;
        this.unlockedUpgrades = new HashSet<>(unlockedUpgrades);
        this.dirty = false;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Radiation core
    // ──────────────────────────────────────────────────────────────────────────

    public UUID getUuid() { return uuid; }

    public double getRadiationLevel() { return radiationLevel; }
    public void setRadiationLevel(double level) {
        this.radiationLevel = Math.max(0, Math.min(1000, level));
        this.dirty = true;
    }

    public int getRadiationStage() { return radiationStage; }
    public void setRadiationStage(int stage) {
        this.radiationStage = Math.max(0, Math.min(4, stage));
        this.dirty = true;
    }

    public long getImmunityTimerEndMs() { return immunityTimerEndMs; }
    public void setImmunityTimerEndMs(long ms) {
        this.immunityTimerEndMs = ms;
        this.dirty = true;
    }
    public boolean isImmune() { return System.currentTimeMillis() < immunityTimerEndMs; }

    public double getInfectionProgress() { return infectionProgress; }
    public void setInfectionProgress(double progress) {
        this.infectionProgress = Math.max(0.0, Math.min(1.0, progress));
        this.dirty = true;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Phase 2 radiation statistics
    // ──────────────────────────────────────────────────────────────────────────

    public long getLastRadiationReceivedMs() { return lastRadiationReceivedMs; }
    public void setLastRadiationReceivedMs(long ms) {
        this.lastRadiationReceivedMs = ms;
        this.dirty = true;
    }

    public String getLastRadiationSource() { return lastRadiationSource; }
    public void setLastRadiationSource(String source) {
        this.lastRadiationSource = source != null ? source : "UNKNOWN";
        this.dirty = true;
    }

    public int getTimesInfected() { return timesInfected; }
    public void setTimesInfected(int count) {
        this.timesInfected = Math.max(0, count);
        this.dirty = true;
    }

    public double getTotalRadiationExposure() { return totalRadiationExposure; }
    public void addTotalRadiationExposure(double amount) {
        this.totalRadiationExposure += Math.max(0, amount);
        this.dirty = true;
    }

    public double getTotalRadiationCured() { return totalRadiationCured; }
    public void addTotalRadiationCured(double amount) {
        this.totalRadiationCured += Math.max(0, amount);
        this.dirty = true;
    }

    public int getRadiationDeaths() { return radiationDeaths; }
    public void setRadiationDeaths(int count) {
        this.radiationDeaths = Math.max(0, count);
        this.dirty = true;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Phase 3 zombie statistics
    // ──────────────────────────────────────────────────────────────────────────

    public int getIrradiatedZombiesKilled() { return irradiatedZombiesKilled; }
    public void incrementIrradiatedZombiesKilled() {
        this.irradiatedZombiesKilled++;
        this.dirty = true;
    }
    public void setIrradiatedZombiesKilled(int count) {
        this.irradiatedZombiesKilled = Math.max(0, count);
        this.dirty = true;
    }

    public int getAlphaZombiesKilled() { return alphaZombiesKilled; }
    public void incrementAlphaZombiesKilled() {
        this.alphaZombiesKilled++;
        this.dirty = true;
    }
    public void setAlphaZombiesKilled(int count) {
        this.alphaZombiesKilled = Math.max(0, count);
        this.dirty = true;
    }

    public int getRadiationCloudsSurvived() { return radiationCloudsSurvived; }
    public void incrementRadiationCloudsSurvived() {
        this.radiationCloudsSurvived++;
        this.dirty = true;
    }
    public void setRadiationCloudsSurvived(int count) {
        this.radiationCloudsSurvived = Math.max(0, count);
        this.dirty = true;
    }

    public int getRadioactiveCoresCollected() { return radioactiveCoresCollected; }
    public void incrementRadioactiveCoresCollected() {
        this.radioactiveCoresCollected++;
        this.dirty = true;
    }
    public void setRadioactiveCoresCollected(int count) {
        this.radioactiveCoresCollected = Math.max(0, count);
        this.dirty = true;
    }

    public int getMutatedSeedsCollected() { return mutatedSeedsCollected; }
    public void incrementMutatedSeedsCollected() {
        this.mutatedSeedsCollected++;
        this.dirty = true;
    }
    public void setMutatedSeedsCollected(int count) {
        this.mutatedSeedsCollected = Math.max(0, count);
        this.dirty = true;
    }

    public int getIrradiatedHeartsCollected() { return irradiatedHeartsCollected; }
    public void incrementIrradiatedHeartsCollected() {
        this.irradiatedHeartsCollected++;
        this.dirty = true;
    }
    public void setIrradiatedHeartsCollected(int count) {
        this.irradiatedHeartsCollected = Math.max(0, count);
        this.dirty = true;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Progression
    // ──────────────────────────────────────────────────────────────────────────

    public int getBossKills() { return bossKills; }
    public void incrementBossKills() {
        this.bossKills++;
        this.dirty = true;
    }

    public Set<String> getUnlockedUpgrades() { return unlockedUpgrades; }
    public boolean hasUpgrade(String id) { return unlockedUpgrades.contains(id); }
    public void unlockUpgrade(String id) { unlockedUpgrades.add(id); this.dirty = true; }
    public void revokeUpgrade(String id) { unlockedUpgrades.remove(id); this.dirty = true; }

    // ──────────────────────────────────────────────────────────────────────────
    // Dirty flag
    // ──────────────────────────────────────────────────────────────────────────

    public boolean isDirty() { return dirty; }
    public void markClean() { this.dirty = false; }
    public void markDirty() { this.dirty = true; }

    @Override
    public String toString() {
        return "PlayerData{uuid=" + uuid
                + ", radiation=" + radiationLevel
                + ", stage=" + radiationStage
                + ", izKills=" + irradiatedZombiesKilled
                + ", source=" + lastRadiationSource + "}";
    }
}
