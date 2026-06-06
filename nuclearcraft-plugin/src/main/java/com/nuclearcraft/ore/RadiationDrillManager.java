package com.nuclearcraft.ore;

import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.items.CustomItem;
import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;

import java.util.Optional;

/**
 * Manages the Radiation Drill — the only tool capable of safely mining Plutonium Ore.
 *
 * Responsibilities:
 *   - Registers the crafting recipe (shaped, requires Radioactive Cores).
 *   - Provides {@link #isDrill(ItemStack)} for fast identity checks.
 *
 * Recipe pattern (3×3):
 *   D  G  D   (D=Diamond, G=Glowstone)
 *   R  R  R   (R=Radioactive Core [MAGMA_CREAM with PDC])
 *   D  S  D   (S=Stick)
 *
 * Requires: 4 Diamonds + 1 Glowstone + 3 Radioactive Cores + 1 Stick
 *
 * Note: Because Radioactive Cores are custom items (PDC-tagged MAGMA_CREAM), the recipe
 * is registered with the base material MAGMA_CREAM. The actual PDC validation is done in
 * OreListener's PrepareItemCraftEvent — the recipe is cancelled if any MAGMA_CREAM
 * ingredient is not a genuine Radioactive Core.
 */
public class RadiationDrillManager {

    private final NuclearCraftPlugin plugin;
    private final ItemManager itemManager;

    private NamespacedKey drillRecipeKey;

    public RadiationDrillManager(NuclearCraftPlugin plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
    }

    public void initialize() {
        drillRecipeKey = new NamespacedKey(plugin, "radiation_drill_recipe");
        registerRecipe();
        NCLogger.info("RadiationDrillManager initialized — recipe registered.");
    }

    public void shutdown() {}

    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Returns true if the given ItemStack is a NuclearCraft Radiation Drill.
     */
    public boolean isDrill(ItemStack item) {
        if (item == null || item.getType() != Material.DIAMOND_PICKAXE) return false;
        Optional<CustomItem> drill = itemManager.getItem("radiation-drill");
        return drill.isPresent() && drill.get().matches(item);
    }

    /**
     * Returns the NamespacedKey used for the drill's crafting recipe.
     */
    public NamespacedKey getRecipeKey() { return drillRecipeKey; }

    // ──────────────────────────────────────────────────────────────────────────

    private void registerRecipe() {
        // Remove old recipe if reloading
        plugin.getServer().removeRecipe(drillRecipeKey);

        Optional<CustomItem> drillOpt = itemManager.getItem("radiation-drill");
        if (drillOpt.isEmpty()) {
            NCLogger.severe("Cannot register drill recipe — 'radiation-drill' item not found in ItemManager.");
            return;
        }

        ItemStack result = drillOpt.get().build(1);
        ShapedRecipe recipe = new ShapedRecipe(drillRecipeKey, result);

        recipe.shape("DGD", "RRR", "DSD");
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('G', Material.GLOWSTONE);
        recipe.setIngredient('R', Material.MAGMA_CREAM); // validated as Radioactive Core in OreListener
        recipe.setIngredient('S', Material.STICK);

        plugin.getServer().addRecipe(recipe);
        NCLogger.debug("Radiation Drill recipe registered with key: " + drillRecipeKey);
    }

    /**
     * Returns the NamespacedKey used to identify the drill in PDC.
     * Used by OreListener's PrepareItemCraftEvent to validate core ingredients.
     */
    public boolean isRadioactiveCore(ItemStack item) {
        if (item == null || item.getType() != Material.MAGMA_CREAM) return false;
        Optional<CustomItem> core = itemManager.getItem("radioactive-core");
        return core.isPresent() && core.get().matches(item);
    }
}
