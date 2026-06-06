package com.nuclearcraft.smelter;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.utils.NCLogger;

import java.util.*;

/**
 * Registry for all Nuclear Smelter processing recipes.
 * Recipes are matched by custom item ID (PDC), not material type.
 * Future recipes (alloys, reactor components, etc.) are registered here.
 */
public class NuclearSmelterRecipeManager {

    private final ConfigManager configManager;
    private final List<SmelterRecipe> recipes = new ArrayList<>();

    public NuclearSmelterRecipeManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void initialize() {
        registerAllRecipes();
        NCLogger.info("NuclearSmelterRecipeManager initialized — " + recipes.size() + " recipe(s) registered.");
    }

    public void reload() {
        recipes.clear();
        registerAllRecipes();
        NCLogger.info("NuclearSmelterRecipeManager reloaded — " + recipes.size() + " recipe(s).");
    }

    public void shutdown() {
        recipes.clear();
    }

    // ──────────────────────────────────────────────────────────────────────────

    private void registerAllRecipes() {
        var cfg = configManager.getSmelter();

        int processingTicks = cfg.getInt("processing.plutonium-fragment-ticks", 300);
        int fuelCost = cfg.getInt("processing.fuel-cost-per-cycle", 1);

        register(new SmelterRecipe(
                "plutonium_refine",
                "raw-plutonium-fragment",
                "refined-plutonium-ingot",
                processingTicks,
                fuelCost
        ));
    }

    /**
     * Registers a recipe. Future phases add recipes here.
     */
    public void register(SmelterRecipe recipe) {
        recipes.add(recipe);
        NCLogger.debug("Registered smelter recipe: %s", recipe.getId());
    }

    /**
     * Finds a matching recipe for the given input item ID.
     * Returns the first match (recipes are checked in registration order).
     */
    public Optional<SmelterRecipe> findRecipe(String inputItemId) {
        if (inputItemId == null) return Optional.empty();
        return recipes.stream()
                .filter(r -> r.getInputItemId().equals(inputItemId))
                .findFirst();
    }

    public List<SmelterRecipe> getAllRecipes() {
        return Collections.unmodifiableList(recipes);
    }
}
