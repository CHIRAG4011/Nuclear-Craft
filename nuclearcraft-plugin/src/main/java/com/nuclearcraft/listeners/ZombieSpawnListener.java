package com.nuclearcraft.listeners;

import com.nuclearcraft.advancements.AdvancementManager;
import com.nuclearcraft.data.PlayerData;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.zombies.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Central event listener for Phase 3 — Irradiated Zombie System.
 *
 * Routes events to the appropriate Phase 3 managers.
 * No game logic lives here — this class is pure delegation.
 *
 * Handled events:
 *   CreatureSpawnEvent          → ZombieSpawnManager (natural zombie conversion)
 *   EntityDeathEvent            → ZombieLootManager, RadiationCloudManager,
 *                                  IrradiatedZombieManager (unregister), stats, advancements
 *   EntityDamageByEntityEvent   → ZombieCombatManager (radiation on hit)
 *   PlayerJoinEvent             → RadiationNightManager (bossbar sync)
 *   PlayerQuitEvent             → RadiationNightManager (bossbar cleanup)
 */
public class ZombieSpawnListener implements Listener {

    private final ZombieSpawnManager spawnManager;
    private final ZombieCombatManager combatManager;
    private final ZombieLootManager lootManager;
    private final RadiationCloudManager cloudManager;
    private final RadiationNightManager nightManager;
    private final IrradiatedZombieManager zombieManager;
    private final PlayerDataManager playerDataManager;
    private final AdvancementManager advancementManager;

    public ZombieSpawnListener(ZombieSpawnManager spawnManager,
                                ZombieCombatManager combatManager,
                                ZombieLootManager lootManager,
                                RadiationCloudManager cloudManager,
                                RadiationNightManager nightManager,
                                IrradiatedZombieManager zombieManager,
                                PlayerDataManager playerDataManager,
                                AdvancementManager advancementManager) {
        this.spawnManager = spawnManager;
        this.combatManager = combatManager;
        this.lootManager = lootManager;
        this.cloudManager = cloudManager;
        this.nightManager = nightManager;
        this.zombieManager = zombieManager;
        this.playerDataManager = playerDataManager;
        this.advancementManager = advancementManager;
    }

    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Converts qualifying natural zombie spawns into irradiated zombies.
     * HIGH priority so we run after most spawn-protection plugins.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        // Exclude baby zombies (configurable — spec doesn't restrict, but keeps lore cleaner)
        if (zombie.isBaby()) return;
        spawnManager.handleSpawn(zombie, event.getSpawnReason());
    }

    /**
     * Handles irradiated zombie death:
     *   - Custom loot (ZombieLootManager)
     *   - Radiation cloud spawn (RadiationCloudManager)
     *   - Unregister from tracking (IrradiatedZombieManager)
     *   - Player stat increments + advancement grants
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (!zombieManager.isIrradiated(zombie)) return;

        // Retrieve level info before unregistering
        var opt = zombieManager.get(zombie);
        ZombieLevel level = opt.map(IrradiatedZombie::getZombieLevel).orElse(ZombieLevel.LEVEL_1);
        boolean isAlpha = opt.map(IrradiatedZombie::isAlphaZombie).orElse(false);

        // Custom loot — clears vanilla drops and sets custom ones
        lootManager.handleDeath(event);

        // Radiation cloud
        cloudManager.handleZombieDeath(zombie);

        // Remove from active tracking
        zombieManager.unregister(zombie.getUniqueId());

        // Player kill stats and advancements
        Player killer = zombie.getKiller();
        if (killer != null) {
            updatePlayerKillStats(killer, level, isAlpha);
        }
    }

    /**
     * Applies radiation when an irradiated zombie hits a player.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        combatManager.handleHit(event.getDamager(), victim);
    }

    /**
     * Adds bossbar to a joining player if a Radiation Surge is active.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        nightManager.onPlayerJoin(event.getPlayer());
    }

    /**
     * Cleans up bossbar when a player disconnects.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        nightManager.onPlayerQuit(event.getPlayer());
    }

    // ──────────────────────────────────────────────────────────────────────────

    private void updatePlayerKillStats(Player killer, ZombieLevel level, boolean isAlpha) {
        PlayerData data = playerDataManager.get(killer.getUniqueId()).orElse(null);
        if (data == null) return;

        boolean firstKill = data.getIrradiatedZombiesKilled() == 0;

        data.incrementIrradiatedZombiesKilled();
        if (isAlpha) data.incrementAlphaZombiesKilled();
        // Data is marked dirty by the increment methods; TaskManager will flush on its schedule.

        // Advancements
        if (firstKill) {
            advancementManager.award(killer, AdvancementManager.Advancement.MUTANT_HUNTER);
        }
        if (isAlpha) {
            advancementManager.award(killer, AdvancementManager.Advancement.ALPHA_SLAYER);
        }
    }
}
