package com.nuclearcraft.utils;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

/**
 * Utility methods for building and inspecting ItemStacks.
 */
public final class ItemUtil {

    private ItemUtil() {}

    /**
     * Quickly builds an ItemStack with a display name and lore lines.
     */
    public static ItemStack build(Material material, String displayName, String... loreMiniMessage) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(ColorUtil.parse(displayName)
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));

        if (loreMiniMessage.length > 0) {
            meta.lore(Arrays.stream(loreMiniMessage)
                    .map(line -> ColorUtil.parse(line)
                            .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false))
                    .toList());
        }

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates a named item with custom model data.
     */
    public static ItemStack build(Material material, String displayName, int customModelData, String... lore) {
        ItemStack item = build(material, displayName, lore);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(customModelData);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Adds a glow effect (Unbreaking I + HIDE_ENCHANTS flag).
     */
    public static ItemStack glow(ItemStack item) {
        ItemStack copy = item.clone();
        ItemMeta meta = copy.getItemMeta();
        if (meta == null) return copy;
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        copy.setItemMeta(meta);
        return copy;
    }

    /**
     * Returns true if the item is non-null and not AIR.
     */
    public static boolean isValid(ItemStack item) {
        return item != null && item.getType() != Material.AIR && item.getAmount() > 0;
    }

    /**
     * Checks if the item's display name (plain text) matches the given string.
     */
    public static boolean hasDisplayName(ItemStack item, String name) {
        if (!isValid(item) || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        String plain = ColorUtil.stripFormatting(ColorUtil.serialize(meta.displayName()));
        return plain.equals(name);
    }

    /**
     * Returns a human-readable string for logging.
     */
    public static String describe(ItemStack item) {
        if (item == null) return "null";
        if (item.getType() == Material.AIR) return "AIR";
        return item.getAmount() + "x " + item.getType().name();
    }
}
