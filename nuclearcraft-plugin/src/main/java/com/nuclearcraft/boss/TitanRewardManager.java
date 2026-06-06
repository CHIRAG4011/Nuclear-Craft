package com.nuclearcraft.boss;

import com.nuclearcraft.boss.events.TitanRewardEvent;
import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.data.PlayerData;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.utils.ColorUtil;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.*;
import org.bukkit.entity.Giant;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * Distributes loot and XP to all participants when the Titan dies.
 * Rewards scale with contribution percentage.
 * A loot chest is spawned at the death location for guaranteed drops.
 */
public class TitanRewardManager {

    private final JavaPlugin        plugin;
    private final ConfigManager     configManager;
    private final ItemManager       itemManager;
    private final PlayerDataManager playerDataManager;

    public TitanRewardManager(JavaPlugin plugin, ConfigManager configManager,
                               ItemManager itemManager, PlayerDataManager playerDataManager) {
        this.plugin           = plugin;
        this.configManager    = configManager;
        this.itemManager      = itemManager;
        this.playerDataManager = playerDataManager;
    }

    public void initialize() {
        NCLogger.debug("TitanRewardManager initialized.");
    }

    public void shutdown() {}

    // ── Death sequence ────────────────────────────────────────────────────────

    public void onTitanDeath(Giant titan, TitanDamageTracker tracker, Player killCredit) {
        Location deathLoc = titan.getLocation().clone();

        playDeathSequence(deathLoc);

        // Broadcast global message
        plugin.getServer().broadcast(ColorUtil.parse(
                "<gradient:#7700ff:#39ff14>☢ THE PLUTONIUM TITAN HAS BEEN DEFEATED ☢</gradient>"));
        if (killCredit != null) {
            plugin.getServer().broadcast(ColorUtil.parse(
                    configManager.getTitan()
                            .getString("titan.rewards.kill-broadcast",
                                    "<gradient:#39ff14:#00ff88>{player} has slain the Plutonium Titan!</gradient>")
                            .replace("{player}", killCredit.getName())));
        }

        // Distribute rewards to participants (delayed to let death animation play)
        plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                distributeRewards(tracker, deathLoc), 40L);
    }

    // ── Reward distribution ───────────────────────────────────────────────────

    private void distributeRewards(TitanDamageTracker tracker, Location deathLoc) {
        Map<UUID, Double> contributions = tracker.snapshot();
        double totalDamage = tracker.getTotalDamage();

        var cfg      = configManager.getTitan();
        int baseXp   = cfg.getInt("titan.rewards.xp", 5000);

        List<Player> onlinePlayers = new ArrayList<>();
        for (UUID uuid : contributions.keySet()) {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null && p.isOnline()) onlinePlayers.add(p);
        }

        for (Player player : onlinePlayers) {
            double pct = totalDamage > 0
                    ? tracker.getContributionPercent(player.getUniqueId()) : (1.0 / onlinePlayers.size());

            if (pct < 0.01) continue; // AFK guard

            List<ItemStack> rewards = buildRewardList(pct);

            int xp = (int)(baseXp * Math.max(0.25, pct));
            player.giveExpLevels(xp / 100);
            player.giveExp(xp);

            for (ItemStack item : rewards) {
                if (item != null) player.getInventory().addItem(item);
            }

            // Update player stats
            PlayerData pd = playerDataManager.get(player.getUniqueId()).orElse(null);
            if (pd != null) {
                pd.incrementTitanKills();
                long cores = rewards.stream()
                        .filter(i -> i != null && itemManager.getItem("titan-core").isPresent()
                                && i.isSimilar(itemManager.getItem("titan-core").get().build()))
                        .mapToLong(ItemStack::getAmount).sum();
                pd.addTitanCoresObtained((int) cores);
                pd.markDirty();
            }

            player.sendMessage(ColorUtil.parse(
                    "<gold>☢ You receive Titan rewards! Contribution: "
                            + (int)(pct * 100) + "%</gold>"));

            // Fire API event
            plugin.getServer().getPluginManager().callEvent(
                    new TitanRewardEvent(player, rewards, xp, pct));
        }

        // Spawn a loot chest at the death location with bonus drops
        spawnLootChest(deathLoc, onlinePlayers.size());
        NCLogger.info("Titan rewards distributed to " + onlinePlayers.size() + " players.");
    }

    private List<ItemStack> buildRewardList(double contributionPct) {
        var cfg    = configManager.getTitan();
        Random rng = new Random();
        List<ItemStack> rewards = new ArrayList<>();

        // Guaranteed: Titan Core (1-2 based on contribution)
        int coreCount = contributionPct >= 0.30 ? 2 : 1;
        itemManager.getItem("titan-core").ifPresent(c ->
                rewards.add(c.build(coreCount)));

        // Guaranteed: Titan Fragments (scales with contribution)
        int fragCount = Math.max(1, (int)(8 * contributionPct));
        itemManager.getItem("titan-fragment").ifPresent(f ->
                rewards.add(f.build(fragCount)));

        // Bonus: Refined Plutonium Ingots
        if (contributionPct >= 0.05) {
            int ingotCount = Math.max(2, (int)(12 * contributionPct));
            itemManager.getItem("refined-plutonium-ingot").ifPresent(i ->
                    rewards.add(i.build(ingotCount)));
        }

        // Rare: Reactor Heart (10% per player regardless of contribution)
        double heartChance = cfg.getDouble("titan.rewards.reactor-heart-chance", 0.10);
        if (rng.nextDouble() < heartChance) {
            itemManager.getItem("reactor-heart").ifPresent(h -> rewards.add(h.build()));
        }

        // Rare: Ancient Reactor Blueprint (5% chance)
        double blueprintChance = cfg.getDouble("titan.rewards.blueprint-chance", 0.05);
        if (rng.nextDouble() < blueprintChance) {
            itemManager.getItem("ancient-reactor-blueprint").ifPresent(b -> rewards.add(b.build()));
        }

        // Rare: Mutated Crystal (8% chance)
        double crystalChance = cfg.getDouble("titan.rewards.mutated-crystal-chance", 0.08);
        if (rng.nextDouble() < crystalChance) {
            itemManager.getItem("mutated-crystal").ifPresent(c -> rewards.add(c.build()));
        }

        return rewards;
    }

    private void spawnLootChest(Location loc, int playerCount) {
        // Place a chest above ground at death location with extra loot
        Location chestLoc = loc.clone().add(0, 1, 0);
        World world       = chestLoc.getWorld();
        if (world == null) return;

        world.getBlockAt(chestLoc).setType(Material.CHEST);
        if (world.getBlockAt(chestLoc).getState() instanceof
                org.bukkit.block.Chest chest) {
            var inv = chest.getInventory();
            itemManager.getItem("titan-core").ifPresent(c ->
                    inv.addItem(c.build(Math.max(1, playerCount / 4))));
            itemManager.getItem("titan-fragment").ifPresent(f ->
                    inv.addItem(f.build(playerCount * 2)));
            itemManager.getItem("refined-plutonium-ingot").ifPresent(i ->
                    inv.addItem(i.build(16)));
        }

        world.spawnParticle(Particle.TOTEM_OF_UNDYING, chestLoc.clone().add(0.5, 0.5, 0.5),
                30, 0.5, 0.5, 0.5, 0.2);
        world.playSound(chestLoc, Sound.ENTITY_PLAYER_LEVELUP, 2.0f, 0.8f);
    }

    // ── Death sequence visuals ────────────────────────────────────────────────

    private void playDeathSequence(Location loc) {
        World world = loc.getWorld();
        if (world == null) return;
        world.spawnParticle(Particle.EXPLOSION_EMITTER, loc, 15, 3.0, 2.0, 3.0, 0.0);
        world.spawnParticle(Particle.ENTITY_EFFECT, loc, 200, 4.0, 3.0, 4.0, 0.1);
        world.spawnParticle(Particle.LAVA, loc, 30, 2.0, 1.0, 2.0, 0.1);
        world.playSound(loc, Sound.ENTITY_WITHER_DEATH, 5.0f, 0.5f);
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 5.0f, 0.4f);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            world.spawnParticle(Particle.EXPLOSION_EMITTER, loc, 8, 4.0, 2.0, 4.0, 0.0);
            world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_DEATH, 3.0f, 0.6f);
        }, 30L);
    }
}
