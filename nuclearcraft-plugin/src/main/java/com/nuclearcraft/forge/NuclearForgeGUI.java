package com.nuclearcraft.forge;

import com.nuclearcraft.gui.GuiButton;
import com.nuclearcraft.gui.GuiMenu;
import com.nuclearcraft.upgrade.UpgradeManager;
import com.nuclearcraft.upgrade.UpgradeTier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
 * 54-slot Nuclear Forge GUI.
 *
 * Layout (6 rows × 9 columns):
 * <pre>
 * Row 0  [BRD][BRD][BRD][BRD][BRD][BRD][BRD][BRD][BRD]
 * Row 1  [BRD][EQP][BRD][ARW][MAT][ARW][CAT][BRD][BRD]
 * Row 2  [BRD][BRD][BRD][BRD][PRG][BRD][BRD][BRD][BRD]
 * Row 3  [BRD][PRV][BRD][BRD][STS][BRD][BRD][ENR][BRD]
 * Row 4  [BRD][BRD][BRD][BRD][BRD][BRD][BRD][BRD][BRD]
 * Row 5  [BRD][BRD][BTN][BRD][INF][BRD][OUT][BRD][BRD]
 * </pre>
 *
 * BRD=border glass, EQP=equipment input(10), MAT=material(13),
 * CAT=catalyst(16), PRG=progress(22), PRV=preview(28),
 * STS=status(31), ENR=energy(34), BTN=forge button(47),
 * INF=info(49), OUT=output(51)
 */
public class NuclearForgeGUI extends GuiMenu {

    // ── Fixed slot indices ───────────────────────────────────────────────────

    public static final int SLOT_EQUIPMENT = 10;
    public static final int SLOT_MATERIAL  = 13;
    public static final int SLOT_CATALYST  = 16;
    public static final int SLOT_PROGRESS  = 22;
    public static final int SLOT_PREVIEW   = 28;
    public static final int SLOT_STATUS    = 31;
    public static final int SLOT_ENERGY    = 34;
    public static final int SLOT_FORGE_BTN = 47;
    public static final int SLOT_INFO      = 49;
    public static final int SLOT_OUTPUT    = 51;

    /** Slots the player may freely interact with (put/take items). */
    private static final List<Integer> INTERACTIVE_SLOTS = List.of(
            SLOT_EQUIPMENT, SLOT_MATERIAL, SLOT_CATALYST, SLOT_OUTPUT);

    // ─────────────────────────────────────────────────────────────────────────

    private final ForgeData forgeData;
    private final NuclearForgeManager forgeManager;
    private final UpgradeManager upgradeManager;
    private final double maxEnergy;

