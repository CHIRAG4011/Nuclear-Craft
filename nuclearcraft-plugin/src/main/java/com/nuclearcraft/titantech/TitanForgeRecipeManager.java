package com.nuclearcraft.titantech;

import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * Manages all Titan Forge crafting recipes and the vanilla shaped recipe
 * for crafting the Titan Reactor Forge block itself.
 */
public class TitanForgeRecipeManager {

    private final JavaPlugin plugin;
    private final ItemManager itemManager;
    private final FileConfiguration cfg;

    private final List<TitanForgeRecipe> recipes = new ArrayList<>();
    private final Map<String, TitanForgeRecipe> recipeById = new LinkedHashMap<>();
    private NamespacedKey forgeRecipeKey;

    public TitanForgeRecipeManager(JavaPlugin plugin, ItemManager itemManager, FileConfiguration cfg) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.cfg = cfg;
    }

    public void initialize() {
        registerVanillaRecipe();
        buildInternalRecipes();
        NCLogger.info("TitanForgeRecipeManager initialized — " + recipes.size() + " titan recipes registered.");
    }

    public void shutdown() {
        if (forgeRecipeKey != null) plugin.getServer().removeRecipe(forgeRecipeKey);
        recipes.clear();
        recipeById.clear();
    }

    // ── Vanilla shaped recipe for the Titan Forge block ──────────────────────

    private void registerVanillaRecipe() {
        ItemStack forgeItem = itemManager.getItem("titan-reactor-forge")
                .map(i -> i.build(1))
                .orElse(new ItemStack(Material.CRYING_OBSIDIAN));

        forgeRecipeKey = new NamespacedKey(plugin, "titan_reactor_forge");
        ShapedRecipe recipe = new ShapedRecipe(forgeRecipeKey, forgeItem);
        recipe.shape("TCT", "PFP", "RRR");
        recipe.setIngredient('T', Material.CRYING_OBSIDIAN);
        recipe.setIngredient('C', Material.NETHER_STAR);
        recipe.setIngredient('P', Material.OBSIDIAN);
        recipe.setIngredient('F', Material.BEACON);
        recipe.setIngredient('R', Material.NETHERITE_INGOT);

        try {
            plugin.getServer().addRecipe(recipe);
            NCLogger.debug("Registered vanilla shaped recipe for Titan Reactor Forge.");
        } catch (IllegalStateException e) {
            NCLogger.debug("Titan Forge recipe already registered.");
        }
    }

    // ── Internal forge recipes (crafted inside the Titan Forge GUI) ──────────

    private void buildInternalRecipes() {
        // Armor
        add("titan-helmet",     "§5☢ Titan Helmet",      "titan-helmet",
                "titan-fragment", 8,  "titan-core", 2, 3, 600, 90.0);
        add("titan-chestplate", "§5☢ Titan Chestplate",  "titan-chestplate",
                "titan-fragment", 12, "titan-core", 3, 4, 800, 85.0);
        add("titan-leggings",   "§5☢ Titan Leggings",    "titan-leggings",
                "titan-fragment", 10, "titan-core", 3, 3, 700, 87.0);
        add("titan-boots",      "§5☢ Titan Boots",       "titan-boots",
                "titan-fragment", 7,  "titan-core", 2, 2, 500, 92.0);

        // Weapons
        add("titan-sword",      "§5☢ Titan Sword",       "titan-sword",
                "titan-fragment", 6,  "titan-core", 2, 3, 500, 90.0);
        add("titan-axe",        "§5☢ Titan Axe",         "titan-axe",
                "titan-fragment", 7,  "titan-core", 2, 3, 500, 88.0);
        add("titan-bow",        "§5☢ Titan Bow",         "titan-bow",
                "titan-fragment", 5,  "titan-core", 1, 3, 450, 90.0);

        // Tools
        add("titan-pickaxe",    "§5☢ Titan Pickaxe",     "titan-pickaxe",
                "titan-fragment", 6,  "titan-core", 2, 3, 480, 90.0);
        add("titan-shovel",     "§5☢ Titan Shovel",      "titan-shovel",
                "titan-fragment", 5,  "titan-core", 1, 3, 420, 92.0);
        add("titan-hoe",        "§5☢ Titan Hoe",         "titan-hoe",
                "titan-fragment", 4,  "titan-core", 1, 2, 400, 93.0);

        // Ammo
        add("titan-arrow",      "§5☢ Titan Arrow (x8)",  "titan-arrow",
                "titan-fragment", 3,  "titan-core", 1, 1, 200, 100.0);
    }

    /**
     * @param id          recipe id
     * @param displayName display name shown in GUI
     * @param outputId    CustomItem id of the output
     * @param mat1Id      CustomItem id of material 1 (MAT1 slot)
     * @param mat1Amt     amount of material 1
     * @param mat2Id      CustomItem id of material 2 (MAT2 slot)
     * @param mat2Amt     amount of material 2
     * @param coresReq    titan-cores required in CATALYST slot
     * @param durationTicks how many ticks crafting takes
     * @param chance      success chance (0-100)
     */
    private void add(String id, String displayName, String outputId,
                     String mat1Id, int mat1Amt, String mat2Id, int mat2Amt,
                     int coresReq, int durationTicks, double chance) {
        String path = "recipes." + id;
        int m1  = cfg.getInt(path + ".material1-amount", mat1Amt);
        int m2  = cfg.getInt(path + ".material2-amount", mat2Amt);
        int cr  = cfg.getInt(path + ".cores-required",   coresReq);
        int dur = cfg.getInt(path + ".duration-ticks",   durationTicks);
        double sc = cfg.getDouble(path + ".success-chance", chance);

        TitanForgeRecipe recipe = new TitanForgeRecipe(id, displayName, outputId,
                mat1Id, m1, mat2Id, m2, cr, dur, sc);
        recipes.add(recipe);
        recipeById.put(id, recipe);
    }

    public List<TitanForgeRecipe> getAllRecipes()  { return Collections.unmodifiableList(recipes); }

    public Optional<TitanForgeRecipe> getRecipeById(String id) {
        return Optional.ofNullable(recipeById.get(id));
    }
}
