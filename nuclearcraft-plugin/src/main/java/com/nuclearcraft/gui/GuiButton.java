package com.nuclearcraft.gui;

import com.nuclearcraft.utils.ColorUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.function.Consumer;

/**
 * Represents a clickable button in a NuclearCraft GUI menu.
 */
public class GuiButton {

    private final ItemStack icon;
    private final Consumer<InventoryClickEvent> clickHandler;
    private boolean closeOnClick;

    private GuiButton(ItemStack icon, Consumer<InventoryClickEvent> clickHandler, boolean closeOnClick) {
        this.icon = icon;
        this.clickHandler = clickHandler;
        this.closeOnClick = closeOnClick;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a simple filler button (no interaction).
     */
    public static GuiButton filler(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtil.parse(" "));
            item.setItemMeta(meta);
        }
        return builder().icon(item).build();
    }

    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (clickHandler != null) {
            clickHandler.accept(event);
        }
        if (closeOnClick && event.getWhoClicked() instanceof Player player) {
            player.closeInventory();
        }
    }

    public ItemStack getIcon() { return icon; }
    public boolean isCloseOnClick() { return closeOnClick; }

    public static class Builder {
        private ItemStack icon;
        private Consumer<InventoryClickEvent> clickHandler;
        private boolean closeOnClick = false;

        public Builder icon(ItemStack icon) {
            this.icon = icon;
            return this;
        }

        public Builder icon(Material material, String displayName, List<String> lore) {
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(ColorUtil.parse(displayName));
                meta.lore(lore.stream()
                        .map(line -> ColorUtil.parse(line)
                                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false))
                        .toList());
                item.setItemMeta(meta);
            }
            this.icon = item;
            return this;
        }

        public Builder onClick(Consumer<InventoryClickEvent> handler) {
            this.clickHandler = handler;
            return this;
        }

        public Builder closeOnClick(boolean close) {
            this.closeOnClick = close;
            return this;
        }

        public GuiButton build() {
            if (icon == null) icon = new ItemStack(Material.BARRIER);
            return new GuiButton(icon, clickHandler, closeOnClick);
        }
    }
}
