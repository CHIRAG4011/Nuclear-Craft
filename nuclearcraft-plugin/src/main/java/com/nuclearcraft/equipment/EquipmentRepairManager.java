package com.nuclearcraft.equipment;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.items.CustomItem;
import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Optional;
import java.util.Set;

/**
 * Handles anvil-based repair of Plutonium Equipment using Refined Plutonium Ingots.
 *
 * <h3>Repair rules</h3>
 * <ul>
 *   <li>Plutonium tools &amp; armor: repaired with Refined Plutonium Ingots (ECHO_SHARD + PDC tag)</li>
 *   <li>Hazmat armor: architecture reserved for Industrial Fabric (future item)</li>
 *   <li>Repair amount per ingot is configurable in {@code equipment.yml}</li>
 * </ul>
 *
 * <p>Called from {@link EquipmentListener} on {@link PrepareAnvilEvent}.
 */
public class EquipmentRepairManager {

    private static final Set<String> PLUTONIUM_EQUIPMENT = Set.of(
            "plutonium-sword", "plutonium-axe", "plutonium-pickaxe",
            "plutonium-shovel", "plutonium-hoe",
            "plutonium-helmet", "plutonium-chestplate",
            "plutonium-leggings", "plutonium-boots"
    );

    private static final Set<String> HAZMAT_EQUIPMENT = Set.of(
            "hazmat-helmet", "hazmat-chestplate", "hazmat-leggings", "hazmat-boots"
    );

    private final ItemManager itemManager;
    private final ConfigManager configManager;

    public EquipmentRepairManager(ItemManager itemManager, ConfigManager configManager) {
        this.itemManager   = itemManager;
        this.configManager = configManager;
    }

    public void initialize() {
        NCLogger.info("EquipmentRepairManager initialized.");
    }

    public void shutdown() {}

    // ──────────────────────────────────────────────────────────────────────────
    // Event handler (called from EquipmentListener)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Intercepts PrepareAnvilEvent to allow repairing Plutonium Equipment
     * with Refined Plutonium Ingots.
     */
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory inv = event.getInventory();
        ItemStack first  = inv.getItem(0); // item to repair
        ItemStack second = inv.getItem(1); // repair material

        if (first == null || second == null) return;

        String equipId = CustomItem.getId(getPlugin(), first);
        if (equipId == null) return;

        if (PLUTONIUM_EQUIPMENT.contains(equipId)) {
            handlePlutoniumRepair(event, first, second);
        } else if (HAZMAT_EQUIPMENT.contains(equipId)) {
            handleHazmatRepair(event, first, second);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Repair logic
    // ──────────────────────────────────────────────────────────────────────────

    private void handlePlutoniumRepair(PrepareAnvilEvent event, ItemStack equipment, ItemStack material) {
        String repairMaterialId = configManager.getEquipment()
                .getString("repair.plutonium-material", "refined-plutonium-ingot");
        int repairPerIngot = configManager.getEquipment()
                .getInt("repair.repair-per-ingot", 300);

        Optional<CustomItem> repairItem = itemManager.getItem(repairMaterialId);
        if (repairItem.isEmpty() || !repairItem.get().matches(material)) return;

        ItemStack result = applyRepair(equipment, material.getAmount(), repairPerIngot);
        if (result == null) return;

        event.setResult(result);
        event.getInventory().setRepairCost(1);
    }

    private void handleHazmatRepair(PrepareAnvilEvent event, ItemStack equipment, ItemStack material) {
        String repairMaterialId = configManager.getEquipment()
                .getString("repair.hazmat-material", "industrial-fabric");
        int repairPerPiece = configManager.getEquipment()
                .getInt("repair.hazmat-repair-per-piece", 200);

        Optional<CustomItem> repairItem = itemManager.getItem(repairMaterialId);
        if (repairItem.isEmpty() || !repairItem.get().matches(material)) return;

        ItemStack result = applyRepair(equipment, material.getAmount(), repairPerPiece);
        if (result == null) return;

        event.setResult(result);
        event.getInventory().setRepairCost(1);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private ItemStack applyRepair(ItemStack equipment, int materialCount, int repairPerUnit) {
        ItemMeta meta = equipment.getItemMeta();
        if (!(meta instanceof Damageable damageable)) return null;

        int currentDamage = damageable.getDamage();
        if (currentDamage <= 0) return null; // already at full durability

        int repairAmount = repairPerUnit * materialCount;
        int newDamage    = Math.max(0, currentDamage - repairAmount);

        ItemStack result = equipment.clone();
        ItemMeta  resultMeta = result.getItemMeta();
        if (resultMeta instanceof Damageable resMeta) {
            resMeta.setDamage(newDamage);
            result.setItemMeta(resultMeta);
        }
        return result;
    }

    /**
     * Returns the plugin instance for PDC key creation.
     * Resolved lazily via Bukkit plugin manager.
     */
    private org.bukkit.plugin.java.JavaPlugin getPlugin() {
        org.bukkit.plugin.Plugin p = org.bukkit.Bukkit.getPluginManager().getPlugin("NuclearCraft");
        return (p instanceof org.bukkit.plugin.java.JavaPlugin jp) ? jp : null;
    }
}
