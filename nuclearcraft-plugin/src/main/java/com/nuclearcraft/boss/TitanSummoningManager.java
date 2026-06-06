package com.nuclearcraft.boss;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.utils.ColorUtil;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Handles the Plutonium Titan summoning ritual.
 *
 * Altar pattern (viewed from above, Y-plane):
 *
 *   C . . . C
 *   . . . . .
 *   . . P . .   ← plutonium block at center
 *   . . . . .
 *   C . . . C
 *
 *   C = Radioactive Core (custom item on top of an obsidian block)
 *   P = Plutonium Block (CRYING_OBSIDIAN) at center
 *
 * An Irradiated Heart must be placed on top of the Plutonium Block
 * to trigger the ritual.
 *
 * Requirements (checked in player inventory at ritual start):
 *   4× Radioactive Core (consumed)
 *   1× Irradiated Heart (consumed)
 *
 * On success: 15-second summoning animation → TitanManager.spawnTitan()
 */
public class TitanSummoningManager {

    private final JavaPlugin     plugin;
    private final ConfigManager  configManager;
    private final ItemManager    itemManager;
    private final TitanManager   titanManager;

    /** UUIDs of players currently performing a ritual (prevents duplicate triggers). */
    private final Set<UUID>    ritualInProgress = new HashSet<>();
    private final Map<UUID, BukkitTask> ritualTasks = new HashMap<>();

    /** Global cooldown after a Titan is summoned (in ms). */
    private long lastSummonTime  = 0L;
    private long summonCooldownMs = 60 * 60 * 1000L; // 60 min default

    // Altar offsets from center block (relative X,Z)
    private static final int[][] CORNER_OFFSETS = {{2,2},{2,-2},{-2,2},{-2,-2}};

    public TitanSummoningManager(JavaPlugin plugin, ConfigManager configManager,
                                  ItemManager itemManager, TitanManager titanManager) {
        this.plugin       = plugin;
        this.configManager = configManager;
        this.itemManager  = itemManager;
        this.titanManager = titanManager;
    }

    public void initialize() {
        summonCooldownMs = configManager.getTitan()
                .getLong("titan.summoning.cooldown-minutes", 60L) * 60_000L;
        NCLogger.debug("TitanSummoningManager initialized.");
    }

    public void shutdown() {
        ritualTasks.values().forEach(BukkitTask::cancel);
        ritualTasks.clear();
        ritualInProgress.clear();
    }

    // ── Public trigger point ──────────────────────────────────────────────────

    /**
     * Called by TitanListener when a player places an Irradiated Heart on a
     * Plutonium Block (CRYING_OBSIDIAN). Validates altar and starts the ritual.
     */
    public void tryTriggerRitual(Player player, Block centerBlock) {
        if (!configManager.getTitan().getBoolean("titan.enabled", true)) return;

        if (titanManager.isTitanAlive()) {
            player.sendMessage(ColorUtil.parse(
                    "<red>☢ The Titan is already active!</red>"));
            return;
        }

        if (ritualInProgress.contains(player.getUniqueId())) return;

        long now = System.currentTimeMillis();
        if (now - lastSummonTime < summonCooldownMs) {
            long remaining = (summonCooldownMs - (now - lastSummonTime)) / 60_000L;
            player.sendMessage(ColorUtil.parse(
                    "<red>☢ The altar is not yet ready. " + remaining + " minutes remaining.</red>"));
            return;
        }

        // Validate altar structure
        if (!isValidAltar(centerBlock)) {
            player.sendMessage(ColorUtil.parse(
                    "<red>☢ The altar is incomplete. Place Radioactive Cores at each corner!</red>"));
            return;
        }

        // Consume items from inventory
        if (!consumeRitualItems(player)) {
            player.sendMessage(ColorUtil.parse(
                    "<red>☢ You need: 4 Radioactive Cores + 1 Irradiated Heart to summon the Titan!</red>"));
            return;
        }

        startRitual(player, centerBlock.getLocation());
    }

    // ── Altar validation ──────────────────────────────────────────────────────

    private boolean isValidAltar(Block center) {
        if (center.getType() != Material.CRYING_OBSIDIAN) return false;
        World world = center.getWorld();
        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();

        for (int[] off : CORNER_OFFSETS) {
            Block cornerBase = world.getBlockAt(cx + off[0], cy, cz + off[1]);
            if (cornerBase.getType() != Material.OBSIDIAN) return false;
        }
        return true;
    }

