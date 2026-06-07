package com.nuclearcraft.titantech;

import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

/**
 * Applies per-piece Titan Armor effects every tick interval.
 *
 * Helmet      — Night Vision
 * Chestplate  — Regeneration, +4 max health bonus (via Attribute)
 * Leggings    — Speed I, Resistance I
 * Boots       — Jump Boost II
 *
 * Full set handled by {@link SetBonusManager}.
 */
public class TitanArmorManager {

    private static final UUID MOD_HEALTH_UUID = UUID.fromString("a8e9b456-3cca-4d10-9d21-8801c9b43421");

    private final JavaPlugin plugin;
    private final ItemManager itemManager;
    private final FileConfiguration cfg;

    private int effectIntervalTicks;
    private BukkitTask task;

    public TitanArmorManager(JavaPlugin plugin, ItemManager itemManager, FileConfiguration cfg) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.cfg = cfg;
    }

    public void initialize() {
        effectIntervalTicks = cfg.getInt("armor.effect-interval-ticks", 40);
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickAll,
                effectIntervalTicks, effectIntervalTicks);
        NCLogger.info("TitanArmorManager initialized.");
    }

    public void shutdown() {
        if (task != null) { task.cancel(); task = null; }
    }

    private void tickAll() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            applyEffects(player);
        }
    }

    public void applyEffects(Player player) {
        PlayerInventory inv = player.getInventory();
        int dur = effectIntervalTicks + 20;

        if (isHelmet(inv.getHelmet())) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, dur, 0, true, false, false));
        }
        if (isChestplate(inv.getChestplate())) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, dur, 0, true, false, false));
            applyHealthBonus(player, 4.0);
        } else {
            removeHealthBonus(player);
        }
        if (isLeggings(inv.getLeggings())) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, dur, 0, true, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, dur, 0, true, false, false));
        }
        if (isBoots(inv.getBoots())) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, dur, 1, true, false, false));
        }
    }

    private void applyHealthBonus(Player player, double extraHearts) {
        var attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) return;
        boolean has = attr.getModifiers().stream().anyMatch(m -> m.getUniqueId().equals(MOD_HEALTH_UUID));
        if (!has) {
            attr.addModifier(new AttributeModifier(MOD_HEALTH_UUID, "titan-health-bonus",
                    extraHearts * 2.0, AttributeModifier.Operation.ADD_NUMBER));
        }
    }

    private void removeHealthBonus(Player player) {
        var attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) return;
        attr.getModifiers().stream()
                .filter(m -> m.getUniqueId().equals(MOD_HEALTH_UUID))
                .findFirst().ifPresent(attr::removeModifier);
    }

    // ── Item detection ───────────────────────────────────────────────────────

    public boolean isHelmet(ItemStack item) {
        return itemManager.getItem("titan-helmet").map(i -> i.matches(item)).orElse(false);
    }

    public boolean isChestplate(ItemStack item) {
        return itemManager.getItem("titan-chestplate").map(i -> i.matches(item)).orElse(false);
    }

    public boolean isLeggings(ItemStack item) {
        return itemManager.getItem("titan-leggings").map(i -> i.matches(item)).orElse(false);
    }

    public boolean isBoots(ItemStack item) {
        return itemManager.getItem("titan-boots").map(i -> i.matches(item)).orElse(false);
    }

    public boolean isAnyTitanArmor(ItemStack item) {
        return isHelmet(item) || isChestplate(item) || isLeggings(item) || isBoots(item);
    }

    public boolean hasFullSet(Player player) {
        PlayerInventory inv = player.getInventory();
        return isHelmet(inv.getHelmet())
                && isChestplate(inv.getChestplate())
                && isLeggings(inv.getLeggings())
                && isBoots(inv.getBoots());
    }

    public void onPlayerQuit(Player player) {
        removeHealthBonus(player);
    }
}
