package com.nuclearcraft.smelter;

import com.nuclearcraft.utils.ColorUtil;
import com.nuclearcraft.utils.NCLogger;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.UUID;

/**
 * Per-machine state for a Nuclear Smelter.
 *
 * Implements {@link InventoryHolder} so the machine's own inventory can be opened
 * directly via {@code player.openInventory(smelterData.getInventory())}.
 * The GUI listener identifies smelter inventories via instanceof checks on the holder.
 *
 * Slot layout (54 slots, 6 rows × 9):
 * <pre>
 *  Row 0: [G ][G ][G ][G ][STA][G ][G ][G ][G ]
 *  Row 1: [G ][G ][IN ][G ][TMP][G ][OUT][G ][G ]
 *  Row 2: [G ][G ][G ][A1 ][A2 ][A3 ][G ][G ][G ]
 *  Row 3: [G ][G ][FUL][G ][FBR][G ][RAD][G ][G ]
 *  Row 4: [G ][G ][G ][G ][G  ][G ][G ][G ][G ]
 *  Row 5: [G ][G ][G ][G ][G  ][G ][G ][G ][G ]
 * </pre>
 *
 * Functional slots (interaction allowed):
 *   INPUT  = 11
 *   OUTPUT = 15
 *   FUEL   = 29
 *
 * All other slots are display/border; clicks are cancelled.
 */
public class SmelterData implements InventoryHolder {

    public static final int INPUT_SLOT  = 11;
    public static final int OUTPUT_SLOT = 15;
    public static final int FUEL_SLOT   = 29;

    private static final int STATUS_SLOT      = 4;
    private static final int TEMP_SLOT        = 13;
    private static final int PROGRESS_SLOT_1  = 21;
    private static final int PROGRESS_SLOT_2  = 22;
    private static final int PROGRESS_SLOT_3  = 23;
    private static final int FUEL_BAR_SLOT    = 31;
    private static final int RADIATION_SLOT   = 33;

    private static final int INVENTORY_SIZE   = 54;

    private final String locationKey;
    private final Location location;

    private SmelterState state;
    private double temperature;
    private int fuelRemaining;
    private int currentRecipeTotalTicks;
    private int progressTicks;
    private String currentRecipeId;

    private boolean dirty;
    private long overheatedUntilMs;
    private UUID lastInteractingPlayerUuid;

    private final Inventory inventory;

