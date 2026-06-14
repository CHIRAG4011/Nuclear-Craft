package com.nuclearcraft.items;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import java.util.List;
import java.util.Optional;

/**
 * Manages registration and lifecycle of all NuclearCraft custom items.
 * Acts as the single access point for custom item creation and lookup.
 *
 * Phase 7 additions: radiation-antidote, radiation-serum.
 */
public class ItemManager {

    private final NuclearCraftPlugin plugin;
    private final ConfigManager configManager;
    private final ItemRegistry registry;

    public ItemManager(NuclearCraftPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.registry = new ItemRegistry(plugin);
    }

    public void initialize() {
        registerCoreItems();
        NCLogger.info("ItemManager initialized — " + registry.size() + " items registered.");
    }

    public void reload() {
        registry.clear();
        registerCoreItems();
        NCLogger.info("ItemManager reloaded — " + registry.size() + " items registered.");
    }

    public void shutdown() {
        registry.clear();
    }

    private void registerCoreItems() {
        // ── Phase 10: Titan items ─────────────────────────────────────────────

        register(new CustomItem(plugin,
                "titan-fragment",
                "<gradient:#7700ff:#39ff14>Titan Fragment</gradient>",
                List.of("A shard of the Plutonium Titan's reactor plating.",
                        "<dark_purple>Used in high-tier crafting</dark_purple>",
                        "<red>☢ Emits extreme radiation</red>"),
                Material.AMETHYST_SHARD,
                1112,
                true));

        register(new CustomItem(plugin,
                "reactor-heart",
                "<gradient:#ff0000:#7700ff>Reactor Heart</gradient>",
                List.of("The beating core of the Titan's reactor.",
                        "<dark_purple>Rare drop — immense energy within</dark_purple>",
                        "<red>☢ CRITICAL radiation hazard</red>"),
                Material.NETHER_STAR,
                1109,
                true));

        register(new CustomItem(plugin,
                "ancient-reactor-blueprint",
                "<gradient:#ffaa00:#ffffff>Ancient Reactor Blueprint</gradient>",
                List.of("Blueprints recovered from the Titan's body.",
                        "<gold>Unlocks advanced reactor crafting</gold>",
                        "<gray>☢ Future content — Phase 11</gray>"),
                Material.PAPER,
                1110,
                false));

        register(new CustomItem(plugin,
                "mutated-crystal",
                "<gradient:#00ffcc:#7700ff>Mutated Crystal</gradient>",
                List.of("A crystallized mass of radioactive energy.",
                        "<aqua>Grown inside the Titan's body</aqua>",
                        "<yellow>☢ Radioactive — handle with care</yellow>"),
                Material.AMETHYST_SHARD,
                1111,
                true));

        register(new CustomItem(plugin,
                "radioactive-core",
                "<gradient:#39ff14:#ffcc00>Radioactive Core</gradient>",
                List.of("A pulsing core of unstable nuclear energy.",
                        "<yellow>☢ Highly radioactive — handle with care</yellow>"),
                Material.MAGMA_CREAM,
                1101,
                true));

        register(new CustomItem(plugin,
                "raw-plutonium-fragment",
                "<gradient:#39ff14:#00ff88>Raw Plutonium Fragment</gradient>",
                List.of("A raw fragment of mined plutonium ore.",
                        "<yellow>☢ Emits low-level radiation</yellow>"),
                Material.PRISMARINE_CRYSTALS,
                1102,
                false));

        register(new CustomItem(plugin,
                "refined-plutonium-ingot",
                "<gradient:#39ff14:#00bfff>Refined Plutonium Ingot</gradient>",
                List.of("Purified in the Nuclear Smelter.",
                        "<green>Used in crafting plutonium gear</green>",
                        "<yellow>☢ Moderate radiation source</yellow>"),
                Material.ECHO_SHARD,
                1103,
                true));

        register(new CustomItem(plugin,
                "mutated-seed",
                "<gradient:#aaff00:#39ff14>Mutated Seed</gradient>",
                List.of("A seed irradiated by plutonium exposure.",
                        "<green>Plant on Radioactive Farmland</green>"),
                Material.WHEAT_SEEDS,
                1104,
                false));

        register(new CustomItem(plugin,
                "healing-petal",
                "<gradient:#ff88cc:#ff44aa>Healing Petal</gradient>",
                List.of("Grown from mutated crops.",
                        "<aqua>Primary ingredient for radiation cures</aqua>",
                        "<green>Harvested from Mutated Healing Plants</green>"),
                Material.PINK_PETALS,
                1105,
                false));

        register(new CustomItem(plugin,
                "irradiated-heart",
                "<gradient:#ff0000:#39ff14>Irradiated Heart</gradient>",
                List.of("Dropped by Irradiated Zombies.",
                        "<red>Pulsing with corrupted life energy</red>"),
                Material.HEART_OF_THE_SEA,
                1106,
                true));

        register(new CustomItem(plugin,
                "radiation-drill",
                "<gradient:#39ff14:#00bfff>Radiation Drill</gradient>",
                List.of("The only tool that can safely mine Plutonium Ore.",
                        "<green>▸ Diamond+ mining speed</green>",
                        "<yellow>☢ Safe Extraction — no radiation burst</yellow>",
                        "<gray>Crafted from Radioactive Cores & Diamonds</gray>"),
                Material.DIAMOND_PICKAXE,
                1108,
                true));

        register(new CustomItem(plugin,
                "titan-core",
                "<gradient:#7700ff:#39ff14>Titan Core</gradient>",
                List.of("The crystallized essence of the Plutonium Titan.",
                        "<dark_purple>Used to summon the Titan</dark_purple>",
                        "<red>☢ EXTREME radiation — keep at a distance</red>"),
                Material.NETHER_STAR,
                1107,
                true));

        // ── Phase 11: Titan Technology Equipment ─────────────────────────────

        register(new CustomItem(plugin,
                "titan-reactor-forge",
                "<gradient:#7700ff:#ff00ff>Titan Reactor Forge</gradient>",
                List.of("<gray>The pinnacle of nuclear engineering.</gray>",
                        "<dark_purple>Crafts legendary Titan equipment</dark_purple>",
                        "<red>☢ EXTREME radiation hazard during operation</red>",
                        "<green>Place and right-click to open interface</green>"),
                Material.CRYING_OBSIDIAN,
                1501,
                true));

        register(new CustomItem(plugin,
                "titan-helmet",
                "<gradient:#7700ff:#39ff14>☢ Titan Helmet</gradient>",
                List.of("<dark_purple>Forged in the Titan Reactor.</dark_purple>",
                        "<purple>☢ Grants Night Vision</purple>",
                        "<green>▸ Radiation Resistance: TOTAL (full set)</green>",
                        "<yellow>Far beyond MK-IV capabilities</yellow>"),
                Material.NETHERITE_HELMET,
                1502,
                true).withEquippableModel(new NamespacedKey("nuclearcraft", "titan")));

        register(new CustomItem(plugin,
                "titan-chestplate",
                "<gradient:#7700ff:#39ff14>☢ Titan Chestplate</gradient>",
                List.of("<dark_purple>Forged in the Titan Reactor.</dark_purple>",
                        "<purple>☢ Grants Regeneration + +4 Max Hearts</purple>",
                        "<green>▸ Radiation Resistance: TOTAL (full set)</green>"),
                Material.NETHERITE_CHESTPLATE,
                1503,
                true).withEquippableModel(new NamespacedKey("nuclearcraft", "titan")));

        register(new CustomItem(plugin,
                "titan-leggings",
                "<gradient:#7700ff:#39ff14>☢ Titan Leggings</gradient>",
                List.of("<dark_purple>Forged in the Titan Reactor.</dark_purple>",
                        "<purple>☢ Grants Speed I + Resistance I</purple>",
                        "<green>▸ Radiation Resistance: TOTAL (full set)</green>"),
                Material.NETHERITE_LEGGINGS,
                1504,
                true).withEquippableModel(new NamespacedKey("nuclearcraft", "titan")));

        register(new CustomItem(plugin,
                "titan-boots",
                "<gradient:#7700ff:#39ff14>☢ Titan Boots</gradient>",
                List.of("<dark_purple>Forged in the Titan Reactor.</dark_purple>",
                        "<purple>☢ Grants Jump Boost II</purple>",
                        "<green>▸ Radiation Resistance: TOTAL (full set)</green>"),
                Material.NETHERITE_BOOTS,
                1505,
                true).withEquippableModel(new NamespacedKey("nuclearcraft", "titan")));

        register(new CustomItem(plugin,
                "titan-sword",
                "<gradient:#7700ff:#ff0044>☢ Titan Sword</gradient>",
                List.of("<dark_purple>Channels the Titan's reactor energy.</dark_purple>",
                        "<red>☢ Radiation blast on hit</red>",
                        "<purple>☢ AOE radiation wave within 4 blocks</purple>",
                        "<gold>15% critical radiation explosion</gold>"),
                Material.NETHERITE_SWORD,
                1506,
                true));

        register(new CustomItem(plugin,
                "titan-axe",
                "<gradient:#7700ff:#ff6600>☢ Titan Axe</gradient>",
                List.of("<dark_purple>A devastating nuclear shockwave weapon.</dark_purple>",
                        "<red>☢ Radiation + Weakness on hit</red>",
                        "<purple>☢ Shockwave knocks back 5-block radius</purple>"),
                Material.NETHERITE_AXE,
                1507,
                true));

        register(new CustomItem(plugin,
                "titan-pickaxe",
                "<gradient:#7700ff:#00ccff>☢ Titan Pickaxe</gradient>",
                List.of("<dark_purple>Mines at unimaginable speed.</dark_purple>",
                        "<aqua>▸ Max Mining Speed</aqua>",
                        "<green>✔ Safe for all ore types</green>"),
                Material.NETHERITE_PICKAXE,
                1508,
                true));

        register(new CustomItem(plugin,
                "titan-shovel",
                "<gradient:#7700ff:#88ff44>☢ Titan Shovel</gradient>",
                List.of("<dark_purple>Terraforms with nuclear force.</dark_purple>",
                        "<aqua>▸ Instant excavation</aqua>"),
                Material.NETHERITE_SHOVEL,
                1509,
                true));

        register(new CustomItem(plugin,
                "titan-hoe",
                "<gradient:#7700ff:#ffdd00>☢ Titan Hoe</gradient>",
                List.of("<dark_purple>Accelerates radioactive crop growth.</dark_purple>",
                        "<green>▸ Instantly tills large areas</green>"),
                Material.NETHERITE_HOE,
                1510,
                true));

        register(new CustomItem(plugin,
                "titan-bow",
                "<gradient:#7700ff:#ff00aa>☢ Titan Bow</gradient>",
                List.of("<dark_purple>Fires arrows supercharged with reactor energy.</dark_purple>",
                        "<red>☢ Fires Titan Arrows automatically</red>",
                        "<purple>☢ AOE radiation burst on impact</purple>",
                        "<gold>Glows targets for 6 seconds</gold>"),
                Material.BOW,
                1511,
                true));

        register(new CustomItem(plugin,
                "titan-arrow",
                "<gradient:#7700ff:#ff00aa>☢ Titan Arrow</gradient>",
                List.of("<dark_purple>Supercharged with reactor radiation.</dark_purple>",
                        "<red>☢ Applies heavy radiation + Poison on impact</red>",
                        "<purple>☢ 3-block AOE burst on detonation</purple>"),
                Material.SPECTRAL_ARROW,
                1512,
                false));

        // ── Phase 6: Equipment crafting materials ─────────────────────────────

        register(new CustomItem(plugin,
                "industrial-fabric",
                "<gradient:#ffcc00:#ffffff>Industrial Fabric</gradient>",
                List.of("Woven from radiation-hardened fibres.",
                        "<green>Used to craft and repair Hazmat Suits</green>",
                        "<yellow>☢ Radiation-resistant material</yellow>"),
                Material.YELLOW_WOOL,
                1113,
                false));

        // ── Phase 5: Nuclear Smelter ──────────────────────────────────────────

        register(new CustomItem(plugin,
                "nuclear-smelter",
                "<gradient:#39ff14:#00bfff>Nuclear Smelter</gradient>",
                List.of("<gray>Industrial-grade plutonium refining machine.</gray>",
                        "<yellow>☢ Refines Raw Plutonium Fragments</yellow>",
                        "<gray>into Refined Plutonium Ingots.</gray>",
                        "<dark_gray>Requires fuel: Coal, Blaze Rod, or Lava Bucket</dark_gray>",
                        "<green>Right-click to open machine interface</green>"),
                Material.SMOKER,
                1201,
                true));

        // ── Phase 8: Nuclear Forge ────────────────────────────────────────────

        register(new CustomItem(plugin,
                "nuclear-forge",
                "<gradient:#39ff14:#ffaa00>Nuclear Forge</gradient>",
                List.of("<gray>Advanced equipment upgrade machine.</gray>",
                        "<yellow>☢ Upgrade Plutonium & Hazmat gear to MK-IV</yellow>",
                        "<gray>Requires Radioactive Cores for energy</gray>",
                        "<green>Right-click to open upgrade interface</green>"),
                Material.CARTOGRAPHY_TABLE,
                1401,
                true));

        // ── Phase 7: Cure items ───────────────────────────────────────────────

        register(new CustomItem(plugin,
                "radiation-antidote",
                "<gradient:#00ffcc:#39ff14>Radiation Antidote</gradient>",
                List.of("<gray>Brewed from Healing Petals and Honey.</gray>",
                        "<aqua>Right-click to consume</aqua>",
                        "<green>✔ Clears all radiation</green>",
                        "<green>✔ Removes infection and debuffs</green>",
                        "<yellow>✘ Does not grant immunity</yellow>"),
                Material.HONEY_BOTTLE,
                1301,
                true));

        register(new CustomItem(plugin,
                "radiation-serum",
                "<gradient:#39ff14:#7700ff>Radiation Serum</gradient>",
                List.of("<gray>Advanced nuclear cure — handle with care.</gray>",
                        "<aqua>Right-click to consume</aqua>",
                        "<green>✔ Instantly clears all radiation</green>",
                        "<green>✔ Removes infection and debuffs</green>",
                        "<gold>✔ Grants 10 min radiation immunity</gold>",
                        "<yellow>☢ Rare and expensive to craft</yellow>"),
                Material.GLASS_BOTTLE,
                1302,
                true));
    }

    private void register(CustomItem item) {
        registry.register(item);
    }

    public Optional<CustomItem> getItem(String id) {
        return registry.get(id);
    }

    public ItemRegistry getRegistry() {
        return registry;
    }
}
