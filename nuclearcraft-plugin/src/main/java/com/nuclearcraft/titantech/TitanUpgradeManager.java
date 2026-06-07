package com.nuclearcraft.titantech;

import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.utils.NCLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages Titan equipment upgrade tiers: Titan-I through Titan-IV.
 *
 * Higher than MK-IV — these are the ultimate upgrade levels.
 * Tier data stored in ItemStack PDC under "titan_tier".
 * Stats boosted per tier via additional lore lines.
 */
public class TitanUpgradeManager {

    public enum TitanTier {
        BASE("Base",      "§5☢ Titan",    0, 0, 0),
        TITAN_I("Titan-I",   "§5§l☢ Titan-I",  1, 5,  5),
        TITAN_II("Titan-II",  "§5§l☢ Titan-II", 2, 10, 10),
        TITAN_III("Titan-III", "§5§l☢ Titan-III",3, 20, 15),
        TITAN_IV("Titan-IV",  "§6§l☢ Titan-IV", 4, 35, 25);

        private final String id;
        private final String displayName;
        private final int level;
        private final int radiationBonus;  // % more radiation dealt
        private final int damageBonus;     // % damage bonus

        TitanTier(String id, String displayName, int level, int radiationBonus, int damageBonus) {
            this.id = id;
            this.displayName = displayName;
            this.level = level;
            this.radiationBonus = radiationBonus;
            this.damageBonus = damageBonus;
        }

        public String getId()          { return id; }
        public String getDisplayName() { return displayName; }
        public int getLevel()          { return level; }
        public int getRadiationBonus() { return radiationBonus; }
        public int getDamageBonus()    { return damageBonus; }

        public TitanTier next() {
            TitanTier[] vals = values();
            int idx = ordinal() + 1;
            return idx < vals.length ? vals[idx] : null;
        }
    }

    private static final String PDC_TIER_KEY = "titan_tier";

    private final JavaPlugin plugin;
    private final ItemManager itemManager;
    private final FileConfiguration cfg;
    private NamespacedKey tierKey;

    public TitanUpgradeManager(JavaPlugin plugin, ItemManager itemManager, FileConfiguration cfg) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.cfg = cfg;
    }

    public void initialize() {
        tierKey = new NamespacedKey(plugin, PDC_TIER_KEY);
        NCLogger.info("TitanUpgradeManager initialized — Titan-I through Titan-IV available.");
    }

    public void shutdown() {}

    // ── Tier read/write ──────────────────────────────────────────────────────

    public TitanTier getTier(ItemStack item) {
        if (item == null) return TitanTier.BASE;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return TitanTier.BASE;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String stored = pdc.getOrDefault(tierKey, PersistentDataType.STRING, TitanTier.BASE.getId());
        for (TitanTier t : TitanTier.values()) {
            if (t.getId().equals(stored)) return t;
        }
        return TitanTier.BASE;
    }

    public void applyTier(ItemStack item, TitanTier tier) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(tierKey, PersistentDataType.STRING, tier.getId());
        updateLore(item, meta, tier);
        item.setItemMeta(meta);
    }

    public ItemStack applyNextTier(ItemStack item) {
        ItemStack result = item.clone();
        TitanTier current = getTier(result);
        TitanTier next = current.next();
        if (next == null) return result;
        applyTier(result, next);
        return result;
    }

    private void updateLore(ItemStack item, ItemMeta meta, TitanTier tier) {
        var serial = LegacyComponentSerializer.legacySection();
        List<Component> lore = new ArrayList<>();
        if (meta.lore() != null) {
            // Strip old tier lines (those starting with "§5☢ Tier:" or "§6☢ Tier:")
            for (Component line : meta.lore()) {
                String plain = serial.serialize(line);
                if (plain.contains("Tier:") || plain.contains("Radiation Bonus:")
                        || plain.contains("Damage Bonus:") || plain.contains("§5§l☢ Titan")
                        || plain.contains("§6§l☢ Titan")) continue;
                lore.add(line);
            }
        }
        // Add tier info
        if (tier != TitanTier.BASE) {
            lore.add(Component.empty());
            lore.add(serial.deserialize(tier.getDisplayName() + " Upgrade")
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("  ☢ Tier: " + tier.getDisplayName(), NamedTextColor.LIGHT_PURPLE)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("  ☢ Radiation Bonus: +" + tier.getRadiationBonus() + "%", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("  ☢ Damage Bonus: +" + tier.getDamageBonus() + "%", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
    }

    public boolean isTitanEquipment(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(tierKey, PersistentDataType.STRING);
    }

    public int getUpgradedRadiation(ItemStack item, int baseRadiation) {
        TitanTier tier = getTier(item);
        return (int)(baseRadiation * (1.0 + tier.getRadiationBonus() / 100.0));
    }

    public double getUpgradedDamageMultiplier(ItemStack item) {
        TitanTier tier = getTier(item);
        return 1.0 + tier.getDamageBonus() / 100.0;
    }

    public NamespacedKey getTierKey() { return tierKey; }
}
