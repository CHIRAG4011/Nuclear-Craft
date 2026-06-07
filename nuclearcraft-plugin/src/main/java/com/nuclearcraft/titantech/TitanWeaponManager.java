package com.nuclearcraft.titantech;

import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.radiation.RadiationManager;
import com.nuclearcraft.radiation.RadiationSource;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;

/**
 * Applies on-hit effects for all Titan weapons.
 *
 * Titan Sword: Heavy Radiation + AOE wave + 15% crit explosion
 * Titan Axe:   Radiation + Shockwave knockback + Weakness
 */
public class TitanWeaponManager {

    private final JavaPlugin plugin;
    private final ItemManager itemManager;
    private final RadiationManager radiationManager;
    private final PlayerDataManager playerDataManager;
    private final FileConfiguration cfg;

    private int swordRadiation;
    private double swordWaveRadius;
    private int swordWaveRadiation;
    private double swordCritChance;
    private int axeRadiation;
    private double axeShockwaveRadius;
    private float axeKnockbackMultiplier;

    public TitanWeaponManager(JavaPlugin plugin, ItemManager itemManager,
                               RadiationManager radiationManager,
                               PlayerDataManager playerDataManager,
                               FileConfiguration cfg) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.radiationManager = radiationManager;
        this.playerDataManager = playerDataManager;
        this.cfg = cfg;
    }

    public void initialize() {
        swordRadiation       = cfg.getInt("weapons.sword.radiation-on-hit", 80);
        swordWaveRadius      = cfg.getDouble("weapons.sword.wave-radius", 4.0);
        swordWaveRadiation   = cfg.getInt("weapons.sword.wave-radiation", 40);
        swordCritChance      = cfg.getDouble("weapons.sword.crit-chance", 15.0);
        axeRadiation         = cfg.getInt("weapons.axe.radiation-on-hit", 60);
        axeShockwaveRadius   = cfg.getDouble("weapons.axe.shockwave-radius", 5.0);
        axeKnockbackMultiplier = (float) cfg.getDouble("weapons.axe.knockback-multiplier", 2.0);
        NCLogger.info("TitanWeaponManager initialized.");
    }

    public void shutdown() {}

    public void onHit(Player attacker, Entity victim, ItemStack weapon) {
        if (isTitanSword(weapon))   handleSwordHit(attacker, victim);
        else if (isTitanAxe(weapon)) handleAxeHit(attacker, victim);

        playerDataManager.get(attacker.getUniqueId()).ifPresent(d -> {
            d.incrementSwordHits();
            d.setDirty(true);
        });
    }

    private void handleSwordHit(Player attacker, Entity victim) {
        Location loc = victim.getLocation();

        if (victim instanceof Player p) {
            radiationManager.addRadiation(p, swordRadiation, RadiationSource.PLUTONIUM_WEAPON);
        } else if (victim instanceof LivingEntity le) {
            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, false, true));
        }

        // AOE wave
        Collection<Entity> nearby = loc.getWorld().getNearbyEntities(loc, swordWaveRadius, swordWaveRadius, swordWaveRadius);
        for (Entity e : nearby) {
            if (e.equals(victim) || e.equals(attacker)) continue;
            if (e instanceof Player p) {
                radiationManager.addRadiation(p, swordWaveRadiation, RadiationSource.PLUTONIUM_WEAPON);
                p.sendMessage("§5☢ Caught in a Titan radiation wave!");
            } else if (e instanceof LivingEntity le) {
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0));
            }
        }

        Location above = loc.clone().add(0, 1, 0);
        loc.getWorld().spawnParticle(Particle.DRAGON_BREATH, above, 20, 0.5, 0.5, 0.5, 0.08);
        loc.getWorld().spawnParticle(Particle.WITCH, above, 10, 0.3, 0.3, 0.3, 0.05);
        loc.getWorld().playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.6f, 1.8f);

        if (Math.random() * 100 < swordCritChance) {
            loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, above, 40, 0.5, 0.5, 0.5, 0.1);
            loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.5f);
            attacker.sendMessage("§5☢ CRITICAL TITAN HIT!");
        }
    }

    private void handleAxeHit(Player attacker, Entity victim) {
        Location loc = victim.getLocation();

        if (victim instanceof Player p) {
            radiationManager.addRadiation(p, axeRadiation, RadiationSource.PLUTONIUM_WEAPON);
        }
        if (victim instanceof LivingEntity le) {
            le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 1, false, true));
        }

        // Shockwave
        Collection<Entity> nearby = loc.getWorld().getNearbyEntities(loc, axeShockwaveRadius, axeShockwaveRadius, axeShockwaveRadius);
        for (Entity e : nearby) {
            if (e.equals(attacker)) continue;
            org.bukkit.util.Vector dir = e.getLocation().toVector()
                    .subtract(loc.toVector()).normalize().multiply(axeKnockbackMultiplier);
            dir.setY(Math.min(dir.getY() + 0.4, 1.5));
            e.setVelocity(dir);
            if (e instanceof Player p) {
                radiationManager.addRadiation(p, axeRadiation / 2, RadiationSource.PLUTONIUM_WEAPON);
            }
        }

        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 4, 1.0, 1.0, 1.0, 0.05);
        loc.getWorld().spawnParticle(Particle.WITCH, loc.clone().add(0, 1, 0), 15, 0.8, 0.8, 0.8, 0.05);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f);
    }

    // ── Detection ────────────────────────────────────────────────────────────

    public boolean isTitanSword(ItemStack item) {
        return itemManager.getItem("titan-sword").map(i -> i.matches(item)).orElse(false);
    }

    public boolean isTitanAxe(ItemStack item) {
        return itemManager.getItem("titan-axe").map(i -> i.matches(item)).orElse(false);
    }

    public boolean isTitanPickaxe(ItemStack item) {
        return itemManager.getItem("titan-pickaxe").map(i -> i.matches(item)).orElse(false);
    }

    public boolean isTitanShovel(ItemStack item) {
        return itemManager.getItem("titan-shovel").map(i -> i.matches(item)).orElse(false);
    }

    public boolean isTitanHoe(ItemStack item) {
        return itemManager.getItem("titan-hoe").map(i -> i.matches(item)).orElse(false);
    }

    public boolean isTitanBow(ItemStack item) {
        return itemManager.getItem("titan-bow").map(i -> i.matches(item)).orElse(false);
    }

    public boolean isAnyTitanWeapon(ItemStack item) {
        return isTitanSword(item) || isTitanAxe(item) || isTitanPickaxe(item)
                || isTitanShovel(item) || isTitanHoe(item) || isTitanBow(item);
    }
}