    private boolean consumeRitualItems(Player player) {
        var inv = player.getInventory();

        // Count what we need
        Optional<ItemStack> coreTemplate = itemManager.getItem("radioactive-core")
                .map(i -> i.build());
        Optional<ItemStack> heartTemplate = itemManager.getItem("irradiated-heart")
                .map(i -> i.build());

        if (coreTemplate.isEmpty() || heartTemplate.isEmpty()) return false;

        int coresFound  = 0;
        int heartsFound = 0;
        for (ItemStack item : inv.getContents()) {
            if (item == null) continue;
            if (item.isSimilar(coreTemplate.get()))  coresFound  += item.getAmount();
            if (item.isSimilar(heartTemplate.get()))  heartsFound += item.getAmount();
        }
        if (coresFound < 4 || heartsFound < 1) return false;

        // Remove items
        removeItems(player, coreTemplate.get(), 4);
        removeItems(player, heartTemplate.get(), 1);
        return true;
    }

    private void removeItems(Player player, ItemStack template, int amount) {
        var inv = player.getInventory();
        int remaining = amount;
        for (int i = 0; i < inv.getSize() && remaining > 0; i++) {
            ItemStack slot = inv.getItem(i);
            if (slot == null || !slot.isSimilar(template)) continue;
            if (slot.getAmount() <= remaining) {
                remaining -= slot.getAmount();
                inv.setItem(i, null);
            } else {
                slot.setAmount(slot.getAmount() - remaining);
                remaining = 0;
            }
        }
    }

    // ── Ritual animation ──────────────────────────────────────────────────────

    private void startRitual(Player summoner, Location center) {
        ritualInProgress.add(summoner.getUniqueId());
        lastSummonTime = System.currentTimeMillis();

        // Announce ritual start to all nearby
        for (Player p : summoner.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(center) <= 200 * 200) {
                p.sendMessage(ColorUtil.parse(
                        "<dark_red>☢ A Titan summoning ritual has begun... ☢</dark_red>"));
            }
        }

        World world = center.getWorld();
        final int[] elapsed = {0};
        final int   DURATION_TICKS = 15 * 20;

        final org.bukkit.scheduler.BukkitTask[] taskRef = {null};
        taskRef[0] = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (elapsed[0] >= DURATION_TICKS) {
                taskRef[0].cancel();
                ritualTasks.remove(summoner.getUniqueId());
                ritualInProgress.remove(summoner.getUniqueId());
                completeSummoning(summoner, center);
                return;
            }

            double progress = (double) elapsed[0] / DURATION_TICKS;
            spawnRitualEffects(world, center, progress);
            elapsed[0]++;
        }, 0L, 1L);

        ritualTasks.put(summoner.getUniqueId(), taskRef[0]);
        NCLogger.info("Titan summoning ritual started by " + summoner.getName());
    }

    private void spawnRitualEffects(World world, Location center, double progress) {
        if (world == null) return;
        double rotation = progress * Math.PI * 10;
        double height   = 1 + progress * 3;

        // Spiral beam upward
        for (int i = 0; i < 3; i++) {
            double angle = rotation + (i * Math.PI * 2 / 3);
            double radius = 2 + Math.sin(progress * Math.PI) * 3;
            double x = center.getX() + Math.cos(angle) * radius;
            double z = center.getZ() + Math.sin(angle) * radius;
            Location pLoc = new Location(world, x, center.getY() + height, z);
            world.spawnParticle(Particle.ENTITY_EFFECT, pLoc, 2, 0.1, 0.1, 0.1, 0.05);
            world.spawnParticle(Particle.ENCHANTED_HIT, pLoc, 1, 0, 0, 0, 0.0);
        }

        // Energy beam from corners to center
        for (int[] off : CORNER_OFFSETS) {
            Location corner = center.clone().add(off[0], 0, off[1]);
            world.spawnParticle(Particle.ELECTRIC_SPARK, corner.clone().add(0, 1, 0),
                    3, 0.2, 0.5, 0.2, 0.0);
        }

        // Ground shaking sound
        if (elapsed(progress) % 20 == 0) {
            world.playSound(center, Sound.ENTITY_RAVAGER_STEP, 2.0f,
                    0.5f + (float)(progress * 0.5f));
        }

        if (elapsed(progress) % 5 == 0) {
            world.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1.5f,
                    0.5f + (float)(progress));
        }
    }

    private int elapsed(double progress) {
        return (int)(progress * 15 * 20);
    }

    private void completeSummoning(Player summoner, Location center) {
        World world = center.getWorld();
        if (world == null) return;

        // Final flash
        world.spawnParticle(Particle.EXPLOSION_EMITTER, center.clone().add(0, 2, 0),
                5, 2.0, 1.0, 2.0, 0.0);
        world.playSound(center, Sound.ENTITY_WITHER_SPAWN, 4.0f, 0.5f);

        // Update stat
        var pd = plugin.getServer().getPluginManager().getPlugin("NuclearCraft");
        // (stats update handled in TitanManager after spawn)

        // Spawn the titan slightly above the altar center
        Location spawnLoc = center.clone().add(0, 2, 0);
        titanManager.spawnTitan(spawnLoc, summoner);
        NCLogger.info("Summoning complete — Titan spawned at " + center);
    }
}
