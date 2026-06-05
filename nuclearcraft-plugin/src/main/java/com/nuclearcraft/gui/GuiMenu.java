package com.nuclearcraft.gui;

import com.nuclearcraft.utils.ColorUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for all NuclearCraft GUI menus.
 * Subclass this to implement specific menus (smelter, forge, boss rewards, etc.)
 */
public abstract class GuiMenu implements InventoryHolder {

    protected final String title;
    protected final int rows;
    protected Inventory inventory;
    protected final Map<Integer, GuiButton> buttons = new HashMap<>();

    protected int currentPage = 0;
    protected int totalPages = 1;

    protected GuiMenu(String title, int rows) {
        this.title = title;
        this.rows = Math.max(1, Math.min(6, rows));
    }

    /**
     * Builds the initial inventory and populates all buttons.
     * Must be called before opening.
     */
    public final void build() {
        Component titleComponent = ColorUtil.parse(title);
        inventory = Bukkit.createInventory(this, rows * 9, titleComponent);
        build(inventory);
        renderButtons();
    }

    /**
     * Override this to add buttons via addButton().
     */
    protected abstract void build(Inventory inventory);

    public final void open(Player player) {
        build();
        player.openInventory(inventory);
    }

    public final void refresh() {
        if (inventory == null) return;
        inventory.clear();
        buttons.clear();
        build(inventory);
        renderButtons();
    }

    protected void addButton(int slot, GuiButton button) {
        if (slot < 0 || slot >= rows * 9) return;
        buttons.put(slot, button);
    }

    protected void fillBorder(GuiButton filler) {
        int size = rows * 9;
        for (int i = 0; i < 9; i++) addButton(i, filler);
        for (int i = size - 9; i < size; i++) addButton(i, filler);
        for (int i = 0; i < rows; i++) {
            addButton(i * 9, filler);
            addButton(i * 9 + 8, filler);
        }
    }

    private void renderButtons() {
        buttons.forEach((slot, button) -> {
            if (slot < inventory.getSize()) {
                inventory.setItem(slot, button.getIcon());
            }
        });
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        GuiButton button = buttons.get(slot);
        if (button != null) {
            button.onClick(event);
        }
    }

    public void handleClose(InventoryCloseEvent event) {}

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public String getTitle() { return title; }
    public int getRows() { return rows; }
    public int getCurrentPage() { return currentPage; }
    public int getTotalPages() { return totalPages; }

    public boolean hasNextPage() { return currentPage < totalPages - 1; }
    public boolean hasPreviousPage() { return currentPage > 0; }

    public void nextPage(Player player) {
        if (hasNextPage()) {
            currentPage++;
            open(player);
        }
    }

    public void previousPage(Player player) {
        if (hasPreviousPage()) {
            currentPage--;
            open(player);
        }
    }
}
