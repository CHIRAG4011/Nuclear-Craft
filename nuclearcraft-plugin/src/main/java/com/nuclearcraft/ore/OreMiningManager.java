package com.nuclearcraft.ore;

import com.nuclearcraft.advancements.AdvancementManager;
import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.data.PlayerData;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.radiation.RadiationManager;
import com.nuclearcraft.radiation.RadiationSource;
import com.nuclearcraft.utils.ColorUtil;
import com.nuclearcraft.utils.NCLogger;
import com.nuclearcraft.utils.RandomUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles all Plutonium Ore mining validation and execution.
 *
 * Two mining paths:
 *
 * ╔══════════════════════════════════════════╗
 * ║  INVALID TOOL (no Radiation Drill)       ║
 * ║  • Cancel break event (ore stays)        ║
 * ║  • Apply 25-radiation burst              ║
 * ║  • Spawn green particles                 ║
 * ║  • Show warning message                  ║
 * ║  • 5-second cooldown to prevent spam     ║
 * ╚══════════════════════════════════════════╝
 *
 * ╔══════════════════════════════════════════╗
 * ║  VALID TOOL (Radiation Drill)            ║
 * ║  • Allow break                           ║
 * ║  • Drop 1-4 fragments (Fortune support)  ║
 * ║  • Play extraction sound + particles     ║
 * ║  • Grant advancement progress            ║
 * ║  • Increment stats                       ║
 * ╚══════════════════════════════════════════╝
 */
public class OreMiningManager {

    private static final long BURST_COOLDOWN_MS = 5_000L;

    private final NuclearCraftPlugin plugin;
    private final ConfigManager configManager;
    private final PlutoniumOreManager oreManager;
    private final RadiationDrillManager drillManager;
    private final RadiationManager radiationManager;
    private final PlayerDataManager playerDataManager;
    private final ItemManager itemManager;
    private final AdvancementManager advancementManager;

    /** UUID → epoch-ms when burst cooldown expires. */
    private final Map<UUID, Long> burstCooldowns = new HashMap<>();

    public OreMiningManager(NuclearCraftPlugin plugin, ConfigManager configManager,
                             PlutoniumOreManager oreManager, RadiationDrillManager drillManager,
                             RadiationManager radiationManager, PlayerDataManager playerDataManager,
                             ItemManager itemManager, AdvancementManager advancementManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.oreManager = oreManager;
        this.drillManager = drillManager;
        this.radiationManager = radiationManager;
        this.playerDataManager = playerDataManager;
        this.itemManager = itemManager;
        this.advancementManager = advancementManager;
    }

    public void initialize() {
        NCLogger.info("OreMiningManager initialized.");
    }

