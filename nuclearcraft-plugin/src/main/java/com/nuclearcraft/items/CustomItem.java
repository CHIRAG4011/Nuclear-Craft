package com.nuclearcraft.items;

import com.nuclearcraft.utils.ColorUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.EquippableComponent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Represents a NuclearCraft custom item.
 * All custom items carry a NamespacedKey identifier in their PersistentDataContainer.
 */
public class CustomItem {

    public static final String PDC_KEY_ID = "nuclearcraft_item_id";

    private final String id;
    private final String displayName;
    private final List<String> lore;
    private final Material material;
    private final int customModelData;
    private final boolean glowing;

    private final NamespacedKey namespacedKey;
    private NamespacedKey equippableModelKey = null;

    public CustomItem(JavaPlugin plugin, String id, String displayName, List<String> lore,
                      Material material, int customModelData, boolean glowing) {
        this.id = id;
        this.displayName = displayName;
        this.lore = lore;
        this.material = material;
        this.customModelData = customModelData;
        this.glowing = glowing;
        this.namespacedKey = new NamespacedKey(plugin, id);
    }

    /**
     * Sets a custom equipment model key for this item's worn armor texture.
     * The key maps to assets/&lt;namespace&gt;/equipment/&lt;name&gt;.json in the resource pack.
     * Must be called before {@link #build(int)}.
     *
     * @return {@code this} for chaining
     */
    public CustomItem withEquippableModel(NamespacedKey key) {
        this.equippableModelKey = key;
        return this;
    }

    /**
     * Creates an ItemStack with all custom meta applied.
     */
    public ItemStack build() {
        return build(1);
    }

    public ItemStack build(int amount) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(ColorUtil.parse(displayName));
        meta.lore(lore.stream()
                .map(line -> ColorUtil.parse("<gray>" + line + "</gray>").decoration(
                        net.kyori.adventure.text.format.TextDecoration.ITALIC, false))
                .toList());

        if (customModelData > 0) {
            meta.setCustomModelData(customModelData);
        }

        if (equippableModelKey != null && meta.hasEquippable()) {
            EquippableComponent eq = meta.getEquippable();
            if (eq != null) {
                eq.setModel(equippableModelKey);
                meta.setEquippable(eq);
            }
        }

        meta.getPersistentDataContainer()
                .set(new NamespacedKey(namespacedKey.namespace(), PDC_KEY_ID),
                        PersistentDataType.STRING, id);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Checks if the given ItemStack is this custom item.
     */
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != material) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        String stored = meta.getPersistentDataContainer()
                .get(new NamespacedKey(namespacedKey.namespace(), PDC_KEY_ID), PersistentDataType.STRING);
        return id.equals(stored);
    }

    /**
     * Retrieves the NuclearCraft item ID from an ItemStack, or null if it's not a custom item.
     */
    public static String getId(JavaPlugin plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer()
                .get(new NamespacedKey(plugin, PDC_KEY_ID), PersistentDataType.STRING);
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public List<String> getLore() { return lore; }
    public Material getMaterial() { return material; }
    public int getCustomModelData() { return customModelData; }
    public boolean isGlowing() { return glowing; }
    public NamespacedKey getNamespacedKey() { return namespacedKey; }

    @Override
    public String toString() {
        return "CustomItem{id='" + id + "', material=" + material + "}";
    }
}
