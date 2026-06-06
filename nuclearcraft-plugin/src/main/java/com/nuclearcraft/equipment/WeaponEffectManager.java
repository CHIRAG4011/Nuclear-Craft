package com.nuclearcraft.equipment;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.data.PlayerData;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.items.CustomItem;
import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.radiation.RadiationManager;
import com.nuclearcraft.radiation.RadiationSource;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.Random;

/**
 * Handles all weapon combat effects for Plutonium equipment.
 *
 * <h3>Plutonium Sword</h3>
 * <ul>
 *   <li>Every hit: applies configurable radiation to the target (if player)</li>
 *   <li>Critical hit (fall velocity detected): extra radiation burst + green explosion particles</li>
 * </ul>
 *
 * <h3>Plutonium Axe</h3>
 * <ul>
 *   <li>Configurable shockwave chance: AoE radiation + knockback on nearby entities</li>
 * </ul>
 */
public class WeaponEffectManager {

    private static final Random RANDOM = new Random();

    private final JavaPlugin plugin;
    private final ItemManager itemManager;
    private final ConfigManager configManager;
    private final RadiationManager radiationManager;
    private final PlayerDataManager playerDataManager;

    public WeaponEffectManager(JavaPlugin plugin, ItemManager itemManager,
                               ConfigManager configManager,
                               RadiationManager radiationManager,
                               PlayerDataManager playerDataManager) {
        this.plugin            = plugin;
        this.itemManager       = itemManager;
        this.configManager     = configManager;
        this.radiationManager  = radiationManager;
        this.playerDataManager = playerDataManager;
    }

    public void initialize() {
        NCLogger.info("WeaponEffectManager initialized.");
    }

    public void shutdown() {}

    // ──────────────────────────────────────────────────────────────────────────
    // Entry points called from EquipmentListener
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Called when any entity is damaged by a player.
     * Dispatches sword and axe logic based on the held item.
     */
    public void onEntityHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;

        ItemStack held = attacker.getInventory().getItemInMainHand();
        String id = getItemId(held);
        if (id == null) return;

        switch (id) {
            case "plutonium-sword" -> handleSwordHit(attacker, event);
            case "plutonium-axe"   -> handleAxeHit(attacker, event);
            default -> {}
        }
    }

    /** Reads the NuclearCraft item ID from an ItemStack's PDC. Returns null if not a NC item. */
    private String getItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer()
                .get(new NamespacedKey(plugin, CustomItem.PDC_KEY_ID), PersistentDataType.STRING);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Sword logic
    // ──────────────────────────────────────────────────────────────────────────

    private void handleSwordHit(Player attacker, EntityDamageByEntityEvent event) {
        var cfg = configManager.getEquipment();
        int radOnHit  = cfg.getInt("tools.plutonium-sword.radiation-on-hit", 10);
        int critBonus = cfg.getInt("tools.plutonium-sword.critical-radiation-bonus", 20);

        Entity victim = event.getEntity();
        boolean isCrit = isCriticalHit(attacker);

        // Base radiation strike
        if (victim instanceof Player target) {
            radiationManager.addRadiation(target, radOnHit, RadiationSource.PLUTONIUM_WEAPON);
        }

        // Radiation particles at victim location
        spawnSwordHitParticles(victim.getLocation());

        // Critical hit bonus
        if (isCrit) {
            if (victim instanceof Player target) {
                radiationManager.addRadiation(target, critBonus, RadiationSource.PLUTONIUM_WEAPON);
            }
            spawnCritParticles(victim.getLocation());
            victim.getWorld().playSound(victim.getLocation(),
                    Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 1.8f);
        }

        // Track stats for attacker
        playerDataManager.get(attacker.getUniqueId()).ifPresent(data -> {
            data.setSwordHits(data.getSwordHits() + 1);
            int totalRad = isCrit ? radOnHit + critBonus : radOnHit;
            data.setRadiationDamageInflicted(data.getRadiationDamageInflicted() + totalRad);
        });
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Axe logic
    // ──────────────────────────────────────────────────────────────────────────

    private void handleAxeHit(Player attacker, EntityDamageByEntityEvent event) {
        var cfg = configManager.getEquipment();
        double shockwaveChance = cfg.getDouble("tools.plutonium-axe.shockwave-chance", 0.15);

        if (RANDOM.nextDouble() >= shockwaveChance) return;

        double radius     = cfg.getDouble("tools.plutonium-axe.shockwave-radius", 3.0);
        int    shockwaveRad = cfg.getInt("tools.plutonium-axe.shockwave-radiation", 15);
        Location epicenter = event.getEntity().getLocation();

        // AoE effect
        for (Entity nearby : epicenter.getWorld().getNearbyEntities(epicenter, radius, radius, radius)) {
            if (nearby.equals(attacker)) continue;
            if (nearby instanceof Player target) {
                radiationManager.addRadiation(target, shockwaveRad, RadiationSource.PLUTONIUM_WEAPON);
            }
            if (nearby instanceof LivingEntity le && !nearby.equals(event.getEntity())) {
                Vector kb = nearby.getLocation().toVector()
                        .subtract(epicenter.toVector())
                        .normalize()
                        .multiply(0.8)
                        .setY(0.35);
                le.setVelocity(kb);
            }
        }

        spawnShockwaveParticles(epicenter, radius);
        epicenter.getWorld().playSound(epicenter, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.5f);

        NCLogger.debug("Axe shockwave at %s radius=%.1f", epicenter, radius);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    /** A player is in a critical hit if they are falling (y-velocity < -0.08). */
    private boolean isCriticalHit(Player player) {
        return player.getVelocity().getY() < -0.08 && !player.isOnGround()
                && !player.isSprinting() && !player.isInsideVehicle();
    }

    private void spawnSwordHitParticles(Location loc) {
        loc.getWorld().spawnParticle(Particle.DUST,
                loc.add(0, 1, 0), 12, 0.3, 0.3, 0.3, 0,
                new Particle.DustOptions(Color.LIME, 1.5f));
        loc.getWorld().spawnParticle(Particle.CRIT,
                loc, 5, 0.2, 0.2, 0.2, 0.1);
    }

    private void spawnCritParticles(Location loc) {
        loc.getWorld().spawnParticle(Particle.DUST,
                loc.add(0, 1, 0), 30, 0.6, 0.6, 0.6, 0,
                new Particle.DustOptions(Color.LIME, 2.0f));
        loc.getWorld().spawnParticle(Particle.EXPLOSION,
                loc, 1, 0, 0, 0, 0);
    }

    private void spawnShockwaveParticles(Location center, double radius) {
        int steps = 36;
        for (int i = 0; i < steps; i++) {
            double angle = 2 * Math.PI * i / steps;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            Location point = new Location(center.getWorld(), x, center.getY() + 0.1, z);
            center.getWorld().spawnParticle(Particle.DUST, point, 3, 0.1, 0.1, 0.1, 0,
                    new Particle.DustOptions(Color.LIME, 1.8f));
        }
    }
}
