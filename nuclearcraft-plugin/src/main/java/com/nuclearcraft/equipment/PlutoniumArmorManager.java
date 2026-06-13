package com.nuclearcraft.equipment;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.Color;
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
 * Registers Hazmat Armor and Plutonium Armor items + crafting recipes.
 *
 * <h3>Hazmat Armor</h3>
 * <ul>
 *   <li>Material: LEATHER (dyeable yellow via {@link EquipmentItem#getLeatherColor()})</li>
 *   <li>Purpose: radiation protection — cumulative reduction per piece, up to 80% full set</li>
 *   <li>CMDs 1306–1309</li>
 * </ul>
 *
 * <h3>Plutonium Armor</h3>
 * <ul>
 *   <li>Material: NETHERITE</li>
 *   <li>Purpose: full environmental radiation immunity (full set) + combat passives</li>
 *   <li>CMDs 1310–1313</li>
 * </ul>
 *
 * <h3>Crafting validation</h3>
 * Plutonium armor recipes use ECHO_SHARD (refined-plutonium-ingot material) and are
 * PDC-validated by {@link EquipmentListener}.  Hazmat recipes use YELLOW_WOOL and
 * require no custom validation.
 */
public class PlutoniumArmorManager {

    private static final Color HAZMAT_COLOR = Color.fromRGB(0xFF, 0xCC, 0x00); // vibrant yellow

    private final JavaPlugin plugin;
    private final ItemManager itemManager;
    private final ConfigManager configManager;

    public PlutoniumArmorManager(JavaPlugin plugin, ItemManager itemManager,
                                 ConfigManager configManager) {
        this.plugin        = plugin;
        this.itemManager   = itemManager;
        this.configManager = configManager;
    }

    public void initialize() {
        registerHazmatItems();
        registerPlutoniumItems();
        registerHazmatRecipes();
        registerPlutoniumRecipes();
        NCLogger.info("PlutoniumArmorManager initialized — 8 armor pieces registered.");
    }

    public void shutdown() {}

    // ──────────────────────────────────────────────────────────────────────────
    // Hazmat Armor
    // ──────────────────────────────────────────────────────────────────────────

    private void registerHazmatItems() {
        var cfg = configManager.getEquipment();

        registerHazmat("helmet",
                "hazmat-helmet", 1306, Material.LEATHER_HELMET, EquipmentSlotGroup.HEAD,
                cfg.getDouble("armor.hazmat.helmet.armor", 2.0),
                cfg.getDouble("armor.hazmat.helmet.armor-toughness", 0.5),
                cfg.getInt("armor.hazmat.helmet.durability", 363), 0,
                "☢ +20% radiation resistance",
                "Yellow industrial safety visor");

        registerHazmat("chestplate",
                "hazmat-chestplate", 1307, Material.LEATHER_CHESTPLATE, EquipmentSlotGroup.CHEST,
                cfg.getDouble("armor.hazmat.chestplate.armor", 5.0),
                cfg.getDouble("armor.hazmat.chestplate.armor-toughness", 0.5),
                cfg.getInt("armor.hazmat.chestplate.durability", 529), 0,
                "☢ +30% radiation resistance",
                "Heavy-duty hazmat jacket");

        registerHazmat("leggings",
                "hazmat-leggings", 1308, Material.LEATHER_LEGGINGS, EquipmentSlotGroup.LEGS,
                cfg.getDouble("armor.hazmat.leggings.armor", 4.0),
                cfg.getDouble("armor.hazmat.leggings.armor-toughness", 0.5),
                cfg.getInt("armor.hazmat.leggings.durability", 496), 0,
                "☢ +20% radiation resistance",
                "Reinforced hazmat trousers");

        registerHazmat("boots",
                "hazmat-boots", 1309, Material.LEATHER_BOOTS, EquipmentSlotGroup.FEET,
                cfg.getDouble("armor.hazmat.boots.armor", 2.0),
                cfg.getDouble("armor.hazmat.boots.armor-toughness", 0.5),
                cfg.getInt("armor.hazmat.boots.durability", 430), 0,
                "☢ +10% radiation resistance",
                "Sealed hazmat footwear");
    }

    private void registerHazmat(String piece, String id, int cmd, Material mat,
                                EquipmentSlotGroup slot,
                                double armor, double toughness, int durability, double knockback,
                                String... loreLines) {
        String modelName = "hazmat";
        EquipmentItem item = new EquipmentItem(plugin, id,
                "<gradient:#ffcc00:#ff8800>Hazmat " + capitalize(piece) + "</gradient>",
                List.of(loreLines),
                mat, cmd, false, durability, HAZMAT_COLOR);
        item.withEquippableModel(new NamespacedKey("nuclearcraft", modelName));
        item.addAttribute(Attribute.ARMOR,
                key(id + "_armor"), armor,
                AttributeModifier.Operation.ADD_NUMBER, slot);
        item.addAttribute(Attribute.ARMOR_TOUGHNESS,
                key(id + "_tough"), toughness,
                AttributeModifier.Operation.ADD_NUMBER, slot);
        if (knockback > 0) {
            item.addAttribute(Attribute.KNOCKBACK_RESISTANCE,
                    key(id + "_kb"), knockback,
                    AttributeModifier.Operation.ADD_NUMBER, slot);
        }
        itemManager.getRegistry().register(item);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Plutonium Armor
    // ──────────────────────────────────────────────────────────────────────────

    private void registerPlutoniumItems() {
        var cfg = configManager.getEquipment();

        registerPlut("helmet",
                "plutonium-helmet", 1310, Material.NETHERITE_HELMET, EquipmentSlotGroup.HEAD,
                cfg.getDouble("armor.plutonium.helmet.armor", 4.0),
                cfg.getDouble("armor.plutonium.helmet.armor-toughness", 3.0),
                cfg.getInt("armor.plutonium.helmet.durability", 500),
                cfg.getDouble("armor.plutonium.helmet.knockback-resistance", 0.05),
                "Full set: 100% environmental radiation immunity",
                "Passive: Night Vision",
                "Black reactor titanium helm");

        registerPlut("chestplate",
                "plutonium-chestplate", 1311, Material.NETHERITE_CHESTPLATE, EquipmentSlotGroup.CHEST,
                cfg.getDouble("armor.plutonium.chestplate.armor", 9.0),
                cfg.getDouble("armor.plutonium.chestplate.armor-toughness", 3.0),
                cfg.getInt("armor.plutonium.chestplate.durability", 728),
                0,
                "Full set: 100% environmental radiation immunity",
                "Passive: Regeneration",
                "Integrated reactor core chestplate");

        registerPlut("leggings",
                "plutonium-leggings", 1312, Material.NETHERITE_LEGGINGS, EquipmentSlotGroup.LEGS,
                cfg.getDouble("armor.plutonium.leggings.armor", 7.0),
                cfg.getDouble("armor.plutonium.leggings.armor-toughness", 3.0),
                cfg.getInt("armor.plutonium.leggings.durability", 683),
                0,
                "Full set: 100% environmental radiation immunity",
                "Passive: Speed",
                "Energy-weave reactor leggings");

        registerPlut("boots",
                "plutonium-boots", 1313, Material.NETHERITE_BOOTS, EquipmentSlotGroup.FEET,
                cfg.getDouble("armor.plutonium.boots.armor", 4.0),
                cfg.getDouble("armor.plutonium.boots.armor-toughness", 3.0),
                cfg.getInt("armor.plutonium.boots.durability", 592),
                cfg.getDouble("armor.plutonium.boots.knockback-resistance", 0.05),
                "Full set: 100% environmental radiation immunity",
                "Passive: Reduced Fall Damage",
                "Shock-absorbing reactor boots");
    }

    private void registerPlut(String piece, String id, int cmd, Material mat,
                               EquipmentSlotGroup slot,
                               double armor, double toughness, int durability, double knockback,
                               String... loreLines) {
        String modelName = "plutonium";
        EquipmentItem item = new EquipmentItem(plugin, id,
                "<gradient:#39ff14:#00bfff>Plutonium " + capitalize(piece) + "</gradient>",
                List.of(loreLines),
                mat, cmd, true, durability);
        item.withEquippableModel(new NamespacedKey("nuclearcraft", modelName));
        item.addAttribute(Attribute.ARMOR,
                key(id + "_armor"), armor,
                AttributeModifier.Operation.ADD_NUMBER, slot);
        item.addAttribute(Attribute.ARMOR_TOUGHNESS,
                key(id + "_tough"), toughness,
                AttributeModifier.Operation.ADD_NUMBER, slot);
        if (knockback > 0) {
            item.addAttribute(Attribute.KNOCKBACK_RESISTANCE,
                    key(id + "_kb"), knockback,
                    AttributeModifier.Operation.ADD_NUMBER, slot);
        }
        itemManager.getRegistry().register(item);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Crafting recipes
    // ──────────────────────────────────────────────────────────────────────────

    private void registerHazmatRecipes() {
        // Hazmat uses YELLOW_WOOL — no custom PDC validation needed
        addArmor("hazmat_helmet",    "hazmat-helmet",    "WWW", "W W", "   ");
        addArmor("hazmat_chestplate","hazmat-chestplate","W W", "WWW", "WWW");
        addArmor("hazmat_leggings",  "hazmat-leggings",  "WWW", "W W", "W W");
        addArmor("hazmat_boots",     "hazmat-boots",     "   ", "W W", "W W");
    }

    private void registerPlutoniumRecipes() {
        // Plutonium uses ECHO_SHARD (refined-plutonium-ingot) — PDC validated in listener
        addPlutoniumArmor("plutonium_helmet",    "plutonium-helmet",    "PPP", "P P", "   ");
        addPlutoniumArmor("plutonium_chestplate","plutonium-chestplate","P P", "PPP", "PPP");
        addPlutoniumArmor("plutonium_leggings",  "plutonium-leggings",  "PPP", "P P", "P P");
        addPlutoniumArmor("plutonium_boots",     "plutonium-boots",     "   ", "P P", "P P");
    }

    private void addArmor(String recipeId, String itemId,
                          String row1, String row2, String row3) {
        ItemStack result = itemManager.getRegistry().get(itemId)
                .map(ci -> ci.build(1)).orElse(null);
        if (result == null) return;
        NamespacedKey recipeKey = new NamespacedKey(plugin, recipeId);
        plugin.getServer().removeRecipe(recipeKey);
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);
        recipe.shape(row1, row2, row3);
        recipe.setIngredient('W', Material.YELLOW_WOOL);
        plugin.getServer().addRecipe(recipe);
    }

    private void addPlutoniumArmor(String recipeId, String itemId,
                                   String row1, String row2, String row3) {
        ItemStack result = itemManager.getRegistry().get(itemId)
                .map(ci -> ci.build(1)).orElse(null);
        if (result == null) return;
        NamespacedKey recipeKey = new NamespacedKey(plugin, recipeId);
        plugin.getServer().removeRecipe(recipeKey);
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);
        recipe.shape(row1, row2, row3);
        recipe.setIngredient('P', Material.ECHO_SHARD);
        plugin.getServer().addRecipe(recipe);
    }

    // ──────────────────────────────────────────────────────────────────────────

    private NamespacedKey key(String name) {
        return new NamespacedKey(plugin, name);
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
