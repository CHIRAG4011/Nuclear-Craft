package com.nuclearcraft.farming;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.equipment.RadioactiveFarmlandManager;
import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.utils.ColorUtil;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Validates and applies mutated seed planting rules.
 *
 * <p>Mutated Seeds can ONLY be planted on Radioactive Farmland.
 * Attempting to plant on any other surface cancels the action and
 * sends the player a warning message.
 */
public class MutatedSeedManager {

    private final ItemManager itemManager;
    private final ConfigManager configManager;
    private final RadioactiveFarmlandManager farmlandManager;
    private final MutatedCropManager cropManager;

    public MutatedSeedManager(ItemManager itemManager,
                               ConfigManager configManager,
                               RadioactiveFarmlandManager farmlandManager,
                               MutatedCropManager cropManager) {
        this.itemManager     = itemManager;
        this.configManager   = configManager;
        this.farmlandManager = farmlandManager;
        this.cropManager     = cropManager;
    }

    public void initialize() {
        NCLogger.info("MutatedSeedManager initialized.");
    }

    public void shutdown() {}

    // ──────────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Returns true if the given ItemStack is a Mutated Seed.
     */
    public boolean isMutatedSeed(ItemStack item) {
        return itemManager.getItem("mutated-seed")
                .map(ci -> ci.matches(item))
                .orElse(false);
    }

    /**
     * Attempts to plant a mutated seed on the clicked farmland block.
     *
     * <p>Returns true if the planting was successful (caller should cancel the original event).
     * Returns false if planting was denied (already cancelled with message).
     *
     * @param player    the player planting the seed
     * @param farmland  the farmland block clicked (must be FARMLAND type)
     * @param seedItem  the seed item in the player's hand
     */
    public boolean attemptPlant(Player player, Block farmland, ItemStack seedItem) {
        // Must be radioactive farmland
        if (!farmlandManager.isFarmland(farmland.getLocation())) {
            player.sendMessage(ColorUtil.parse(
                    configManager.getMessage("farming.wrong-farmland")));
            return false;
        }

        // Target location is one block ABOVE the farmland
        Block cropBlock = farmland.getRelative(0, 1, 0);

        // Space must be air
        if (!cropBlock.getType().isAir()) {
            player.sendMessage(ColorUtil.parse(
                    configManager.getMessage("farming.no-space")));
            return false;
        }

        // Already tracked (shouldn't happen, but guard)
        if (cropManager.isMutatedCrop(cropBlock.getLocation())) {
            return false;
        }

        // Plant the crop
        UUID planterUuid = player.getUniqueId();
        cropManager.registerCrop(cropBlock.getLocation(), planterUuid);

        // Consume 1 seed from hand
        if (seedItem.getAmount() > 1) {
            seedItem.setAmount(seedItem.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        player.sendMessage(ColorUtil.parse(
                configManager.getMessage("farming.seed-planted")));
        NCLogger.debug("Mutated seed planted by %s at %s", player.getName(), cropBlock.getLocation());
        return true;
    }
}
