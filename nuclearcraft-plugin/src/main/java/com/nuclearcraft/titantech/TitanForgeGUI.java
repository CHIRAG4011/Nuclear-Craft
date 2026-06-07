package com.nuclearcraft.titantech;

import com.nuclearcraft.gui.GuiMenu;
import com.nuclearcraft.items.ItemManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 54-slot Titan Reactor Forge GUI.
 *
 * Layout:
 *  Row 0: border
 *  Row 1: [BRD][MAT1][→][MAT2][→][CATALYST][BRD][BRD][BRD]
 *  Row 2: [BRD][BRD][BRD][BRD][PROGRESS][BRD][BRD][BRD][BRD]
 *  Row 3: [BRD][RECIPE][BRD][BRD][STATUS][BRD][BRD][ENERGY][BRD]
 *  Row 4: border
 *  Row 5: [BRD][BRD][CRAFT_BTN][BRD][INFO][BRD][OUTPUT][BRD][BRD]
 */
public class TitanForgeGUI extends GuiMenu {

    public static final int SLOT_MAT1     = 10;
    public static final int SLOT_MAT2     = 13;
    public static final int SLOT_CATALYST = 16;
    public static final int SLOT_PROGRESS = 22;
    public static final int SLOT_RECIPE   = 28;
    public static final int SLOT_STATUS   = 31;
    public static final int SLOT_ENERGY   = 34;
    public static final int SLOT_CRAFT_BTN= 47;
    public static final int SLOT_INFO     = 49;
    public static final int SLOT_OUTPUT   = 51;

    private static final List<Integer> INTERACTIVE_SLOTS =
            List.of(SLOT_MAT1, SLOT_MAT2, SLOT_CATALYST, SLOT_OUTPUT);

    private final TitanForgeData forgeData;
    private final TitanForgeManager forgeManager;
    private final TitanForgeRecipeManager recipeManager;

    private int selectedRecipeIndex = 0;

    public TitanForgeGUI(String title, TitanForgeData forgeData,
                          TitanForgeManager forgeManager,
                          TitanForgeRecipeManager recipeManager,
                          ItemManager itemManager, double maxEnergy) {
        super(title, 6);
        this.forgeData = forgeData;
        this.forgeManager = forgeManager;
        this.recipeManager = recipeManager;
    }

    @Override
    protected void build(Inventory inv) {
        ItemStack border = borderGlass();
        for (int i = 0; i < 54; i++) {
            if (!INTERACTIVE_SLOTS.contains(i)) inv.setItem(i, border);
        }
        inv.setItem(12, arrow());
        inv.setItem(15, arrow());
        refreshDisplay(inv);
    }

    public void refreshDisplay(Inventory inv) {
        if (inv == null) return;
        inv.setItem(SLOT_PROGRESS,  buildProgressItem());
        inv.setItem(SLOT_RECIPE,    buildRecipeItem());
        inv.setItem(SLOT_STATUS,    buildStatusItem());
        inv.setItem(SLOT_ENERGY,    buildEnergyItem(10000.0));
        inv.setItem(SLOT_CRAFT_BTN, buildCraftButton());
        inv.setItem(SLOT_INFO,      buildInfoItem());
        if (forgeData.hasOutput() && isEmpty(inv.getItem(SLOT_OUTPUT))) {
            inv.setItem(SLOT_OUTPUT, forgeData.getPendingOutput());
            forgeData.clearOutput();
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        if (slot == SLOT_CRAFT_BTN) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player p) {
                TitanForgeRecipe selected = getSelectedRecipe();
                if (selected != null) {
                    forgeManager.tryStartCraft(p, forgeData, inventory, selected);
                } else {
                    p.sendMessage("§c☢ Select a recipe first — click the recipe slot to cycle.");
                }
            }
            return;
        }

        if (slot == SLOT_RECIPE) {
            event.setCancelled(true);
            List<TitanForgeRecipe> all = recipeManager.getAllRecipes();
            if (!all.isEmpty()) {
                if (event.isRightClick()) {
                    selectedRecipeIndex = (selectedRecipeIndex - 1 + all.size()) % all.size();
                } else {
                    selectedRecipeIndex = (selectedRecipeIndex + 1) % all.size();
                }
                refreshDisplay(inventory);
            }
            return;
        }

