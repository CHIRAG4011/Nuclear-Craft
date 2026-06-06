package com.nuclearcraft.farming;

import com.nuclearcraft.advancements.AdvancementManager;
import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.events.RadiationCureEvent;
import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.radiation.RadiationManager;
import com.nuclearcraft.utils.ColorUtil;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Handles consumption of the Radiation Serum.
 *
 * <h3>Effects on use</h3>
 * <ul>
 *   <li>Instantly clears all radiation.</li>
 *   <li>Clears infection progress.</li>
 *   <li>Removes radiation-related potion effects.</li>
 *   <li>Grants radiation immunity for a configurable duration (default 10 minutes).</li>
 * </ul>
 */
public class SerumManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final ItemManager itemManager;
    private final PlayerDataManager playerDataManager;
    private final RadiationManager radiationManager;
    private final RadiationImmunityManager immunityManager;
    private final AdvancementManager advancementManager;

    public SerumManager(JavaPlugin plugin,
                        ConfigManager configManager,
                        ItemManager itemManager,
                        PlayerDataManager playerDataManager,
                        RadiationManager radiationManager,
                        RadiationImmunityManager immunityManager,
                        AdvancementManager advancementManager) {
        this.plugin             = plugin;
        this.configManager      = configManager;
        this.itemManager        = itemManager;
        this.playerDataManager  = playerDataManager;
        this.radiationManager   = radiationManager;
        this.immunityManager    = immunityManager;
        this.advancementManager = advancementManager;
    }

    public void initialize() {
        NCLogger.info("SerumManager initialized.");
    }

    public void shutdown() {}

    // ──────────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Returns true if the given item is a Radiation Serum.
     */
    public boolean isSerum(ItemStack item) {
        return itemManager.getItem("radiation-serum")
                .map(ci -> ci.matches(item))
                .orElse(false);
    }

    /**
     * Applies the serum effect to the player.
     * Consumes 1 serum from their hand.
     *
     * @param player   the player using the serum
     * @param heldItem the item in their hand
     * @return true if the serum was applied
     */
    public boolean applySerum(Player player, ItemStack heldItem) {
        double radiationBefore = radiationManager.getRadiation(player);

        RadiationCureEvent event = new RadiationCureEvent(player,
                RadiationCureEvent.CureType.SERUM, radiationBefore);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        // Clear radiation
        radiationManager.clearRadiation(player);

        // Clear infection progress
        playerDataManager.get(player).ifPresent(pd -> {
            pd.setInfectionProgress(0.0);
            pd.incrementRadiationCuresUsed();
            pd.incrementSerumsCrafted();
            pd.markDirty();
        });

        // Remove debuffs
        removeRadiationDebuffs(player);

        // Grant immunity
        immunityManager.grantImmunity(player);

        // Consume item
        consumeItem(player, heldItem);

        // Feedback
        long durationMin = configManager.getFarming()
                .getLong("cure.serum.immunity-duration-minutes", 10L);
        player.sendMessage(ColorUtil.parse(
                configManager.getMessage("farming.serum-used")
                        .replace("{minutes}", String.valueOf(durationMin))));
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0f, 0.8f);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 1.8f);

        // Advancements
        advancementManager.award(player, AdvancementManager.Advancement.RADIATION_SURVIVOR);
        playerDataManager.get(player).ifPresent(pd -> {
            if (radiationBefore >= 750) {
                advancementManager.award(player, AdvancementManager.Advancement.CURED_AT_LAST);
            }
        });

        NCLogger.debug("Serum used by %s — cleared %.0f radiation, immunity granted for %d min",
                player.getName(), radiationBefore, durationMin);
        return true;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private void removeRadiationDebuffs(Player player) {
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.WEAKNESS);
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS);
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.HUNGER);
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.NAUSEA);
    }

    private void consumeItem(Player player, ItemStack held) {
        if (held.getAmount() > 1) {
            held.setAmount(held.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
    }
}
