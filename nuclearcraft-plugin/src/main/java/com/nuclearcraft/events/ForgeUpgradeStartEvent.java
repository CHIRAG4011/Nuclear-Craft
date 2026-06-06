package com.nuclearcraft.events;

import com.nuclearcraft.forge.ForgeData;
import com.nuclearcraft.upgrade.UpgradeTier;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/**
 * Fired when a player initiates an upgrade in the Nuclear Forge.
 * Cancelling this event prevents the upgrade from starting.
 */
public class ForgeUpgradeStartEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final ForgeData forgeData;
    private final ItemStack equipment;
    private final UpgradeTier currentTier;
    private final UpgradeTier targetTier;
    private boolean cancelled;

    public ForgeUpgradeStartEvent(Player player, ForgeData forgeData,
                                   ItemStack equipment, UpgradeTier currentTier,
                                   UpgradeTier targetTier) {
        this.player = player;
        this.forgeData = forgeData;
        this.equipment = equipment;
        this.currentTier = currentTier;
        this.targetTier = targetTier;
        this.cancelled = false;
    }

    public Player getPlayer()           { return player; }
    public ForgeData getForgeData()     { return forgeData; }
    public ItemStack getEquipment()     { return equipment; }
    public UpgradeTier getCurrentTier() { return currentTier; }
    public UpgradeTier getTargetTier()  { return targetTier; }

    @Override public boolean isCancelled()              { return cancelled; }
    @Override public void setCancelled(boolean cancel)  { this.cancelled = cancel; }
    @Override public HandlerList getHandlers()          { return HANDLERS; }
    public static HandlerList getHandlerList()          { return HANDLERS; }
}
