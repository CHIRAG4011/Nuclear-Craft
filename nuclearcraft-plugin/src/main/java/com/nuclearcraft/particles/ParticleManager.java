package com.nuclearcraft.particles;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.performance.PerformanceManager;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Categorized particle effect manager for NuclearCraft.
 *
 * Respects per-player particle limits exposed by {@link PerformanceManager}.
 * Each public method corresponds to a gameplay category and can be called
 * from any listener or manager.
 *
 * Categories:
 *  - Radiation
 *  - Ore Effects
 *  - Machine Effects
 *  - Weapon Effects
 *  - Armor Effects
 *  - Boss Effects
 *  - Farming Effects
 *  - Healing Effects
 *  - Upgrade Effects
 *
 * Phase 12 addition.
 */
public class ParticleManager {

    // Neon green
    private static final Color GREEN  = Color.fromRGB(57, 255, 20);
    // Reactor orange
    private static final Color ORANGE = Color.fromRGB(255, 100, 0);
    // Toxic yellow
    private static final Color YELLOW = Color.fromRGB(255, 230, 0);
    // Titan violet
    private static final Color VIOLET = Color.fromRGB(119, 0, 255);
    // Heal blue
    private static final Color BLUE   = Color.fromRGB(100, 200, 255);
    // Cure cyan
    private static final Color CYAN   = Color.fromRGB(0, 255, 200);

    private final NuclearCraftPlugin plugin;
    private final ConfigManager configManager;
    private PerformanceManager performanceManager;

    private int maxParticlesPerPlayer;

