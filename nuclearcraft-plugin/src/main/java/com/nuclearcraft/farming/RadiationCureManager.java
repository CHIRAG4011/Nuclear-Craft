package com.nuclearcraft.farming;

import com.nuclearcraft.utils.NCLogger;

/**
 * Orchestrator for the Phase 7 radiation cure system.
 *
 * <p>Owns {@link AntidoteManager}, {@link SerumManager}, and
 * {@link RadiationImmunityManager}. Provides a single access point
 * for FarmingManager.
 */
public class RadiationCureManager {

    private final AntidoteManager antidoteManager;
    private final SerumManager serumManager;
    private final RadiationImmunityManager immunityManager;

    public RadiationCureManager(AntidoteManager antidoteManager,
                                 SerumManager serumManager,
                                 RadiationImmunityManager immunityManager) {
        this.antidoteManager = antidoteManager;
        this.serumManager    = serumManager;
        this.immunityManager = immunityManager;
    }

    public void initialize() {
        antidoteManager.initialize();
        serumManager.initialize();
        immunityManager.initialize();
        NCLogger.info("RadiationCureManager initialized.");
    }

    public void shutdown() {
        immunityManager.shutdown();
        serumManager.shutdown();
        antidoteManager.shutdown();
        NCLogger.info("RadiationCureManager shut down.");
    }

    public AntidoteManager getAntidoteManager()           { return antidoteManager; }
    public SerumManager getSerumManager()                 { return serumManager; }
    public RadiationImmunityManager getImmunityManager()  { return immunityManager; }
}
