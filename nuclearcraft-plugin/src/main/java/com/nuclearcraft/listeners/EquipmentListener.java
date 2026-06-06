package com.nuclearcraft.listeners;

import com.nuclearcraft.advancements.AdvancementManager;
import com.nuclearcraft.data.PlayerData;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.equipment.*;
import com.nuclearcraft.items.CustomItem;
import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.NamespacedKey;

import java.util.Random;
import java.util.Set;

/**
 * Central event listener for Phase 6: Plutonium Equipment System.
 *
 * <p>Routes all equipment-related events to the appropriate sub-manager:
 * <ul>
 *   <li>Combat hits → {@link WeaponEffectManager}</li>
 *   <li>Block break → debris/soil creation, cleanup</li>
 *   <li>Player interact → Plutonium Hoe farmland conversion</li>
 *   <li>Bow shoot / projectile hit → {@link PlutoniumArrowManager}</li>
 *   <li>Craft preparation → PDC validation for ECHO_SHARD ingredients</li>
 *   <li>Craft completion → advancement tracking</li>
 *   <li>Anvil → {@link EquipmentRepairManager}</li>
 *   <li>Fall damage → boot passive</li>
 *   <li>Crop growth → farmland growth bonus</li>
 * </ul>
 */
public class EquipmentListener implements Listener {

    private static final Set<Material> TILLABLE_MATERIALS =
            Set.of(Material.GRASS_BLOCK, Material.DIRT, Material.COARSE_DIRT);

    private static final Set<String> PLUTONIUM_TOOLS = Set.of(
            "plutonium-sword", "plutonium-axe", "plutonium-pickaxe",
            "plutonium-shovel", "plutonium-hoe");

    private static final Random RANDOM = new Random();

    private final JavaPlugin plugin;
    private final ItemManager itemManager;
    private final PlayerDataManager playerDataManager;
    private final AdvancementManager advancementManager;
    private final EquipmentManager equipmentManager;

