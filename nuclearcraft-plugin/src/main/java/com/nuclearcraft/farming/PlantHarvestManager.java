package com.nuclearcraft.farming;

import com.nuclearcraft.advancements.AdvancementManager;
import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.data.PlayerData;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.events.PlantHarvestEvent;
import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.utils.ColorUtil;
import com.nuclearcraft.utils.NCLogger;
import com.nuclearcraft.utils.RandomUtil;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles harvesting of fully grown Mutated Healing Plants.
 *
 * <p>On successful harvest:
 * <ul>
 *   <li>Fires {@link PlantHarvestEvent}.</li>
 *   <li>Drops 1–3 Healing Petals.</li>
 *   <li>Drops 0–2 Mutated Seeds.</li>
 *   <li>Drops 2–5 XP orbs.</li>
 *   <li>Plays particle effects.</li>
 *   <li>Updates player statistics and checks advancements.</li>
 * </ul>
 */
public class PlantHarvestManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final ItemManager itemManager;
    private final PlayerDataManager playerDataManager;
    private final AdvancementManager advancementManager;
    private final MutatedCropManager cropManager;

    public PlantHarvestManager(JavaPlugin plugin,
                                ConfigManager configManager,
                                ItemManager itemManager,
                                PlayerDataManager playerDataManager,
                                AdvancementManager advancementManager,
                                MutatedCropManager cropManager) {
        this.plugin              = plugin;
        this.configManager       = configManager;
        this.itemManager         = itemManager;
        this.playerDataManager   = playerDataManager;
        this.advancementManager  = advancementManager;
        this.cropManager         = cropManager;
    }

    public void initialize() {
        NCLogger.info("PlantHarvestManager initialized.");
    }

    public void shutdown() {}

    // ──────────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Attempts to harvest the mutated crop at the given location.
     * Only succeeds if the crop is fully grown.
     *
     * @param player  the harvesting player
     * @param loc     the crop location
     * @return true if a harvest occurred (caller should cancel original event)
     */
    public boolean attemptHarvest(Player player, Location loc) {
        var optData = cropManager.getCrop(loc);
        if (optData.isEmpty()) return false;

        MutatedCropData data = optData.get();
        if (!data.isFullyGrown()) {
            player.sendMessage(ColorUtil.parse(
                    configManager.getMessage("farming.not-ready")));
            return true; // still consumed the event
        }

        // Fire harvest event
        PlantHarvestEvent harvestEvent = new PlantHarvestEvent(player, loc);
        plugin.getServer().getPluginManager().callEvent(harvestEvent);
        if (harvestEvent.isCancelled()) return true;

        // Remove crop and play effects
        cropManager.removeCrop(loc);
        spawnHarvestParticles(loc);

        // Build and drop items
        List<ItemStack> drops = buildDrops();
        Location dropLoc = loc.clone().add(0.5, 0.5, 0.5);
        for (ItemStack drop : drops) {
            loc.getWorld().dropItemNaturally(dropLoc, drop);
        }

        // Drop XP
        int xp = RandomUtil.nextInt(
                configManager.getFarming().getInt("plant.harvest.xp-min", 2),
                configManager.getFarming().getInt("plant.harvest.xp-max", 5));
        ExperienceOrb orb = loc.getWorld().spawn(dropLoc, ExperienceOrb.class);
        orb.setExperience(xp);

        // Update player stats
        playerDataManager.get(player).ifPresent(pd -> {
            pd.incrementPlantsHarvested();
            int petals = drops.stream()
                    .filter(i -> itemManager.getItem("healing-petal")
                            .map(ci -> ci.matches(i)).orElse(false))
                    .mapToInt(ItemStack::getAmount)
                    .sum();
            pd.addHealingPetalsCollected(petals);
            pd.markDirty();
        });

        // Advancements
        checkAdvancements(player);

        player.sendMessage(ColorUtil.parse(configManager.getMessage("farming.harvested")));
        NCLogger.debug("Harvest by %s at %s — %d drops", player.getName(), loc, drops.size());
        return true;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────────────────

    private List<ItemStack> buildDrops() {
        List<ItemStack> drops = new ArrayList<>();

        // Healing Petals: 1-3
        int petalMin = configManager.getFarming().getInt("plant.harvest.petals-min", 1);
        int petalMax = configManager.getFarming().getInt("plant.harvest.petals-max", 3);
        int petals = RandomUtil.nextInt(petalMin, petalMax);
        itemManager.getItem("healing-petal").ifPresent(ci -> drops.add(ci.build(petals)));

        // Extra Seeds: 0-2
        int seedMin = configManager.getFarming().getInt("plant.harvest.seeds-min", 0);
        int seedMax = configManager.getFarming().getInt("plant.harvest.seeds-max", 2);
        int seeds = RandomUtil.nextInt(seedMin, seedMax);
        if (seeds > 0) {
            itemManager.getItem("mutated-seed").ifPresent(ci -> drops.add(ci.build(seeds)));
        }

        return drops;
    }

    private void spawnHarvestParticles(Location loc) {
        Location center = loc.clone().add(0.5, 0.5, 0.5);
        loc.getWorld().spawnParticle(Particle.DUST,
                center, 20, 0.4, 0.4, 0.4, 0,
                new Particle.DustOptions(Color.fromRGB(0x00BFFF), 1.2f));
        loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                center, 8, 0.3, 0.3, 0.3, 0);
    }

    private void checkAdvancements(Player player) {
        playerDataManager.get(player).ifPresent(pd -> {
            // First Harvest
            if (pd.getPlantsHarvested() == 1) {
                advancementManager.award(player, AdvancementManager.Advancement.FIRST_HARVEST);
            }
            // Master Botanist (100 harvests)
            if (pd.getPlantsHarvested() >= 100) {
                advancementManager.award(player, AdvancementManager.Advancement.MASTER_BOTANIST);
            }
        });
    }
}
