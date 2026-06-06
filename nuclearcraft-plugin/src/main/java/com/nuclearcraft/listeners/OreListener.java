package com.nuclearcraft.listeners;

import com.nuclearcraft.advancements.AdvancementManager;
import com.nuclearcraft.ore.OreGenerationManager;
import com.nuclearcraft.ore.OreMiningManager;
import com.nuclearcraft.ore.PlutoniumOreManager;
import com.nuclearcraft.ore.RadiationDrillManager;
import com.nuclearcraft.utils.ColorUtil;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;

/**
 * Event router for all Plutonium Ore interactions.
 *
 * Handles:
 *   - ChunkLoadEvent (new chunks)   → OreGenerationManager
 *   - BlockBreakEvent               → OreMiningManager
 *   - PlayerMoveEvent               → ore discovery title/advancement
 *   - PrepareItemCraftEvent         → validates Radioactive Core authenticity in drill recipe
 *   - BlockFromToEvent              → protects ore from water/lava flow
 *   - BlockExplodeEvent             → removes ore tracking on TNT/explosion
 *   - EntityExplodeEvent            → same for mob explosions
 */
public class OreListener implements Listener {

    private final OreGenerationManager generationManager;
    private final OreMiningManager miningManager;
    private final PlutoniumOreManager oreManager;
    private final RadiationDrillManager drillManager;
    private final AdvancementManager advancementManager;

    public OreListener(OreGenerationManager generationManager, OreMiningManager miningManager,
                       PlutoniumOreManager oreManager, RadiationDrillManager drillManager,
                       AdvancementManager advancementManager) {
        this.generationManager = generationManager;
        this.miningManager = miningManager;
        this.oreManager = oreManager;
        this.drillManager = drillManager;
        this.advancementManager = advancementManager;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // World generation
    // ──────────────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!event.isNewChunk()) return;
        if (event.getWorld().getEnvironment() != World.Environment.NORMAL) return;
        generationManager.generateInChunk(event.getChunk());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Mining
    // ──────────────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        miningManager.handleBreak(event);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Ore discovery on proximity
    // ──────────────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Only process when player moves to a new block
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;

        var player = event.getPlayer();
        if (player.getWorld().getEnvironment() != World.Environment.NORMAL) return;

        // Check if any ore is within 5 blocks (discovery range)
        Location loc = player.getLocation();
        int px = loc.getBlockX(), py = loc.getBlockY(), pz = loc.getBlockZ();
        World world = player.getWorld();

        for (int dx = -5; dx <= 5; dx++) {
            for (int dy = -5; dy <= 5; dy++) {
                for (int dz = -5; dz <= 5; dz++) {
                    if (oreManager.isOre(new Location(world, px + dx, py + dy, pz + dz))) {
                        if (oreManager.checkFirstDiscovery(player)) {
                            sendDiscoveryTitle(player);
                            advancementManager.award(player, AdvancementManager.Advancement.NUCLEAR_DISCOVERY);
                        }
                        return;
                    }
                }
            }
        }
    }

    private void sendDiscoveryTitle(Player player) {
        player.sendTitle(
                "§a☢ §cWARNING §a☢",
                "§eRadioactive Material Detected",
                10, 60, 20);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.5f);
        player.sendMessage(ColorUtil.parse(
                "<red>☢ <bold>ALERT:</bold> <yellow>Plutonium Ore has been detected nearby. Stay back unless equipped!</yellow></red>"));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Crafting validation — Radiation Drill
    // ──────────────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        CraftingInventory inv = event.getInventory();
        if (inv.getResult() == null) return;

        // Only validate our drill recipe
        var recipe = event.getRecipe();
        if (recipe == null) return;
        if (!(recipe instanceof org.bukkit.inventory.ShapedRecipe shaped)) return;
        if (!shaped.getKey().equals(drillManager.getRecipeKey())) return;

        // Verify every MAGMA_CREAM slot is a genuine Radioactive Core
        for (ItemStack ingredient : inv.getMatrix()) {
            if (ingredient == null) continue;
            if (ingredient.getType() == org.bukkit.Material.MAGMA_CREAM) {
                if (!drillManager.isRadioactiveCore(ingredient)) {
                    inv.setResult(null); // block crafting
                    return;
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Ore integrity protection
    // ──────────────────────────────────────────────────────────────────────────

    /** Prevent water/lava from flowing over tracked ore and destroying tracking. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFluidFlow(BlockFromToEvent event) {
        if (oreManager.isOre(event.getToBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    /** Remove ore from tracking if destroyed by block explosion (TNT). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> {
            if (oreManager.isOre(block.getLocation())) {
                oreManager.removeOre(block.getLocation());
                return false; // allow destruction but track the removal
            }
            return false;
        });
    }

    /** Remove ore from tracking if destroyed by entity explosion (Creeper, etc.). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        for (org.bukkit.block.Block block : event.blockList()) {
            if (oreManager.isOre(block.getLocation())) {
                oreManager.removeOre(block.getLocation());
            }
        }
    }
}
