package com.nuclearcraft.events;

import com.nuclearcraft.forge.ForgeData;
import com.nuclearcraft.upgrade.UpgradeTier;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/**
 * Fired when a Nuclear Forge upgrade succeeds and the upgraded item is produced.
 */
public class ForgeUpgradeSuccessEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final ForgeData forgeData;
    private final ItemStack upgradedItem;
    private final UpgradeTier achievedTier;

    public ForgeUpgradeSuccessEvent(Player player, ForgeData forgeData,
                                     ItemStack upgradedItem, UpgradeTier achievedTier) {
        this.player = player;
        this.forgeData = forgeData;
        this.upgradedItem = upgradedItem;
        this.achievedTier = achievedTier;
    }

    public Player getPlayer()               { return player; }
    public ForgeData getForgeData()         { return forgeData; }
    public ItemStack getUpgradedItem()      { return upgradedItem; }
    public UpgradeTier getAchievedTier()    { return achievedTier; }

    @Override public HandlerList getHandlers()  { return HANDLERS; }
    public static HandlerList getHandlerList()  { return HANDLERS; }
}
