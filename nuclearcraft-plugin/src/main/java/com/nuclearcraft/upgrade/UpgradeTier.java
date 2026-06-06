package com.nuclearcraft.upgrade;

/**
 * Defines the five upgrade tiers available through the Nuclear Forge.
 *
 * All percentage bonuses are applied on top of the base Plutonium equipment stats.
 * Requirements are the minimum number of each material consumed on a successful upgrade.
 */
public enum UpgradeTier {

    MK_0(0, "MK-0",
            0, 0, 0, 0,
            100.0, false,
            0, 0, 0,
            0, 0),

    MK_I(1, "MK-I",
            5, 5, 5, 5,
            100.0, false,
            2, 1, 0,
            60, 500),

    MK_II(2, "MK-II",
            10, 10, 10, 10,
            90.0, false,
            4, 2, 0,
            100, 1200),

    MK_III(3, "MK-III",
            20, 20, 20, 20,
            75.0, false,
            8, 4, 1,
            160, 2500),

    MK_IV(4, "MK-IV",
            35, 35, 35, 35,
            50.0, true,
            16, 8, 2,
            240, 5000);

    /** 0–4 numeric rank. */
    private final int level;
    /** Human-readable label shown in GUI and item lore. */
    private final String displayName;

    /** Bonus percentages over base equipment values. */
    private final int damageBonusPct;
    private final int durabilityBonusPct;
    private final int speedBonusPct;
    private final int armorBonusPct;

    /** Probability (0–100) that an upgrade attempt succeeds. */
    private final double successChance;
    /** Whether this tier grants the Radiation Aura passive. */
    private final boolean hasRadiationAura;

    /** Materials consumed on a successful upgrade attempt. */
    private final int ingotsRequired;     // Refined Plutonium Ingots
    private final int coresRequired;      // Radioactive Cores
    private final int heartsRequired;     // Irradiated Hearts

    /** Ticks the forge processes before resolving (configurable via forge.yml). */
    private final int baseDurationTicks;

    /** Forge energy consumed by one upgrade attempt of this tier. */
    private final int energyCost;

    UpgradeTier(int level, String displayName,
                int damageBonusPct, int durabilityBonusPct,
                int speedBonusPct, int armorBonusPct,
                double successChance, boolean hasRadiationAura,
                int ingotsRequired, int coresRequired, int heartsRequired,
                int baseDurationTicks, int energyCost) {
        this.level = level;
        this.displayName = displayName;
        this.damageBonusPct = damageBonusPct;
        this.durabilityBonusPct = durabilityBonusPct;
        this.speedBonusPct = speedBonusPct;
        this.armorBonusPct = armorBonusPct;
        this.successChance = successChance;
        this.hasRadiationAura = hasRadiationAura;
        this.ingotsRequired = ingotsRequired;
        this.coresRequired = coresRequired;
        this.heartsRequired = heartsRequired;
        this.baseDurationTicks = baseDurationTicks;
        this.energyCost = energyCost;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public int getLevel()               { return level; }
    public String getDisplayName()      { return displayName; }
    public int getDamageBonusPct()      { return damageBonusPct; }
    public int getDurabilityBonusPct()  { return durabilityBonusPct; }
    public int getSpeedBonusPct()       { return speedBonusPct; }
    public int getArmorBonusPct()       { return armorBonusPct; }
    public double getSuccessChance()    { return successChance; }
    public boolean hasRadiationAura()   { return hasRadiationAura; }
    public int getIngotsRequired()      { return ingotsRequired; }
    public int getCoresRequired()       { return coresRequired; }
    public int getHeartsRequired()      { return heartsRequired; }
    public int getBaseDurationTicks()   { return baseDurationTicks; }
    public int getEnergyCost()          { return energyCost; }

    /**
     * Returns the next upgrade tier, or null if already at MK-IV.
     */
    public UpgradeTier next() {
        UpgradeTier[] values = values();
        if (level + 1 >= values.length) return null;
        return values[level + 1];
    }

    /**
     * Returns the tier for the given level, or MK_0 if out of range.
     */
    public static UpgradeTier fromLevel(int level) {
        for (UpgradeTier t : values()) {
            if (t.level == level) return t;
        }
        return MK_0;
    }
}