    public ParticleManager(NuclearCraftPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void initialize() {
        maxParticlesPerPlayer = configManager.getConfig(ConfigManager.ConfigFile.MAIN)
                .getInt("performance.max-particles-per-player", 50);
        NCLogger.info("[ParticleManager] Initialized. Particle cap: " + maxParticlesPerPlayer);
    }

    public void reload() {
        initialize();
    }

    public void shutdown() {}

    /** Inject PerformanceManager after it has been created. */
    public void setPerformanceManager(PerformanceManager pm) {
        this.performanceManager = pm;
    }

    // ── Radiation Particles ───────────────────────────────────────────────────

    /** Green toxic dust ring around a player experiencing radiation gain. */
    public void spawnRadiationGain(Player player) {
        if (!shouldSpawn()) return;
        Location loc = player.getLocation().add(0, 1, 0);
        spawnDustRing(loc, GREEN, 1.2f, 12, 0.8);
    }

    /** Dense green burst for a radiation stage change. */
    public void spawnRadiationStageChange(Player player) {
        if (!shouldSpawn()) return;
        Location loc = player.getLocation().add(0, 1, 0);
        spawnDustBurst(loc, GREEN, 1.5f, 30, 1.2);
        spawnDustBurst(loc, YELLOW, 1.0f, 15, 1.0);
    }

    /** Upward green column for a radiation surge event. */
    public void spawnRadiationSurge(Location center) {
        if (!shouldSpawn()) return;
        spawnColumn(center, Particle.WITCH, 40, 4.0, 8.0);
    }

    /** Small ambient green mist around a player in a radiation zone. */
    public void spawnRadiationAmbient(Player player) {
        if (!shouldSpawn()) return;
        Location loc = player.getLocation().add(
                random(-1, 1), random(0, 2), random(-1, 1));
        spawnDust(loc, GREEN, 0.8f, 2);
    }

    // ── Ore Effects ───────────────────────────────────────────────────────────

    /** Green sparkle effect when plutonium ore is discovered nearby. */
    public void spawnOreDiscover(Location oreLoc) {
        if (!shouldSpawn()) return;
        spawnDustBurst(oreLoc.clone().add(0.5, 0.5, 0.5), GREEN, 1.2f, 20, 0.5);
    }

    /** Dust cloud when ore is broken. */
    public void spawnOreMine(Location oreLoc) {
        if (!shouldSpawn()) return;
        World w = oreLoc.getWorld();
        if (w == null) return;
        w.spawnParticle(Particle.BLOCK, oreLoc.clone().add(0.5, 0.5, 0.5), 20,
                0.4, 0.4, 0.4, 0.05,
                org.bukkit.Material.MOSSY_COBBLESTONE.createBlockData());
        spawnDust(oreLoc.clone().add(0.5, 0.9, 0.5), GREEN, 1.0f, 8);
    }

    /** Drill sparks during mining operation. */
    public void spawnDrillEffect(Location loc) {
        if (!shouldSpawn()) return;
        World w = loc.getWorld();
        if (w == null) return;
        w.spawnParticle(Particle.CRIT, loc, 5, 0.2, 0.2, 0.2, 0.1);
        spawnDust(loc, GREEN, 0.6f, 3);
    }

    /** Ambient green glow around an ore block. */
    public void spawnOreAmbient(Location oreLoc) {
        if (!shouldSpawn()) return;
        Location offset = oreLoc.clone().add(
                random(0, 1), random(0, 1), random(0, 1));
        spawnDust(offset, GREEN, 0.8f, 1);
    }

    // ── Machine Effects ───────────────────────────────────────────────────────

    /** Orange flame + green smoke from a running nuclear smelter. */
    public void spawnSmelterRunning(Location machineLoc) {
        if (!shouldSpawn()) return;
        Location top = machineLoc.clone().add(0.5, 1.1, 0.5);
        World w = top.getWorld();
        if (w == null) return;
        w.spawnParticle(Particle.FLAME, top, 3, 0.2, 0.0, 0.2, 0.02);
        w.spawnParticle(Particle.SMOKE, top, 2, 0.1, 0.2, 0.1, 0.01);
        spawnDust(top, GREEN, 0.8f, 2);
    }

    /** Bright burst when smelter completes a recipe. */
    public void spawnSmelterComplete(Location machineLoc) {
        if (!shouldSpawn()) return;
        Location top = machineLoc.clone().add(0.5, 1.2, 0.5);
        spawnDustBurst(top, GREEN, 1.5f, 20, 0.6);
        World w = top.getWorld();
        if (w != null) {
            w.spawnParticle(Particle.HAPPY_VILLAGER, top, 8, 0.4, 0.3, 0.4, 0);
        }
    }

    /** Red-orange burst when a smelter overheats. */
    public void spawnSmelterOverheat(Location machineLoc) {
        if (!shouldSpawn()) return;
        Location top = machineLoc.clone().add(0.5, 1.0, 0.5);
        spawnDustBurst(top, ORANGE, 2.0f, 30, 1.0);
        World w = top.getWorld();
        if (w != null) {
            w.spawnParticle(Particle.LARGE_SMOKE, top, 10, 0.3, 0.5, 0.3, 0.05);
            w.spawnParticle(Particle.FLAME, top, 15, 0.5, 0.5, 0.5, 0.05);
        }
    }

    /** Amber energy swirl during a forge upgrade operation. */
    public void spawnForgeUpgrading(Location machineLoc) {
        if (!shouldSpawn()) return;
        spawnSpiral(machineLoc.clone().add(0.5, 0, 0.5), ORANGE, 1.5f, 8, 0.6, 1.5);
    }

    /** Violet-green burst on a successful forge upgrade. */
    public void spawnForgeUpgradeSuccess(Location machineLoc) {
        if (!shouldSpawn()) return;
        Location top = machineLoc.clone().add(0.5, 1.5, 0.5);
        spawnDustBurst(top, VIOLET, 2.0f, 25, 1.0);
        spawnDustBurst(top, GREEN, 1.5f, 15, 0.8);
        World w = top.getWorld();
        if (w != null) {
            w.spawnParticle(Particle.ENCHANT, top, 20, 0.5, 0.5, 0.5, 1.0);
        }
    }

    /** Red sparks on a failed forge upgrade. */
    public void spawnForgeUpgradeFail(Location machineLoc) {
        if (!shouldSpawn()) return;
        Location top = machineLoc.clone().add(0.5, 1.0, 0.5);
        World w = top.getWorld();
        if (w != null) {
            w.spawnParticle(Particle.CRIT, top, 20, 0.4, 0.4, 0.4, 0.1);
        }
    }

    // ── Weapon Effects ────────────────────────────────────────────────────────

    /** Green radiation burst when a plutonium weapon hits. */
    public void spawnWeaponRadiationHit(Location hitLoc) {
        if (!shouldSpawn()) return;
        spawnDustBurst(hitLoc, GREEN, 1.2f, 12, 0.4);
    }

    /** Violet shockwave ring on a Titan weapon AOE hit. */
    public void spawnTitanWeaponAoe(Location center) {
        if (!shouldSpawn()) return;
        spawnDustRing(center, VIOLET, 1.5f, 20, 3.0);
        spawnDustBurst(center, GREEN, 1.0f, 15, 1.5);
    }

    /** Radiation aura pulse around a player wearing a radiation aura. */
    public void spawnAuraPulse(Player player) {
        if (!shouldSpawn()) return;
        Location loc = player.getLocation().add(0, 1, 0);
        spawnDustRing(loc, GREEN, 1.0f, 16, 2.0);
    }

    // ── Armor Effects ─────────────────────────────────────────────────────────

    /** Subtle green ambient glow for plutonium armor wearers. */
    public void spawnPlutoniumArmorAmbient(Player player) {
        if (!shouldSpawn()) return;
        Location loc = player.getLocation().add(
                random(-0.3, 0.3), random(0.5, 1.8), random(-0.3, 0.3));
        spawnDust(loc, GREEN, 0.6f, 1);
    }

    /** Violet + gold orbiting shards for Titan armor wearers. */
    public void spawnTitanArmorAmbient(Player player) {
        if (!shouldSpawn()) return;
        Location loc = player.getLocation().add(0, 1.2, 0);
        spawnDustRing(loc, VIOLET, 1.0f, 8, 0.9);
    }

    /** Hazmat suit green barrier shimmer when blocking radiation. */
    public void spawnHazmatShield(Player player) {
        if (!shouldSpawn()) return;
        spawnDustRing(player.getLocation().add(0, 1, 0), CYAN, 0.8f, 12, 0.7);
    }

    // ── Boss Effects ──────────────────────────────────────────────────────────

    /** Massive green mushroom cloud for Titan spawn. */
    public void spawnTitanSpawn(Location titanLoc) {
        if (!shouldSpawn()) return;
        spawnDustBurst(titanLoc, GREEN, 2.0f, 80, 4.0);
        spawnColumn(titanLoc, Particle.WITCH, 60, 6.0, 12.0);
        World w = titanLoc.getWorld();
        if (w != null) {
            w.spawnParticle(Particle.EXPLOSION, titanLoc, 5, 2.0, 1.0, 2.0, 0);
        }
    }

    /** Expanding ring for each Titan phase transition. */
    public void spawnTitanPhaseChange(Location titanLoc) {
        if (!shouldSpawn()) return;
        for (int i = 1; i <= 3; i++) {
            spawnDustRing(titanLoc, VIOLET, 2.0f, 24, i * 2.0);
            spawnDustRing(titanLoc, GREEN, 1.5f, 16, i * 2.5);
        }
    }

    /** Continuous dripping radiation particles around the Titan entity. */
    public void spawnTitanAmbient(Location titanLoc) {
        if (!shouldSpawn()) return;
        World w = titanLoc.getWorld();
        if (w == null) return;
        w.spawnParticle(Particle.WITCH, titanLoc.clone().add(
                random(-2, 2), random(0, 4), random(-2, 2)), 3, 0, 0, 0, 0);
        spawnDust(titanLoc.clone().add(random(-1, 1), random(0, 3), random(-1, 1)),
                GREEN, 1.2f, 2);
    }

    /** Titan death implosion — converging violet particles. */
    public void spawnTitanDeath(Location titanLoc) {
        if (!shouldSpawn()) return;
        spawnDustBurst(titanLoc, VIOLET, 2.5f, 100, 6.0);
        spawnDustBurst(titanLoc, GREEN, 2.0f, 60, 4.0);
        World w = titanLoc.getWorld();
        if (w != null) {
            w.spawnParticle(Particle.EXPLOSION_EMITTER, titanLoc, 3, 2, 1, 2, 0);
        }
    }

    // ── Farming Effects ───────────────────────────────────────────────────────

    /** Green sparkle when a mutated crop grows. */
    public void spawnCropGrowth(Location cropLoc) {
        if (!shouldSpawn()) return;
        spawnDust(cropLoc.clone().add(0.5, 0.5, 0.5), GREEN, 0.8f, 6);
    }

    /** Toxic yellow puff when a Toxic Bloom spreads. */
    public void spawnToxicBloom(Location loc) {
        if (!shouldSpawn()) return;
        spawnDustBurst(loc.clone().add(0.5, 0.5, 0.5), YELLOW, 1.5f, 20, 1.5);
        World w = loc.getWorld();
        if (w != null) {
            w.spawnParticle(Particle.WITCH, loc.clone().add(0.5, 0.8, 0.5), 8, 0.5, 0.3, 0.5, 0);
        }
    }

    /** Green ambient glow on radioactive farmland. */
    public void spawnFarmlandAmbient(Location farmLoc) {
        if (!shouldSpawn()) return;
        Location top = farmLoc.clone().add(random(0, 1), 1.05, random(0, 1));
        spawnDust(top, GREEN, 0.5f, 1);
    }

    // ── Healing Effects ───────────────────────────────────────────────────────

    /** Cyan + blue spiral when a player drinks an antidote. */
    public void spawnAntidoteEffect(Player player) {
        if (!shouldSpawn()) return;
        spawnSpiral(player.getLocation(), CYAN, 1.2f, 16, 0.5, 2.0);
        World w = player.getWorld();
        w.spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0),
                10, 0.4, 0.5, 0.4, 0);
    }

    /** Violet + cyan spiral for the serum — more intense than antidote. */
    public void spawnSerumEffect(Player player) {
        if (!shouldSpawn()) return;
        spawnSpiral(player.getLocation(), VIOLET, 1.5f, 24, 0.5, 2.5);
        spawnSpiral(player.getLocation(), CYAN, 1.0f, 16, 0.4, 2.0);
        World w = player.getWorld();
        w.spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0),
                20, 0.4, 0.5, 0.4, 1.5);
    }

    // ── Upgrade Effects ───────────────────────────────────────────────────────

    /** Energy swirl during a forge upgrade operation. */
    public void spawnUpgradeCharging(Location forgeLoc) {
        if (!shouldSpawn()) return;
        spawnSpiral(forgeLoc.clone().add(0.5, 0, 0.5), ORANGE, 1.2f, 12, 0.5, 1.2);
    }

    /** Burst on a successful upgrade. */
    public void spawnUpgradeSuccess(Player player) {
        if (!shouldSpawn()) return;
        spawnDustBurst(player.getLocation().add(0, 1, 0), GREEN, 1.5f, 30, 1.2);
        spawnDustBurst(player.getLocation().add(0, 1, 0), VIOLET, 1.0f, 15, 0.8);
        player.getWorld().spawnParticle(Particle.ENCHANT,
                player.getLocation().add(0, 1.5, 0), 25, 0.5, 0.5, 0.5, 2.0);
    }

    /** Red sparks on upgrade failure. */
    public void spawnUpgradeFail(Player player) {
        if (!shouldSpawn()) return;
        player.getWorld().spawnParticle(Particle.CRIT,
                player.getLocation().add(0, 1, 0), 20, 0.4, 0.4, 0.4, 0.1);
    }

    // ── Internal primitives ───────────────────────────────────────────────────

    private void spawnDust(Location loc, Color color, float size, int count) {
        World w = loc.getWorld();
        if (w == null) return;
        Particle.DustOptions opts = new Particle.DustOptions(color, size);
        w.spawnParticle(Particle.DUST, loc, count, 0.1, 0.1, 0.1, 0, opts);
    }

    private void spawnDustBurst(Location loc, Color color, float size, int count, double spread) {
        World w = loc.getWorld();
        if (w == null) return;
        Particle.DustOptions opts = new Particle.DustOptions(color, size);
        w.spawnParticle(Particle.DUST, loc, count, spread, spread, spread, 0, opts);
    }

    private void spawnDustRing(Location center, Color color, float size, int points, double radius) {
        World w = center.getWorld();
        if (w == null) return;
        Particle.DustOptions opts = new Particle.DustOptions(color, size);
        double step = (2 * Math.PI) / points;
        for (int i = 0; i < points; i++) {
            double angle = step * i;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            w.spawnParticle(Particle.DUST, center.clone().add(x, 0, z), 1, 0, 0, 0, 0, opts);
        }
    }

    private void spawnSpiral(Location base, Color color, float size, int steps, double radiusPerStep, double height) {
        World w = base.getWorld();
        if (w == null) return;
        Particle.DustOptions opts = new Particle.DustOptions(color, size);
        for (int i = 0; i < steps; i++) {
            double t = (double) i / steps;
            double angle = t * 4 * Math.PI;
            double r = t * radiusPerStep;
            double y = t * height;
            double x = Math.cos(angle) * r;
            double z = Math.sin(angle) * r;
            w.spawnParticle(Particle.DUST, base.clone().add(x, y, z), 1, 0, 0, 0, 0, opts);
        }
    }

    private void spawnColumn(Location base, Particle particle, int count, double spread, double height) {
        World w = base.getWorld();
        if (w == null) return;
        w.spawnParticle(particle, base, count, spread, height, spread, 0);
    }

    /** Returns false when the performance manager signals high server load. */
    private boolean shouldSpawn() {
        if (performanceManager != null) {
            return performanceManager.allowParticles();
        }
        return true;
    }

    private double random(double min, double max) {
        return min + ThreadLocalRandom.current().nextDouble() * (max - min);
    }
}
