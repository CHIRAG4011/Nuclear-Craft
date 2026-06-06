package com.nuclearcraft.upgrade;

import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.utils.NCLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Manages reading, writing, and applying MK upgrade tiers on custom equipment.
 *
 * Upgrade tier is stored in item PDC under {@link #KEY_UPGRADE_TIER}.
 * All attribute bonus application is done here so no other system needs to
 * know about specific upgrade values.
 */
public class UpgradeManager {

    /** PDC key that stores the upgrade tier level (0–4) on an item. */
    public static final String PDC_UPGRADE_TIER = "upgrade_tier";

    /** Item IDs of equipment that can be upgraded. */
    private static final Set<String> UPGRADEABLE_ITEM_IDS = Set.of(
            "plutonium-sword",
            "plutonium-axe",
            "plutonium-pickaxe",
            "plutonium-shovel",
            "plutonium-hoe",
            "plutonium-helmet",
            "plutonium-chestplate",
            "plutonium-leggings",
            "plutonium-boots",
            "hazmat-helmet",
            "hazmat-chestplate",
            "hazmat-leggings",
            "hazmat-boots"
    );

    private static final Set<String> WEAPON_IDS = Set.of(
            "plutonium-sword", "plutonium-axe");

    private static final Set<String> TOOL_IDS = Set.of(
            "plutonium-pickaxe", "plutonium-shovel", "plutonium-hoe");

    private static final Set<String> ARMOR_IDS = Set.of(
            "plutonium-helmet", "plutonium-chestplate", "plutonium-leggings", "plutonium-boots",
            "hazmat-helmet", "hazmat-chestplate", "hazmat-leggings", "hazmat-boots");

    private final NuclearCraftPlugin plugin;
    private NamespacedKey tierKey;
    private NamespacedKey itemIdKey;

    public UpgradeManager(NuclearCraftPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        tierKey   = new NamespacedKey(plugin, PDC_UPGRADE_TIER);
        itemIdKey = new NamespacedKey(plugin, "nuclearcraft_item_id");
        NCLogger.info("UpgradeManager initialized — " + UPGRADEABLE_ITEM_IDS.size() + " upgradeable items registered.");
    }

    public void shutdown() {}

    // ──────────────────────────────────────────────────────────────────────────
    // Tier read / write
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Returns the current {@link UpgradeTier} of an item, or {@link UpgradeTier#MK_0}
     * if none is stored.
     */
    public UpgradeTier getTier(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return UpgradeTier.MK_0;
        ItemMeta meta = item.getItemMeta();
        if (!meta.getPersistentDataContainer().has(tierKey)) return UpgradeTier.MK_0;
        int level = meta.getPersistentDataContainer().getOrDefault(tierKey, PersistentDataType.INTEGER, 0);
        return UpgradeTier.fromLevel(level);
    }

    /**
     * Returns true if this item carries the NuclearCraft item ID for an upgradeable piece of equipment.
     */
    public boolean isUpgradeable(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        String id = item.getItemMeta().getPersistentDataContainer()
                .get(itemIdKey, PersistentDataType.STRING);
        return id != null && UPGRADEABLE_ITEM_IDS.contains(id);
    }

    /**
     * Returns the item ID stored in the PDC, or null if absent.
     */
    public String getItemId(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer()
                .get(itemIdKey, PersistentDataType.STRING);
    }

    /**
     * Applies the given upgrade tier to the item, stores the PDC tier key,
     * updates lore, and adjusts attribute modifiers accordingly.
     *
     * @return the modified item (same instance, mutated in-place)
     */
    public ItemStack applyTier(ItemStack item, UpgradeTier tier) {
        if (item == null || !item.hasItemMeta()) return item;
        ItemMeta meta = item.getItemMeta();

        // Store tier
        meta.getPersistentDataContainer().set(tierKey, PersistentDataType.INTEGER, tier.getLevel());

        // Update lore
        applyLore(meta, tier);

        // Apply attribute bonuses
        String id = meta.getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
        if (id != null) {
            applyAttributes(meta, id, tier);
        }

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Convenience: applies the NEXT tier to the item.
     * Returns the same item after modification, or the same item unchanged if already MK-IV.
     */
    public ItemStack applyNextTier(ItemStack item) {
        UpgradeTier current = getTier(item);
        UpgradeTier next = current.next();
        if (next == null) return item;
        return applyTier(item, next);
    }

    /**
     * Downgrades the item by one tier (used on MK-IV failure with downgrade proc).
     */
    public ItemStack applyPreviousTier(ItemStack item) {
        UpgradeTier current = getTier(item);
        if (current == UpgradeTier.MK_0) return item;
        UpgradeTier prev = UpgradeTier.fromLevel(current.getLevel() - 1);
        return applyTier(item, prev);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Lore
    // ──────────────────────────────────────────────────────────────────────────

    private void applyLore(ItemMeta meta, UpgradeTier tier) {
        List<Component> lore = new ArrayList<>();

        if (meta.lore() != null) {
            for (Component line : meta.lore()) {
                String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(line);
                // Strip old MK lines
                if (plain.contains("MK-") || plain.contains("Upgrade Tier") || plain.contains("Damage Bonus")
                        || plain.contains("Durability") || plain.contains("Radiation Aura")) continue;
                lore.add(line);
            }
        }

        if (tier != UpgradeTier.MK_0) {
            lore.add(Component.empty());
            lore.add(Component.text("☢ Upgrade Tier: ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(tier.getDisplayName(), NamedTextColor.GREEN, TextDecoration.BOLD)));
            if (tier.getDamageBonusPct() > 0)
                lore.add(Component.text("  ▸ Damage Bonus: +", NamedTextColor.GRAY)
                        .append(Component.text(tier.getDamageBonusPct() + "%", NamedTextColor.GREEN)));
            if (tier.getDurabilityBonusPct() > 0)
                lore.add(Component.text("  ▸ Durability:   +", NamedTextColor.GRAY)
                        .append(Component.text(tier.getDurabilityBonusPct() + "%", NamedTextColor.GREEN)));
            if (tier.getSpeedBonusPct() > 0)
                lore.add(Component.text("  ▸ Speed:        +", NamedTextColor.GRAY)
                        .append(Component.text(tier.getSpeedBonusPct() + "%", NamedTextColor.GREEN)));
            if (tier.getArmorBonusPct() > 0)
                lore.add(Component.text("  ▸ Defense:      +", NamedTextColor.GRAY)
                        .append(Component.text(tier.getArmorBonusPct() + "%", NamedTextColor.GREEN)));
            if (tier.hasRadiationAura())
                lore.add(Component.text("  ☢ Radiation Aura Active", NamedTextColor.GOLD, TextDecoration.BOLD));
        }

        meta.lore(lore);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Attribute modifiers
    // ──────────────────────────────────────────────────────────────────────────

    private void applyAttributes(ItemMeta meta, String itemId, UpgradeTier tier) {
        // Remove existing NuclearCraft upgrade attribute modifiers
        removeUpgradeModifiers(meta);

        if (tier == UpgradeTier.MK_0) return;

        boolean isWeapon = WEAPON_IDS.contains(itemId);
        boolean isTool   = TOOL_IDS.contains(itemId);
        boolean isArmor  = ARMOR_IDS.contains(itemId);

        if (isWeapon || isTool) {
            // Damage bonus
            double damageBonus = tier.getDamageBonusPct() / 10.0; // 5% → +0.5 damage
            addModifier(meta, Attribute.ATTACK_DAMAGE,
                    "nuclearcraft:upgrade_damage_" + tier.getLevel(),
                    damageBonus, AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.MAINHAND);

            // Speed bonus
            double speedBonus = tier.getSpeedBonusPct() / 200.0; // 5% → +0.025 speed
            addModifier(meta, Attribute.ATTACK_SPEED,
                    "nuclearcraft:upgrade_speed_" + tier.getLevel(),
                    speedBonus, AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.MAINHAND);
        }

        if (isArmor) {
            double armorBonus = tier.getArmorBonusPct() / 20.0; // 5% → +0.25 armor
            EquipmentSlotGroup slotGroup = getArmorSlotGroup(itemId);

            addModifier(meta, Attribute.ARMOR,
                    "nuclearcraft:upgrade_armor_" + tier.getLevel(),
                    armorBonus, AttributeModifier.Operation.ADD_NUMBER,
                    slotGroup);

            if (tier.getLevel() >= 2) {
                // MK-II+: minor regeneration via max health bonus as proxy
                addModifier(meta, Attribute.MAX_HEALTH,
                        "nuclearcraft:upgrade_health_" + tier.getLevel(),
                        tier.getLevel() * 0.5, AttributeModifier.Operation.ADD_NUMBER,
                        slotGroup);
            }

            if (tier.getLevel() >= 3) {
                // MK-III+: movement speed
                addModifier(meta, Attribute.MOVEMENT_SPEED,
                        "nuclearcraft:upgrade_movespeed_" + tier.getLevel(),
                        0.005, AttributeModifier.Operation.ADD_NUMBER,
                        slotGroup);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void addModifier(ItemMeta meta, Attribute attribute, String keyString,
                              double amount, AttributeModifier.Operation operation,
                              EquipmentSlotGroup slotGroup) {
        NamespacedKey key = NamespacedKey.fromString(keyString, plugin);
        if (key == null) return;
        AttributeModifier mod = new AttributeModifier(key, amount, operation, slotGroup);
        meta.addAttributeModifier(attribute, mod);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
    }

    private void removeUpgradeModifiers(ItemMeta meta) {
        for (Attribute attr : Attribute.values()) {
            var mods = meta.getAttributeModifiers(attr);
            if (mods == null) continue;
            for (AttributeModifier mod : mods) {
                String keyStr = mod.getKey().getKey();
                if (keyStr.startsWith("nuclearcraft:upgrade_")) {
                    meta.removeAttributeModifier(attr, mod);
                }
            }
        }
    }

    private EquipmentSlotGroup getArmorSlotGroup(String itemId) {
        if (itemId.contains("helmet"))      return EquipmentSlotGroup.HEAD;
        if (itemId.contains("chestplate")) return EquipmentSlotGroup.CHEST;
        if (itemId.contains("leggings"))   return EquipmentSlotGroup.LEGS;
        if (itemId.contains("boots"))      return EquipmentSlotGroup.FEET;
        return EquipmentSlotGroup.ARMOR;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Utility
    // ──────────────────────────────────────────────────────────────────────────

    public boolean isWeapon(String itemId) { return WEAPON_IDS.contains(itemId); }
    public boolean isTool(String itemId)   { return TOOL_IDS.contains(itemId); }
    public boolean isArmor(String itemId)  { return ARMOR_IDS.contains(itemId); }

    public boolean hasAura(ItemStack item) {
        return getTier(item).hasRadiationAura();
    }

    public NamespacedKey getTierKey() { return tierKey; }
}
