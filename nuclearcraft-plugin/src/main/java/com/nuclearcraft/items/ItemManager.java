package com.nuclearcraft.items;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.Material;

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

        // ── Phase 6: Equipment crafting materials ─────────────────────────────

        register(new CustomItem(plugin,
                "industrial-fabric",
                "<gradient:#ffcc00:#ffffff>Industrial Fabric</gradient>",
                List.of("Woven from radiation-hardened fibres.",
                        "<green>Used to craft and repair Hazmat Suits</green>",
                        "<yellow>☢ Radiation-resistant material</yellow>"),
                Material.YELLOW_WOOL,
                1315,
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
                Material.BLAST_FURNACE,
                1201,
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
