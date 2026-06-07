package com.nuclearcraft.titantech;

import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.radiation.RadiationManager;
import com.nuclearcraft.radiation.RadiationSource;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;

/**
 * Titan Aura: 5-block radius radiation field emitted by Titan weapon carriers.
 *
 * Hostile mobs: receive radiation-equivalent weakness/slowness + damage
 * Players (PvP only): receive radiation
 *
 * Active when the player holds a Titan weapon in their main hand.
 * Efficient caching: only scans nearby entities once every aura-interval ticks.
 */
public class TitanAuraManager {

    private final JavaPlugin plugin;
    private final ItemManager itemManager;
    private final RadiationManager radiationManager;
    private final PlayerDataManager playerDataManager;
    private final TitanWeaponManager weaponManager;
    private final FileConfiguration cfg;

    private BukkitTask task;
    private int intervalTicks;
    private double radius;
    private int mobWeaknessDuration;
    private int mobSlownessDuration;
    private double mobDamage;
    private int pvpRadiation;
    private boolean pvpEnabled;

    public TitanAuraManager(JavaPlugin plugin, ItemManager itemManager,
                             RadiationManager radiationManager,
                             PlayerDataManager playerDataManager,
                             TitanWeaponManager weaponManager,
                             FileConfiguration cfg) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.radiationManager = radiationManager;
        this.playerDataManager = playerDataManager;
        this.weaponManager = weaponManager;
        this.cfg = cfg;
    }

    public void initialize() {
        intervalTicks      = cfg.getInt("aura.interval-ticks", 40);
        radius             = cfg.getDouble("aura.radius", 5.0);
        mobWeaknessDuration= cfg.getInt("aura.mob-weakness-duration", 60);
        mobSlownessDuration= cfg.getInt("aura.mob-slowness-duration", 60);
        mobDamage          = cfg.getDouble("aura.mob-damage", 2.0);
        pvpRadiation       = cfg.getInt("aura.pvp-radiation", 15);
        pvpEnabled         = cfg.getBoolean("aura.pvp-enabled", true);

        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickAll, intervalTicks, intervalTicks);
        NCLogger.info("TitanAuraManager initialized — radius=" + radius + " ticks=" + intervalTicks);
    }

    public void shutdown() {
        if (task != null) { task.cancel(); task = null; }
    }

    private void tickAll() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (!weaponManager.isAnyTitanWeapon(mainHand)) continue;
            applyAura(player);
        }
    }

    private void applyAura(Player player) {
        Collection<Entity> nearby = player.getWorld().getNearbyEntities(
                player.getLocation(), radius, radius, radius);

        for (Entity entity : nearby) {
            if (entity.equals(player)) continue;

            if (entity instanceof Player target) {
                if (!pvpEnabled) continue;
                // Only in PvP — check same world, both alive
                if (target.isDead()) continue;
                radiationManager.addRadiation(target, pvpRadiation, RadiationSource.EQUIPMENT_AURA);
            } else if (entity instanceof Mob mob) {
                mob.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, mobWeaknessDuration, 1, false, false));
                mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, mobSlownessDuration, 1, false, false));
                if (mobDamage > 0) {
                    mob.damage(mobDamage, player);
                }
            }
        }

        // Aura particle ring
        spawnAuraParticles(player);
    }

    private void spawnAuraParticles(Player player) {
        org.bukkit.Location center = player.getLocation().add(0, 1, 0);
        double step = Math.PI * 2 / 12;
        for (int i = 0; i < 12; i++) {
            double angle = step * i;
            double x = Math.cos(angle) * 2.5;
            double z = Math.sin(angle) * 2.5;
            center.getWorld().spawnParticle(Particle.DRAGON_BREATH,
                    center.clone().add(x, 0, z), 1, 0, 0, 0, 0);
        }
    }
}
