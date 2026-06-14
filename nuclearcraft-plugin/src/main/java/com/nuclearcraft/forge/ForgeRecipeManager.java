package com.nuclearcraft.forge;

import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;

/**
 * Registers the vanilla crafting recipe that produces the Nuclear Forge block item.
 *
 * Recipe (3×3):
 *   [Obsidian] [Ancient Debris] [Obsidian]
 *   [Echo Shard] [Smithing Table] [Echo Shard]
 *   [Obsidian] [Ancient Debris] [Obsidian]
 *
 * The base materials (Echo Shard for Refined Plutonium Ingot, etc.) are used because
 * vanilla ShapedRecipe cannot natively filter by CustomModelData.
 * The ForgeListener handles detecting the placement and assigning PDC data.
 */
public class ForgeRecipeManager {

    private final NuclearCraftPlugin plugin;
    private final ItemManager itemManager;
    private NamespacedKey recipeKey;

    public ForgeRecipeManager(NuclearCraftPlugin plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
    }

    public void initialize() {
        recipeKey = new NamespacedKey(plugin, "nuclear_forge_recipe");
        registerForgeRecipe();
        NCLogger.info("ForgeRecipeManager initialized — Nuclear Forge recipe registered.");
    }

    public void shutdown() {
        plugin.getServer().removeRecipe(recipeKey);
    }

    public void reload() {
        shutdown();
        initialize();
    }

    // ──────────────────────────────────────────────────────────────────────────

    private void registerForgeRecipe() {
        ItemStack forgeItem = itemManager.getItem("nuclear-forge")
                .map(ci -> ci.build(1))
                .orElseGet(() -> new ItemStack(Material.CARTOGRAPHY_TABLE));

        ShapedRecipe recipe = new ShapedRecipe(recipeKey, forgeItem);

        // Pattern: O=Obsidian, D=Ancient Debris, E=Echo Shard(Refined Ingot), C=Cartography Table
        recipe.shape("ODO", "ECE", "ODO");
        recipe.setIngredient('O', Material.OBSIDIAN);
        recipe.setIngredient('D', Material.ANCIENT_DEBRIS);
        recipe.setIngredient('E', Material.ECHO_SHARD);
        recipe.setIngredient('C', Material.CARTOGRAPHY_TABLE);

        plugin.getServer().removeRecipe(recipeKey);
        plugin.getServer().addRecipe(recipe);
        NCLogger.debug("Nuclear Forge crafting recipe registered with key: " + recipeKey);
    }

    public NamespacedKey getRecipeKey() {
        return recipeKey;
    }
}
