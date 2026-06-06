package com.nuclearcraft.farming;

import com.nuclearcraft.utils.NCLogger;

/**
 * Orchestrator for the Mutated Healing Plant subsystem.
 *
 * <p>Owns {@link MutatedCropManager}, {@link MutatedSeedManager},
 * {@link PlantGrowthManager}, {@link PlantHarvestManager}, and
 * {@link ToxicBloomManager}.
 */
public class MutatedPlantManager {

    private final MutatedCropManager cropManager;
    private final MutatedSeedManager seedManager;
    private final PlantGrowthManager growthManager;
    private final PlantHarvestManager harvestManager;
    private final ToxicBloomManager toxicBloomManager;

    public MutatedPlantManager(MutatedCropManager cropManager,
                                MutatedSeedManager seedManager,
                                PlantGrowthManager growthManager,
                                PlantHarvestManager harvestManager,
                                ToxicBloomManager toxicBloomManager) {
        this.cropManager     = cropManager;
        this.seedManager     = seedManager;
        this.growthManager   = growthManager;
        this.harvestManager  = harvestManager;
        this.toxicBloomManager = toxicBloomManager;
    }

    public void initialize() {
        cropManager.initialize();
        toxicBloomManager.initialize();
        NCLogger.info("MutatedPlantManager initialized.");
    }

    public void shutdown() {
        toxicBloomManager.shutdown();
        cropManager.shutdown();
        NCLogger.info("MutatedPlantManager shut down.");
    }

    // Sub-managers are initialized inline; PlantGrowthManager/PlantHarvestManager
    // have no tasks, so they initialize trivially.
    public void initializeSubManagers() {
        seedManager.initialize();
        growthManager.initialize();
        harvestManager.initialize();
    }

    public MutatedCropManager getCropManager()         { return cropManager; }
    public MutatedSeedManager getSeedManager()         { return seedManager; }
    public PlantGrowthManager getGrowthManager()       { return growthManager; }
    public PlantHarvestManager getHarvestManager()     { return harvestManager; }
    public ToxicBloomManager getToxicBloomManager()    { return toxicBloomManager; }
}