    public SmelterData(Location location) {
        this.locationKey = serializeLocation(location);
        this.location = location.clone();
        this.state = SmelterState.OFFLINE;
        this.temperature = 20.0;
        this.fuelRemaining = 0;
        this.progressTicks = 0;
        this.currentRecipeTotalTicks = 0;
        this.currentRecipeId = null;
        this.dirty = false;
        this.overheatedUntilMs = 0L;

        this.inventory = Bukkit.createInventory(this, INVENTORY_SIZE,
                ColorUtil.parse("<dark_gray>☢ <gradient:#39ff14:#00bfff>Nuclear Smelter</gradient> ☢</dark_gray>"));
        buildStaticDisplay();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // InventoryHolder
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Display helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Builds the static border/filler panes. Called once on construction.
     * Dynamic display items are updated each tick via {@link #refreshDisplay}.
     */
    private void buildStaticDisplay() {
        ItemStack border = makeDisplay(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            if (!isFunctional(i)) {
                inventory.setItem(i, border);
            }
        }
        // Label the input slot
        inventory.setItem(10, makeDisplay(Material.LIME_STAINED_GLASS_PANE,
                "<green>Input</green>",
                List.of("<gray>Place Raw Plutonium Fragments here</gray>")));
        // Label the output slot
        inventory.setItem(16, makeDisplay(Material.LIME_STAINED_GLASS_PANE,
                "<aqua>Output</aqua>",
                List.of("<gray>Refined Plutonium Ingots appear here</gray>")));
        // Label the fuel slot
        inventory.setItem(28, makeDisplay(Material.ORANGE_STAINED_GLASS_PANE,
                "<gold>Fuel</gold>",
                List.of("<gray>Coal, Blaze Rod, or Lava Bucket</gray>")));
        // Initial dynamic updates
        refreshDisplay();
    }

    /**
     * Updates all dynamic display items to reflect current machine state.
     * Called each machine tick and when a player opens the GUI.
     */
    public void refreshDisplay() {
        refreshStatus();
        refreshTemperature();
        refreshProgress();
        refreshFuelBar();
        refreshRadiation();
    }

    private void refreshStatus() {
        Material mat = switch (state) {
            case ACTIVE     -> Material.LIME_STAINED_GLASS_PANE;
            case HEATING    -> Material.YELLOW_STAINED_GLASS_PANE;
            case COOLING    -> Material.LIGHT_BLUE_STAINED_GLASS_PANE;
            case OVERHEATED -> Material.RED_STAINED_GLASS_PANE;
            case ERROR      -> Material.PURPLE_STAINED_GLASS_PANE;
            default         -> Material.GRAY_STAINED_GLASS_PANE;
        };
        inventory.setItem(STATUS_SLOT, makeDisplay(mat,
                "<" + state.getColor() + ">" + state.getDisplayName() + "</" + state.getColor() + ">",
                List.of(
                        "<gray>" + state.getDescription() + "</gray>",
                        "<dark_gray>Temperature: <white>" + String.format("%.0f", temperature) + "°C</white></dark_gray>"
                )));
    }

    private void refreshTemperature() {
        Material mat;
        String color;
        if (temperature >= 1400) {
            mat = Material.RED_STAINED_GLASS_PANE;
            color = "red";
        } else if (temperature >= 1000) {
            mat = Material.ORANGE_STAINED_GLASS_PANE;
            color = "gold";
        } else if (temperature >= 500) {
            mat = Material.YELLOW_STAINED_GLASS_PANE;
            color = "yellow";
        } else {
            mat = Material.BLUE_STAINED_GLASS_PANE;
            color = "aqua";
        }
        inventory.setItem(TEMP_SLOT, makeDisplay(mat,
                "<" + color + ">☄ Temperature: " + String.format("%.0f", temperature) + "°C</" + color + ">",
                List.of(
                        "<gray>Min processing: <yellow>500°C</yellow></gray>",
                        "<gray>Overheat threshold: <red>1500°C</red></gray>",
                        temperature >= 500
                                ? "<green>✔ Ready to process</green>"
                                : "<red>✖ Too cold to process</red>"
                )));
    }

    private void refreshProgress() {
        double pct = (currentRecipeTotalTicks > 0)
                ? (double) progressTicks / currentRecipeTotalTicks
                : 0.0;

        Material p1 = pct >= 0.33 ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
        Material p2 = pct >= 0.66 ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
        Material p3 = pct >= 0.99 ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;

        String label = state.isProcessing()
                ? String.format("<green>Progress: %.0f%%</green>", pct * 100)
                : "<gray>Progress: —</gray>";

        inventory.setItem(PROGRESS_SLOT_1, makeDisplay(p1, label, List.of("<gray>◀ 0–33%</gray>")));
        inventory.setItem(PROGRESS_SLOT_2, makeDisplay(p2, label, List.of("<gray>33–66%</gray>")));
        inventory.setItem(PROGRESS_SLOT_3, makeDisplay(p3, label, List.of("<gray>66–100% ▶</gray>")));
    }

    private void refreshFuelBar() {
        String fuelText = fuelRemaining > 0
                ? "<gold>Fuel: " + fuelRemaining + " units</gold>"
                : "<red>No Fuel</red>";
        Material mat = fuelRemaining > 0
                ? Material.ORANGE_STAINED_GLASS_PANE
                : Material.GRAY_STAINED_GLASS_PANE;
        inventory.setItem(FUEL_BAR_SLOT, makeDisplay(mat, fuelText,
                List.of(
                        "<gray>Coal: 100 units</gray>",
                        "<gray>Charcoal: 80 units</gray>",
                        "<gray>Coal Block: 900 units</gray>",
                        "<gray>Blaze Rod: 120 units</gray>",
                        "<gray>Lava Bucket: 1000 units</gray>"
                )));
    }

    private void refreshRadiation() {
        String radText = state.isProcessing()
                ? "<yellow>☢ Emitting radiation (3 block radius)</yellow>"
                : "<green>☢ Inactive — no emission</green>";
        inventory.setItem(RADIATION_SLOT, makeDisplay(Material.LIME_STAINED_GLASS_PANE,
                "<gradient:#39ff14:#ffcc00>Radiation Meter</gradient>",
                List.of(
                        radText,
                        "<gray>Hazmat armor: <aqua>80% protection</aqua></gray>",
                        "<gray>Plutonium armor: <green>100% protection</green></gray>"
                )));
    }

    private static ItemStack makeDisplay(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtil.parse(name)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore.stream()
                    .map(line -> ColorUtil.parse(line)
                            .decoration(TextDecoration.ITALIC, false))
                    .toList());
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isFunctional(int slot) {
        return slot == INPUT_SLOT || slot == OUTPUT_SLOT || slot == FUEL_SLOT;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Inventory accessors (functional slots)
    // ──────────────────────────────────────────────────────────────────────────

    public ItemStack getInputItem() {
        return inventory.getItem(INPUT_SLOT);
    }

    public void setInputItem(ItemStack item) {
        inventory.setItem(INPUT_SLOT, item);
    }

    public ItemStack getOutputItem() {
        return inventory.getItem(OUTPUT_SLOT);
    }

    public void setOutputItem(ItemStack item) {
        inventory.setItem(OUTPUT_SLOT, item);
    }

    public ItemStack getFuelItem() {
        return inventory.getItem(FUEL_SLOT);
    }

    public void setFuelItem(ItemStack item) {
        inventory.setItem(FUEL_SLOT, item);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // State
    // ──────────────────────────────────────────────────────────────────────────

    public SmelterState getState()                  { return state; }
    public void setState(SmelterState state)        { this.state = state; this.dirty = true; }

    public double getTemperature()                  { return temperature; }
    public void setTemperature(double t)            { this.temperature = Math.max(20.0, t); this.dirty = true; }

    public int getFuelRemaining()                   { return fuelRemaining; }
    public void setFuelRemaining(int fuel)          { this.fuelRemaining = Math.max(0, fuel); this.dirty = true; }

    public int getProgressTicks()                   { return progressTicks; }
    public void setProgressTicks(int ticks)         { this.progressTicks = Math.max(0, ticks); this.dirty = true; }

    public int getCurrentRecipeTotalTicks()         { return currentRecipeTotalTicks; }
    public void setCurrentRecipeTotalTicks(int t)   { this.currentRecipeTotalTicks = t; this.dirty = true; }

    public String getCurrentRecipeId()              { return currentRecipeId; }
    public void setCurrentRecipeId(String id)       { this.currentRecipeId = id; this.dirty = true; }

    public long getOverheatedUntilMs()              { return overheatedUntilMs; }
    public void setOverheatedUntilMs(long ms)       { this.overheatedUntilMs = ms; this.dirty = true; }

    public UUID getLastInteractingPlayerUuid()              { return lastInteractingPlayerUuid; }
    public void setLastInteractingPlayerUuid(UUID uuid)     { this.lastInteractingPlayerUuid = uuid; }

    public boolean isDirty()                        { return dirty; }
    public void markClean()                         { this.dirty = false; }
    public void markDirty()                         { this.dirty = true; }

    public String getLocationKey()                  { return locationKey; }
    public Location getLocation()                   { return location.clone(); }

    // ──────────────────────────────────────────────────────────────────────────
    // Progress
    // ──────────────────────────────────────────────────────────────────────────

    public double getProgressFraction() {
        if (currentRecipeTotalTicks <= 0) return 0.0;
        return Math.min(1.0, (double) progressTicks / currentRecipeTotalTicks);
    }

    public boolean isComplete() {
        return currentRecipeTotalTicks > 0 && progressTicks >= currentRecipeTotalTicks;
    }

    public void resetProgress() {
        this.progressTicks = 0;
        this.currentRecipeTotalTicks = 0;
        this.currentRecipeId = null;
        this.dirty = true;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Serialization helpers
    // ──────────────────────────────────────────────────────────────────────────

    public static String serializeLocation(Location loc) {
        return loc.getWorld().getName()
                + "," + loc.getBlockX()
                + "," + loc.getBlockY()
                + "," + loc.getBlockZ();
    }

    public static Location deserializeLocation(String key) {
        try {
            String[] parts = key.split(",");
            var world = Bukkit.getWorld(parts[0]);
            if (world == null) return null;
            return new Location(world,
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3]));
        } catch (Exception e) {
            NCLogger.warn("Failed to deserialize smelter location: " + key);
            return null;
        }
    }

    @Override
    public String toString() {
        return "SmelterData{key=" + locationKey + ", state=" + state
                + ", temp=" + String.format("%.0f", temperature)
                + ", fuel=" + fuelRemaining + ", progress=" + progressTicks + "}";
    }
}
