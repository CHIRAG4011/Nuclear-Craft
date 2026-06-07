package com.nuclearcraft.listeners;

import com.nuclearcraft.titantech.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Handles all Titan item interaction events:
 *  - Weapon on-hit effects (sword, axe)
 *  - Titan Bow shoot & Titan Arrow impact
 *  - Titan Armor per-tick effects + set-bonus reflection
 *  - Anvil repair with Titan materials
 *  - Player join/quit for armor effect cleanup
 */
public class TitanItemListener implements Listener {

    private final TitanWeaponManager weaponManager;
    private final TitanArmorManager armorManager;
    private final SetBonusManager setBonusManager;
    private final TitanArrowManager arrowManager;
    private final TitanRepairManager repairManager;

    public TitanItemListener(TitanTechManager titan, Plugin plugin) {
        this.weaponManager  = titan.getWeaponManager();
        this.armorManager   = titan.getArmorManager();
        this.setBonusManager = titan.getSetBonusManager();
        this.arrowManager   = titan.getArrowManager();
        this.repairManager  = titan.getRepairManager();
    }

    // ── Weapon hit ────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity victim  = event.getEntity();

        Player attacker = null;
        if (damager instanceof Player p) attacker = p;
        else if (damager instanceof Projectile proj && proj.getShooter() instanceof Player p) attacker = p;
        if (attacker == null) return;

        if (victim instanceof Player defPlayer) {
            setBonusManager.tryReflect(defPlayer, attacker, 30);
        }

        ItemStack mainHand = attacker.getInventory().getItemInMainHand();
        if (weaponManager.isAnyTitanWeapon(mainHand)) {
            weaponManager.onHit(attacker, victim, mainHand);
        }
    }

    // ── Titan Bow shoot ───────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player shooter)) return;
        if (!arrowManager.isTitanBow(event.getBow())) return;
        if (!(event.getProjectile() instanceof Arrow arrow)) return;
        arrowManager.onTitanBowShoot(shooter, arrow);
    }

    // ── Titan Arrow hit ───────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!arrowManager.isTitanArrow(arrow)) return;
        Entity hitEntity = event.getHitEntity();
        arrowManager.onArrowHit(arrow, hitEntity, arrow.getLocation());
    }

    // ── Anvil repair ──────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onAnvilPrepare(PrepareAnvilEvent event) {
        repairManager.handleAnvilRepair(event);
    }

    // ── Player join/quit ──────────────────────────────────────────────────────

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        armorManager.applyEffects(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        armorManager.onPlayerQuit(player);
        setBonusManager.onPlayerQuit(player);
    }
}
