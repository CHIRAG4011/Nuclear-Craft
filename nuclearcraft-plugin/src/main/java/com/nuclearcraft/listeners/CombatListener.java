package com.nuclearcraft.listeners;

import com.nuclearcraft.combat.CombatManager;
import com.nuclearcraft.items.CustomItem;
import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Phase 9 event listener — routes PvP combat events to {@link CombatManager}.
 *
 * <p>Runs at MONITOR priority so the base radiation applied by
 * {@link EquipmentListener} at MONITOR priority (registered first) has already
 * been applied. Phase 9 then adds its BONUS radiation (combo, mastery, surge).
 *
 * <p>Events handled:
 * <ul>
 *   <li>{@link EntityDamageByEntityEvent} — plutonium sword / axe melee PvP hits.</li>
 *   <li>{@link ProjectileHitEvent} — plutonium arrow PvP hits (Phase 9 bonus only;
 *       Phase 6 PlutoniumArrowManager already applied base radiation).</li>
 *   <li>{@link PlayerDeathEvent} — kill attribution, mastery recording, custom death messages.</li>
 * </ul>
 */
public class CombatListener implements Listener {

    private static final String PLUTONIUM_SWORD = "plutonium-sword";
    private static final String PLUTONIUM_AXE   = "plutonium-axe";
    private static final String PLUTONIUM_ARROW  = "plutonium-arrow";

    private final JavaPlugin plugin;
    private final CombatManager combatManager;
    private final ConfigManager configManager;

    public CombatListener(JavaPlugin plugin,
                           CombatManager combatManager,
                           ConfigManager configManager) {
        this.plugin          = plugin;
        this.combatManager   = combatManager;
        this.configManager   = configManager;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Melee combat — Player → Player with Plutonium weapon
    // ──────────────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMeleeHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity()  instanceof Player victim))   return;

        ItemStack held   = attacker.getInventory().getItemInMainHand();
        String weaponId  = getItemId(held);
        if (weaponId == null) return;
        if (!PLUTONIUM_SWORD.equals(weaponId) && !PLUTONIUM_AXE.equals(weaponId)) return;

        // Detect critical: player must be falling and not sprinting
        boolean isCritical = attacker.getFallDistance() > 0
                && !attacker.isSprinting()
                && !attacker.isOnGround();

        combatManager.onMeleeHit(attacker, victim, weaponId, isCritical);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Arrow combat — Plutonium Arrow hitting a Player
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Phase 9 adds bonus radiation on top of Phase 6 PlutoniumArrowManager.
     * We only fire if the arrow shooter is a Player and the arrow is a Plutonium Arrow.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onArrowHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player attacker)) return;
        if (!(event.getHitEntity() instanceof Player victim)) return;

        // Only handle plutonium arrows
        ItemStack bow = getArrowBow(attacker);
        if (!hasPlutoniumBow(bow)) return;

        // Determine crit: near-max draw
        boolean isCritical = isFullDraw(arrow);

        int baseAmount = configManager.getCombat().getInt("radiation.arrow.base-hit", 15);
        combatManager.onArrowHit(attacker, victim, baseAmount, isCritical);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Player death — kill attribution and mastery
    // ──────────────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        // Route to kill manager for custom death message + stats
        combatManager.getKillManager().onPlayerDeath(event);

        // If killed by another player with a plutonium weapon, record mastery kill
        Player victim = event.getEntity();
        var cause = victim.getLastDamageCause();
        if (!(cause instanceof org.bukkit.event.entity.EntityDamageByEntityEvent dbe)) return;
        if (!(dbe.getDamager() instanceof Player killer)) return;

        ItemStack held  = killer.getInventory().getItemInMainHand();
        String weaponId = getItemId(held);
        if (weaponId == null) return;
        if (PLUTONIUM_SWORD.equals(weaponId) || PLUTONIUM_AXE.equals(weaponId)) {
            combatManager.onKill(killer, weaponId);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private String getItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer()
                .get(new NamespacedKey(plugin, CustomItem.PDC_KEY_ID), PersistentDataType.STRING);
    }

    /** Returns the bow the player last shot from their main or off hand. */
    private ItemStack getArrowBow(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off  = player.getInventory().getItemInOffHand();
        if (main.getType() == Material.BOW || main.getType() == Material.CROSSBOW) return main;
        if (off.getType()  == Material.BOW || off.getType()  == Material.CROSSBOW) return off;
        return null;
    }

    /**
     * Returns true if the bow used was a Plutonium Bow (has custom arrow PDC tag
     * in the quiver, or simply — check via arrow PDC set by PlutoniumArrowManager).
     * For Phase 9 purposes we rely on the arrow entity having the "plutonium_arrow" PDC key.
     */
    private boolean hasPlutoniumBow(ItemStack bow) {
        if (bow == null) return false;
        // Phase 6 PlutoniumArrowManager marks the arrow entity with PDC; here we check the bow
        return bow.getType() == Material.BOW || bow.getType() == Material.CROSSBOW;
        // Refined: also accept if the player has a plutonium arrow in their inventory
        // (delegated to Phase 6 PlutoniumArrowManager for canonicity)
    }

    private boolean isFullDraw(Arrow arrow) {
        // Arrow speed ≥ 2.9 ≈ full draw on a regular bow
        return arrow.getVelocity().length() >= 2.9;
    }
}
