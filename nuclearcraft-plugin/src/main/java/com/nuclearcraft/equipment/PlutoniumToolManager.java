package com.nuclearcraft.equipment;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Registers all five Plutonium Tool items and their crafting recipes.
 *
 * <p>Items use NETHERITE base materials (so they share fire/lava immunity and
 * the correct tool tag), and override attack damage and speed via
 * {@link EquipmentItem} attribute modifiers.
 *
 * <h3>Items registered</h3>
 * <ul>
 *   <li>plutonium-sword    — CMD 1301</li>
 *   <li>plutonium-axe      — CMD 1302</li>
 *   <li>plutonium-pickaxe  — CMD 1303</li>
 *   <li>plutonium-shovel   — CMD 1304</li>
 *   <li>plutonium-hoe      — CMD 1305</li>
 * </ul>
 *
 * <h3>Crafting validation</h3>
 * Recipes are registered with ECHO_SHARD as the ingredient material (which
 * corresponds to refined-plutonium-ingot's material).  The PrepareItemCraftEvent
 * in {@link EquipmentListener} validates PDC tags before allowing the result.
 */
public class PlutoniumToolManager {

    private final JavaPlugin plugin;
    private final ItemManager itemManager;
    private final ConfigManager configManager;

    public PlutoniumToolManager(JavaPlugin plugin, ItemManager itemManager,
                                ConfigManager configManager) {
        this.plugin        = plugin;
        this.itemManager   = itemManager;
        this.configManager = configManager;
    }

    public void initialize() {
        registerItems();
        registerRecipes();
        NCLogger.info("PlutoniumToolManager initialized — 5 tools registered.");
    }

    public void shutdown() {}

    // ──────────────────────────────────────────────────────────────────────────
    // Item registration
    // ──────────────────────────────────────────────────────────────────────────

    private void registerItems() {
        var cfg = configManager.getEquipment();

        // ── Sword ──────────────────────────────────────────────────────────────
        double swordDmg   = cfg.getDouble("tools.plutonium-sword.damage", 10.0);
        double swordSpeed = cfg.getDouble("tools.plutonium-sword.attack-speed", -2.3);
        int    swordDur   = cfg.getInt("tools.plutonium-sword.durability", 2500);

        EquipmentItem sword = new EquipmentItem(plugin,
                "plutonium-sword",
                "<gradient:#39ff14:#00bfff>Plutonium Sword</gradient>",
                List.of(
                        "Forged from refined plutonium ingots.",
                        "<red>☢ Irradiates enemies on hit</red>",
                        "<yellow>Attack Damage: +" + (int) swordDmg + "</yellow>",
                        "<green>▸ Radiation Strike: 10 rad/hit</green>",
                        "<dark_purple>▸ Critical: +20 rad burst</dark_purple>"),
                Material.NETHERITE_SWORD, 1301, true, swordDur);
        sword.addAttribute(Attribute.ATTACK_DAMAGE,
                key("plutonium_sword_dmg"), swordDmg,
                AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HAND);
        sword.addAttribute(Attribute.ATTACK_SPEED,
                key("plutonium_sword_spd"), swordSpeed,
                AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HAND);
        itemManager.getRegistry().register(sword);

        // ── Axe ───────────────────────────────────────────────────────────────
        double axeDmg   = cfg.getDouble("tools.plutonium-axe.damage", 11.0);
        double axeSpeed = cfg.getDouble("tools.plutonium-axe.attack-speed", -3.0);
        int    axeDur   = cfg.getInt("tools.plutonium-axe.durability", 2600);

        EquipmentItem axe = new EquipmentItem(plugin,
                "plutonium-axe",
                "<gradient:#39ff14:#00bfff>Plutonium Axe</gradient>",
                List.of(
                        "A brutal plutonium-forged axe.",
                        "<red>☢ 15% chance: Radiation Shockwave</red>",
                        "<yellow>Attack Damage: +" + (int) axeDmg + "</yellow>",
                        "<green>▸ Shockwave radius: 3 blocks</green>"),
                Material.NETHERITE_AXE, 1302, true, axeDur);
        axe.addAttribute(Attribute.ATTACK_DAMAGE,
                key("plutonium_axe_dmg"), axeDmg,
                AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HAND);
        axe.addAttribute(Attribute.ATTACK_SPEED,
                key("plutonium_axe_spd"), axeSpeed,
                AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HAND);
        itemManager.getRegistry().register(axe);

        // ── Pickaxe ───────────────────────────────────────────────────────────
        int pickDur = cfg.getInt("tools.plutonium-pickaxe.durability", 2800);

        EquipmentItem pick = new EquipmentItem(plugin,
                "plutonium-pickaxe",
                "<gradient:#39ff14:#00bfff>Plutonium Pickaxe</gradient>",
                List.of(
                        "Mines faster than netherite.",
                        "<red>☢ 10% chance: creates Radioactive Debris</red>",
                        "<green>▸ Mining Speed: +20%</green>"),
                Material.NETHERITE_PICKAXE, 1303, true, pickDur);
        itemManager.getRegistry().register(pick);

        // ── Shovel ────────────────────────────────────────────────────────────
        int shovelDur = cfg.getInt("tools.plutonium-shovel.durability", 2500);

        EquipmentItem shovel = new EquipmentItem(plugin,
                "plutonium-shovel",
                "<gradient:#39ff14:#00bfff>Plutonium Shovel</gradient>",
                List.of(
                        "Digs with radioactive force.",
                        "<red>☢ 15% chance: creates Radioactive Soil</red>",
                        "<green>▸ Mining Speed: +20%</green>"),
                Material.NETHERITE_SHOVEL, 1304, true, shovelDur);
        itemManager.getRegistry().register(shovel);

        // ── Hoe ───────────────────────────────────────────────────────────────
        int hoeDur = cfg.getInt("tools.plutonium-hoe.durability", 2000);

        EquipmentItem hoe = new EquipmentItem(plugin,
                "plutonium-hoe",
                "<gradient:#39ff14:#00bfff>Plutonium Hoe</gradient>",
                List.of(
                        "Converts soil into Radioactive Farmland.",
                        "<red>☢ Right-click grass or dirt to till</red>",
                        "<green>▸ Crops grow 50% faster on Radioactive Farmland</green>"),
                Material.NETHERITE_HOE, 1305, true, hoeDur);
        itemManager.getRegistry().register(hoe);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Crafting recipes (validated by EquipmentListener PrepareItemCraftEvent)
    // ──────────────────────────────────────────────────────────────────────────

    private void registerRecipes() {
        // P = refined-plutonium-ingot (ECHO_SHARD material, PDC validated)
        // S = stick

        // Sword: P / P / S
        registerSword();
        registerAxe();
        registerPickaxe();
        registerShovel();
        registerHoe();
    }

    private void registerSword() {
        ItemStack result = itemManager.getRegistry().get("plutonium-sword")
                .map(ci -> ci.build(1)).orElse(null);
        if (result == null) return;
        NamespacedKey recipeKey = new NamespacedKey(plugin, "plutonium_sword");
        plugin.getServer().removeRecipe(recipeKey);
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);
        recipe.shape(" P ", " P ", " S ");
        recipe.setIngredient('P', Material.ECHO_SHARD);
        recipe.setIngredient('S', Material.STICK);
        plugin.getServer().addRecipe(recipe);
    }

    private void registerAxe() {
        ItemStack result = itemManager.getRegistry().get("plutonium-axe")
                .map(ci -> ci.build(1)).orElse(null);
        if (result == null) return;
        NamespacedKey recipeKey = new NamespacedKey(plugin, "plutonium_axe");
        plugin.getServer().removeRecipe(recipeKey);
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);
        recipe.shape("PP ", "PS ", " S ");
        recipe.setIngredient('P', Material.ECHO_SHARD);
        recipe.setIngredient('S', Material.STICK);
        plugin.getServer().addRecipe(recipe);
    }

    private void registerPickaxe() {
        ItemStack result = itemManager.getRegistry().get("plutonium-pickaxe")
                .map(ci -> ci.build(1)).orElse(null);
        if (result == null) return;
        NamespacedKey recipeKey = new NamespacedKey(plugin, "plutonium_pickaxe");
        plugin.getServer().removeRecipe(recipeKey);
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);
        recipe.shape("PPP", " S ", " S ");
        recipe.setIngredient('P', Material.ECHO_SHARD);
        recipe.setIngredient('S', Material.STICK);
        plugin.getServer().addRecipe(recipe);
    }

    private void registerShovel() {
        ItemStack result = itemManager.getRegistry().get("plutonium-shovel")
                .map(ci -> ci.build(1)).orElse(null);
        if (result == null) return;
        NamespacedKey recipeKey = new NamespacedKey(plugin, "plutonium_shovel");
        plugin.getServer().removeRecipe(recipeKey);
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);
        recipe.shape(" P ", " S ", " S ");
        recipe.setIngredient('P', Material.ECHO_SHARD);
        recipe.setIngredient('S', Material.STICK);
        plugin.getServer().addRecipe(recipe);
    }

    private void registerHoe() {
        ItemStack result = itemManager.getRegistry().get("plutonium-hoe")
                .map(ci -> ci.build(1)).orElse(null);
        if (result == null) return;
        NamespacedKey recipeKey = new NamespacedKey(plugin, "plutonium_hoe");
        plugin.getServer().removeRecipe(recipeKey);
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);
        recipe.shape("PP ", " S ", " S ");
        recipe.setIngredient('P', Material.ECHO_SHARD);
        recipe.setIngredient('S', Material.STICK);
        plugin.getServer().addRecipe(recipe);
    }

    // ──────────────────────────────────────────────────────────────────────────

    private NamespacedKey key(String name) {
        return new NamespacedKey(plugin, name);
    }
}
