package com.nuclearcraft.equipment;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.items.CustomItem;
import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.Optional;

/**
 * Manages passive effects granted by wearing Plutonium Armor.
 *
 * <h3>Piece passives (only while wearing that specific piece)</h3>
 * <ul>
 *   <li><b>Helmet</b>   — Night Vision (refreshed every interval)</li>
 *   <li><b>Chestplate</b> — Regeneration I (refreshed every interval)</li>
 *   <li><b>Leggings</b> — Speed I (refreshed every interval)</li>
 *   <li><b>Boots</b>    — 40% fall-damage reduction (handled via event in {@link EquipmentListener})</li>
 * </ul>
 *
 * Effects are applied by refreshing them before they expire.
 * Removing a piece immediately stops the refresh cycle; vanilla potion timers expire naturally.
 */
public class ArmorEffectManager {

    private final JavaPlugin plugin;
    private final ItemManager itemManager;
    private final ConfigManager configManager;

    private BukkitTask passiveTask;

    public ArmorEffectManager(JavaPlugin plugin, ItemManager itemManager,
                              ConfigManager configManager) {
        this.plugin        = plugin;
        this.itemManager   = itemManager;
        this.configManager = configManager;
    }

    public void initialize() {
        startPassiveTask();
        NCLogger.info("ArmorEffectManager initialized.");
    }

    public void shutdown() {
        if (passiveTask != null) passiveTask.cancel();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Passive scheduler
    // ──────────────────────────────────────────────────────────────────────────

    private void startPassiveTask() {
        int interval = configManager.getEquipment().getInt("armor.plutonium.passive-interval-ticks", 20);
        int nvAmp    = configManager.getEquipment().getInt("armor.plutonium.night-vision-amplifier", 0);
        int regenAmp = configManager.getEquipment().getInt("armor.plutonium.regen-amplifier", 0);
        int speedAmp = configManager.getEquipment().getInt("armor.plutonium.speed-amplifier", 0);

        // Effect duration = 2 * interval so it never flickers
        int duration = interval * 2 + 20;

        passiveTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                applyPassives(player, duration, nvAmp, regenAmp, speedAmp);
            }
        }, interval, interval);
    }

    private void applyPassives(Player player, int duration, int nvAmp, int regenAmp, int speedAmp) {
        // Helmet → Night Vision
        if (isPiece(player.getInventory().getHelmet(), "plutonium-helmet")) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.NIGHT_VISION, duration, nvAmp, true, false, false));
        }
        // Chestplate → Regeneration
        if (isPiece(player.getInventory().getChestplate(), "plutonium-chestplate")) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.REGENERATION, duration, regenAmp, true, false, false));
        }
        // Leggings → Speed
        if (isPiece(player.getInventory().getLeggings(), "plutonium-leggings")) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.SPEED, duration, speedAmp, true, false, false));
        }
        // Boots → handled in EquipmentListener (EntityDamageEvent FALL reduction)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Fall damage reduction (called from EquipmentListener)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Reduces fall damage by 40% if the player is wearing Plutonium Boots.
     * Should be called from {@link EquipmentListener} on {@link EntityDamageEvent}
     * when cause is {@link EntityDamageEvent.DamageCause#FALL}.
     */
    public void handleFallDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isPiece(player.getInventory().getBoots(), "plutonium-boots")) return;

        double reduced = event.getDamage() * 0.60; // 40% reduction
        event.setDamage(reduced);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private boolean isPiece(org.bukkit.inventory.ItemStack item, String id) {
        if (item == null) return false;
        Optional<CustomItem> ci = itemManager.getItem(id);
        return ci.isPresent() && ci.get().matches(item);
    }
}