    public NuclearForgeGUI(String title,
                            ForgeData forgeData,
                            NuclearForgeManager forgeManager,
                            UpgradeManager upgradeManager,
                            double maxEnergy) {
        super(title, 6);
        this.forgeData = forgeData;
        this.forgeManager = forgeManager;
        this.upgradeManager = upgradeManager;
        this.maxEnergy = maxEnergy;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Build
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    protected void build(Inventory inv) {
        // Fill border with black glass
        ItemStack border = borderGlass();
        for (int i = 0; i < 54; i++) {
            if (!INTERACTIVE_SLOTS.contains(i)) {
                inv.setItem(i, border);
            }
        }

        // Arrows between input slots
        inv.setItem(12, arrow());
        inv.setItem(15, arrow());

        // Dynamic display slots
        refreshDisplay(inv);
    }

    /**
     * Updates only the display slots without touching interactive slots.
     * Call this on the tick loop to reflect state changes without flickering.
     */
    public void refreshDisplay(Inventory inv) {
        if (inv == null) return;

        // Progress bar
        inv.setItem(SLOT_PROGRESS, buildProgressItem());

        // Upgrade preview
        ItemStack equipment = inv.getItem(SLOT_EQUIPMENT);
        inv.setItem(SLOT_PREVIEW, buildPreviewItem(equipment));

        // Status
        inv.setItem(SLOT_STATUS, buildStatusItem());

        // Energy meter
        inv.setItem(SLOT_ENERGY, buildEnergyItem());

        // Forge button
        inv.setItem(SLOT_FORGE_BTN, buildForgeButton(equipment, inv.getItem(SLOT_MATERIAL), inv.getItem(SLOT_CATALYST)));

        // Info panel
        inv.setItem(SLOT_INFO, buildInfoItem(equipment));

        // Output (only set if there's a completed result)
        if (forgeData.hasOutput() && isEmpty(inv.getItem(SLOT_OUTPUT))) {
            inv.setItem(SLOT_OUTPUT, forgeData.getOutputItem());
            forgeData.clearOutputItem();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Click handling
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        // Forge button
        if (slot == SLOT_FORGE_BTN) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                forgeManager.tryStartUpgrade(player, forgeData, inventory);
            }
            return;
        }

        // Catalyst (Radioactive Core for energy) — also functional
        if (slot == SLOT_CATALYST) {
            // Allow interaction, forgeManager will validate on upgrade start
            return;
        }

        // Block interaction with upgrade slots while upgrading
        if (forgeData.isUpgrading()) {
            if (slot == SLOT_EQUIPMENT || slot == SLOT_MATERIAL) {
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player p) {
                    p.sendMessage(Component.text("☢ The forge is processing an upgrade!", NamedTextColor.RED));
                }
                return;
            }
        }

        // Interactive slots: allow put/take freely
        if (INTERACTIVE_SLOTS.contains(slot)) {
            return;
        }

