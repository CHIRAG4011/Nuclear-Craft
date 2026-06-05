package com.nuclearcraft.zombies;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.radiation.RadiationManager;
import com.nuclearcraft.radiation.RadiationSource;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;

/**
 * Handles combat interactions involving Irradiated Zombies.
 *
 * On every hit against a Player:
 *   1. Looks up radiation power from the zombie's PDC.
 *   2. Applies radiation via RadiationManager (fires RadiationGainEvent).
 *   3. During a Radiation Surge, radiation damage is doubled.
 *
 * Called from ZombieSpawnListener — no tick loops.
 */
public class ZombieCombatManager {

    private final NuclearCraftPlugin plugin;
    private final ConfigManager configManager;
    private final IrradiatedZombieManager zombieManager;
    private final RadiationManager radiationManager;
    private final ZombieSpawnManager spawnManager;

    public ZombieCombatManager(NuclearCraftPlugin plugin, ConfigManager configManager,
                                IrradiatedZombieManager zombieManager,
                                RadiationManager radiationManager,
                                ZombieSpawnManager spawnManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.zombieManager = zombieManager;
        this.radiationManager = radiationManager;
        this.spawnManager = spawnManager;
    }

    public void initialize() {
        NCLogger.info("ZombieCombatManager initialized.");
    }

    public void shutdown() {}

    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Called when any entity damages a player. Checks if the attacker is an
     * Irradiated Zombie and applies radiation accordingly.
     *
     * @param attacker the entity dealing damage
     * @param victim   the player receiving damage
     */
    public void handleHit(Entity attacker, Player victim) {
        if (!(attacker instanceof Zombie z)) return;
        if (!zombieManager.isIrradiated(attacker)) return;

        int radiationPower = IrradiatedZombie.getRadiationPower(z);
        if (radiationPower <= 0) return;

        // Double during Radiation Surge
        if (spawnManager.isSurgeActive()) {
            boolean surgeMult = configManager.getZombies().getBoolean("night-event.double-radiation", true);
            if (surgeMult) radiationPower *= 2;
        }

        radiationManager.addRadiation(victim, radiationPower, RadiationSource.IRRADIATED_ZOMBIE);

        NCLogger.debug("ZombieCombat: %s hit %s for %d radiation",
                z.getType().name(), victim.getName(), radiationPower);
    }
}
