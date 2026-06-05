package com.nuclearcraft.zombies;

import com.nuclearcraft.utils.NCLogger;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Zombie;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks every living Irradiated Zombie on the server.
 *
 * UUID → IrradiatedZombie map kept in memory for O(1) lookup.
 * References are cleaned on entity death (called from listener).
 * The map is never iterated on every tick — only on specific events.
 */
public class IrradiatedZombieManager {

    private final Map<UUID, IrradiatedZombie> activeZombies = new ConcurrentHashMap<>();

    // Running counters for /nc zombie stats
    private volatile long totalSpawned = 0;
    private volatile long totalAlphaSpawned = 0;

    public IrradiatedZombieManager() {}

    public void initialize() {
        NCLogger.info("IrradiatedZombieManager initialized.");
    }

    public void shutdown() {
        activeZombies.clear();
        NCLogger.info("IrradiatedZombieManager shut down — zombie cache cleared.");
    }

    // ──────────────────────────────────────────────────────────────────────────

    /** Registers a newly created irradiated zombie. */
    public void register(IrradiatedZombie iz) {
        activeZombies.put(iz.getEntity().getUniqueId(), iz);
        totalSpawned++;
        if (iz.isAlphaZombie()) totalAlphaSpawned++;
        NCLogger.debug("Registered irradiated zombie [L%d] UUID=%s", iz.getZombieLevel().getLevel(),
                iz.getEntity().getUniqueId());
    }

    /** Removes a zombie from tracking (on death or chunk-based cleanup). */
    public void unregister(UUID uuid) {
        activeZombies.remove(uuid);
    }

    /** Returns the tracked IrradiatedZombie for the given entity, or empty. */
    public Optional<IrradiatedZombie> get(UUID uuid) {
        return Optional.ofNullable(activeZombies.get(uuid));
    }

    /** Returns the tracked IrradiatedZombie for a given entity, or empty. */
    public Optional<IrradiatedZombie> get(Entity entity) {
        if (!(entity instanceof Zombie z)) return Optional.empty();
        // Fast path — already tracked
        IrradiatedZombie iz = activeZombies.get(entity.getUniqueId());
        if (iz != null) return Optional.of(iz);
        // Fallback — check PDC for zombies that were missed (e.g., loaded from disk)
        if (IrradiatedZombie.isIrradiated(z)) {
            IrradiatedZombie restored = IrradiatedZombie.fromExisting(z);
            if (restored != null) {
                activeZombies.put(z.getUniqueId(), restored);
                return Optional.of(restored);
            }
        }
        return Optional.empty();
    }

    /** Returns true if this entity is a tracked (or PDC-tagged) irradiated zombie. */
    public boolean isIrradiated(Entity entity) {
        if (!(entity instanceof Zombie z)) return false;
        return activeZombies.containsKey(entity.getUniqueId()) || IrradiatedZombie.isIrradiated(z);
    }

    /** Returns an unmodifiable view of all active zombies. */
    public Collection<IrradiatedZombie> getAll() {
        return Collections.unmodifiableCollection(activeZombies.values());
    }

    /** Removes all dead or invalid entries. Call periodically to avoid stale refs. */
    public void purgeInvalid() {
        activeZombies.entrySet().removeIf(e -> !e.getValue().isAlive());
    }

    public int getActiveCount()      { return activeZombies.size(); }
    public long getTotalSpawned()    { return totalSpawned; }
    public long getTotalAlphaSpawned() { return totalAlphaSpawned; }

    public int getActiveAlphaCount() {
        return (int) activeZombies.values().stream().filter(IrradiatedZombie::isAlphaZombie).count();
    }
}
