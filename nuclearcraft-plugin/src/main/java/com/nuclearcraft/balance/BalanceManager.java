package com.nuclearcraft.balance;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Central balance configuration manager for NuclearCraft.
 *
 * Reads all gameplay balance values from balance.yml and exposes
 * typed getters. Systems that previously hardcoded values should
 * migrate to reading from this manager.
 *
 * Also validates ranges on startup and logs warnings for suspect values.
 *
 * Phase 12 addition.
 */
public class BalanceManager {

    private final NuclearCraftPlugin plugin;
    private final ConfigManager configManager;

    private FileConfiguration cfg;

    public BalanceManager(NuclearCraftPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void initialize() {
        cfg = configManager.getConfig(ConfigManager.ConfigFile.BALANCE);
        validate();
        NCLogger.info("[BalanceManager] Balance values loaded and validated.");
    }

    public void reload() {
        cfg = configManager.getConfig(ConfigManager.ConfigFile.BALANCE);
        validate();
    }

    public void shutdown() {}

    // ── Radiation ─────────────────────────────────────────────────────────────

    public double getRadiationDecayRate()       { return cfg.getDouble("radiation.decay-rate-per-tick", 0.05); }
    public double getRadiationOreMiningGain()   { return cfg.getDouble("radiation.ore-mining-gain", 25.0); }
    public double getRadiationZombieHitGain()   { return cfg.getDouble("radiation.zombie-hit-gain", 15.0); }
    public double getRadiationContagionRate()   { return cfg.getDouble("radiation.contagion-rate", 5.0); }
    public double getRadiationAntidoteCost()    { return cfg.getDouble("radiation.antidote-petal-cost", 8); }
    public double getRadiationSerumCost()       { return cfg.getDouble("radiation.serum-craft-cost", 4); }

    public double getStageOneThreshold()        { return cfg.getDouble("radiation.stage-thresholds.stage-1", 100); }
    public double getStageTwoThreshold()        { return cfg.getDouble("radiation.stage-thresholds.stage-2", 300); }
    public double getStageThreeThreshold()      { return cfg.getDouble("radiation.stage-thresholds.stage-3", 500); }
    public double getStageFourThreshold()       { return cfg.getDouble("radiation.stage-thresholds.stage-4", 750); }
    public double getStageFiveThreshold()       { return cfg.getDouble("radiation.stage-thresholds.stage-5", 950); }

    // ── Zombies ───────────────────────────────────────────────────────────────

    public double getZombieIrradiatedHealth()   { return cfg.getDouble("zombies.irradiated.health", 30.0); }
    public double getZombieIrradiatedDamage()   { return cfg.getDouble("zombies.irradiated.damage", 6.0); }
    public double getZombieAlphaHealth()        { return cfg.getDouble("zombies.alpha.health", 80.0); }
    public double getZombieAlphaDamage()        { return cfg.getDouble("zombies.alpha.damage", 12.0); }
    public int    getZombieAlphaGlowRadius()    { return cfg.getInt("zombies.alpha.glow-radius", 8); }
    public double getZombieSpawnChance()        { return cfg.getDouble("zombies.spawn-chance", 0.10); }
    public int    getZombieNightSpawnMultiplier(){ return cfg.getInt("zombies.night-spawn-multiplier", 3); }

    // ── Ore ───────────────────────────────────────────────────────────────────

    public int    getOreVeinSize()              { return cfg.getInt("ore.vein-size", 3); }
    public int    getOreVeinsPerChunk()         { return cfg.getInt("ore.veins-per-chunk", 1); }
    public int    getOreMinY()                  { return cfg.getInt("ore.min-y", -64); }
    public int    getOreMaxY()                  { return cfg.getInt("ore.max-y", -20); }
    public double getOreDrillRadiationReduction(){ return cfg.getDouble("ore.drill-radiation-reduction", 0.75); }

    // ── Machines ─────────────────────────────────────────────────────────────

    public int    getSmelterProcessTicks()      { return cfg.getInt("machines.smelter.process-ticks", 200); }
    public double getSmelterRadiationPerTick()  { return cfg.getDouble("machines.smelter.radiation-per-tick", 0.5); }
    public int    getSmelterFuelCoalTicks()     { return cfg.getInt("machines.smelter.fuel.coal", 800); }
    public int    getSmelterFuelBlazeRodTicks() { return cfg.getInt("machines.smelter.fuel.blaze-rod", 1200); }
    public int    getSmelterFuelLavaTicks()     { return cfg.getInt("machines.smelter.fuel.lava-bucket", 2000); }

    public int    getForgeEnergyCapacity()      { return cfg.getInt("machines.forge.energy-capacity", 1000); }
    public int    getForgeEnergyCostMk1()       { return cfg.getInt("machines.forge.energy-cost.mk1", 100); }
    public int    getForgeEnergyCostMk2()       { return cfg.getInt("machines.forge.energy-cost.mk2", 250); }
    public int    getForgeEnergyCostMk3()       { return cfg.getInt("machines.forge.energy-cost.mk3", 500); }
    public int    getForgeEnergyCostMk4()       { return cfg.getInt("machines.forge.energy-cost.mk4", 1000); }
    public double getForgeSuccessChanceMk1()    { return cfg.getDouble("machines.forge.success-chance.mk1", 0.90); }
    public double getForgeSuccessChanceMk2()    { return cfg.getDouble("machines.forge.success-chance.mk2", 0.75); }
    public double getForgeSuccessChanceMk3()    { return cfg.getDouble("machines.forge.success-chance.mk3", 0.60); }
    public double getForgeSuccessChanceMk4()    { return cfg.getDouble("machines.forge.success-chance.mk4", 0.40); }

    // ── Equipment ─────────────────────────────────────────────────────────────

    public double getPlutoniumSwordDamage()     { return cfg.getDouble("equipment.plutonium.sword-damage", 9.0); }
    public double getPlutoniumPickaxeSpeed()    { return cfg.getDouble("equipment.plutonium.pickaxe-speed", 8.0); }
    public double getPlutoniumArmorToughness()  { return cfg.getDouble("equipment.plutonium.armor-toughness", 2.0); }
    public double getHazmatResistancePercent()  { return cfg.getDouble("equipment.hazmat.radiation-resistance", 0.75); }
    public double getPlutoniumArmorResistance() { return cfg.getDouble("equipment.plutonium-armor.radiation-resistance", 0.50); }

    // ── Farming ───────────────────────────────────────────────────────────────

    public double getMutatedCropGrowthBonus()   { return cfg.getDouble("farming.mutated-crop-growth-bonus", 0.5); }
    public double getToxicBloomSpreadChance()   { return cfg.getDouble("farming.toxic-bloom-spread-chance", 0.15); }
    public int    getToxicBloomRadius()         { return cfg.getInt("farming.toxic-bloom-radius", 3); }
    public double getFarmlandRadiationGain()    { return cfg.getDouble("farming.farmland-radiation-per-tick", 1.0); }
    public int    getAntidotePetalCount()       { return cfg.getInt("farming.antidote-petal-count", 8); }
    public int    getSerumImmunityMinutes()     { return cfg.getInt("farming.serum-immunity-minutes", 10); }

    // ── Combat ────────────────────────────────────────────────────────────────

    public double getCombatRadiationDamageMultiplier() { return cfg.getDouble("combat.radiation-damage-multiplier", 0.5); }
    public double getCombatInfectionChance()           { return cfg.getDouble("combat.infection-on-hit-chance", 0.10); }
    public double getCombatAoeRadius()                 { return cfg.getDouble("combat.aoe-radius", 4.0); }
    public int    getCombatMasteryMaxLevel()            { return cfg.getInt("combat.mastery-max-level", 10); }
    public int    getCombatMasteryKillsPerLevel()       { return cfg.getInt("combat.mastery-kills-per-level", 50); }

    // ── Boss — Plutonium Titan ────────────────────────────────────────────────

    public double getTitanBaseHealth()          { return cfg.getDouble("boss.titan.base-health", 800.0); }
    public double getTitanDamageScaling()       { return cfg.getDouble("boss.titan.damage-scaling", 1.0); }
    public double getTitanRadiationAuraRadius() { return cfg.getDouble("boss.titan.radiation-aura-radius", 8.0); }
    public double getTitanRadiationAuraGain()   { return cfg.getDouble("boss.titan.radiation-aura-gain", 10.0); }
    public int    getTitanPhase2Threshold()     { return cfg.getInt("boss.titan.phase2-health-percent", 75); }
    public int    getTitanPhase3Threshold()     { return cfg.getInt("boss.titan.phase3-health-percent", 50); }
    public int    getTitanPhase4Threshold()     { return cfg.getInt("boss.titan.phase4-health-percent", 25); }
    public int    getTitanAbilityCooldownTicks() { return cfg.getInt("boss.titan.ability-cooldown-ticks", 100); }
    public int    getTitanMaxSummons()          { return cfg.getInt("boss.titan.max-summons", 6); }

    // ── Titan Technology ──────────────────────────────────────────────────────

    public double getTitanArmorSetBonusResistance() { return cfg.getDouble("titantech.set-bonus.radiation-resistance", 1.0); }
    public double getTitanSwordBaseDamage()         { return cfg.getDouble("titantech.titan-sword.base-damage", 20.0); }
    public double getTitanWeaponRadiationOnHit()    { return cfg.getDouble("titantech.weapons.radiation-on-hit", 50.0); }

    // ── Economy guards ────────────────────────────────────────────────────────

    public int    getBossRespawnCooldownMinutes() { return cfg.getInt("economy.boss-respawn-cooldown-minutes", 60); }
    public int    getAfkFarmingTimeout()         { return cfg.getInt("economy.afk-farming-timeout-seconds", 300); }
    public int    getMaxMachinesPerPlayer()      { return cfg.getInt("economy.max-machines-per-player", 5); }

    // ── Internal validation ────────────────────────────────────────────────────

    private void validate() {
        if (getForgeSuccessChanceMk4() > 1.0 || getForgeSuccessChanceMk4() < 0)
            NCLogger.warn("[BalanceManager] forge success chance mk4 out of range [0,1]: " + getForgeSuccessChanceMk4());
        if (getRadiationDecayRate() <= 0)
            NCLogger.warn("[BalanceManager] radiation decay-rate-per-tick <= 0; radiation will never decay.");
        if (getTitanBaseHealth() < 100)
            NCLogger.warn("[BalanceManager] Titan base health very low (" + getTitanBaseHealth() + "). Intended?");
        if (getHazmatResistancePercent() > 1.0)
            NCLogger.warn("[BalanceManager] hazmat radiation-resistance > 1.0; players will be fully immune.");
        if (getToxicBloomSpreadChance() > 0.5)
            NCLogger.warn("[BalanceManager] toxic-bloom-spread-chance > 0.5; bloom may spread aggressively.");
    }
}