    public void shutdown() {
        burstCooldowns.clear();
    }

    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Called from OreListener on BlockBreakEvent (main thread).
     * Returns true if the event was handled (ore block).
     */
    public boolean handleBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!oreManager.isOre(block.getLocation())) return false;

        Player player = event.getPlayer();
        ItemStack heldItem = player.getInventory().getItemInMainHand();

        if (drillManager.isDrill(heldItem)) {
            handleValidMine(event, player, block, heldItem);
        } else {
            handleInvalidMine(event, player, block);
        }
        return true;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Valid mine (Radiation Drill)
    // ──────────────────────────────────────────────────────────────────────────

    private void handleValidMine(BlockBreakEvent event, Player player, Block block, ItemStack drill) {
        // Allow the break but suppress vanilla drops (we manage them)
        event.setDropItems(false);
        event.setExpToDrop(configManager.getOre().getInt("plutonium-ore.drop.xp-max", 7));

        // Remove from tracking
        oreManager.removeOre(block.getLocation());

        // Fortune-modified fragment drops
        int fortuneLevel = drill.getEnchantmentLevel(Enchantment.FORTUNE);
        int fragmentCount = rollFragmentCount(fortuneLevel);
        itemManager.getItem("raw-plutonium-fragment").ifPresent(item -> {
            block.getWorld().dropItemNaturally(block.getLocation(), item.build(fragmentCount));
        });

        // Effects
        block.getWorld().playSound(block.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.0f, 0.7f);
        block.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR,
                block.getLocation().add(0.5, 0.5, 0.5), 30, 0.4, 0.4, 0.4);

        // Stats
        PlayerData data = playerDataManager.get(player.getUniqueId()).orElse(null);
        if (data != null) {
            data.incrementPlutoniumOreMined();
            data.addFragmentsCollected(fragmentCount);
            data.incrementDrillUses();
        }

        // Advancements
        advancementManager.award(player, AdvancementManager.Advancement.SAFE_EXTRACTION);

        // Check radioactive hoarder (64+ fragments in inventory)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            checkRadioactiveHoarder(player);
        }, 5L);

        NCLogger.debug("Valid plutonium mine by %s at %s — %d fragments dropped",
                player.getName(), block.getLocation().toVector(), fragmentCount);
    }

    private int rollFragmentCount(int fortuneLevel) {
        int base = configManager.getOre().getInt("plutonium-ore.drop.base-amount", 1);
        return switch (fortuneLevel) {
            case 1 -> RandomUtil.nextInt(base, 2);
            case 2 -> RandomUtil.nextInt(base, 3);
            case 3 -> RandomUtil.nextInt(base, 4);
            default -> base;
        };
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Invalid mine (no Radiation Drill)
    // ──────────────────────────────────────────────────────────────────────────

    private void handleInvalidMine(BlockBreakEvent event, Player player, Block block) {
        event.setCancelled(true);

        // Cooldown check
        long now = System.currentTimeMillis();
        Long cooldownEnd = burstCooldowns.get(player.getUniqueId());
        if (cooldownEnd != null && now < cooldownEnd) return;
        burstCooldowns.put(player.getUniqueId(), now + BURST_COOLDOWN_MS);

        // Apply radiation burst
        int burstAmount = configManager.getOre().getInt("plutonium-ore.mining.burst-radiation", 25);
        radiationManager.addRadiation(player, burstAmount, RadiationSource.PLUTONIUM_ORE);

        // Particles at ore location
        block.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR,
                block.getLocation().add(0.5, 0.5, 0.5), 20, 0.5, 0.5, 0.5);
        block.getWorld().playSound(block.getLocation(), Sound.ENTITY_CREEPER_PRIMED, 1.0f, 0.5f);

        // Warning message
        player.sendMessage(ColorUtil.parse(
                "<red>☢ <bold>WARNING!</bold> <yellow>You cannot mine Plutonium Ore without a Radiation Drill!</yellow></red>"));
        player.sendMessage(ColorUtil.parse(
                "<gray>You absorbed <red>" + burstAmount + " radiation</red> from the unstable ore.</gray>"));

        // Stats + advancements
        PlayerData data = playerDataManager.get(player.getUniqueId()).orElse(null);
        if (data != null) {
            data.incrementUnsafeMiningAttempts();
            data.incrementRadiationBurstsTriggered();
        }
        advancementManager.award(player, AdvancementManager.Advancement.UNSAFE_MINER);

        NCLogger.debug("Invalid mine attempt by %s at %s — burst applied", player.getName(), block.getLocation().toVector());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Radioactive Hoarder check
    // ──────────────────────────────────────────────────────────────────────────

    private void checkRadioactiveHoarder(Player player) {
        int total = countFragments(player);
        if (total >= 64) {
            advancementManager.award(player, AdvancementManager.Advancement.RADIOACTIVE_HOARDER);
        }
    }

    /** Counts total raw-plutonium-fragment items in the player's inventory. */
    public int countFragments(Player player) {
        var fragmentItem = itemManager.getItem("raw-plutonium-fragment");
        if (fragmentItem.isEmpty()) return 0;
        var custom = fragmentItem.get();
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && custom.matches(item)) {
                total += item.getAmount();
            }
        }
        return total;
    }
}
