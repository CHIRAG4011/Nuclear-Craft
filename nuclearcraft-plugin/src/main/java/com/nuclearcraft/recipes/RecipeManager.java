package com.nuclearcraft.recipes;

import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.*;

import java.util.*;

/**
 * Manages all NuclearCraft crafting recipes.
 * Supports shaped, shapeless, and future machine recipes.
 * Tracks registered keys for clean removal on reload.
 */
public class RecipeManager {

    private final NuclearCraftPlugin plugin;
    private final ItemManager itemManager;
    private final List<NamespacedKey> registeredKeys = new ArrayList<>();

    public RecipeManager(NuclearCraftPlugin plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
    }

    public void initialize() {
        registerAllRecipes();
        NCLogger.info("RecipeManager initialized — " + registeredKeys.size() + " recipes registered.");
    }

    public void reload() {
        removeAllRegistered();
        registerAllRecipes();
        NCLogger.info("RecipeManager reloaded — " + registeredKeys.size() + " recipes registered.");
    }

    public void shutdown() {
        removeAllRegistered();
    }

    private void registerAllRecipes() {
        registerSmeltingRecipes();
    }

    private void registerSmeltingRecipes() {
        itemManager.getItem("raw-plutonium-fragment").ifPresent(rawFragment -> {
            itemManager.getItem("refined-plutonium-ingot").ifPresent(refinedIngot -> {
                NamespacedKey key = new NamespacedKey(plugin, "smelt_raw_plutonium");
                FurnaceRecipe recipe = new FurnaceRecipe(
                        key,
                        refinedIngot.build(),
                        rawFragment.getMaterial(),
                        0.5f,
                        200
                );
                plugin.getServer().addRecipe(recipe);
                registeredKeys.add(key);
                NCLogger.debug("Registered recipe: %s", key.getKey());
            });
        });
    }

    /**
     * Registers a shaped crafting recipe.
     *
     * @param keyName  Unique recipe key name
     * @param result   Output item stack
     * @param shape    Recipe shape (e.g. ["AAA", "ABA", "AAA"])
     * @param ingredients Map of character to RecipeChoice
     */
    public void registerShaped(String keyName, ItemStack result, String[] shape,
                                Map<Character, RecipeChoice> ingredients) {
        NamespacedKey key = new NamespacedKey(plugin, keyName);
        if (isRegistered(key)) {
            NCLogger.warn("Skipping duplicate recipe registration: " + keyName);
            return;
        }
        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(shape);
        ingredients.forEach(recipe::setIngredient);
        plugin.getServer().addRecipe(recipe);
        registeredKeys.add(key);
        NCLogger.debug("Registered shaped recipe: %s", keyName);
    }

    /**
     * Registers a shapeless crafting recipe.
     */
    public void registerShapeless(String keyName, ItemStack result, List<RecipeChoice> ingredients) {
        NamespacedKey key = new NamespacedKey(plugin, keyName);
        if (isRegistered(key)) {
            NCLogger.warn("Skipping duplicate recipe registration: " + keyName);
            return;
        }
        ShapelessRecipe recipe = new ShapelessRecipe(key, result);
        ingredients.forEach(recipe::addIngredient);
        plugin.getServer().addRecipe(recipe);
        registeredKeys.add(key);
        NCLogger.debug("Registered shapeless recipe: %s", keyName);
    }

    private boolean isRegistered(NamespacedKey key) {
        return registeredKeys.contains(key);
    }

    private void removeAllRegistered() {
        for (NamespacedKey key : registeredKeys) {
            plugin.getServer().removeRecipe(key);
        }
        registeredKeys.clear();
    }

    public List<NamespacedKey> getRegisteredKeys() {
        return Collections.unmodifiableList(registeredKeys);
    }
}
