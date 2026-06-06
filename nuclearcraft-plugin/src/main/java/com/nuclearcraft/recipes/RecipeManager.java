package com.nuclearcraft.recipes;

import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.*;

import java.util.*;

/**
 * Manages all NuclearCraft crafting recipes.
 * Supports shaped, shapeless, and future machine recipes.
 * Tracks registered keys for clean removal on reload.
 *
 * Phase 7 additions: Radiation Antidote and Radiation Serum.
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
        registerNuclearSmelterRecipe();
        registerRadiationAntidoteRecipe();
        registerRadiationSerumRecipe();
    }

    // ── Phase 5: Nuclear Smelter ──────────────────────────────────────────────

    private void registerNuclearSmelterRecipe() {
        itemManager.getItem("nuclear-smelter").ifPresent(smelterItem -> {
            registerShaped(
                    "craft_nuclear_smelter",
                    smelterItem.build(),
                    new String[]{"DOD", "OFO", "DOD"},
                    Map.of(
                            'D', new RecipeChoice.MaterialChoice(Material.DIAMOND),
                            'O', new RecipeChoice.MaterialChoice(Material.OBSIDIAN),
                            'F', new RecipeChoice.MaterialChoice(Material.FURNACE)
                    )
            );
        });
    }

    // ── Phase 7: Radiation Cures ──────────────────────────────────────────────

    /**
     * Radiation Antidote recipe (shapeless):
     *   2× Healing Petal + 1× Honey Bottle → 1 Radiation Antidote
     *
     * Does NOT grant immunity — use the Serum for that.
     */
    private void registerRadiationAntidoteRecipe() {
        itemManager.getItem("radiation-antidote").ifPresent(antidoteItem -> {
            itemManager.getItem("healing-petal").ifPresent(petalItem -> {
                registerShapeless(
                        "craft_radiation_antidote",
                        antidoteItem.build(1),
                        List.of(
                                new RecipeChoice.ExactChoice(petalItem.build(1)),
                                new RecipeChoice.ExactChoice(petalItem.build(1)),
                                new RecipeChoice.MaterialChoice(Material.HONEY_BOTTLE)
                        )
                );
            });
        });
    }

    /**
     * Radiation Serum recipe (shaped):
     *
     *   P R P
     *   H G H
     *   P B P
     *
     * Where:
     *   P = Healing Petal
     *   R = Radioactive Core
     *   H = Gold Nugget
     *   G = Golden Apple
     *   B = Glass Bottle
     *
     * Results in 1 Radiation Serum.
     */
    private void registerRadiationSerumRecipe() {
        itemManager.getItem("radiation-serum").ifPresent(serumItem -> {
            itemManager.getItem("healing-petal").ifPresent(petalItem -> {
                itemManager.getItem("radioactive-core").ifPresent(coreItem -> {
                    registerShaped(
                            "craft_radiation_serum",
                            serumItem.build(1),
                            new String[]{"PRP", "HGH", "PBP"},
                            Map.of(
                                    'P', new RecipeChoice.ExactChoice(petalItem.build(1)),
                                    'R', new RecipeChoice.ExactChoice(coreItem.build(1)),
                                    'H', new RecipeChoice.MaterialChoice(Material.GOLD_NUGGET),
                                    'G', new RecipeChoice.MaterialChoice(Material.GOLDEN_APPLE),
                                    'B', new RecipeChoice.MaterialChoice(Material.GLASS_BOTTLE)
                            )
                    );
                });
            });
        });
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Registration helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Registers a shaped crafting recipe.
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
