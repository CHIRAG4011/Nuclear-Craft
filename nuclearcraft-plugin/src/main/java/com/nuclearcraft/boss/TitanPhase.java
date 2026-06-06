package com.nuclearcraft.boss;

/**
 * Represents the four combat phases of the Plutonium Titan.
 * Phase transitions are triggered by health percentage thresholds.
 */
public enum TitanPhase {

    PHASE_1(1.00, 0.75, "Phase I",   "<green>"),
    PHASE_2(0.75, 0.50, "Phase II",  "<yellow>"),
    PHASE_3(0.50, 0.25, "Phase III", "<red>"),
    PHASE_4(0.25, 0.00, "Phase IV",  "<dark_red>");

    private final double upperThreshold;
    private final double lowerThreshold;
    private final String displayName;
    private final String colorTag;

    TitanPhase(double upperThreshold, double lowerThreshold,
               String displayName, String colorTag) {
        this.upperThreshold = upperThreshold;
        this.lowerThreshold = lowerThreshold;
        this.displayName    = displayName;
        this.colorTag       = colorTag;
    }

    public double getUpperThreshold() { return upperThreshold; }
    public double getLowerThreshold() { return lowerThreshold; }
    public String getDisplayName()    { return displayName; }
    public String getColorTag()       { return colorTag; }

    public boolean contains(double healthPercent) {
        return healthPercent <= upperThreshold && healthPercent > lowerThreshold;
    }

    public static TitanPhase fromHealthPercent(double pct) {
        if (pct > 0.75) return PHASE_1;
        if (pct > 0.50) return PHASE_2;
        if (pct > 0.25) return PHASE_3;
        return PHASE_4;
    }
}
