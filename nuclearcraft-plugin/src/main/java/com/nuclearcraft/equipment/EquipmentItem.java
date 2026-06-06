package com.nuclearcraft.equipment;

import com.nuclearcraft.items.CustomItem;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Extension of {@link CustomItem} that supports:
 * <ul>
 *   <li>Custom attribute modifiers (damage, speed, armor, toughness, etc.)</li>
 *   <li>Custom maximum durability</li>
 *   <li>Leather armor color tinting</li>
 * </ul>
 *
 * All custom modifiers are applied on top of the item meta produced by the parent class.
 */
public class EquipmentItem extends CustomItem {

    private final List<AttributeEntry> attributeEntries = new ArrayList<>();
    private final int maxDurability;   // 0 = use material default
    private final Color leatherColor;  // null = no tint

    public EquipmentItem(JavaPlugin plugin, String id, String displayName, List<String> lore,
                         Material material, int customModelData, boolean glowing,
                         int maxDurability, Color leatherColor) {
        super(plugin, id, displayName, lore, material, customModelData, glowing);
        this.maxDurability = maxDurability;
        this.leatherColor  = leatherColor;
    }

    /** Convenience constructor without color (tools, netherite armor). */
    public EquipmentItem(JavaPlugin plugin, String id, String displayName, List<String> lore,
                         Material material, int customModelData, boolean glowing, int maxDurability) {
        this(plugin, id, displayName, lore, material, customModelData, glowing, maxDurability, null);
    }

    // ── Fluent builder ────────────────────────────────────────────────────────

    /**
     * Adds an attribute modifier to be applied when the item is built.
     *
     * @param attribute the attribute to modify
     * @param key       namespaced key that uniquely identifies this modifier
     * @param value     modifier value (e.g. +10 for attack damage)
     * @param operation how the value is applied
     * @param slot      equipment slot group the modifier is active for
     * @return {@code this} for chaining
     */
    public EquipmentItem addAttribute(Attribute attribute, NamespacedKey key,
                                      double value, AttributeModifier.Operation operation,
                                      EquipmentSlotGroup slot) {
        attributeEntries.add(new AttributeEntry(attribute,
                new AttributeModifier(key, value, operation, slot)));
        return this;
    }

    // ── ItemStack construction ─────────────────────────────────────────────────

    @Override
    public ItemStack build(int amount) {
        ItemStack item = super.build(amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // Remove default attribute modifiers so our custom ones are the only values shown
        meta.setAttributeModifiers(com.google.common.collect.ArrayListMultimap.create());

        // Apply custom attribute modifiers
        for (AttributeEntry entry : attributeEntries) {
            meta.addAttributeModifier(entry.attribute(), entry.modifier());
        }

        // Custom durability
        if (maxDurability > 0 && meta instanceof Damageable damageable) {
            damageable.setMaxDamage(maxDurability);
        }

        // Leather armor tint
        if (leatherColor != null && meta instanceof LeatherArmorMeta lam) {
            lam.setColor(leatherColor);
        }

        item.setItemMeta(meta);
        return item;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public int getMaxDurability()  { return maxDurability; }
    public Color getLeatherColor() { return leatherColor; }

    // ── Private record ────────────────────────────────────────────────────────

    private record AttributeEntry(Attribute attribute, AttributeModifier modifier) {}
}
