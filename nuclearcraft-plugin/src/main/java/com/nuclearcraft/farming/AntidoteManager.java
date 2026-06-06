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
 * Handles consumption of the Radiation Antidote.
 *
 * <h3>Effects on use</h3>
 * <ul>
 *   <li>Clears all radiation (sets level to 0).</li>
 *   <li>Clears infection progress.</li>
 *   <li>Removes radiation-related potion effects.</li>
 *   <li>Does NOT grant radiation immunity.</li>
 * </ul>
 */
public class AntidoteManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final ItemManager itemManager;
    private final PlayerDataManager playerDataManager;
    private final RadiationManager radiationManager;
    private final AdvancementManager advancementManager;

    public AntidoteManager(JavaPlugin plugin,
                            ConfigManager configManager,
                            ItemManager itemManager,
                            PlayerDataManager playerDataManager,
                            RadiationManager radiationManager,
                            AdvancementManager advancementManager) {
        this.plugin             = plugin;
        this.configManager      = configManager;
        this.itemManager        = itemManager;
        this.playerDataManager  = playerDataManager;
        this.radiationManager   = radiationManager;
        this.advancementManager = advancementManager;
    }

    public void initialize() {
        NCLogger.info("AntidoteManager initialized.");
    }

    public void shutdown() {}

    // ──────────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Returns true if the given item is a Radiation Antidote.
     */
    public boolean isAntidote(ItemStack item) {
        return itemManager.getItem("radiation-antidote")
                .map(ci -> ci.matches(item))
                .orElse(false);
    }

    /**
     * Applies the antidote effect to the player.
     * Consumes 1 antidote from their hand.
     *
     * @param player   the player using the antidote
     * @param heldItem the item in their hand (used to consume)
     * @return true if the antidote was applied
     */
    public boolean applyAntidote(Player player, ItemStack heldItem) {
        double radiationBefore = radiationManager.getRadiation(player);

        RadiationCureEvent event = new RadiationCureEvent(player,
                RadiationCureEvent.CureType.ANTIDOTE, radiationBefore);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        // Clear radiation
        radiationManager.clearRadiation(player);

        // Clear infection progress
        playerDataManager.get(player).ifPresent(pd -> {
            pd.setInfectionProgress(0.0);
            pd.incrementRadiationCuresUsed();
            pd.incrementAntidotesCrafted(); // count as "used antidote crafted"
            pd.markDirty();
        });

        // Remove radiation debuffs
        removeRadiationDebuffs(player);

        // Consume item
        consumeItem(player, heldItem);

        // Feedback
        player.sendMessage(ColorUtil.parse(configManager.getMessage("farming.antidote-used")));
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0f, 1.2f);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 0.8f, 1.5f);

        // Advancements
        advancementManager.award(player, AdvancementManager.Advancement.RADIATION_SURVIVOR);
        playerDataManager.get(player).ifPresent(pd -> {
            if (pd.getRadiationCuresUsed() >= 1 && radiationBefore >= 750) {
                advancementManager.award(player, AdvancementManager.Advancement.CURED_AT_LAST);
            }
        });

        NCLogger.debug("Antidote used by %s — cleared %.0f radiation", player.getName(), radiationBefore);
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
