package com.nuclearcraft.balance;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Centralises all loot drop rate configuration for NuclearCraft.
 *
 * Provides validated drop rates for:
 *   - Irradiated Zombie loot (per level)
 *   - Plutonium Titan reward table
 *   - Plutonium Ore fragment drops (base + Fortune modifiers)
 *
 * Phase 13 addition. All values sourced from balance.yml.
 */
public class LootBalanceManager {

    private final NuclearCraftPlugin plugin;
    private final ConfigManager configManager;

    private FileConfiguration cfg;

    public LootBalanceManager(NuclearCraftPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void initialize() {
        cfg = configManager.getConfig(ConfigManager.ConfigFile.BALANCE);
        validate();
        NCLogger.info("[LootBalanceManager] Loot rates loaded and validated.");
    }

    public void reload() {
        cfg = configManager.getConfig(ConfigManager.ConfigFile.BALANCE);
        validate();
    }

    public void shutdown() {}

    // ── Zombie loot ───────────────────────────────────────────────────────────

    /** Chance that a level-N irradiated zombie drops a Radioactive Core (0–1). */
    public double getZombieCoreChance(int level) {
        return clamp(cfg.getDouble("loot.zombie.level-" + level + ".radioactive-core-chance",
                defaultCoreChance(level)));
    }

    /** Chance that a level-N irradiated zombie drops a Mutated Seed (0–1). */
    public double getZombieSeedChance(int level) {
        return clamp(cfg.getDouble("loot.zombie.level-" + level + ".mutated-seed-chance",
                defaultSeedChance(level)));
    }

    /** Chance that a level-N irradiated zombie drops an Irradiated Heart (0–1). */
    public double getZombieHeartChance(int level) {
        return clamp(cfg.getDouble("loot.zombie.level-" + level + ".irradiated-heart-chance",
                defaultHeartChance(level)));
    }

    /** Maximum rotten flesh drops per zombie. */
    public int getZombieFleshMax() {
        return Math.max(0, cfg.getInt("loot.zombie.flesh-max", 3));
    }

    // ── Titan loot ────────────────────────────────────────────────────────────

    /** Minimum titan fragments dropped on boss death. */
    public int getTitanFragmentMin() {
        return Math.max(1, cfg.getInt("loot.titan.fragment-min", 8));
    }

    /** Maximum titan fragments dropped on boss death. */
    public int getTitanFragmentMax() {
        return Math.max(getTitanFragmentMin(), cfg.getInt("loot.titan.fragment-max", 16));
    }

    /** Extra titan fragments awarded per significant contributor. */
    public int getTitanBonusFragmentPerContributor() {
        return Math.max(0, cfg.getInt("loot.titan.bonus-fragment-per-contributor", 2));
    }

    /** Guaranteed drops: Reactor Heart (1.0 = always). */
    public double getTitanReactorHeartChance() {
        return clamp(cfg.getDouble("loot.titan.reactor-heart-chance", 1.0));
    }

    /** Chance of dropping Ancient Reactor Blueprint. */
    public double getTitanAncientBlueprintChance() {
        return clamp(cfg.getDouble("loot.titan.ancient-blueprint-chance", 0.70));
    }

    /** Chance of dropping a Mutated Crystal. */
    public double getTitanMutatedCrystalChance() {
        return clamp(cfg.getDouble("loot.titan.mutated-crystal-chance", 0.50));
    }

    /** Guaranteed drops: Titan Core (1.0 = always). */
    public double getTitanCoreChance() {
        return clamp(cfg.getDouble("loot.titan.titan-core-chance", 1.0));
    }

    // ── Ore loot ─────────────────────────────────────────────────────────────

    /** Base number of Raw Plutonium Fragments dropped per ore block. */
    public int getOreFragmentBase() {
        return Math.max(1, cfg.getInt("loot.ore.fragment-base", 2));
    }

    /** Bonus fragments added for Fortune I. */
    public int getOreFortuneBonusLevel1() {
        return Math.max(0, cfg.getInt("loot.ore.fortune-level1-bonus", 1));
    }

    /** Bonus fragments added for Fortune II. */
    public int getOreFortuneBonusLevel2() {
        return Math.max(0, cfg.getInt("loot.ore.fortune-level2-bonus", 2));
    }

    /** Bonus fragments added for Fortune III. */
    public int getOreFortuneBonusLevel3() {
        return Math.max(0, cfg.getInt("loot.ore.fortune-level3-bonus", 3));
    }

    /**
     * Calculates total ore fragment drop count for a given Fortune level.
     *
     * @param fortuneLevel  Enchantment level (0 = none, 1–3 = Fortune I/II/III)
     * @return total fragment count
     */
    public int rollOreFragments(int fortuneLevel) {
        int base = getOreFragmentBase();
        return switch (fortuneLevel) {
            case 1  -> base + getOreFortuneBonusLevel1();
            case 2  -> base + getOreFortuneBonusLevel2();
            case 3  -> base + getOreFortuneBonusLevel3();
            default -> base;
        };
    }

    // ── Defaults (aligned with ZombieLevel hardcoded fallbacks) ───────────────

    private double defaultCoreChance(int level) {
        return switch (level) {
            case 1  -> 0.06;
            case 2  -> 0.10;
            case 3  -> 0.15;
            default -> 0.06;
        };
    }

    private double defaultSeedChance(int level) {
        return switch (level) {
            case 1  -> 0.04;
            case 2  -> 0.07;
            case 3  -> 0.10;
            default -> 0.04;
        };
    }

    private double defaultHeartChance(int level) {
        return switch (level) {
            case 1  -> 0.008;
            case 2  -> 0.010;
            case 3  -> 0.012;
            default -> 0.008;
        };
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private void validate() {
        for (int lvl = 1; lvl <= 3; lvl++) {
            warnIfOver1("zombie level-" + lvl + " core chance",  getZombieCoreChance(lvl));
            warnIfOver1("zombie level-" + lvl + " seed chance",  getZombieSeedChance(lvl));
            warnIfOver1("zombie level-" + lvl + " heart chance", getZombieHeartChance(lvl));
        }
        if (getTitanFragmentMin() > getTitanFragmentMax())
            NCLogger.warn("[LootBalanceManager] titan.fragment-min > fragment-max. Clamping to min.");
        if (getOreFragmentBase() < 1)
            NCLogger.warn("[LootBalanceManager] ore.fragment-base < 1. At least 1 fragment should drop.");
    }

    private void warnIfOver1(String key, double value) {
        if (value > 1.0)
            NCLogger.warn("[LootBalanceManager] " + key + " > 1.0 (" + value + ") — clamped to 1.0");
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