        if (forgeData.isCrafting()) {
            if (slot == SLOT_MAT1 || slot == SLOT_MAT2) {
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player p) {
                    p.sendMessage(Component.text("☢ Titan Forge is crafting — cannot remove materials!", NamedTextColor.RED));
                }
                return;
            }
        }

        if (INTERACTIVE_SLOTS.contains(slot)) return;
        event.setCancelled(true);
    }

    @Override
    public void handleClose(InventoryCloseEvent event) {
        if (forgeData.isCrafting()) return;
        Player player = (Player) event.getPlayer();
        returnSlot(player, SLOT_MAT1);
        returnSlot(player, SLOT_MAT2);
        returnSlot(player, SLOT_CATALYST);
        ItemStack out = inventory.getItem(SLOT_OUTPUT);
        if (!isEmpty(out)) {
            forgeData.setPendingOutput(out);
            forgeData.setHasOutput(true);
            inventory.setItem(SLOT_OUTPUT, null);
        }
    }

    // ── Item builders ────────────────────────────────────────────────────────

    private ItemStack buildProgressItem() {
        long tick = forgeManager.getPlugin().getServer().getCurrentTick();
        double progress = forgeData.getCraftProgress(tick);
        int filled = (int)(progress * 7);

        Material mat = forgeData.isCrafting()
                ? (filled > 4 ? Material.PURPLE_STAINED_GLASS_PANE : Material.MAGENTA_STAINED_GLASS_PANE)
                : Material.GRAY_STAINED_GLASS_PANE;

        String pct = forgeData.isCrafting() ? String.format("%.0f%%", progress * 100) : "0%";
        List<String> lore = new ArrayList<>();
        if (forgeData.isCrafting() && forgeData.getActiveRecipe() != null) {
            lore.add("§7Crafting: " + forgeData.getActiveRecipe().getDisplayName());
        }
        lore.add("");
        StringBuilder bar = new StringBuilder("§5");
        for (int i = 0; i < 7; i++) bar.append(i < filled ? "█" : "§8█");
        lore.add(bar.toString());
        return namedItem(mat, "§5Crafting Progress: " + pct, lore);
    }

    private ItemStack buildRecipeItem() {
        List<TitanForgeRecipe> all = recipeManager.getAllRecipes();
        if (all.isEmpty()) return namedItem(Material.BARRIER, "§cNo Recipes", List.of("§7No titan recipes loaded."));
        if (selectedRecipeIndex >= all.size()) selectedRecipeIndex = 0;
        TitanForgeRecipe r = all.get(selectedRecipeIndex);

        List<String> lore = new ArrayList<>();
        lore.add("§7Recipe " + (selectedRecipeIndex + 1) + " of " + all.size());
        lore.add("§8───────────────────");
        lore.add("§7Material 1: §f" + r.getMaterial1Id() + " §8x" + r.getMaterial1Amount());
        lore.add("§7Material 2: §f" + r.getMaterial2Id() + " §8x" + r.getMaterial2Amount());
        lore.add("§7Titan Cores (Catalyst): §f" + r.getCoresRequired());
        lore.add("§7Success: §e" + r.getSuccessChance() + "%");
        lore.add("");
        lore.add("§bLeft-click: Next  §bRight-click: Prev");
        return namedItem(Material.NETHER_STAR, r.getDisplayName(), lore);
    }

    private ItemStack buildStatusItem() {
        Material mat = switch (forgeData.getState()) {
            case OFFLINE    -> Material.RED_STAINED_GLASS_PANE;
            case READY      -> Material.LIME_STAINED_GLASS_PANE;
            case CRAFTING   -> Material.PURPLE_STAINED_GLASS_PANE;
            case POWERED    -> Material.CYAN_STAINED_GLASS_PANE;
            case OVERLOADED -> Material.MAGENTA_STAINED_GLASS_PANE;
            case ERROR      -> Material.ORANGE_STAINED_GLASS_PANE;
        };
        return namedItem(mat, forgeData.getState().displayName(), List.of(
                "§7Crafts: §f" + forgeData.getTotalCrafts(),
                "§7Success: §a" + forgeData.getSuccessfulCrafts(),
                "§7Failed: §c" + forgeData.getFailedCrafts(),
                "§7Overloads: §4" + forgeData.getOverloads()));
    }

    private ItemStack buildEnergyItem(double maxEnergy) {
        double pct = forgeData.getEnergyPercent(maxEnergy);
        int bars = (int)(pct * 10);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            if (i < bars) bar.append(pct > 0.8 ? "§c" : pct > 0.4 ? "§e" : "§5").append("█");
            else bar.append("§8█");
        }
        Material mat = pct > 0.9 ? Material.RED_STAINED_GLASS_PANE
                : pct > 0.5 ? Material.YELLOW_STAINED_GLASS_PANE
                : Material.PURPLE_STAINED_GLASS_PANE;
        return namedItem(mat, "§5☢ Reactor Energy", List.of(
                "§7Energy: §f" + (int)forgeData.getEnergy() + " / " + (int)maxEnergy,
                bar.toString(),
                "",
                "§8Insert Titan Cores into",
                "§8the Catalyst slot to charge."));
    }

    private ItemStack buildCraftButton() {
        if (forgeData.isCrafting()) {
            return namedItem(Material.PURPLE_STAINED_GLASS_PANE, "§5⚙ Crafting...",
                    List.of("§7Please wait for completion."));
        }
        if (forgeData.getState() == TitanForgeState.OVERLOADED) {
            return namedItem(Material.MAGENTA_STAINED_GLASS_PANE, "§4☢ OVERLOADED",
                    List.of("§cWait for the forge to stabilize."));
        }
        if (forgeData.hasOutput()) {
            return namedItem(Material.CYAN_STAINED_GLASS_PANE, "§b✔ Collect Output",
                    List.of("§7Take your item from the output slot."));
        }
        List<TitanForgeRecipe> all = recipeManager.getAllRecipes();
        if (all.isEmpty()) return namedItem(Material.RED_STAINED_GLASS_PANE, "§c✘ No Recipes", List.of());
        TitanForgeRecipe r = all.get(Math.min(selectedRecipeIndex, all.size() - 1));
        return namedItem(Material.PURPLE_STAINED_GLASS_PANE, "§5§l⚙ START CRAFT", List.of(
                "§7Crafting: " + r.getDisplayName(),
                "",
                "§7Required:",
                "  §f" + r.getMaterial1Id() + " §8x" + r.getMaterial1Amount(),
                "  §f" + r.getMaterial2Id() + " §8x" + r.getMaterial2Amount(),
                "  §fTitan Cores §8x" + r.getCoresRequired(),
                "",
                "§7Success: §e" + r.getSuccessChance() + "%",
                "",
                "§5▶ Click to Start Crafting"));
    }

    private ItemStack buildInfoItem() {
        return namedItem(Material.BOOK, "§5☢ Titan Reactor Forge", List.of(
                "§7Forge legendary Titan equipment",
                "§7from Titan Fragments and Cores.",
                "",
                "§8Slot MAT1: Primary material",
                "§8Slot MAT2: Secondary material",
                "§8Slot CATALYST: Titan Cores for energy",
                "",
                "§bCycle recipes with the selector.",
                "§5Click 'Start Craft' to begin."));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private ItemStack borderGlass() {
        ItemStack g = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta m = g.getItemMeta();
        m.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
        g.setItemMeta(m);
        return g;
    }

    private ItemStack arrow() {
        return namedItem(Material.ARROW, "§7→", List.of());
    }

    private ItemStack namedItem(Material mat, String name, List<String> loreStr) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        var serial = LegacyComponentSerializer.legacySection();
        meta.displayName(serial.deserialize(name).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        for (String s : loreStr) lore.add(serial.deserialize(s).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private boolean isEmpty(ItemStack i) { return i == null || i.getType().isAir(); }

    private void returnSlot(Player player, int slot) {
        ItemStack item = inventory.getItem(slot);
        if (!isEmpty(item)) {
            player.getInventory().addItem(item.clone()).forEach((k, v) ->
                    player.getWorld().dropItemNaturally(player.getLocation(), v));
            inventory.setItem(slot, null);
        }
    }

    public TitanForgeRecipe getSelectedRecipe() {
        List<TitanForgeRecipe> all = recipeManager.getAllRecipes();
        if (all.isEmpty()) return null;
        if (selectedRecipeIndex >= all.size()) selectedRecipeIndex = 0;
        return all.get(selectedRecipeIndex);
    }
}