        // All other slots: block
        event.setCancelled(true);
    }

    @Override
    public void handleClose(InventoryCloseEvent event) {
        if (forgeData.isUpgrading()) {
            // Don't return input items – they're stored in forgeData
            return;
        }

        Player player = (Player) event.getPlayer();

        // Return any items left in input slots
        returnSlot(player, SLOT_EQUIPMENT);
        returnSlot(player, SLOT_MATERIAL);
        returnSlot(player, SLOT_CATALYST);

        // If there's a completed item in output but player didn't take it, store it
        ItemStack out = inventory.getItem(SLOT_OUTPUT);
        if (!isEmpty(out)) {
            forgeData.setOutputItem(out);
            inventory.setItem(SLOT_OUTPUT, null);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Item builders
    // ──────────────────────────────────────────────────────────────────────────

    private ItemStack buildProgressItem() {
        long currentTick = plugin() == null ? 0 : plugin().getServer().getCurrentTick();
        double progress = forgeData.getUpgradeProgress(currentTick);
        int filled = (int)(progress * 7);

        Material mat = forgeData.isUpgrading()
                ? (filled > 3 ? Material.LIME_STAINED_GLASS_PANE : Material.YELLOW_STAINED_GLASS_PANE)
                : Material.GRAY_STAINED_GLASS_PANE;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        String pct = forgeData.isUpgrading() ? String.format("%.0f%%", progress * 100) : "0%";
        meta.displayName(Component.text("Upgrade Progress: " + pct, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        if (forgeData.isUpgrading() && forgeData.getPendingTargetTier() != null) {
            lore.add(Component.text("Upgrading to: " + forgeData.getPendingTargetTier().getDisplayName(),
                    NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.empty());
        // Progress bar characters
        StringBuilder bar = new StringBuilder("§a");
        for (int i = 0; i < 7; i++) {
            bar.append(i < filled ? "█" : "§7█");
        }
        lore.add(Component.text(bar.toString()).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildPreviewItem(ItemStack equipment) {
        if (isEmpty(equipment) || !upgradeManager.isUpgradeable(equipment)) {
            return namedItem(Material.GRAY_STAINED_GLASS_PANE, "§7Upgrade Preview",
                    List.of("§8Place equipment in the", "§8left input slot to preview."));
        }

        UpgradeTier current = upgradeManager.getTier(equipment);
        UpgradeTier next = current.next();
        if (next == null) {
            return namedItem(Material.LIME_STAINED_GLASS_PANE, "§a§lMax Tier Reached",
                    List.of("§7This item is already at §aMK-IV§7.", "§7No further upgrades available."));
        }

        // Clone item and apply next tier for preview (but don't modify the original)
        ItemStack preview = equipment.clone();
        upgradeManager.applyTier(preview, next);

        // Add a note that this is a preview
        ItemMeta meta = preview.getItemMeta();
        if (meta != null) {
            List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
            lore.add(Component.empty());
            lore.add(Component.text("[ PREVIEW — not the real item ]", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, true));
            meta.lore(lore);
            preview.setItemMeta(meta);
        }
        return preview;
    }

    private ItemStack buildStatusItem() {
        Material mat = switch (forgeData.getState()) {
            case OFFLINE   -> Material.RED_STAINED_GLASS_PANE;
            case READY     -> Material.GREEN_STAINED_GLASS_PANE;
            case UPGRADING -> Material.YELLOW_STAINED_GLASS_PANE;
            case COMPLETED -> Material.BLUE_STAINED_GLASS_PANE;
            case ERROR     -> Material.ORANGE_STAINED_GLASS_PANE;
            case OVERLOADED-> Material.PURPLE_STAINED_GLASS_PANE;
        };
        List<String> lore = new ArrayList<>();
        lore.add("§7State: " + forgeData.getState().displayName());
        lore.add("");
        lore.add("§7Upgrades Done: §f" + forgeData.getTotalForgeUses());
        lore.add("§7Successes: §a" + forgeData.getSuccessfulUpgrades());
        lore.add("§7Failures:  §c" + forgeData.getFailedUpgrades());
        lore.add("§7MK-IV Made: §6" + forgeData.getMk4Creations());
        return namedItem(mat, forgeData.getState().displayName(), convertLore(lore));
    }

    private ItemStack buildEnergyItem() {
        double pct = forgeData.getEnergyPercent(maxEnergy);
        int bars = (int)(pct * 10);

        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            if (i < bars) bar.append(pct > 0.75 ? "§c" : pct > 0.4 ? "§e" : "§a").append("█");
            else bar.append("§8█");
        }

        Material mat = pct > 0.9 ? Material.RED_STAINED_GLASS_PANE
                : pct > 0.5 ? Material.YELLOW_STAINED_GLASS_PANE
                : Material.GREEN_STAINED_GLASS_PANE;

        List<String> lore = List.of(
                "§7Energy: §f" + (int) forgeData.getEnergy() + " / " + (int) maxEnergy,
                bar.toString(),
                "",
                "§8Insert Radioactive Cores",
                "§8into the Catalyst slot to",
                "§8charge the forge.");

        return namedItem(mat, "§e☢ Forge Energy", convertLore(lore));
    }

    private ItemStack buildForgeButton(ItemStack equipment, ItemStack material, ItemStack catalyst) {
        boolean canUpgrade = !isEmpty(equipment)
                && upgradeManager.isUpgradeable(equipment)
                && upgradeManager.getTier(equipment).next() != null;

        if (forgeData.isUpgrading()) {
            return namedItem(Material.ORANGE_STAINED_GLASS_PANE, "§e⚙ Upgrading...",
                    List.of("§7Please wait for the", "§7current upgrade to finish."));
        }

        if (forgeData.getState() == ForgeState.OVERLOADED) {
            return namedItem(Material.PURPLE_STAINED_GLASS_PANE, "§4☢ OVERLOADED",
                    List.of("§cThe forge has overloaded!", "§7Please wait for it to cool down."));
        }

        if (forgeData.getState() == ForgeState.COMPLETED) {
            return namedItem(Material.BLUE_STAINED_GLASS_PANE, "§b✔ Collect Output",
                    List.of("§7Take your upgraded item", "§7from the output slot."));
        }

        if (!canUpgrade) {
            return namedItem(Material.RED_STAINED_GLASS_PANE, "§c✘ Cannot Upgrade",
                    List.of("§7Place upgradeable equipment", "§7in the input slot."));
        }

        UpgradeTier current = upgradeManager.getTier(equipment);
        UpgradeTier next = current.next();
        List<String> lore = new ArrayList<>();
        lore.add("§7Current Tier: §f" + current.getDisplayName());
        lore.add("§7Target Tier:  §a" + next.getDisplayName());
        lore.add("");
        lore.add("§7Materials needed:");
        lore.add("§8  Plutonium Ingots: §f" + next.getIngotsRequired());
        lore.add("§8  Radioactive Cores: §f" + next.getCoresRequired());
        if (next.getHeartsRequired() > 0)
            lore.add("§8  Irradiated Hearts: §f" + next.getHeartsRequired());
        lore.add("");
        lore.add("§7Success Chance: §e" + next.getSuccessChance() + "%");
        lore.add("§7Energy Cost:   §e" + next.getEnergyCost());
        lore.add("");
        lore.add("§a▶ Click to Start Upgrade");

        return namedItem(Material.LIME_STAINED_GLASS_PANE, "§a§l⚙ START UPGRADE", convertLore(lore));
    }

    private ItemStack buildInfoItem(ItemStack equipment) {
        if (isEmpty(equipment) || !upgradeManager.isUpgradeable(equipment)) {
            return namedItem(Material.BOOK, "§b☢ Nuclear Forge",
                    List.of("§7Place Plutonium or Hazmat", "§7equipment in the input slot.",
                            "", "§7Add upgrade materials and", "§7fuel (Radioactive Cores)",
                            "§7in the material slots.", "",
                            "§7Then click §aStart Upgrade§7."));
        }

        UpgradeTier tier = upgradeManager.getTier(equipment);
        UpgradeTier next = tier.next();
        List<String> lore = new ArrayList<>();
        lore.add("§7Current: §f" + tier.getDisplayName());
        lore.add("§7Next:    §a" + (next != null ? next.getDisplayName() : "§6MAX"));
        lore.add("");
        lore.add("§7Item ID: §8" + upgradeManager.getItemId(equipment));
        lore.add("§7Upgrades done: §f" + forgeData.getTotalForgeUses());
        if (tier.hasRadiationAura())
            lore.add("§6☢ Radiation Aura active");
        return namedItem(Material.BOOK, "§b☢ Equipment Info", convertLore(lore));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helper builders
    // ──────────────────────────────────────────────────────────────────────────

    private ItemStack borderGlass() {
        ItemStack g = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta m = g.getItemMeta();
        m.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
        g.setItemMeta(m);
        return g;
    }

    private ItemStack arrow() {
        return namedItem(Material.ARROW, "§7→", List.of());
    }

    private ItemStack namedItem(Material mat, String name, List<String> loreParts) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        // Deserialize legacy color codes (§ or &) into Adventure components
        var serial = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection();
        meta.displayName(serial.deserialize(name).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        for (String s : loreParts) {
            lore.add(serial.deserialize(s).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private List<String> convertLore(List<String> raw) { return raw; }

    private boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir();
    }

    private void returnSlot(Player player, int slot) {
        ItemStack item = inventory.getItem(slot);
        if (!isEmpty(item)) {
            player.getInventory().addItem(item.clone()).forEach((k, v) ->
                    player.getWorld().dropItemNaturally(player.getLocation(), v));
            inventory.setItem(slot, null);
        }
    }

    /** Lazy plugin reference via forgeManager. */
    private org.bukkit.plugin.java.JavaPlugin plugin() {
        return forgeManager.getPlugin();
    }
}
