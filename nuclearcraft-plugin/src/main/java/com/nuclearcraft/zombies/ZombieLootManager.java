package com.nuclearcraft.zombies;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.utils.NCLogger;
import com.nuclearcraft.utils.RandomUtil;
import org.bukkit.Material;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles custom drop tables for Irradiated Zombies.
 *
 * On death:
 *   1. Clears vanilla drops.
 *   2. Applies custom loot table based on zombie level.
 *   3. Doubles loot during Radiation Surge.
 *   4. Drops extra XP.
 *
 * Called from ZombieSpawnListener on EntityDeathEvent — no tick loops.
 */
public class ZombieLootManager {

    private final NuclearCraftPlugin plugin;
    private final ConfigManager configManager;
    private final IrradiatedZombieManager zombieManager;
    private final ItemManager itemManager;
    private final ZombieSpawnManager spawnManager;

    public ZombieLootManager(NuclearCraftPlugin plugin, ConfigManager configManager,
                              IrradiatedZombieManager zombieManager, ItemManager itemManager,
                              ZombieSpawnManager spawnManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.zombieManager = zombieManager;
        this.itemManager = itemManager;
        this.spawnManager = spawnManager;
    }

    public void initialize() {
        NCLogger.info("ZombieLootManager initialized.");
    }

    public void shutdown() {}

    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Called from ZombieSpawnListener on EntityDeathEvent.
     * Returns true if the event was handled (irradiated zombie).
     */
    public boolean handleDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Zombie z)) return false;

        var opt = zombieManager.get(z);
        if (opt.isEmpty()) return false;

        IrradiatedZombie iz = opt.get();
        ZombieLevel level = iz.getZombieLevel();
        boolean surge = spawnManager.isSurgeActive();

        // Clear default drops
        event.getDrops().clear();

        // Build custom drops
        List<ItemStack> drops = buildDrops(level, surge);
        event.getDrops().addAll(drops);

        // Custom XP (override default)
        int xp = level.getXpReward();
        if (surge && configManager.getZombies().getBoolean("night-event.double-loot", true)) {
            xp *= 2;
        }
        event.setDroppedExp(xp);

        NCLogger.debug("ZombieLoot: L%d drops=%d xp=%d surge=%b",
                level.getLevel(), drops.size(), xp, surge);
        return true;
    }

    private List<ItemStack> buildDrops(ZombieLevel level, boolean surge) {
        List<ItemStack> drops = new ArrayList<>();
        boolean doubleLoot = surge && configManager.getZombies().getBoolean("night-event.double-loot", true);

        // Rotten flesh (0-3)
        int flesh = RandomUtil.nextInt(0, 3);
        if (doubleLoot) flesh = Math.min(flesh * 2, 6);
        if (flesh > 0) drops.add(new ItemStack(Material.ROTTEN_FLESH, flesh));

        // Radioactive Core
        double coreChance = configManager.getZombies().getDouble(
                "loot.level-" + level.getLevel() + ".radioactive-core-chance",
                level.getRadioactiveCoreChance());
        if (RandomUtil.chance(doubleLoot ? Math.min(coreChance * 2, 1.0) : coreChance)) {
            itemManager.getItem("radioactive-core").ifPresent(item -> drops.add(item.build(1)));
        }

        // Mutated Seed
        double seedChance = configManager.getZombies().getDouble(
                "loot.level-" + level.getLevel() + ".mutated-seed-chance",
                level.getMutatedSeedChance());
        if (RandomUtil.chance(doubleLoot ? Math.min(seedChance * 2, 1.0) : seedChance)) {
            itemManager.getItem("mutated-seed").ifPresent(item -> drops.add(item.build(1)));
        }

        // Irradiated Heart
        double heartChance = configManager.getZombies().getDouble(
                "loot.level-" + level.getLevel() + ".irradiated-heart-chance",
                level.getIrradiatedHeartChance());
        if (RandomUtil.chance(doubleLoot ? Math.min(heartChance * 2, 1.0) : heartChance)) {
            itemManager.getItem("irradiated-heart").ifPresent(item -> drops.add(item.build(1)));
        }

        return drops;
    }
}
