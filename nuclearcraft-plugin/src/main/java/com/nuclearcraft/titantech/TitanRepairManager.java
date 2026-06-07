package com.nuclearcraft.titantech;

import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Handles Titan equipment repair using Titan Fragments and Titan Cores in an anvil.
 *
 * Repair amounts (configurable via titan_items.yml):
 *  - Titan Fragment: repairs fragmentRepairPercent% max durability per item
 *  - Titan Core:     repairs coreRepairPercent% max durability per item
 */
public class TitanRepairManager {

    private final JavaPlugin plugin;
    private final ItemManager itemManager;
    private final FileConfiguration cfg;

    private double fragmentRepairPercent;
    private double coreRepairPercent;

    private static final List<String> TITAN_EQUIPMENT_IDS = List.of(
            "titan-helmet", "titan-chestplate", "titan-leggings", "titan-boots",
            "titan-sword", "titan-axe", "titan-pickaxe", "titan-shovel", "titan-hoe", "titan-bow"
    );

    public TitanRepairManager(JavaPlugin plugin, ItemManager itemManager, FileConfiguration cfg) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.cfg = cfg;
    }

    public void initialize() {
        fragmentRepairPercent = cfg.getDouble("repair.fragment-repair-percent", 20.0);
        coreRepairPercent     = cfg.getDouble("repair.core-repair-percent", 50.0);
        NCLogger.info("TitanRepairManager initialized.");
    }

    public void shutdown() {}

    /**
     * Intercepts PrepareAnvilEvent for titan equipment repair.
     * @return true if handled (event should not be processed further)
     */
    public boolean handleAnvilRepair(PrepareAnvilEvent event) {
        AnvilInventory inv = event.getInventory();
        ItemStack base   = inv.getItem(0);
        ItemStack repair = inv.getItem(1);

        if (!isTitanEquipment(base)) return false;
        if (repair == null || repair.getType().isAir()) return false;

        if (isTitanFragment(repair)) {
            double pct = fragmentRepairPercent * repair.getAmount() / 100.0;
            ItemStack result = repairItem(base, pct);
            if (result == null) return false;
            event.setResult(result);
            inv.setRepairCost(5 + repair.getAmount());
            return true;
        }

        if (isTitanCore(repair)) {
            double pct = coreRepairPercent * repair.getAmount() / 100.0;
            ItemStack result = repairItem(base, pct);
            if (result == null) return false;
            event.setResult(result);
            inv.setRepairCost(10 + repair.getAmount() * 5);
            return true;
        }

        return false;
    }

    private ItemStack repairItem(ItemStack base, double repairPct) {
        if (!(base.getItemMeta() instanceof Damageable dam)) return null;
        int maxDur = base.getType().getMaxDurability();
        if (maxDur <= 0) return null;

        int newDamage = Math.max(0, dam.getDamage() - (int)(maxDur * repairPct));
        ItemStack result = base.clone();
        ItemMeta m = result.getItemMeta();
        ((Damageable) m).setDamage(newDamage);
        result.setItemMeta(m);
        return result;
    }

    // ── Detection ────────────────────────────────────────────────────────────

    private boolean isTitanEquipment(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        for (String id : TITAN_EQUIPMENT_IDS) {
            if (itemManager.getItem(id).map(i -> i.matches(item)).orElse(false)) return true;
        }
        return false;
    }

    private boolean isTitanFragment(ItemStack item) {
        return itemManager.getItem("titan-fragment").map(i -> i.matches(item)).orElse(false);
    }

    private boolean isTitanCore(ItemStack item) {
        return itemManager.getItem("titan-core").map(i -> i.matches(item)).orElse(false);
    }
}
