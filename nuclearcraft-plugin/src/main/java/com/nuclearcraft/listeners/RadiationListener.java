package com.nuclearcraft.listeners;

import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.data.PlayerData;
import com.nuclearcraft.events.RadiationDeathEvent;
import com.nuclearcraft.radiation.ContagionManager;
import com.nuclearcraft.radiation.RadiationManager;
import com.nuclearcraft.utils.ColorUtil;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Event listener for the radiation and contagion systems.
 *
 * Handles:
 *   - Physical-contact radiation spread on player-vs-player hits
 *   - Radiation death detection (custom damage source tracking)
 *   - Custom death message broadcast
 *   - RadiationDeathEvent dispatch
 */
public class RadiationListener implements Listener {

    /**
     * UUIDs of players currently receiving radiation-sourced damage.
     * Used to attribute radiation deaths in PlayerDeathEvent.
     */
    private final Set<UUID> radiationDamageInProgress =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final NuclearCraftPlugin plugin;
    private final RadiationManager radiationManager;
    private final ContagionManager contagionManager;

    public RadiationListener(NuclearCraftPlugin plugin,
                              RadiationManager radiationManager,
                              ContagionManager contagionManager) {
        this.plugin = plugin;
        this.radiationManager = radiationManager;
        this.contagionManager = contagionManager;
    }

    /**
     * Tracks players being damaged by our radiation tasks.
     * Call before player.damage() and clear immediately after.
     */
    public void markRadiationDamage(UUID uuid) {
        radiationDamageInProgress.add(uuid);
        // Clear after a short window so we don't permanently dirty the set
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> radiationDamageInProgress.remove(uuid), 1L);
    }

    // ──────────────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity victim = event.getEntity();

        if (!(damager instanceof Player attacker)) return;
        if (!(victim instanceof Player target)) return;

        // Physical contact spread (35% chance, handled in ContagionManager)
        contagionManager.handlePhysicalContact(attacker, target);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Check if the player's health dropped to zero from radiation damage
        boolean radiationDeath = radiationDamageInProgress.contains(uuid)
                || isDeathFromRadiation(player, event);

        if (!radiationDeath) return;

        int stage = radiationManager.getStage(player);
        double radiation = radiationManager.getRadiation(player);

        // Fire RadiationDeathEvent
        RadiationDeathEvent deathEvent = new RadiationDeathEvent(player, radiation, stage);
        plugin.getServer().getPluginManager().callEvent(deathEvent);

        // Custom death message
        String deathMsg = plugin.getConfigManager().getRadiation()
                .getString("death.message", "{player} succumbed to radiation poisoning.")
                .replace("{player}", player.getName());
        event.deathMessage(ColorUtil.parse("<dark_green>" + deathMsg + "</dark_green>"));

        // Track stat
        plugin.getPlayerDataManager().get(uuid).ifPresent(data -> {
            data.setRadiationDeaths(data.getRadiationDeaths() + 1);
            NCLogger.debug("Radiation death #%d for %s", data.getRadiationDeaths(), player.getName());
        });

        radiationDamageInProgress.remove(uuid);
    }

    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Heuristic: if the player had stage >= 3 and the death cause is CUSTOM or MAGIC,
     * it was likely from our radiation damage call.
     */
    private boolean isDeathFromRadiation(Player player, PlayerDeathEvent event) {
        if (radiationManager.getStage(player) < 1) return false;
        var cause = player.getLastDamageCause();
        if (cause == null) return false;
        return cause.getCause() == EntityDamageEvent.DamageCause.CUSTOM
                || cause.getCause() == EntityDamageEvent.DamageCause.MAGIC;
    }
}
