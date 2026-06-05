package com.nuclearcraft.zombies;

import com.nuclearcraft.utils.RandomUtil;

/**
 * Defines the four tiers of Irradiated Zombie, including their stats, loot chances, and rarity.
 * All values are defaults — the config layer can override them at runtime.
 */
public enum ZombieLevel {

    LEVEL_1(1,
            "Irradiated Zombie",
            35.0,   // health
            5.0,    // attack damage
            1.15,   // speed multiplier (15% faster than base zombie)
            0.15,   // knockback resistance
            1.20,   // follow range multiplier (20% increase)
            10,     // radiation on hit
            10,     // xp reward
            0.80,   // spawn weight
            0.15,   // radioactive core drop chance
            0.05,   // mutated seed drop chance
            0.01,   // irradiated heart drop chance
            false), // isAlpha

    LEVEL_2(2,
            "Irradiated Zombie",
            45.0,
            7.0,
            1.20,
            0.20,
            1.20,
            20,
            20,
            0.15,
            0.25,
            0.10,
            0.03,
            false),

    LEVEL_3(3,
            "Irradiated Zombie",
            60.0,
            9.0,
            1.25,
            0.30,
            1.20,
            35,
            40,
            0.04,
            0.40,
            0.20,
            0.07,
            false),

    LEVEL_4(4,
            "Alpha Irradiated Zombie",
            80.0,
            12.0,
            1.30,   // 20% faster than base = ~30% above normal zombie (base * 1.30)
            0.50,   // 50% knockback resistance
            1.50,   // +50% follow range
            50,
            100,
            0.01,
            1.00,   // guaranteed radioactive core
            0.50,
            0.25,
            true);

    private final int level;
    private final String displayName;
    private final double health;
    private final double attackDamage;
    private final double speedMultiplier;
    private final double knockbackResistance;
    private final double followRangeMultiplier;
    private final int radiationOnHit;
    private final int xpReward;
    private final double spawnWeight;
    private final double radioactiveCoreChance;
    private final double mutatedSeedChance;
    private final double irradiatedHeartChance;
    private final boolean alpha;

    ZombieLevel(int level, String displayName, double health, double attackDamage,
                double speedMultiplier, double knockbackResistance, double followRangeMultiplier,
                int radiationOnHit, int xpReward, double spawnWeight,
                double radioactiveCoreChance, double mutatedSeedChance,
                double irradiatedHeartChance, boolean alpha) {
        this.level = level;
        this.displayName = displayName;
        this.health = health;
        this.attackDamage = attackDamage;
        this.speedMultiplier = speedMultiplier;
        this.knockbackResistance = knockbackResistance;
        this.followRangeMultiplier = followRangeMultiplier;
        this.radiationOnHit = radiationOnHit;
        this.xpReward = xpReward;
        this.spawnWeight = spawnWeight;
        this.radioactiveCoreChance = radioactiveCoreChance;
        this.mutatedSeedChance = mutatedSeedChance;
        this.irradiatedHeartChance = irradiatedHeartChance;
        this.alpha = alpha;
    }

    /**
     * Rolls a random level based on weighted spawn chances.
     * Weights: L1=80%, L2=15%, L3=4%, L4=1%.
     */
    public static ZombieLevel rollLevel() {
        double roll = RandomUtil.nextDouble(0, 1);
        double cumulative = 0;
        for (ZombieLevel level : values()) {
            cumulative += level.spawnWeight;
            if (roll < cumulative) return level;
        }
        return LEVEL_1;
    }

    public static ZombieLevel fromInt(int level) {
        return switch (level) {
            case 2 -> LEVEL_2;
            case 3 -> LEVEL_3;
            case 4 -> LEVEL_4;
            default -> LEVEL_1;
        };
    }

    public int getLevel() { return level; }
    public String getDisplayName() { return displayName; }
    public double getHealth() { return health; }
    public double getAttackDamage() { return attackDamage; }
    public double getSpeedMultiplier() { return speedMultiplier; }
    public double getKnockbackResistance() { return knockbackResistance; }
    public double getFollowRangeMultiplier() { return followRangeMultiplier; }
    public int getRadiationOnHit() { return radiationOnHit; }
    public int getXpReward() { return xpReward; }
    public double getSpawnWeight() { return spawnWeight; }
    public double getRadioactiveCoreChance() { return radioactiveCoreChance; }
    public double getMutatedSeedChance() { return mutatedSeedChance; }
    public double getIrradiatedHeartChance() { return irradiatedHeartChance; }
    public boolean isAlpha() { return alpha; }
}
