package com.nuclearcraft.zombies;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.utils.NCLogger;
import com.nuclearcraft.utils.RandomUtil;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

import java.util.Set;

/**
 * Decides whether a naturally spawning zombie becomes an Irradiated Zombie.
 *
 * Decision matrix:
 *   Normal mode:  60% chance on qualifying spawn reasons.
 *   Surge mode:   100% chance on all qualifying spawn reasons.
 *
 * Qualifying reasons (all others are skipped):
 *   NATURAL, JOCKEY, MOUNT, REINFORCEMENTS, VILLAGE_DEFENSE, CHUNK_GEN
 *
 * Non-qualifying (always skipped):
 *   SPAWNER (configurable), SPAWN_EGG, DISPENSE_EGG, CUSTOM, BREEDING,
 *   COMMAND, DEFAULT, INFECTION (zombie villager conversion)
 */
public class ZombieSpawnManager {

    private static final Set<SpawnReason> ELIGIBLE_REASONS = Set.of(
            SpawnReason.NATURAL,
            SpawnReason.JOCKEY,
            SpawnReason.MOUNT,
            SpawnReason.REINFORCEMENTS,
            SpawnReason.VILLAGE_DEFENSE,
            SpawnReason.CHUNK_GEN
    );

    private final NuclearCraftPlugin plugin;
    private final ConfigManager configManager;
    private final IrradiatedZombieManager zombieManager;

    /** True during a Radiation Surge — 100% of qualifying spawns become irradiated. */
    private boolean surgeActive = false;

    public ZombieSpawnManager(NuclearCraftPlugin plugin, ConfigManager configManager,
                               IrradiatedZombieManager zombieManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.zombieManager = zombieManager;
    }

    public void initialize() {
        IrradiatedZombie.initKeys(plugin);
        NCLogger.info("ZombieSpawnManager initialized.");
    }

    public void shutdown() {
        NCLogger.info("ZombieSpawnManager shut down.");
    }

    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Called from {@link com.nuclearcraft.listeners.ZombieSpawnListener} on CreatureSpawnEvent.
     * Returns the new IrradiatedZombie if conversion occurred, or null if skipped.
     */
    public IrradiatedZombie handleSpawn(Zombie zombie, SpawnReason reason) {
        if (!isEligible(reason)) return null;
        if (!shouldConvert()) return null;

        ZombieLevel level = rollLevel();
        IrradiatedZombie iz = IrradiatedZombie.create(plugin, zombie, level, surgeActive);
        zombieManager.register(iz);

        NCLogger.debug("Converted zombie → IrradiatedZombie [L%d, %s] at %s",
                level.getLevel(), iz.getVariant(), zombie.getLocation().toVector());
        return iz;
    }

    /**
     * Spawns an irradiated zombie at a location programmatically (admin command).
     */
    public IrradiatedZombie spawnAt(org.bukkit.Location location, ZombieLevel level) {
        Zombie zombie = location.getWorld().spawn(location, Zombie.class, z -> {
            // Applied before the entity is added to the world
        });
        IrradiatedZombie iz = IrradiatedZombie.create(plugin, zombie, level, false);
        zombieManager.register(iz);
        return iz;
    }

    private boolean isEligible(SpawnReason reason) {
        if (ELIGIBLE_REASONS.contains(reason)) return true;
        // Spawner eligibility from config
        if (reason == SpawnReason.SPAWNER) {
            return configManager.getZombies().getBoolean("spawn.allow-spawner-conversion", false);
        }
        return false;
    }

    private boolean shouldConvert() {
        if (surgeActive) return true;
        double chance = configManager.getZombies().getDouble("spawn.irradiated-chance", 0.60);
        return RandomUtil.chance(chance);
    }

    private ZombieLevel rollLevel() {
        // During surge: boost level probabilities slightly
        if (surgeActive) {
            double roll = RandomUtil.nextDouble(0, 1);
            if (roll < 0.05) return ZombieLevel.LEVEL_4;
            if (roll < 0.25) return ZombieLevel.LEVEL_3;
            if (roll < 0.50) return ZombieLevel.LEVEL_2;
            return ZombieLevel.LEVEL_1;
        }
        return ZombieLevel.rollLevel();
    }

    public void setSurgeActive(boolean active) {
        this.surgeActive = active;
        NCLogger.info("Radiation Surge: " + (active ? "ACTIVE" : "ENDED"));
    }

    public boolean isSurgeActive() { return surgeActive; }
}
