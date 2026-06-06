package com.nuclearcraft.equipment;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.items.CustomItem;
import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.radiation.RadiationSource;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.Set;

/**
 * Centralises all radiation-resistance calculations for the NuclearCraft equipment system.
 *
 * <p>All radiation-applying code passes through {@link #getMultiplier(Player, RadiationSource)}
 * to determine how much radiation actually lands on the player. No duplicate calculations.
 *
 * <h3>Protection rules</h3>
 * <ul>
 *   <li><b>Full Plutonium Armor set</b> — 100% immunity to all <em>environmental</em> sources.</li>
 *   <li><b>Hazmat Armor pieces</b> — cumulative percentage reduction capped at the full-set cap.
 *       Per-piece values come from {@code equipment.yml}.</li>
 *   <li><b>Combat sources</b> — never blocked by plutonium armor (PvP balance).
 *       Hazmat armor still reduces combat sources normally.</li>
 * </ul>
 *
 * <h3>Environmental sources (blocked by full plutonium set)</h3>
 * PLUTONIUM_ORE, RADIOACTIVE_DEBRIS, RADIOACTIVE_SOIL, RADIOACTIVE_FARMLAND,
 * PLUTONIUM_FRAGMENT, NUCLEAR_SMELTER, RADIATION_CLOUD
 */
public class RadiationResistanceManager {

    /** Sources considered "environmental" — blocked by the full plutonium armor set. */
    private static final Set<RadiationSource> ENVIRONMENTAL = Set.of(
            RadiationSource.PLUTONIUM_ORE,
            RadiationSource.RADIOACTIVE_DEBRIS,
            RadiationSource.RADIOACTIVE_SOIL,
            RadiationSource.RADIOACTIVE_FARMLAND,
            RadiationSource.PLUTONIUM_FRAGMENT,
            RadiationSource.NUCLEAR_SMELTER,
            RadiationSource.RADIATION_CLOUD
    );

    private final ItemManager itemManager;
    private final ConfigManager configManager;

    public RadiationResistanceManager(ItemManager itemManager, ConfigManager configManager) {
        this.itemManager   = itemManager;
        this.configManager = configManager;
    }

    public void initialize() {
        NCLogger.info("RadiationResistanceManager initialized.");
    }

    public void shutdown() {}

    // ──────────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Returns the fraction of radiation that passes through the player's armor protection.
     * <ul>
     *   <li>1.0 = no protection — full radiation applied.</li>
     *   <li>0.0 = complete immunity — radiation is fully blocked.</li>
     * </ul>
     *
     * @param player the player receiving radiation
     * @param source the source of the radiation
     * @return value in [0.0, 1.0]
     */
    public double getMultiplier(Player player, RadiationSource source) {
        if (source == RadiationSource.COMMAND) return 1.0; // admin override

        // Full plutonium set provides environmental immunity
        if (ENVIRONMENTAL.contains(source) && wearingFullPlutoniumSet(player)) {
            return 0.0;
        }

        // Hazmat reduction applies to all sources
        double hazmateReduction = getHazmatReduction(player);
        return Math.max(0.0, 1.0 - hazmateReduction);
    }

    /**
     * Returns true if the player is wearing a complete Plutonium Armor set.
     */
    public boolean wearingFullPlutoniumSet(Player player) {
        var inv = player.getInventory();
        return isPiece(inv.getHelmet(),    "plutonium-helmet")
            && isPiece(inv.getChestplate(),"plutonium-chestplate")
            && isPiece(inv.getLeggings(),  "plutonium-leggings")
            && isPiece(inv.getBoots(),     "plutonium-boots");
    }

    /**
     * Returns true if the player is wearing a complete Hazmat Armor set.
     */
    public boolean wearingFullHazmatSet(Player player) {
        var inv = player.getInventory();
        return isPiece(inv.getHelmet(),    "hazmat-helmet")
            && isPiece(inv.getChestplate(),"hazmat-chestplate")
            && isPiece(inv.getLeggings(),  "hazmat-leggings")
            && isPiece(inv.getBoots(),     "hazmat-boots");
    }

    /**
     * Counts how many plutonium armor pieces the player is wearing (0–4).
     */
    public int plutoniumPiecesWorn(Player player) {
        var inv = player.getInventory();
        int count = 0;
        if (isPiece(inv.getHelmet(),    "plutonium-helmet"))    count++;
        if (isPiece(inv.getChestplate(),"plutonium-chestplate"))count++;
        if (isPiece(inv.getLeggings(),  "plutonium-leggings"))  count++;
        if (isPiece(inv.getBoots(),     "plutonium-boots"))     count++;
        return count;
    }

    /**
     * Checks whether the player is wearing a specific NuclearCraft equipment piece.
     */
    public boolean isWearing(Player player, String itemId) {
        var inv = player.getInventory();
        return isPiece(inv.getHelmet(),    itemId)
            || isPiece(inv.getChestplate(),itemId)
            || isPiece(inv.getLeggings(),  itemId)
            || isPiece(inv.getBoots(),     itemId);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private double getHazmatReduction(Player player) {
        var cfg = configManager.getEquipment();
        var inv = player.getInventory();

        double reduction = 0.0;
        if (isPiece(inv.getHelmet(),    "hazmat-helmet"))
            reduction += cfg.getDouble("armor.hazmat.helmet.radiation-reduction", 0.20);
        if (isPiece(inv.getChestplate(),"hazmat-chestplate"))
            reduction += cfg.getDouble("armor.hazmat.chestplate.radiation-reduction", 0.30);
        if (isPiece(inv.getLeggings(),  "hazmat-leggings"))
            reduction += cfg.getDouble("armor.hazmat.leggings.radiation-reduction", 0.20);
        if (isPiece(inv.getBoots(),     "hazmat-boots"))
            reduction += cfg.getDouble("armor.hazmat.boots.radiation-reduction", 0.10);

        double cap = cfg.getDouble("armor.hazmat.full-set-bonus", 0.80);
        return Math.min(reduction, cap);
    }

    private boolean isPiece(ItemStack item, String id) {
        if (item == null) return false;
        Optional<CustomItem> ci = itemManager.getItem(id);
        return ci.isPresent() && ci.get().matches(item);
    }
}
