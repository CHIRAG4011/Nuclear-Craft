package com.nuclearcraft.events;

import com.nuclearcraft.forge.ForgeData;
import com.nuclearcraft.upgrade.UpgradeTier;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/**
 * Fired when a Nuclear Forge upgrade fails.
 * Materials are always lost on failure.
 * Equipment is returned safely (possible downgrade only at MK-IV per config).
 */
public class ForgeUpgradeFailEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final ForgeData forgeData;
    private final ItemStack returnedEquipment;
    private final UpgradeTier attemptedTier;
    private final boolean downgraded;

    public ForgeUpgradeFailEvent(Player player, ForgeData forgeData,
                                  ItemStack returnedEquipment,
                                  UpgradeTier attemptedTier, boolean downgraded) {
        this.player = player;
        this.forgeData = forgeData;
        this.returnedEquipment = returnedEquipment;
        this.attemptedTier = attemptedTier;
        this.downgraded = downgraded;
    }

    public Player getPlayer()                   { return player; }
    public ForgeData getForgeData()             { return forgeData; }
    public ItemStack getReturnedEquipment()      { return returnedEquipment; }
    public UpgradeTier getAttemptedTier()       { return attemptedTier; }
    /** True if MK-IV downgrade proc'd and the item was demoted to MK-III. */
    public boolean isDowngraded()               { return downgraded; }

    @Override public HandlerList getHandlers()  { return HANDLERS; }
    public static HandlerList getHandlerList()  { return HANDLERS; }
}
