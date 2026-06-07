package com.nuclearcraft.debug;

import com.nuclearcraft.boss.TitanManager;
import com.nuclearcraft.combat.CombatManager;
import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.farming.FarmingManager;
import com.nuclearcraft.forge.NuclearForgeManager;
import com.nuclearcraft.ore.OreGenerationManager;
import com.nuclearcraft.ore.PlutoniumOreManager;
import com.nuclearcraft.admin.MemoryManager;
import com.nuclearcraft.performance.PerformanceManager;
import com.nuclearcraft.radiation.RadiationManager;
import com.nuclearcraft.release.ReleaseManager;
import com.nuclearcraft.smelter.NuclearSmelterManager;
import com.nuclearcraft.utils.NCLogger;
import com.nuclearcraft.zombies.IrradiatedZombieManager;
import com.nuclearcraft.zombies.ZombieSpawnManager;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Centralises all debug diagnostics exposed via /nc debug <system>.
 *
 * Each method produces a multi-line report for a command sender.
 * The DebugManager delegates to the relevant system managers and formats
 * the results in a readable way.
 *
 * Phase 12 addition.
 */
public class DebugManager {

    private final NuclearCraftPlugin     plugin;
    private final ConfigManager          configManager;
    private final RadiationManager       radiationManager;
    private final IrradiatedZombieManager zombieManager;
    private final ZombieSpawnManager     zombieSpawnManager;
    private final PlutoniumOreManager    oreManager;
    private final OreGenerationManager   oreGenerationManager;
    private final NuclearSmelterManager  smelterManager;
    private final NuclearForgeManager    forgeManager;
    private final FarmingManager         farmingManager;
    private final CombatManager          combatManager;
    private final TitanManager           titanManager;
    private final PerformanceManager     performanceManager;
    private final ReleaseManager         releaseManager;
    private MemoryManager                memoryManager;

    public static final List<String> DEBUG_SUBCOMMANDS =
            List.of("radiation", "zombies", "ore", "smelter", "forge", "farming",
                    "combat", "titan", "performance", "memory", "all");

    public DebugManager(NuclearCraftPlugin plugin,
                        ConfigManager configManager,
                        RadiationManager radiationManager,
                        IrradiatedZombieManager zombieManager,
                        ZombieSpawnManager zombieSpawnManager,
                        PlutoniumOreManager oreManager,
                        OreGenerationManager oreGenerationManager,
                        NuclearSmelterManager smelterManager,
                        NuclearForgeManager forgeManager,
                        FarmingManager farmingManager,
                        CombatManager combatManager,
                        TitanManager titanManager,
                        PerformanceManager performanceManager,
                        ReleaseManager releaseManager) {
        this.plugin               = plugin;
        this.configManager        = configManager;
        this.radiationManager     = radiationManager;
        this.zombieManager        = zombieManager;
        this.zombieSpawnManager   = zombieSpawnManager;
        this.oreManager           = oreManager;
        this.oreGenerationManager = oreGenerationManager;
        this.smelterManager       = smelterManager;
        this.forgeManager         = forgeManager;
        this.farmingManager       = farmingManager;
        this.combatManager        = combatManager;
        this.titanManager         = titanManager;
        this.performanceManager   = performanceManager;
        this.releaseManager       = releaseManager;
    }

    public void initialize() {
        NCLogger.info("[DebugManager] Initialized.");
    }

    public void shutdown() {}

    /** Injected after Phase 14 init. */
    public void setMemoryManager(MemoryManager memoryManager) {
        this.memoryManager = memoryManager;
    }

    // ── Public dispatch ───────────────────────────────────────────────────────

