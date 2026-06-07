package com.nuclearcraft.equipment;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.items.CustomItem;
import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages passive effects granted by wearing Plutonium Armor.
 *
 * <h3>Piece passives (only while wearing that specific piece)</h3>
 * <ul>
 *   <li><b>Helmet</b>   — Night Vision (refreshed every interval)</li>
 *   <li><b>Chestplate</b> — Regeneration I (refreshed every interval)</li>
 *   <li><b>Leggings</b> — Speed I (refreshed every interval)</li>
 *   <li><b>Boots</b>    — 40% fall-damage reduction (handled via event)</li>
 * </ul>
 *
 * Ambient particles: neon-green dust motes + WITCH sparks pop out continuously
 * around any player wearing at least one piece of plutonium armour.
 */
public class ArmorEffectManager {

    private static final Color GREEN_GLOW   = Color.fromRGB(57, 255, 20);
    private static final Color GREEN_BRIGHT = Color.fromRGB(135, 255, 105);
    private static final Particle.DustOptions DUST_GLOW   = new Particle.DustOptions(GREEN_GLOW,   1.0f);
    private static final Particle.DustOptions DUST_BRIGHT = new Particle.DustOptions(GREEN_BRIGHT, 0.7f);

    private final JavaPlugin plugin;
    private final ItemManager itemManager;
    private final ConfigManager configManager;

    private BukkitTask passiveTask;
    private BukkitTask particleTask;

    public ArmorEffectManager(JavaPlugin plugin, ItemManager itemManager,
                              ConfigManager configManager) {
        this.plugin        = plugin;
        this.itemManager   = itemManager;
        this.configManager = configManager;
    }

    public void initialize() {
        startPassiveTask();
        startParticleTask();
        NCLogger.info("ArmorEffectManager initialized.");
    }

    public void shutdown() {
        if (passiveTask  != null) passiveTask.cancel();
        if (particleTask != null) particleTask.cancel();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Passive potion effects
    // ──────────────────────────────────────────────────────────────────────────

    private void startPassiveTask() {
        int interval = configManager.getEquipment().getInt("armor.plutonium.passive-interval-ticks", 20);
        int nvAmp    = configManager.getEquipment().getInt("armor.plutonium.night-vision-amplifier", 0);
        int regenAmp = configManager.getEquipment().getInt("armor.plutonium.regen-amplifier", 0);
        int speedAmp = configManager.getEquipment().getInt("armor.plutonium.speed-amplifier", 0);
        int duration = interval * 2 + 20;

        passiveTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                applyPassives(player, duration, nvAmp, regenAmp, speedAmp);
            }
        }, interval, interval);
    }

    private void applyPassives(Player player, int duration, int nvAmp, int regenAmp, int speedAmp) {
        if (isPiece(player.getInventory().getHelmet(), "plutonium-helmet")) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.NIGHT_VISION, duration, nvAmp, true, false, false));
        }
        if (isPiece(player.getInventory().getChestplate(), "plutonium-chestplate")) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.REGENERATION, duration, regenAmp, true, false, false));
        }
        if (isPiece(player.getInventory().getLeggings(), "plutonium-leggings")) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.SPEED, duration, speedAmp, true, false, false));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Ambient armor particles — runs every 5 ticks for smooth effect
    // ──────────────────────────────────────────────────────────────────────────

    private void startParticleTask() {
        particleTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                int pieces = countPlutoniumPieces(player);
                if (pieces == 0) continue;
                spawnArmorParticles(player, pieces);
            }
        }, 5L, 5L);
    }

    /**
     * Spawns ambient radiation particles around the player.
     * More pieces = more particles (1 piece: subtle; full set: dramatic).
     */
    private void spawnArmorParticles(Player player, int pieces) {
        var world = player.getWorld();
        var base  = player.getLocation();
        var rand  = ThreadLocalRandom.current();

        // Number of dust motes scales with equipped pieces
        int motes = pieces * 2;

        for (int i = 0; i < motes; i++) {
            double ox = rand.nextDouble(-0.5, 0.5);
            double oy = rand.nextDouble(0.0, 2.1);
            double oz = rand.nextDouble(-0.5, 0.5);
            Location p = base.clone().add(ox, oy, oz);
            world.spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, DUST_GLOW);
        }

        // Every full set: extra WITCH sparks and bright pop
        if (pieces == 4) {
            double ox = rand.nextDouble(-0.6, 0.6);
            double oy = rand.nextDouble(0.5, 1.8);
            double oz = rand.nextDouble(-0.6, 0.6);
            Location p = base.clone().add(ox, oy, oz);
            world.spawnParticle(Particle.WITCH, p, 1, 0.1, 0.1, 0.1, 0);
            world.spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, DUST_BRIGHT);
        }

        // Random upward rising spark — "radiation flowing off the armor"
        if (rand.nextDouble() < 0.35) {
            double ox = rand.nextDouble(-0.3, 0.3);
            double oz2 = rand.nextDouble(-0.3, 0.3);
            Location risePoint = base.clone().add(ox, rand.nextDouble(0.2, 1.9), oz2);
            world.spawnParticle(Particle.DUST, risePoint, 1,
                    rand.nextDouble(-0.02, 0.02), 0.04, rand.nextDouble(-0.02, 0.02),
                    0, DUST_GLOW);
        }
    }

    /** Counts how many plutonium armor pieces the player is wearing (0–4). */
    private int countPlutoniumPieces(Player player) {
        int count = 0;
        if (isPiece(player.getInventory().getHelmet(),     "plutonium-helmet"))     count++;
        if (isPiece(player.getInventory().getChestplate(), "plutonium-chestplate")) count++;
        if (isPiece(player.getInventory().getLeggings(),   "plutonium-leggings"))   count++;
        if (isPiece(player.getInventory().getBoots(),      "plutonium-boots"))      count++;
        return count;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Fall damage reduction
    // ──────────────────────────────────────────────────────────────────────────

    public void handleFallDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isPiece(player.getInventory().getBoots(), "plutonium-boots")) return;
        event.setDamage(event.getDamage() * 0.60);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private boolean isPiece(ItemStack item, String id) {
        if (item == null) return false;
        Optional<CustomItem> ci = itemManager.getItem(id);
        return ci.isPresent() && ci.get().matches(item);
    }
}
