package com.nuclearcraft.titantech;

import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.radiation.RadiationManager;
import com.nuclearcraft.radiation.RadiationSource;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Manages Titan Bow and Titan Arrow mechanics.
 *
 * On impact:
 *  - Heavy radiation
 *  - Poison + Glowing on target
 *  - AOE radiation burst within 3 blocks
 *  - Purple particle trail during flight
 */
public class TitanArrowManager {

    private final JavaPlugin plugin;
    private final ItemManager itemManager;
    private final RadiationManager radiationManager;
    private final PlayerDataManager playerDataManager;
    private final FileConfiguration cfg;

    private final Set<UUID> titanArrows = new HashSet<>();
    private BukkitTask trailTask;

    private int arrowRadiation;
    private int arrowAoeRadiation;
    private double arrowAoeRadius;
    private int poisonDuration;
    private int glowDuration;

    public TitanArrowManager(JavaPlugin plugin, ItemManager itemManager,
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
        arrowRadiation    = cfg.getInt("weapons.bow.arrow-radiation", 120);
        arrowAoeRadiation = cfg.getInt("weapons.bow.aoe-radiation", 60);
        arrowAoeRadius    = cfg.getDouble("weapons.bow.aoe-radius", 3.0);
        poisonDuration    = cfg.getInt("weapons.bow.poison-duration-ticks", 100);
        glowDuration      = cfg.getInt("weapons.bow.glow-duration-ticks", 120);

        trailTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickTrails, 1L, 1L);
        NCLogger.info("TitanArrowManager initialized.");
    }

    public void shutdown() {
        if (trailTask != null) { trailTask.cancel(); trailTask = null; }
        titanArrows.clear();
    }

    public void onTitanBowShoot(Player shooter, Arrow arrow) {
        titanArrows.add(arrow.getUniqueId());
        playerDataManager.get(shooter.getUniqueId()).ifPresent(d -> {
            d.incrementArrowsFired();
            d.setDirty(true);
        });
    }

    public void onArrowHit(Arrow arrow, Entity hitEntity, Location hitLocation) {
        if (!titanArrows.remove(arrow.getUniqueId())) return;

        // Impact particles & sound
        hitLocation.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, hitLocation, 30, 0.5, 0.5, 0.5, 0.08);
        hitLocation.getWorld().spawnParticle(Particle.WITCH, hitLocation, 15, 0.4, 0.4, 0.4, 0.05);
        hitLocation.getWorld().spawnParticle(Particle.DRAGON_BREATH, hitLocation, 10, 0.3, 0.3, 0.3, 0.05);
        hitLocation.getWorld().playSound(hitLocation, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.5f);

        // Direct hit
        if (hitEntity instanceof Player target) {
            radiationManager.addRadiation(target, arrowRadiation, RadiationSource.PLUTONIUM_ARROW);
            target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, poisonDuration, 1, false, true));
            target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, glowDuration, 0, false, true));
            target.sendMessage("§5☢ A Titan Arrow pierced you!");
        } else if (hitEntity instanceof LivingEntity le) {
            le.addPotionEffect(new PotionEffect(PotionEffectType.POISON, poisonDuration, 1, false, true));
            le.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, glowDuration, 0, false, true));
            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 2, false, true));
        }

        // AOE burst
        Collection<Entity> nearby = hitLocation.getWorld().getNearbyEntities(
                hitLocation, arrowAoeRadius, arrowAoeRadius, arrowAoeRadius);
        for (Entity e : nearby) {
            if (e.equals(hitEntity)) continue;
            if (e instanceof Player p) {
                radiationManager.addRadiation(p, arrowAoeRadiation, RadiationSource.PLUTONIUM_ARROW);
                p.sendMessage("§5☢ Caught in a Titan Arrow radiation burst!");
            } else if (e instanceof LivingEntity le) {
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));
            }
        }
    }

    public boolean isTitanArrow(Arrow arrow) {
        return titanArrows.contains(arrow.getUniqueId());
    }

    private void tickTrails() {
        Iterator<UUID> iter = titanArrows.iterator();
        while (iter.hasNext()) {
            UUID uuid = iter.next();
            boolean found = false;
            for (World world : plugin.getServer().getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity.getUniqueId().equals(uuid) && entity instanceof Arrow arrow) {
                        world.spawnParticle(Particle.DRAGON_BREATH, arrow.getLocation(), 3, 0.1, 0.1, 0.1, 0.01);
                        world.spawnParticle(Particle.WITCH, arrow.getLocation(), 2, 0.05, 0.05, 0.05, 0.01);
                        found = true;
                        break;
                    }
                }
                if (found) break;
            }
            if (!found) iter.remove();
        }
    }

    public boolean isTitanBow(ItemStack item) {
        return itemManager.getItem("titan-bow").map(i -> i.matches(item)).orElse(false);
    }
}