    public EquipmentListener(JavaPlugin plugin,
                             ItemManager itemManager,
                             PlayerDataManager playerDataManager,
                             AdvancementManager advancementManager,
                             EquipmentManager equipmentManager) {
        this.plugin             = plugin;
        this.itemManager        = itemManager;
        this.playerDataManager  = playerDataManager;
        this.advancementManager = advancementManager;
        this.equipmentManager   = equipmentManager;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Combat
    // ──────────────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        equipmentManager.getWeaponEffectManager().onEntityHit(event);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Fall damage (plutonium boots passive)
    // ──────────────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        equipmentManager.getArmorEffectManager().handleFallDamage(event);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Block break — debris cleanup, soil cleanup, farmland cleanup
    //               and pickaxe/shovel special abilities
    // ──────────────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block    = event.getBlock();
        Player player  = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        String heldId  = getItemId(held);

        // ── Cleanup: remove tracking if a custom world block is broken ────────
        if (equipmentManager.getDebrisManager().isDebris(block.getLocation())) {
            equipmentManager.getDebrisManager().removeDebris(block.getLocation());
        }
        if (equipmentManager.getSoilManager().isSoil(block.getLocation())) {
            equipmentManager.getSoilManager().removeSoil(block.getLocation());
        }
        if (equipmentManager.getFarmlandManager().isFarmland(block.getLocation())) {
            equipmentManager.getFarmlandManager().removeFarmland(block.getLocation());
        }

        if (heldId == null) return;

        // ── Plutonium Pickaxe: 10% chance to create Radioactive Debris ────────
        if ("plutonium-pickaxe".equals(heldId)) {
            double chance = plugin.getConfig().getDouble(
                    "tools.plutonium-pickaxe.debris-chance",
                    equipmentManager.getDebrisManager() != null ? 0.10 : 0.10);
            // Read from equipment.yml
            chance = getEquipmentConfig("tools.plutonium-pickaxe.debris-chance", 0.10);
            if (RANDOM.nextDouble() < chance) {
                equipmentManager.getDebrisManager().createDebris(block.getLocation());
                playerDataManager.get(player.getUniqueId()).ifPresent(data ->
                        data.setDebrisGenerated(data.getDebrisGenerated() + 1));
                event.setDropItems(false); // block becomes debris, no drops
            }
        }

        // ── Plutonium Shovel: 15% chance to create Radioactive Soil ──────────
        if ("plutonium-shovel".equals(heldId)) {
            double chance = getEquipmentConfig("tools.plutonium-shovel.soil-conversion-chance", 0.15);
            if (RANDOM.nextDouble() < chance) {
                equipmentManager.getSoilManager().createSoil(block.getLocation());
                playerDataManager.get(player.getUniqueId()).ifPresent(data ->
                        data.setBlocksConverted(data.getBlocksConverted() + 1));
                event.setDropItems(false); // block becomes soil, no drops
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Player interact — Plutonium Hoe: dirt/grass → Radioactive Farmland
    // ──────────────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        Player player  = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!"plutonium-hoe".equals(getItemId(held))) return;

        if (!TILLABLE_MATERIALS.contains(block.getType())) return;

        event.setCancelled(true); // prevent default hoe behaviour

        RadioactiveFarmlandManager farmlandManager = equipmentManager.getFarmlandManager();
        if (!farmlandManager.isTillable(block)) return;

        farmlandManager.createFarmland(block, player);

        // Play a tilling sound
        block.getWorld().playSound(block.getLocation(),
                org.bukkit.Sound.ITEM_HOE_TILL, 1.0f, 1.0f);
        block.getWorld().spawnParticle(org.bukkit.Particle.DUST,
                block.getLocation().add(0.5, 1.0, 0.5), 8, 0.4, 0.1, 0.4, 0,
                new org.bukkit.Particle.DustOptions(org.bukkit.Color.LIME, 1.2f));

        checkFarmlandAdvancement(player);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Bow / Arrow
    // ──────────────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBowShoot(EntityShootBowEvent event) {
        equipmentManager.getArrowManager().onBowShoot(event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onProjectileHit(ProjectileHitEvent event) {
        equipmentManager.getArrowManager().onProjectileHit(event);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Crop growth
    // ──────────────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockGrow(BlockGrowEvent event) {
        equipmentManager.getFarmlandManager().handleCropGrow(event);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Recipe validation (PrepareItemCraftEvent)
    // Ensures ECHO_SHARD slots hold genuine refined-plutonium-ingots via PDC
    // ──────────────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        ItemStack result = event.getRecipe() == null ? null : event.getRecipe().getResult();
        if (result == null) return;

        String resultId = getItemId(result);
        if (resultId == null) return;

        // Only validate recipes that need refined-plutonium-ingot
        boolean needsIngot = PLUTONIUM_TOOLS.contains(resultId)
                || resultId.startsWith("plutonium-helmet")
                || resultId.startsWith("plutonium-chestplate")
                || resultId.startsWith("plutonium-leggings")
                || resultId.startsWith("plutonium-boots")
                || "plutonium-arrow".equals(resultId);

        if (!needsIngot) return;

        // Scan the crafting matrix for ECHO_SHARD items
        for (ItemStack slot : event.getInventory().getMatrix()) {
            if (slot == null || slot.getType() != Material.ECHO_SHARD) continue;
            // Must be a genuine refined-plutonium-ingot (PDC match)
            if (!isRefinedIngot(slot)) {
                event.getInventory().setResult(new ItemStack(Material.AIR));
                return;
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Craft completion (advancement triggers)
    // ──────────────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack result = event.getRecipe().getResult();
        String craftedId = getItemId(result);
        if (craftedId == null) return;

        // Schedule advancement check after inventory updates
        plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                checkAdvancementsAfterCraft(player, craftedId), 1L);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Anvil repair
    // ──────────────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        equipmentManager.getRepairManager().onPrepareAnvil(event);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Advancement checks
    // ──────────────────────────────────────────────────────────────────────────

    private void checkAdvancementsAfterCraft(Player player, String craftedId) {
        if ("plutonium-sword".equals(craftedId)) {
            advancementManager.award(player, AdvancementManager.Advancement.NUCLEAR_WARRIOR);
        }

        // Check set-based advancements by scanning inventory
        var inv = player.getInventory();

        if (hasItem(inv.getContents(), "hazmat-helmet")
                && hasItem(inv.getContents(), "hazmat-chestplate")
                && hasItem(inv.getContents(), "hazmat-leggings")
                && hasItem(inv.getContents(), "hazmat-boots")) {
            advancementManager.award(player, AdvancementManager.Advancement.PROTECTED_WORKER);
        }

        if (hasItem(inv.getContents(), "plutonium-helmet")
                && hasItem(inv.getContents(), "plutonium-chestplate")
                && hasItem(inv.getContents(), "plutonium-leggings")
                && hasItem(inv.getContents(), "plutonium-boots")) {
            advancementManager.award(player, AdvancementManager.Advancement.NUCLEAR_KNIGHT);
        }

        if (hasItem(inv.getContents(), "plutonium-sword")
                && hasItem(inv.getContents(), "plutonium-axe")
                && hasItem(inv.getContents(), "plutonium-pickaxe")
                && hasItem(inv.getContents(), "plutonium-shovel")
                && hasItem(inv.getContents(), "plutonium-hoe")) {
            advancementManager.award(player, AdvancementManager.Advancement.RADIOACTIVE_ARSENAL);
        }
    }

    private void checkFarmlandAdvancement(Player player) {
        // Award on first farmland creation tracked via PlayerData
        playerDataManager.get(player.getUniqueId()).ifPresent(data -> {
            if (data.getFarmlandCreated() == 1) {
                // First farmland — could add a specific advancement in future
                NCLogger.debug("%s created their first Radioactive Farmland.", player.getName());
            }
        });
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private String getItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer()
                .get(new NamespacedKey(plugin, CustomItem.PDC_KEY_ID), PersistentDataType.STRING);
    }

    private boolean isRefinedIngot(ItemStack item) {
        if (item == null) return false;
        return "refined-plutonium-ingot".equals(getItemId(item));
    }

    private boolean hasItem(ItemStack[] contents, String id) {
        for (ItemStack item : contents) {
            if (id.equals(getItemId(item))) return true;
        }
        return false;
    }

    private double getEquipmentConfig(String path, double defaultValue) {
        try {
            var equipConfig = ((com.nuclearcraft.core.NuclearCraftPlugin) plugin)
                    .getConfigManager().getEquipment();
            return equipConfig.getDouble(path, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
