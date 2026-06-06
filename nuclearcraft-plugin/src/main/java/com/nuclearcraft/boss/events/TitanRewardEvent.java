package com.nuclearcraft.boss.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** Fired when a player receives rewards for participating in the Titan fight. */
public class TitanRewardEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player          player;
    private final List<ItemStack> rewards;
    private final int             xp;
    private final double          contributionPercent;

    public TitanRewardEvent(Player player, List<ItemStack> rewards,
                            int xp, double contributionPercent) {
        this.player              = player;
        this.rewards             = rewards;
        this.xp                  = xp;
        this.contributionPercent = contributionPercent;
    }

    public Player getPlayer()               { return player; }
    public List<ItemStack> getRewards()     { return rewards; }
    public int getXp()                      { return xp; }
    public double getContributionPercent()  { return contributionPercent; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList()  { return HANDLERS; }
}
