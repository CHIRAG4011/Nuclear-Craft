package com.nuclearcraft.listeners;

import com.nuclearcraft.boss.TitanManager;
import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Giant;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Bukkit event listener for the Plutonium Titan.
 *
 * Handles:
 *  - Block placement to detect altar completion (summoning ritual trigger)
 *  - Damage to/from the Titan entity
 *  - Titan death
 *  - Periodic boss-bar / phase updates
 */
public class TitanListener implements Listener {

    private final JavaPlugin   plugin;
    private final TitanManager titanManager;
    private final ItemManager  itemManager;

    private BukkitTask phaseTick;

    public TitanListener(JavaPlugin plugin, TitanManager titanManager, ItemManager itemManager) {
        this.plugin       = plugin;
        this.titanManager = titanManager;
        this.itemManager  = itemManager;
        startPhaseTick();
    }

    // ── Boss-bar / phase update ───────────────────────────────────────────────

    private void startPhaseTick() {
        phaseTick = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (titanManager.isTitanAlive()) titanManager.tickBossBar();
        }, 20L, 5L);
    }

    public void shutdown() {
        if (phaseTick != null) { phaseTick.cancel(); phaseTick = null; }
    }

    // ── Damage events ─────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onTitanDamaged(EntityDamageByEntityEvent event) {
        Entity damaged  = event.getEntity();
        Entity attacker = event.getDamager();

        if (!(damaged instanceof Giant giant)) return;
        if (!titanManager.isTitan(giant)) return;
        if (!(attacker instanceof Player player)) return;

        double finalDamage = event.getFinalDamage();
        titanManager.handleDamage(giant, player, finalDamage);

        // Add player to boss bar when they first attack
        titanManager.getPhaseManager().addPlayerToBossBar(player);
        NCLogger.debug("Player " + player.getName() + " dealt " + finalDamage + " to Titan.");
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onTitanAttackPlayer(EntityDamageByEntityEvent event) {
        Entity damaged  = event.getEntity();
        Entity attacker = event.getDamager();

        if (!(attacker instanceof Giant giant)) return;
        if (!titanManager.isTitan(giant)) return;
        if (!(damaged instanceof Player player)) return;

        // TitanCombatManager handles melee directly; this catches any vanilla damage
        NCLogger.debug("Titan dealt damage to " + player.getName());
    }

    // ── Titan death ───────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTitanDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Giant giant)) return;
        if (!titanManager.isTitan(giant)) return;

        // Suppress vanilla drops — rewards handled by TitanRewardManager
        event.getDrops().clear();
        event.setDroppedExp(0);

        Player killer = giant.getKiller();
        titanManager.handleDeath(giant, killer);
        NCLogger.info("Titan EntityDeathEvent fired. Killer: "
                + (killer != null ? killer.getName() : "none"));
    }

    // ── Summoning ritual trigger ──────────────────────────────────────────────

    /**
     * Detects when a player places an Irradiated Heart on top of a
     * Plutonium Block (CRYING_OBSIDIAN) to begin the summoning ritual.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player     = event.getPlayer();
        Block  placed     = event.getBlockPlaced();
        Block  against    = event.getBlockAgainst();
        ItemStack inHand  = event.getItemInHand();

        // Must be placing an Irradiated Heart item (represented as a block via custom item)
        if (inHand == null) return;
        if (!isIrradiatedHeart(inHand)) return;
        if (against.getType() != Material.CRYING_OBSIDIAN) return;

        // Cancel the block placement itself — we just use it as the trigger event
        event.setCancelled(true);

        // Validate altar and start ritual
        titanManager.getSummoningManager().tryTriggerRitual(player, against);
    }

    private boolean isIrradiatedHeart(ItemStack item) {
        return itemManager.getItem("irradiated-heart")
                .map(ci -> item.isSimilar(ci.build()))
                .orElse(false);
    }
}
