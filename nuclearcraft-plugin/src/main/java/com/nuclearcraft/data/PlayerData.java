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
 * Phase 4 additions: plutoniumOreFound, plutoniumOreMined, fragmentsCollected,
 *                    radiationBurstsTriggered, drillUses, unsafeMiningAttempts.
 *
 * Phase 5 additions: machinesBuilt, fragmentsProcessed, ingotsProduced,
 *                    fuelConsumed, overheatsTriggered.
 *
 * Phase 6 additions: swordHits, radiationDamageInflicted, blocksConverted,
 *                    farmlandCreated, debrisGenerated, arrowsFired.
 *
 * Phase 7 additions: seedsPlanted, plantsHarvested, healingPetalsCollected,
 *                    antidotesCrafted, serumsCrafted, radiationCuresUsed,
 *                    toxicBloomsGenerated.
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

    // Ore statistics (Phase 4)
    private volatile int plutoniumOreFound;
    private volatile int plutoniumOreMined;
    private volatile int fragmentsCollected;
    private volatile int radiationBurstsTriggered;
    private volatile int drillUses;
    private volatile int unsafeMiningAttempts;

    // Smelter statistics (Phase 5)
    private volatile int machinesBuilt;
    private volatile int fragmentsProcessed;
    private volatile int ingotsProduced;
    private volatile int fuelConsumed;
    private volatile int overheatsTriggered;

    // Equipment statistics (Phase 6)
    private volatile int swordHits;
    private volatile int radiationDamageInflicted;
    private volatile int blocksConverted;
    private volatile int farmlandCreated;
    private volatile int debrisGenerated;
    private volatile int arrowsFired;

    // Forge statistics (Phase 8)
    private volatile int forgeUses;
    private volatile int successfulUpgrades;
    private volatile int failedUpgrades;
    private volatile int mk4Creations;

    // Combat statistics (Phase 9)
    private volatile int pvpKills;
    private volatile int pvpRadiationKills;
    private volatile int pvpArrowKills;
    private volatile int pvpAuraKills;
    private volatile int pvpArrowHits;
    private volatile long totalPvPRadiationInflicted;
    private volatile long auraDamageDealt;
    private volatile int swordMasteryXp;
    private volatile int axeMasteryXp;
    private volatile int bowMasteryXp;

    // Farming statistics (Phase 7)
    private volatile int seedsPlanted;
    private volatile int plantsHarvested;
    private volatile int healingPetalsCollected;
    private volatile int antidotesCrafted;
    private volatile int serumsCrafted;
    private volatile int radiationCuresUsed;
    private volatile int toxicBloomsGenerated;

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
        this.plutoniumOreFound = 0;
        this.plutoniumOreMined = 0;
        this.fragmentsCollected = 0;
        this.radiationBurstsTriggered = 0;
        this.drillUses = 0;
        this.unsafeMiningAttempts = 0;
        this.machinesBuilt = 0;
        this.fragmentsProcessed = 0;
        this.ingotsProduced = 0;
        this.fuelConsumed = 0;
        this.overheatsTriggered = 0;
        this.swordHits = 0;
        this.radiationDamageInflicted = 0;
        this.blocksConverted = 0;
        this.farmlandCreated = 0;
        this.debrisGenerated = 0;
        this.arrowsFired = 0;
        this.forgeUses = 0;
        this.successfulUpgrades = 0;
        this.failedUpgrades = 0;
        this.mk4Creations = 0;
        this.pvpKills = 0;
        this.pvpRadiationKills = 0;
        this.pvpArrowKills = 0;
        this.pvpAuraKills = 0;
        this.pvpArrowHits = 0;
        this.totalPvPRadiationInflicted = 0L;
        this.auraDamageDealt = 0L;
        this.swordMasteryXp = 0;
        this.axeMasteryXp = 0;
        this.bowMasteryXp = 0;
        this.seedsPlanted = 0;
        this.plantsHarvested = 0;
        this.healingPetalsCollected = 0;
        this.antidotesCrafted = 0;
        this.serumsCrafted = 0;
        this.radiationCuresUsed = 0;
        this.toxicBloomsGenerated = 0;
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
                      int plutoniumOreFound, int plutoniumOreMined,
                      int fragmentsCollected, int radiationBurstsTriggered,
                      int drillUses, int unsafeMiningAttempts,
                      int machinesBuilt, int fragmentsProcessed,
                      int ingotsProduced, int fuelConsumed, int overheatsTriggered,
                      int bossKills,
                      Set<String> unlockedUpgrades,
                      // Phase 6
                      int swordHits, int radiationDamageInflicted,
                      int blocksConverted, int farmlandCreated,
                      int debrisGenerated, int arrowsFired,
                      // Phase 7
                      int seedsPlanted, int plantsHarvested,
                      int healingPetalsCollected, int antidotesCrafted,
                      int serumsCrafted, int radiationCuresUsed,
                      int toxicBloomsGenerated) {
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
        this.plutoniumOreFound = Math.max(0, plutoniumOreFound);
        this.plutoniumOreMined = Math.max(0, plutoniumOreMined);
        this.fragmentsCollected = Math.max(0, fragmentsCollected);
        this.radiationBurstsTriggered = Math.max(0, radiationBurstsTriggered);
        this.drillUses = Math.max(0, drillUses);
        this.unsafeMiningAttempts = Math.max(0, unsafeMiningAttempts);
        this.machinesBuilt = Math.max(0, machinesBuilt);
        this.fragmentsProcessed = Math.max(0, fragmentsProcessed);
        this.ingotsProduced = Math.max(0, ingotsProduced);
        this.fuelConsumed = Math.max(0, fuelConsumed);
        this.overheatsTriggered = Math.max(0, overheatsTriggered);
        this.bossKills = bossKills;
        this.unlockedUpgrades = new HashSet<>(unlockedUpgrades);
        this.swordHits = Math.max(0, swordHits);
        this.radiationDamageInflicted = Math.max(0, radiationDamageInflicted);
        this.blocksConverted = Math.max(0, blocksConverted);
        this.farmlandCreated = Math.max(0, farmlandCreated);
        this.debrisGenerated = Math.max(0, debrisGenerated);
        this.arrowsFired = Math.max(0, arrowsFired);
        this.seedsPlanted = Math.max(0, seedsPlanted);
        this.plantsHarvested = Math.max(0, plantsHarvested);
        this.healingPetalsCollected = Math.max(0, healingPetalsCollected);
        this.antidotesCrafted = Math.max(0, antidotesCrafted);
        this.serumsCrafted = Math.max(0, serumsCrafted);
        this.radiationCuresUsed = Math.max(0, radiationCuresUsed);
        this.toxicBloomsGenerated = Math.max(0, toxicBloomsGenerated);
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
    public void setLastRadiationReceivedMs(long ms) { this.lastRadiationReceivedMs = ms; this.dirty = true; }

    public String getLastRadiationSource() { return lastRadiationSource; }
    public void setLastRadiationSource(String source) {
        this.lastRadiationSource = source != null ? source : "UNKNOWN";
        this.dirty = true;
    }

    public int getTimesInfected() { return timesInfected; }
    public void setTimesInfected(int count) { this.timesInfected = Math.max(0, count); this.dirty = true; }

    public double getTotalRadiationExposure() { return totalRadiationExposure; }
    public void addTotalRadiationExposure(double amount) { this.totalRadiationExposure += Math.max(0, amount); this.dirty = true; }

    public double getTotalRadiationCured() { return totalRadiationCured; }
    public void addTotalRadiationCured(double amount) { this.totalRadiationCured += Math.max(0, amount); this.dirty = true; }

    public int getRadiationDeaths() { return radiationDeaths; }
    public void setRadiationDeaths(int count) { this.radiationDeaths = Math.max(0, count); this.dirty = true; }

    // ──────────────────────────────────────────────────────────────────────────
    // Phase 3 zombie statistics
    // ──────────────────────────────────────────────────────────────────────────

    public int getIrradiatedZombiesKilled() { return irradiatedZombiesKilled; }
    public void incrementIrradiatedZombiesKilled() { this.irradiatedZombiesKilled++; this.dirty = true; }
    public void setIrradiatedZombiesKilled(int count) { this.irradiatedZombiesKilled = Math.max(0, count); this.dirty = true; }

    public int getAlphaZombiesKilled() { return alphaZombiesKilled; }
    public void incrementAlphaZombiesKilled() { this.alphaZombiesKilled++; this.dirty = true; }
    public void setAlphaZombiesKilled(int count) { this.alphaZombiesKilled = Math.max(0, count); this.dirty = true; }

    public int getRadiationCloudsSurvived() { return radiationCloudsSurvived; }
    public void incrementRadiationCloudsSurvived() { this.radiationCloudsSurvived++; this.dirty = true; }
    public void setRadiationCloudsSurvived(int count) { this.radiationCloudsSurvived = Math.max(0, count); this.dirty = true; }

    public int getRadioactiveCoresCollected() { return radioactiveCoresCollected; }
    public void incrementRadioactiveCoresCollected() { this.radioactiveCoresCollected++; this.dirty = true; }
    public void setRadioactiveCoresCollected(int count) { this.radioactiveCoresCollected = Math.max(0, count); this.dirty = true; }

    public int getMutatedSeedsCollected() { return mutatedSeedsCollected; }
    public void incrementMutatedSeedsCollected() { this.mutatedSeedsCollected++; this.dirty = true; }
    public void setMutatedSeedsCollected(int count) { this.mutatedSeedsCollected = Math.max(0, count); this.dirty = true; }

    public int getIrradiatedHeartsCollected() { return irradiatedHeartsCollected; }
    public void incrementIrradiatedHeartsCollected() { this.irradiatedHeartsCollected++; this.dirty = true; }
    public void setIrradiatedHeartsCollected(int count) { this.irradiatedHeartsCollected = Math.max(0, count); this.dirty = true; }

    // ──────────────────────────────────────────────────────────────────────────
    // Phase 4 ore statistics
    // ──────────────────────────────────────────────────────────────────────────

    public int getPlutoniumOreFound() { return plutoniumOreFound; }
    public void incrementPlutoniumOreFound() { this.plutoniumOreFound++; this.dirty = true; }
    public void setPlutoniumOreFound(int count) { this.plutoniumOreFound = Math.max(0, count); this.dirty = true; }

    public int getPlutoniumOreMined() { return plutoniumOreMined; }
    public void incrementPlutoniumOreMined() { this.plutoniumOreMined++; this.dirty = true; }
    public void setPlutoniumOreMined(int count) { this.plutoniumOreMined = Math.max(0, count); this.dirty = true; }

    public int getFragmentsCollected() { return fragmentsCollected; }
    public void addFragmentsCollected(int amount) { this.fragmentsCollected += Math.max(0, amount); this.dirty = true; }
    public void setFragmentsCollected(int count) { this.fragmentsCollected = Math.max(0, count); this.dirty = true; }

    public int getRadiationBurstsTriggered() { return radiationBurstsTriggered; }
    public void incrementRadiationBurstsTriggered() { this.radiationBurstsTriggered++; this.dirty = true; }
    public void setRadiationBurstsTriggered(int count) { this.radiationBurstsTriggered = Math.max(0, count); this.dirty = true; }

    public int getDrillUses() { return drillUses; }
    public void incrementDrillUses() { this.drillUses++; this.dirty = true; }
    public void setDrillUses(int count) { this.drillUses = Math.max(0, count); this.dirty = true; }

    public int getUnsafeMiningAttempts() { return unsafeMiningAttempts; }
    public void incrementUnsafeMiningAttempts() { this.unsafeMiningAttempts++; this.dirty = true; }
    public void setUnsafeMiningAttempts(int count) { this.unsafeMiningAttempts = Math.max(0, count); this.dirty = true; }

    // ──────────────────────────────────────────────────────────────────────────
    // Phase 5 smelter statistics
    // ──────────────────────────────────────────────────────────────────────────

    public int getMachinesBuilt() { return machinesBuilt; }
    public void incrementMachinesBuilt() { this.machinesBuilt++; this.dirty = true; }
    public void setMachinesBuilt(int count) { this.machinesBuilt = Math.max(0, count); this.dirty = true; }

    public int getFragmentsProcessed() { return fragmentsProcessed; }
    public void incrementFragmentsProcessed() { this.fragmentsProcessed++; this.dirty = true; }
    public void setFragmentsProcessed(int count) { this.fragmentsProcessed = Math.max(0, count); this.dirty = true; }

    public int getIngotsProduced() { return ingotsProduced; }
    public void incrementIngotsProduced() { this.ingotsProduced++; this.dirty = true; }
    public void setIngotsProduced(int count) { this.ingotsProduced = Math.max(0, count); this.dirty = true; }

    public int getFuelConsumed() { return fuelConsumed; }
    public void addFuelConsumed(int amount) { this.fuelConsumed += Math.max(0, amount); this.dirty = true; }
    public void setFuelConsumed(int count) { this.fuelConsumed = Math.max(0, count); this.dirty = true; }

    public int getOverheatsTriggered() { return overheatsTriggered; }
    public void incrementOverheatsTriggered() { this.overheatsTriggered++; this.dirty = true; }
    public void setOverheatsTriggered(int count) { this.overheatsTriggered = Math.max(0, count); this.dirty = true; }

    // ──────────────────────────────────────────────────────────────────────────
    // Phase 6 equipment statistics
    // ──────────────────────────────────────────────────────────────────────────

    public int getSwordHits() { return swordHits; }
    public void setSwordHits(int count) { this.swordHits = Math.max(0, count); this.dirty = true; }

    public int getRadiationDamageInflicted() { return radiationDamageInflicted; }
    public void setRadiationDamageInflicted(int count) { this.radiationDamageInflicted = Math.max(0, count); this.dirty = true; }

    public int getBlocksConverted() { return blocksConverted; }
    public void setBlocksConverted(int count) { this.blocksConverted = Math.max(0, count); this.dirty = true; }

    public int getFarmlandCreated() { return farmlandCreated; }
    public void setFarmlandCreated(int count) { this.farmlandCreated = Math.max(0, count); this.dirty = true; }

    public int getDebrisGenerated() { return debrisGenerated; }
    public void setDebrisGenerated(int count) { this.debrisGenerated = Math.max(0, count); this.dirty = true; }

    public int getArrowsFired() { return arrowsFired; }
    public void setArrowsFired(int count) { this.arrowsFired = Math.max(0, count); this.dirty = true; }

    // ──────────────────────────────────────────────────────────────────────────
    // Phase 7 farming statistics
    // ──────────────────────────────────────────────────────────────────────────

    public int getSeedsPlanted() { return seedsPlanted; }
    public void incrementSeedsPlanted() { this.seedsPlanted++; this.dirty = true; }
    public void setSeedsPlanted(int count) { this.seedsPlanted = Math.max(0, count); this.dirty = true; }

    public int getPlantsHarvested() { return plantsHarvested; }
    public void incrementPlantsHarvested() { this.plantsHarvested++; this.dirty = true; }
    public void setPlantsHarvested(int count) { this.plantsHarvested = Math.max(0, count); this.dirty = true; }

    public int getHealingPetalsCollected() { return healingPetalsCollected; }
    public void addHealingPetalsCollected(int amount) { this.healingPetalsCollected += Math.max(0, amount); this.dirty = true; }
    public void setHealingPetalsCollected(int count) { this.healingPetalsCollected = Math.max(0, count); this.dirty = true; }

    public int getAntidotesCrafted() { return antidotesCrafted; }
    public void incrementAntidotesCrafted() { this.antidotesCrafted++; this.dirty = true; }
    public void setAntidotesCrafted(int count) { this.antidotesCrafted = Math.max(0, count); this.dirty = true; }

    public int getSerumsCrafted() { return serumsCrafted; }
    public void incrementSerumsCrafted() { this.serumsCrafted++; this.dirty = true; }
    public void setSerumsCrafted(int count) { this.serumsCrafted = Math.max(0, count); this.dirty = true; }

    public int getRadiationCuresUsed() { return radiationCuresUsed; }
    public void incrementRadiationCuresUsed() { this.radiationCuresUsed++; this.dirty = true; }
    public void setRadiationCuresUsed(int count) { this.radiationCuresUsed = Math.max(0, count); this.dirty = true; }

    public int getToxicBloomsGenerated() { return toxicBloomsGenerated; }
    public void incrementToxicBloomsGenerated() { this.toxicBloomsGenerated++; this.dirty = true; }
    public void setToxicBloomsGenerated(int count) { this.toxicBloomsGenerated = Math.max(0, count); this.dirty = true; }

    // ──────────────────────────────────────────────────────────────────────────
    // Progression
    // ──────────────────────────────────────────────────────────────────────────

    public int getBossKills() { return bossKills; }
    public void incrementBossKills() { this.bossKills++; this.dirty = true; }

    // ──────────────────────────────────────────────────────────────────────────
    // Phase 8 forge statistics
    // ──────────────────────────────────────────────────────────────────────────

    public int getForgeUses()                { return forgeUses; }
    public void incrementForgeUses()         { this.forgeUses++; this.dirty = true; }
    public void setForgeUses(int count)      { this.forgeUses = Math.max(0, count); this.dirty = true; }

    public int getSuccessfulUpgrades()       { return successfulUpgrades; }
    public void incrementSuccessfulUpgrades(){ this.successfulUpgrades++; this.dirty = true; }
    public void setSuccessfulUpgrades(int v) { this.successfulUpgrades = Math.max(0, v); this.dirty = true; }

    public int getFailedUpgrades()           { return failedUpgrades; }
    public void incrementFailedUpgrades()    { this.failedUpgrades++; this.dirty = true; }
    public void setFailedUpgrades(int v)     { this.failedUpgrades = Math.max(0, v); this.dirty = true; }

    public int getMk4Creations()             { return mk4Creations; }
    public void incrementMk4Creations()      { this.mk4Creations++; this.dirty = true; }
    public void setMk4Creations(int v)       { this.mk4Creations = Math.max(0, v); this.dirty = true; }

    // ──────────────────────────────────────────────────────────────────────────
    // Phase 9 combat statistics
    // ──────────────────────────────────────────────────────────────────────────

    public int getPvPKills()                { return pvpKills; }
    public void incrementPvPKills()         { this.pvpKills++; this.dirty = true; }

    public int getPvPRadiationKills()       { return pvpRadiationKills; }
    public void incrementPvPRadiationKills(){ this.pvpRadiationKills++; this.dirty = true; }

    public int getPvPArrowKills()           { return pvpArrowKills; }
    public void incrementPvPArrowKills()    { this.pvpArrowKills++; this.dirty = true; }

    public int getPvPAuraKills()            { return pvpAuraKills; }
    public void incrementPvPAuraKills()     { this.pvpAuraKills++; this.dirty = true; }

    public int getPvPArrowHits()            { return pvpArrowHits; }
    public void incrementPvPArrowHits()     { this.pvpArrowHits++; this.dirty = true; }

    public long getTotalPvPRadiationInflicted() { return totalPvPRadiationInflicted; }
    public void addPvPRadiationInflicted(int amount) {
        this.totalPvPRadiationInflicted += Math.max(0, amount);
        this.dirty = true;
    }

    public long getAuraDamageDealt()        { return auraDamageDealt; }
    public void addAuraDamageDealt(int amount) {
        this.auraDamageDealt += Math.max(0, amount);
        this.dirty = true;
    }

    public int getSwordMasteryXp()          { return swordMasteryXp; }
    public void addSwordMasteryXp(int xp)   { this.swordMasteryXp += Math.max(0, xp); this.dirty = true; }

    public int getAxeMasteryXp()            { return axeMasteryXp; }
    public void addAxeMasteryXp(int xp)     { this.axeMasteryXp += Math.max(0, xp); this.dirty = true; }

    public int getBowMasteryXp()            { return bowMasteryXp; }
    public void addBowMasteryXp(int xp)     { this.bowMasteryXp += Math.max(0, xp); this.dirty = true; }

    /** Expose dirty flag so managers can mark without going through a stat increment. */
    public void setDirty(boolean dirty)      { this.dirty = dirty; }

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
                + ", oreFound=" + plutoniumOreFound
                + ", oreMined=" + plutoniumOreMined
                + ", swordHits=" + swordHits
                + ", seedsPlanted=" + seedsPlanted
                + ", plantsHarvested=" + plantsHarvested
                + ", source=" + lastRadiationSource + "}";
    }
}