    public void handleDebugCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§e[NuclearCraft Debug] Usage: /nc debug <" + String.join("|", DEBUG_SUBCOMMANDS) + ">");
            return;
        }
        switch (args[1].toLowerCase()) {
            case "radiation"   -> debugRadiation(sender);
            case "zombies"     -> debugZombies(sender);
            case "ore"         -> debugOre(sender);
            case "smelter"     -> debugSmelter(sender);
            case "forge"       -> debugForge(sender);
            case "farming"     -> debugFarming(sender);
            case "combat"      -> debugCombat(sender);
            case "titan"       -> debugTitan(sender);
            case "performance" -> debugPerformance(sender);
            case "memory"      -> debugMemory(sender);
            case "all"         -> {
                debugRadiation(sender);
                debugZombies(sender);
                debugOre(sender);
                debugSmelter(sender);
                debugForge(sender);
                debugFarming(sender);
                debugCombat(sender);
                debugTitan(sender);
                debugPerformance(sender);
                debugMemory(sender);
            }
            default -> sender.sendMessage("§cUnknown debug target: " + args[1]);
        }
    }

    // ── System reporters ─────────────────────────────────────────────────────

    private void debugRadiation(CommandSender sender) {
        header(sender, "RADIATION");
        line(sender, "Online players tracked: " + plugin.getServer().getOnlinePlayers().size());
        line(sender, "Debug mode: " + (NCLogger.isDebugMode() ? "ON" : "OFF"));
        try {
            line(sender, "Radiation system: " + (radiationManager != null ? "ACTIVE" : "NULL"));
        } catch (Exception e) {
            line(sender, "Error reading radiation state: " + e.getMessage());
        }
    }

    private void debugZombies(CommandSender sender) {
        header(sender, "ZOMBIES");
        try {
            line(sender, "Active irradiated zombies: " + zombieManager.getActiveCount());
            line(sender, "Total spawned: " + zombieManager.getTotalSpawned());
            line(sender, "Alpha spawned: " + zombieManager.getTotalAlphaSpawned());
            line(sender, "Zombie spawn manager: " + (zombieSpawnManager != null ? "ACTIVE" : "NULL"));
        } catch (Exception e) {
            line(sender, "Error: " + e.getMessage());
        }
    }

    private void debugOre(CommandSender sender) {
        header(sender, "ORE");
        try {
            line(sender, "Tracked plutonium ore blocks: " + oreManager.getTrackedCount());
            line(sender, "Ore generation: " + (oreGenerationManager != null ? "ACTIVE" : "NULL"));
        } catch (Exception e) {
            line(sender, "Error: " + e.getMessage());
        }
    }

    private void debugSmelter(CommandSender sender) {
        header(sender, "SMELTER");
        try {
            line(sender, "Active machines: " + smelterManager.getMachineCount());
        } catch (Exception e) {
            line(sender, "Error: " + e.getMessage());
        }
    }

    private void debugForge(CommandSender sender) {
        header(sender, "FORGE");
        try {
            line(sender, "Active forges: " + forgeManager.getMachineCount());
        } catch (Exception e) {
            line(sender, "Error: " + e.getMessage());
        }
    }

    private void debugFarming(CommandSender sender) {
        header(sender, "FARMING");
        try {
            line(sender, "Farming manager: " + (farmingManager != null ? "ACTIVE" : "NULL"));
        } catch (Exception e) {
            line(sender, "Error: " + e.getMessage());
        }
    }

    private void debugCombat(CommandSender sender) {
        header(sender, "COMBAT");
        try {
            line(sender, "Combat manager: " + (combatManager != null ? "ACTIVE" : "NULL"));
        } catch (Exception e) {
            line(sender, "Error: " + e.getMessage());
        }
    }

    private void debugTitan(CommandSender sender) {
        header(sender, "TITAN");
        try {
            boolean active = titanManager != null && titanManager.isTitanAlive();
            line(sender, "Titan alive: " + active);
            if (active && titanManager.getActiveTitan() != null) {
                var titan = titanManager.getActiveTitan();
                line(sender, "Location: " + titan.getLocation().toVector());
                if (titanManager.getPhaseManager() != null) {
                    line(sender, "Phase: " + titanManager.getPhaseManager().getCurrentPhase());
                }
                var attr = titan.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                if (attr != null) {
                    line(sender, String.format("Health: %.0f / %.0f", titan.getHealth(), attr.getValue()));
                }
            }
        } catch (Exception e) {
            line(sender, "Error: " + e.getMessage());
        }
    }

    private void debugPerformance(CommandSender sender) {
        header(sender, "PERFORMANCE");
        try {
            line(sender, String.format("Estimated TPS: %.1f", performanceManager.getEstimatedTps()));
            line(sender, "Particles allowed: " + performanceManager.allowParticles());
            line(sender, "Heavy tasks allowed: " + performanceManager.allowHeavyTasks());
            line(sender, "Radiation players: " + performanceManager.getTrackedRadiationPlayers());
            line(sender, "Active machines: " + performanceManager.getActiveMachines());
            line(sender, "Tracked crops: " + performanceManager.getTrackedCrops());
            line(sender, "Tracked entities: " + performanceManager.getTrackedEntities());

            // Memory
            Runtime rt = Runtime.getRuntime();
            long usedMB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
            long maxMB  = rt.maxMemory() / 1024 / 1024;
            line(sender, "Heap: " + usedMB + " MB / " + maxMB + " MB");

            if (releaseManager != null) {
                line(sender, releaseManager.getBuildLine());
            }
        } catch (Exception e) {
            line(sender, "Error: " + e.getMessage());
        }
    }

    private void debugMemory(CommandSender sender) {
        header(sender, "MEMORY");
        try {
            if (memoryManager != null) {
                line(sender, memoryManager.getMemorySummary());
                line(sender, "Memory critical: " + (memoryManager.isMemoryCritical() ? "§cYES" : "§aNo"));
                line(sender, "Heap %: §f" + String.format("%.1f", memoryManager.getCurrentHeapPercent() * 100) + "%");
            } else {
                Runtime rt = Runtime.getRuntime();
                long usedMB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
                long maxMB  = rt.maxMemory() / 1024 / 1024;
                line(sender, "Heap: " + usedMB + " MB / " + maxMB + " MB");
                line(sender, "(MemoryManager not yet initialized)");
            }
        } catch (Exception e) {
            line(sender, "Error: " + e.getMessage());
        }
    }

    // ── Formatting helpers ────────────────────────────────────────────────────

    private void header(CommandSender sender, String system) {
        sender.sendMessage("§2§l[NuclearCraft Debug: " + system + "]");
    }

    private void line(CommandSender sender, String text) {
        sender.sendMessage("§7  " + text);
    }
}
