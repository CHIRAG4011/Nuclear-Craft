package com.nuclearcraft.commands;

import com.nuclearcraft.advancements.AdvancementManager;
import com.nuclearcraft.boss.TitanManager;
import com.nuclearcraft.combat.CombatManager;
import com.nuclearcraft.admin.AdminManager;
import com.nuclearcraft.admin.BuildManager;
import com.nuclearcraft.debug.DebugManager;
import com.nuclearcraft.titantech.TitanTechManager;
import com.nuclearcraft.combat.WeaponMasteryManager;
import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.data.PlayerData;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.equipment.EquipmentManager;
import com.nuclearcraft.farming.FarmingManager;
import com.nuclearcraft.forge.ForgeData;
import com.nuclearcraft.forge.NuclearForgeManager;
import com.nuclearcraft.upgrade.UpgradeManager;
import com.nuclearcraft.upgrade.UpgradeTier;
import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.ore.OreGenerationManager;
import com.nuclearcraft.ore.OreMiningManager;
import com.nuclearcraft.ore.PlutoniumOreManager;
import com.nuclearcraft.zombies.ZombieLevel;
import com.nuclearcraft.radiation.RadiationManager;
import com.nuclearcraft.radiation.RadiationSource;
import com.nuclearcraft.smelter.NuclearSmelterManager;
import com.nuclearcraft.smelter.SmelterData;
import com.nuclearcraft.utils.ColorUtil;
import com.nuclearcraft.utils.NCLogger;
import com.nuclearcraft.zombies.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles /nuclearcraft and all subcommands.
 *
 * Phase 2: radiation check|add|remove|set|clear
 * Phase 3: zombie spawn irradiated|alpha
 *          zombie stats
 *          zombie surge start|stop
 * Phase 4: ore spawn plutonium
 *          ore give fragment [amount]
 *          ore stats
 *          Permission: nuclearcraft.admin.ore
 * Phase 5: smelter give [player]
 *          smelter stats
 *          smelter debug
 *          Permission: nuclearcraft.admin.smelter
 * Phase 6: equipment give <type>
 *          equipment stats
 *          Permission: nuclearcraft.admin.equipment
 * Phase 7: farming give <seed|petal|antidote|serum> [amount]
 *          farming growall [radius]
 *          farming stats
 *          Permission: nuclearcraft.admin.farming
 */
public class NuclearCraftCommand implements CommandExecutor, TabCompleter {

    private static final List<String> TOP_SUBCOMMANDS =
            List.of("help", "reload", "info", "debug", "give", "version",
                    "radiation", "zombie", "ore", "smelter", "equipment", "farming", "forge", "combat", "titan", "titantech",
                    "health", "diagnostics", "performance", "cleanup", "fixdata", "dumpdata", "serverreport");
    private static final List<String> ADMIN_SUBCOMMANDS =
            List.of("health", "diagnostics", "performance", "cleanup", "fixdata", "dumpdata", "serverreport");
    private static final List<String> TITANTECH_SUBCOMMANDS =
            List.of("give", "stats", "aura", "setbonusinfo");
    private static final List<String> TITANTECH_GIVE_TYPES = List.of(
            "titan-reactor-forge",
            "titan-helmet", "titan-chestplate", "titan-leggings", "titan-boots",
            "titan-sword", "titan-axe", "titan-pickaxe", "titan-shovel", "titan-hoe",
            "titan-bow", "titan-arrow", "titan-fragment", "titan-core");
    private static final List<String> COMBAT_SUBCOMMANDS =
            List.of("stats", "mastery", "radiation");
    private static final List<String> TITAN_SUBCOMMANDS =
            List.of("spawn", "kill", "phase", "stats");
    private static final List<String> FORGE_SUBCOMMANDS =
            List.of("give", "energy", "upgrade", "stats");
    private static final List<String> FORGE_ENERGY_ACTIONS =
            List.of("set", "add", "clear");
    private static final List<String> FORGE_UPGRADE_TIERS =
            List.of("mk1", "mk2", "mk3", "mk4");
    private static final List<String> EQUIPMENT_SUBCOMMANDS =
            List.of("give", "stats");
    private static final List<String> EQUIPMENT_GIVE_TYPES = List.of(
            "plutonium-sword", "plutonium-axe", "plutonium-pickaxe", "plutonium-shovel", "plutonium-hoe",
            "plutonium-helmet", "plutonium-chestplate", "plutonium-leggings", "plutonium-boots",
            "hazmat-helmet", "hazmat-chestplate", "hazmat-leggings", "hazmat-boots",
            "plutonium-arrow", "refined-plutonium-ingot", "industrial-fabric");
    private static final List<String> RADIATION_SUBCOMMANDS =
            List.of("check", "add", "remove", "set", "clear");
    private static final List<String> ZOMBIE_SUBCOMMANDS =
            List.of("spawn", "stats", "surge");
    private static final List<String> ZOMBIE_SPAWN_TYPES =
            List.of("irradiated", "alpha");
    private static final List<String> ZOMBIE_SURGE_ACTIONS =
            List.of("start", "stop");
    private static final List<String> ORE_SUBCOMMANDS =
            List.of("spawn", "give", "stats");
    private static final List<String> ORE_SPAWN_TYPES =
            List.of("plutonium");
    private static final List<String> ORE_GIVE_TYPES =
            List.of("fragment", "drill");
    private static final List<String> SMELTER_SUBCOMMANDS =
            List.of("give", "stats", "debug");
    private static final List<String> FARMING_SUBCOMMANDS =
            List.of("give", "growall", "stats");
    private static final List<String> FARMING_GIVE_TYPES =
            List.of("mutated-seed", "healing-petal", "radiation-antidote", "radiation-serum");

    private final NuclearCraftPlugin plugin;
    private final ConfigManager configManager;
    private final PlayerDataManager playerDataManager;
    private final ItemManager itemManager;
    private final RadiationManager radiationManager;
    private final IrradiatedZombieManager irradiatedZombieManager;
    private final ZombieSpawnManager zombieSpawnManager;
    private final RadiationCloudManager radiationCloudManager;
    private final RadiationNightManager radiationNightManager;
    private final AdvancementManager advancementManager;
    private final PlutoniumOreManager plutoniumOreManager;
    private final OreMiningManager oreMiningManager;
    private final NuclearSmelterManager nuclearSmelterManager;
    private final EquipmentManager equipmentManager;
    private final FarmingManager farmingManager;
    private final NuclearForgeManager nuclearForgeManager;
    private final UpgradeManager upgradeManager;
    private CombatManager combatManager;
    private TitanManager titanManager;
    private TitanTechManager titanTechManager;
    private DebugManager debugManager;
    private AdminManager adminManager;
    private BuildManager buildManager;

    public NuclearCraftCommand(NuclearCraftPlugin plugin, ConfigManager configManager,
                                PlayerDataManager playerDataManager, ItemManager itemManager,
                                RadiationManager radiationManager,
                                IrradiatedZombieManager irradiatedZombieManager,
                                ZombieSpawnManager zombieSpawnManager,
                                RadiationCloudManager radiationCloudManager,
                                RadiationNightManager radiationNightManager,
                                AdvancementManager advancementManager,
                                PlutoniumOreManager plutoniumOreManager,
                                OreMiningManager oreMiningManager,
                                NuclearSmelterManager nuclearSmelterManager,
                                EquipmentManager equipmentManager,
                                FarmingManager farmingManager,
                                NuclearForgeManager nuclearForgeManager,
                                UpgradeManager upgradeManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.playerDataManager = playerDataManager;
        this.itemManager = itemManager;
        this.radiationManager = radiationManager;
        this.irradiatedZombieManager = irradiatedZombieManager;
        this.zombieSpawnManager = zombieSpawnManager;
        this.radiationCloudManager = radiationCloudManager;
        this.radiationNightManager = radiationNightManager;
        this.advancementManager = advancementManager;
        this.plutoniumOreManager = plutoniumOreManager;
        this.oreMiningManager = oreMiningManager;
        this.nuclearSmelterManager = nuclearSmelterManager;
        this.equipmentManager = equipmentManager;
        this.farmingManager = farmingManager;
        this.nuclearForgeManager = nuclearForgeManager;
        this.upgradeManager = upgradeManager;
    }

    /** Called after Phase 9 init so command can display combat stats. */
    public void setCombatManager(CombatManager combatManager) {
        this.combatManager = combatManager;
    }

    /** Called after Phase 10 init so command can manage the Titan. */
    public void setTitanManager(TitanManager titanManager) {
        this.titanManager = titanManager;
    }

    /** Called after Phase 11 init so command can manage Titan Tech. */
    public void setTitanTechManager(TitanTechManager titanTechManager) {
        this.titanTechManager = titanTechManager;
    }

    /** Called after Phase 12 init so command can delegate debug subcommands. */
    public void setDebugManager(DebugManager debugManager) {
        this.debugManager = debugManager;
    }

    /** Called after Phase 14 init so command can delegate admin subcommands. */
    public void setAdminManager(AdminManager adminManager) {
        this.adminManager = adminManager;
    }

    /** Called after Phase 14 init for build/version info. */
    public void setBuildManager(BuildManager buildManager) {
        this.buildManager = buildManager;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Routing
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { sendHelp(sender); return true; }

        return switch (args[0].toLowerCase()) {
            case "help"      -> { sendHelp(sender);                yield true; }
            case "version"   -> { sendVersion(sender);             yield true; }
            case "info"      -> { sendInfo(sender);                yield true; }
            case "reload"    -> { handleReload(sender);            yield true; }
            case "debug"     -> { handleDebug(sender, args);        yield true; }
            case "give"      -> { handleGive(sender, args);        yield true; }
            case "radiation" -> { handleRadiation(sender, args);   yield true; }
            case "zombie"    -> { handleZombie(sender, args);      yield true; }
            case "ore"       -> { handleOre(sender, args);         yield true; }
            case "smelter"   -> { handleSmelter(sender, args);     yield true; }
            case "equipment" -> { handleEquipment(sender, args);   yield true; }
            case "farming"   -> { handleFarming(sender, args);     yield true; }
            case "forge"     -> { handleForge(sender, args);       yield true; }
            case "combat"    -> { handleCombat(sender, args);      yield true; }
            case "titan"     -> { handleTitan(sender, args);       yield true; }
            case "titantech"    -> { handleTitanTech(sender, args);   yield true; }
            case "health"       -> { handleAdminCmd(sender, "health");       yield true; }
            case "diagnostics"  -> { handleAdminCmd(sender, "diagnostics");  yield true; }
            case "performance"  -> { handleAdminCmd(sender, "performance");  yield true; }
            case "cleanup"      -> { handleAdminCmd(sender, "cleanup");      yield true; }
            case "fixdata"      -> { handleAdminCmd(sender, "fixdata");      yield true; }
            case "dumpdata"     -> { handleAdminCmd(sender, "dumpdata");     yield true; }
            case "serverreport" -> { handleAdminCmd(sender, "serverreport"); yield true; }
            default -> {
                sender.sendMessage(ColorUtil.parse(configManager.getMessage("general.unknown-command")));
                yield true;
            }
        };
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Phase 1 subcommands
    // ──────────────────────────────────────────────────────────────────────────

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ColorUtil.parse(configManager.getRawMessage("commands.help-header")));
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("nuclearcraft help", "Show this help message");
        entries.put("nuclearcraft version", "Display plugin version");
        entries.put("nuclearcraft info", "Show plugin runtime info");
        entries.put("nuclearcraft radiation check", "View your radiation status");
        if (sender.hasPermission("nuclearcraft.reload"))
            entries.put("nuclearcraft reload", "Reload configuration files");
        if (sender.hasPermission("nuclearcraft.debug"))
            entries.put("nuclearcraft debug", "Toggle debug mode");
        if (sender.hasPermission("nuclearcraft.give"))
            entries.put("nuclearcraft give <player> <item> [amount]", "Give a custom item");
        if (sender.hasPermission("nuclearcraft.admin.radiation")) {
            entries.put("nuclearcraft radiation add <player> <amount>",    "Add radiation to player");
            entries.put("nuclearcraft radiation remove <player> <amount>", "Remove radiation from player");
            entries.put("nuclearcraft radiation set <player> <amount>",    "Set player radiation");
            entries.put("nuclearcraft radiation clear <player>",           "Clear player radiation");
        }
        if (sender.hasPermission("nuclearcraft.admin.zombies")) {
            entries.put("nuclearcraft zombie spawn irradiated", "Spawn irradiated zombie at your location");
            entries.put("nuclearcraft zombie spawn alpha",      "Spawn an Alpha zombie at your location");
            entries.put("nuclearcraft zombie stats",            "Show zombie system statistics");
            entries.put("nuclearcraft zombie surge start|stop", "Force-start or stop a Radiation Surge");
        }
        if (sender.hasPermission("nuclearcraft.admin.ore")) {
            entries.put("nuclearcraft ore spawn plutonium",   "Spawn a Plutonium Ore block at your location");
            entries.put("nuclearcraft ore give fragment [n]", "Give Raw Plutonium Fragments");
            entries.put("nuclearcraft ore give drill",        "Give a Radiation Drill");
            entries.put("nuclearcraft ore stats",             "Show ore system statistics");
        }
        if (sender.hasPermission("nuclearcraft.admin.smelter")) {
            entries.put("nuclearcraft smelter give [player]", "Give a Nuclear Smelter to self or target");
            entries.put("nuclearcraft smelter stats",         "Show smelter system statistics");
            entries.put("nuclearcraft smelter debug",         "List all active machines and their state");
        }
        if (sender.hasPermission("nuclearcraft.admin.equipment")) {
            entries.put("nuclearcraft equipment give <type>", "Give a plutonium/hazmat equipment item");
            entries.put("nuclearcraft equipment stats",       "Show Phase 6 equipment stats for online players");
        }
        if (sender.hasPermission("nuclearcraft.admin.farming")) {
            entries.put("nuclearcraft farming give <type> [amount]", "Give a farming/cure item");
            entries.put("nuclearcraft farming growall [radius]",     "Force-grow all mutated crops nearby");
            entries.put("nuclearcraft farming stats",                "Show Phase 7 farming statistics");
        }
        if (sender.hasPermission("nuclearcraft.admin.titan")) {
            entries.put("nuclearcraft titan spawn",          "Spawn the Plutonium Titan at your location");
            entries.put("nuclearcraft titan kill",           "Force-kill the active Titan");
            entries.put("nuclearcraft titan phase <1-4>",   "Force the Titan into a specific phase");
            entries.put("nuclearcraft titan stats [player]","Show Titan statistics for self or target");
        }
        if (sender.hasPermission("nuclearcraft.admin")) {
            entries.put("nuclearcraft health",       "Server health overview (TPS, memory, systems)");
            entries.put("nuclearcraft diagnostics",  "Run all startup checks and config validation");
            entries.put("nuclearcraft performance",  "Detailed performance metrics report");
            entries.put("nuclearcraft cleanup",      "Purge caches and request GC");
            entries.put("nuclearcraft fixdata",      "Validate and auto-repair corrupt data");
            entries.put("nuclearcraft dumpdata",     "Write full debug dump to disk");
            entries.put("nuclearcraft serverreport", "Print production server report");
        }
        entries.forEach((cmd, desc) ->
                sender.sendMessage(ColorUtil.parse(
                        configManager.getRawMessage("commands.help-entry")
                                .replace("{command}", cmd)
                                .replace("{description}", desc))));
    }

    private void sendVersion(CommandSender sender) {
        sender.sendMessage(ColorUtil.parse(configManager.getMessage("commands.version")
                .replace("{version}", plugin.getDescription().getVersion())
                .replace("{server}", plugin.getServer().getVersion())));
    }

    private void sendInfo(CommandSender sender) {
        sender.sendMessage(ColorUtil.parse(configManager.getRawMessage("commands.info-header")));
        sender.sendMessage(ColorUtil.parse(configManager.getRawMessage("commands.info-version")
                .replace("{version}", plugin.getDescription().getVersion())));
        sender.sendMessage(ColorUtil.parse(configManager.getRawMessage("commands.info-players")
                .replace("{players}", String.valueOf(plugin.getServer().getOnlinePlayers().size()))));
        String debugStatus = NCLogger.isDebugMode()
                ? "<green>ENABLED</green>" : "<red>DISABLED</red>";
        sender.sendMessage(ColorUtil.parse(configManager.getRawMessage("commands.info-debug")
                .replace("{status}", debugStatus)));
        sender.sendMessage(ColorUtil.parse(
                " <gray>Tracked Plutonium Ore:</gray> <white>"
                        + plutoniumOreManager.getTrackedCount() + "</white>"));
        sender.sendMessage(ColorUtil.parse(
                " <gray>Active Smelters:</gray> <white>"
                        + nuclearSmelterManager.getMachineCount() + "</white>"));
        sender.sendMessage(ColorUtil.parse(
                " <gray>Active Mutated Crops:</gray> <white>"
                        + farmingManager.getCropManager().getCropCount() + "</white>"));
        sender.sendMessage(ColorUtil.parse(
                " <gray>Active Toxic Blooms:</gray> <white>"
                        + farmingManager.getToxicBloomManager().getBloomCount() + "</white>"));
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("nuclearcraft.reload")) {
            sender.sendMessage(ColorUtil.parse(configManager.getMessage("general.no-permission")));
            return;
        }
        sender.sendMessage(ColorUtil.parse(configManager.getMessage("general.reload-start")));
        try {
            plugin.reload();
            sender.sendMessage(ColorUtil.parse(configManager.getMessage("general.reload-complete")));
        } catch (Exception e) {
            NCLogger.severe("Reload failed", e);
            sender.sendMessage(ColorUtil.parse(configManager.getMessage("general.reload-failed")));
        }
    }

    private void handleDebug(CommandSender sender, String[] args) {
        if (!sender.hasPermission("nuclearcraft.debug")) {
            sender.sendMessage(ColorUtil.parse(configManager.getMessage("general.no-permission")));
            return;
        }
        if (args.length >= 2 && debugManager != null) {
            debugManager.handleDebugCommand(sender, args);
            return;
        }
        boolean newState = !NCLogger.isDebugMode();
        NCLogger.setDebugMode(newState);
        sender.sendMessage(ColorUtil.parse(configManager.getMessage(
                newState ? "commands.debug-enabled" : "commands.debug-disabled")));
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("nuclearcraft.give")) {
            sender.sendMessage(ColorUtil.parse(configManager.getMessage("general.no-permission")));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(ColorUtil.parse("<red>Usage: /nc give <player> <item> [amount]</red>"));
            return;
        }
        var target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ColorUtil.parse(
                    configManager.getMessage("general.player-not-found").replace("{player}", args[1])));
            return;
        }
        int amount = 1;
        if (args.length >= 4) {
            try { amount = Math.max(1, Math.min(64, Integer.parseInt(args[3]))); }
            catch (NumberFormatException e) {
                sender.sendMessage(ColorUtil.parse(
                        configManager.getMessage("general.invalid-number").replace("{input}", args[3])));
                return;
            }
        }
        var optItem = itemManager.getItem(args[2]);
        if (optItem.isEmpty()) {
            sender.sendMessage(ColorUtil.parse(
                    configManager.getMessage("commands.give-invalid-item").replace("{item}", args[2])));
            return;
        }
        target.getInventory().addItem(optItem.get().build(amount));
        sender.sendMessage(ColorUtil.parse(configManager.getMessage("commands.give-success")
                .replace("{amount}", String.valueOf(amount))
                .replace("{item}", args[2])
                .replace("{player}", target.getName())));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Phase 2: /nc radiation ...
    // ──────────────────────────────────────────────────────────────────────────

    private void handleRadiation(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.parse(
                    "<red>Usage: /nc radiation <check|add|remove|set|clear> [player] [amount]</red>"));
            return;
        }
        switch (args[1].toLowerCase()) {
            case "check"  -> handleRadiationCheck(sender, args);
            case "add"    -> handleRadiationAdd(sender, args);
            case "remove" -> handleRadiationRemove(sender, args);
            case "set"    -> handleRadiationSet(sender, args);
            case "clear"  -> handleRadiationClear(sender, args);
            default -> sender.sendMessage(ColorUtil.parse(
                    "<red>Unknown radiation subcommand. Try: check, add, remove, set, clear</red>"));
        }
    }

    private void handleRadiationCheck(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 3 && sender.hasPermission("nuclearcraft.admin.radiation")) {
            target = plugin.getServer().getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(ColorUtil.parse(
                        configManager.getMessage("general.player-not-found").replace("{player}", args[2])));
                return;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage(ColorUtil.parse("<red>Console must specify a player: /nc radiation check <player></red>"));
            return;
        }
        Optional<PlayerData> opt = playerDataManager.get(target);
        if (opt.isEmpty()) {
            sender.sendMessage(ColorUtil.parse("<red>No data loaded for " + target.getName() + ".</red>"));
            return;
        }
        PlayerData data = opt.get();
        int stage = data.getRadiationStage();
        String[] stageColors = {"<green>", "<yellow>", "<gold>", "<red>", "<dark_red>"};
        String[] stageNames  = {"Healthy", "Minor Exposure", "Moderate Exposure", "Severe Exposure", "Critical Poisoning"};
        String stageColor = stageColors[Math.min(stage, 4)];
        boolean contagious = radiationManager.isContagious(target);
        long immunityLeft = Math.max(0, data.getImmunityTimerEndMs() - System.currentTimeMillis()) / 1000;

        sender.sendMessage(ColorUtil.parse(
                "<dark_gray>── <gradient:#39ff14:#00ff88>Radiation Status: " + target.getName() + "</gradient> ──</dark_gray>"));
        sender.sendMessage(ColorUtil.parse(
                " <gray>Radiation:</gray> <white>" + (int) data.getRadiationLevel() + " / 1000</white>"));
        sender.sendMessage(ColorUtil.parse(
                " <gray>Stage:</gray> " + stageColor + stage + " — " + stageNames[Math.min(stage, 4)] + "</>"));
        sender.sendMessage(ColorUtil.parse(
                " <gray>Contagious:</gray> " + (contagious ? "<red>YES</red>" : "<green>NO</green>")));
        sender.sendMessage(ColorUtil.parse(
                " <gray>Immunity:</gray> " + (immunityLeft > 0 ? "<aqua>" + immunityLeft + "s remaining</aqua>" : "<gray>None</gray>")));
        sender.sendMessage(ColorUtil.parse(
                " <gray>Last Source:</gray> <white>" + data.getLastRadiationSource() + "</white>"));
        sender.sendMessage(ColorUtil.parse(
                " <gray>Zombies Killed:</gray> <white>" + data.getIrradiatedZombiesKilled()
                + " (" + data.getAlphaZombiesKilled() + " Alpha)</white>"));
        sender.sendMessage(ColorUtil.parse(
                " <gray>Ore Mined:</gray> <white>" + data.getPlutoniumOreMined()
                + " (Found: " + data.getPlutoniumOreFound() + ")</white>"));
        sender.sendMessage(ColorUtil.parse(
                " <gray>Fragments Collected:</gray> <white>" + data.getFragmentsCollected() + "</white>"));
        sender.sendMessage(ColorUtil.parse(
                " <gray>Ingots Produced:</gray> <white>" + data.getIngotsProduced() + "</white>"));
        sender.sendMessage(ColorUtil.parse(
                " <gray>Machines Built:</gray> <white>" + data.getMachinesBuilt() + "</white>"));
        sender.sendMessage(ColorUtil.parse(
                " <gray>Seeds Planted:</gray> <white>" + data.getSeedsPlanted() + "</white>"));
        sender.sendMessage(ColorUtil.parse(
                " <gray>Plants Harvested:</gray> <white>" + data.getPlantsHarvested() + "</white>"));
        sender.sendMessage(ColorUtil.parse(
                " <gray>Petals Collected:</gray> <white>" + data.getHealingPetalsCollected() + "</white>"));
        sender.sendMessage(ColorUtil.parse(
                " <gray>Cures Used:</gray> <white>"
                + data.getRadiationCuresUsed()
                + " (Antidotes: " + data.getAntidotesCrafted()
                + " / Serums: " + data.getSerumsCrafted() + ")</white>"));
    }

    private void handleRadiationAdd(CommandSender sender, String[] args) {
        if (!sender.hasPermission("nuclearcraft.admin.radiation")) {
            sender.sendMessage(ColorUtil.parse(configManager.getMessage("general.no-permission"))); return;
        }
        if (args.length < 4) { sender.sendMessage(ColorUtil.parse("<red>Usage: /nc radiation add <player> <amount></red>")); return; }
        Player target = resolvePlayer(sender, args[2]); if (target == null) return;
        int amount = parseAmount(sender, args[3]); if (amount < 0) return;
        radiationManager.addRadiation(target, amount, RadiationSource.COMMAND);
        sender.sendMessage(ColorUtil.parse("<green>Added <yellow>" + amount + "</yellow> radiation to <aqua>" + target.getName()
                + "</aqua>. New level: <white>" + (int) radiationManager.getRadiation(target) + "</white></green>"));
    }

    private void handleRadiationRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("nuclearcraft.admin.radiation")) {
            sender.sendMessage(ColorUtil.parse(configManager.getMessage("general.no-permission"))); return;
        }
        if (args.length < 4) { sender.sendMessage(ColorUtil.parse("<red>Usage: /nc radiation remove <player> <amount></red>")); return; }
        Player target = resolvePlayer(sender, args[2]); if (target == null) return;
        int amount = parseAmount(sender, args[3]); if (amount < 0) return;
        radiationManager.removeRadiation(target, amount);
        sender.sendMessage(ColorUtil.parse("<green>Removed <yellow>" + amount + "</yellow> radiation from <aqua>" + target.getName()
                + "</aqua>. New level: <white>" + (int) radiationManager.getRadiation(target) + "</white></green>"));
    }

    private void handleRadiationSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("nuclearcraft.admin.radiation")) {
            sender.sendMessage(ColorUtil.parse(configManager.getMessage("general.no-permission"))); return;
        }
        if (args.length < 4) { sender.sendMessage(ColorUtil.parse("<red>Usage: /nc radiation set <player> <amount></red>")); return; }
        Player target = resolvePlayer(sender, args[2]); if (target == null) return;
        int amount = parseAmount(sender, args[3]); if (amount < 0) return;
        radiationManager.setRadiation(target, amount);
        sender.sendMessage(ColorUtil.parse("<green>Set <aqua>" + target.getName() + "</aqua> radiation to <yellow>" + amount + "</yellow>.</green>"));
    }

    private void handleRadiationClear(CommandSender sender, String[] args) {
        if (!sender.hasPermission("nuclearcraft.admin.radiation")) {
            sender.sendMessage(ColorUtil.parse(configManager.getMessage("general.no-permission"))); return;
        }
        if (args.length < 3) { sender.sendMessage(ColorUtil.parse("<red>Usage: /nc radiation clear <player></red>")); return; }
        Player target = resolvePlayer(sender, args[2]); if (target == null) return;
        radiationManager.clearRadiation(target);
        sender.sendMessage(ColorUtil.parse("<green>Cleared all radiation from <aqua>" + target.getName() + "</aqua>.</green>"));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Phase 3: /nc zombie ...
    // ──────────────────────────────────────────────────────────────────────────

    private void handleZombie(CommandSender sender, String[] args) {
        if (!sender.hasPermission("nuclearcraft.admin.zombies")) {
            sender.sendMessage(ColorUtil.parse(configManager.getMessage("general.no-permission")));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.parse("<red>Usage: /nc zombie <spawn|stats|surge> [args...]</red>"));
            return;
        }
        switch (args[1].toLowerCase()) {
            case "spawn" -> handleZombieSpawn(sender, args);
            case "stats" -> handleZombieStats(sender);
            case "surge" -> handleZombieSurge(sender, args);
            default -> sender.sendMessage(ColorUtil.parse("<red>Unknown zombie subcommand.</red>"));
        }
    }

    private void handleZombieSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.parse("<red>Only players can spawn zombies.</red>")); return;
        }
        if (args.length < 3) {
            sender.sendMessage(ColorUtil.parse("<red>Usage: /nc zombie spawn <irradiated|alpha></red>")); return;
        }
        switch (args[2].toLowerCase()) {
            case "irradiated" -> {
                zombieSpawnManager.spawnAt(player.getLocation(), ZombieLevel.LEVEL_1);
                sender.sendMessage(ColorUtil.parse("<green>Spawned Irradiated Zombie.</green>"));
            }
            case "alpha" -> {
                zombieSpawnManager.spawnAt(player.getLocation(), ZombieLevel.LEVEL_4);
                sender.sendMessage(ColorUtil.parse("<green>Spawned Alpha Zombie.</green>"));
            }
            default -> sender.sendMessage(ColorUtil.parse("<red>Unknown zombie type. Use: irradiated, alpha</red>"));
        }
    }

    private void handleZombieStats(CommandSender sender) {
        sender.sendMessage(ColorUtil.parse("<dark_gray>── <gradient:#39ff14:#00ff88>Zombie Stats</gradient> ──</dark_gray>"));
        sender.sendMessage(ColorUtil.parse(" <gray>Tracked Zombies:</gray> <white>" + irradiatedZombieManager.getActiveCount() + "</white>"));
        sender.sendMessage(ColorUtil.parse(" <gray>Night Surge Active:</gray> " + (radiationNightManager.isSurgeActive() ? "<red>YES</red>" : "<green>NO</green>")));
    }

    private void handleZombieSurge(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ColorUtil.parse("<red>Usage: /nc zombie surge <start|stop></red>")); return;
        }
        switch (args[2].toLowerCase()) {
            case "start" -> {
                radiationNightManager.forceSurge();
                sender.sendMessage(ColorUtil.parse("<green>Radiation Surge started.</green>"));
            }
            case "stop" -> {
                radiationNightManager.forceEndSurge();
                sender.sendMessage(ColorUtil.parse("<green>Radiation Surge stopped.</green>"));
            }
            default -> sender.sendMessage(ColorUtil.parse("<red>Unknown surge action. Use: start, stop</red>"));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Phase 4: /nc ore ...
    // ──────────────────────────────────────────────────────────────────────────

    private void handleOre(CommandSender sender, String[] args) {
        if (!sender.hasPermission("nuclearcraft.admin.ore")) {
            sender.sendMessage(ColorUtil.parse(configManager.getMessage("general.no-permission"))); return;
        }
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.parse("<red>Usage: /nc ore <spawn|give|stats> [args...]</red>")); return;
        }
        switch (args[1].toLowerCase()) {
            case "spawn" -> handleOreSpawn(sender, args);
            case "give"  -> handleOreGive(sender, args);
            case "stats" -> handleOreStats(sender);
            default -> sender.sendMessage(ColorUtil.parse("<red>Unknown ore subcommand.</red>"));
        }
    }

    private void handleOreSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.parse("<red>Only players can spawn ore.</red>")); return;
        }
        Block oreBlock = player.getLocation().getBlock();
        oreBlock.setType(OreGenerationManager.ORE_MATERIAL);
        plutoniumOreManager.registerOre(oreBlock.getLocation());
        sender.sendMessage(ColorUtil.parse("<green>Plutonium Ore placed at your location.</green>"));
    }

    private void handleOreGive(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.parse("<red>Only players can receive ore items.</red>")); return;
        }
        if (args.length < 3) {
            sender.sendMessage(ColorUtil.parse("<red>Usage: /nc ore give <fragment|drill> [amount]</red>")); return;
        }
        String type = args[2].toLowerCase();
        int amount = args.length >= 4 ? Math.max(1, Math.min(64, parseAmount(sender, args[3]))) : 1;
        switch (type) {
            case "fragment" -> itemManager.getItem("raw-plutonium-fragment").ifPresent(
                    ci -> player.getInventory().addItem(ci.build(amount)));
            case "drill" -> itemManager.getItem("radiation-drill").ifPresent(
                    ci -> player.getInventory().addItem(ci.build(1)));
            default -> { sender.sendMessage(ColorUtil.parse("<red>Unknown ore give type.</red>")); return; }
        }
        sender.sendMessage(ColorUtil.parse("<green>Gave " + amount + "x " + type + " to " + player.getName() + ".</green>"));
    }

    private void handleOreStats(CommandSender sender) {
        sender.sendMessage(ColorUtil.parse("<dark_gray>── <gradient:#39ff14:#00ff88>Ore Stats</gradient> ──</dark_gray>"));
        sender.sendMessage(ColorUtil.parse(" <gray>Tracked Ore:</gray> <white>" + plutoniumOreManager.getTrackedCount() + "</white>"));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Phase 5: /nc smelter ...
    // ──────────────────────────────────────────────────────────────────────────

    private void handleSmelter(CommandSender sender, String[] args) {
        if (!sender.hasPermission("nuclearcraft.admin.smelter")) {
            sender.sendMessage(ColorUtil.parse(configManager.getMessage("general.no-permission"))); return;
        }
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.parse("<red>Usage: /nc smelter <give|stats|debug> [args...]</red>")); return;
        }
        switch (args[1].toLowerCase()) {
            case "give"  -> handleSmelterGive(sender, args);
            case "stats" -> handleSmelterStats(sender);
            case "debug" -> handleSmelterDebug(sender);
            default -> sender.sendMessage(ColorUtil.parse("<red>Unknown smelter subcommand.</red>"));
        }
    }

    private void handleSmelterGive(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 3) {
            target = plugin.getServer().getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(ColorUtil.parse(
                        configManager.getMessage("general.player-not-found").replace("{player}", args[2]))); return;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage(ColorUtil.parse("<red>Console must specify a player.</red>")); return;
        }
        itemManager.getItem("nuclear-smelter").ifPresent(ci -> target.getInventory().addItem(ci.build(1)));
        sender.sendMessage(ColorUtil.parse("<green>Gave Nuclear Smelter to <aqua>" + target.getName() + "</aqua>.</green>"));
    }

    private void handleSmelterStats(CommandSender sender) {
        sender.sendMessage(ColorUtil.parse("<dark_gray>── <gradient:#39ff14:#00ff88>Smelter Stats</gradient> ──</dark_gray>"));
        sender.sendMessage(ColorUtil.parse(" <gray>Active Machines:</gray> <white>" + nuclearSmelterManager.getMachineCount() + "</white>"));
    }

    private void handleSmelterDebug(CommandSender sender) {
        sender.sendMessage(ColorUtil.parse("<dark_gray>── <gradient:#39ff14:#00ff88>Smelter Debug</gradient> ──</dark_gray>"));
        var machines = nuclearSmelterManager.getAllMachines();
        if (machines.isEmpty()) {
            sender.sendMessage(ColorUtil.parse(" <gray>No active smelters.</gray>")); return;
        }
        for (SmelterData d : machines) {
            sender.sendMessage(ColorUtil.parse(
                    " <gray>" + d.getLocationKey() + "</gray> → <white>Progress: "
                    + d.getProgressTicks() + "t | Fuel: " + d.getFuelRemaining() + "t</white>"));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Phase 6: /nc equipment ...
    // ──────────────────────────────────────────────────────────────────────────

    private void handleEquipment(CommandSender sender, String[] args) {
        if (!sender.hasPermission("nuclearcraft.admin.equipment")) {
            sender.sendMessage(ColorUtil.parse(configManager.getMessage("general.no-permission"))); return;
        }
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.parse("<red>Usage: /nc equipment <give|stats> [args...]</red>")); return;
        }
        switch (args[1].toLowerCase()) {
            case "give"  -> handleEquipmentGive(sender, args);
            case "stats" -> handleEquipmentStats(sender);
            default -> sender.sendMessage(ColorUtil.parse("<red>Unknown equipment subcommand.</red>"));
        }
    }

    private void handleEquipmentGive(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.parse("<red>Only players can receive items.</red>")); return;
        }
        if (args.length < 3) {
            sender.sendMessage(ColorUtil.parse("<red>Usage: /nc equipment give <type></red>")); return;
        }
        String type = args[2].toLowerCase();
        var optItem = itemManager.getItem(type);
        if (optItem.isEmpty()) {
            sender.sendMessage(ColorUtil.parse("<red>Unknown equipment type: " + type + "</red>")); return;
        }
        player.getInventory().addItem(optItem.get().build(1));
        sender.sendMessage(ColorUtil.parse("<green>Gave <yellow>" + type + "</yellow> to you.</green>"));
    }

    private void handleEquipmentStats(CommandSender sender) {
        sender.sendMessage(ColorUtil.parse("<dark_gray>── <gradient:#39ff14:#00ff88>Equipment Stats</gradient> ──</dark_gray>"));
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            playerDataManager.get(p).ifPresent(pd ->
                    sender.sendMessage(ColorUtil.parse(
                            " <aqua>" + p.getName() + "</aqua> — <gray>Sword Hits: </gray><white>"
                            + pd.getSwordHits() + "</white> | <gray>Farmland Created: </gray><white>"
                            + pd.getFarmlandCreated() + "</white>")));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Phase 7: /nc farming ...
    // ──────────────────────────────────────────────────────────────────────────

    private void handleFarming(CommandSender sender, String[] args) {
        if (!sender.hasPermission("nuclearcraft.admin.farming")) {
            sender.sendMessage(ColorUtil.parse(configManager.getMessage("general.no-permission"))); return;
        }
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.parse("<red>Usage: /nc farming <give|growall|stats> [args...]</red>")); return;
        }
        switch (args[1].toLowerCase()) {
            case "give"    -> handleFarmingGive(sender, args);
            case "growall" -> handleFarmingGrowall(sender, args);
            case "stats"   -> handleFarmingStats(sender);
            default -> sender.sendMessage(ColorUtil.parse("<red>Unknown farming subcommand.</red>"));
        }
    }

    private void handleFarmingGive(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.parse("<red>Only players can receive items.</red>")); return;
        }
        if (args.length < 3) {
            sender.sendMessage(ColorUtil.parse(
                    "<red>Usage: /nc farming give <mutated-seed|healing-petal|radiation-antidote|radiation-serum> [amount]</red>"));
            return;
        }
        String type = args[2].toLowerCase();
        int amount = 1;
        if (args.length >= 4) {
            amount = Math.max(1, Math.min(64, parseAmount(sender, args[3])));
        }
        var optItem = itemManager.getItem(type);
        if (optItem.isEmpty()) {
            sender.sendMessage(ColorUtil.parse("<red>Unknown farming item: " + type + "</red>")); return;
        }
        player.getInventory().addItem(optItem.get().build(amount));
        sender.sendMessage(ColorUtil.parse(
                "<green>Gave <yellow>" + amount + "x " + type + "</yellow> to you.</green>"));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Phase 8: Forge subcommands
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * /nc forge give [player]             — give a Nuclear Forge to a player
     * /nc forge energy set <amount>       — set energy of looked-at forge
     * /nc forge energy add <amount>       — add energy to looked-at forge
     * /nc forge energy clear              — zero out energy of looked-at forge
     * /nc forge upgrade <mk1|2|3|4>       — instantly apply tier to held item
     * /nc forge stats                     — show aggregate forge statistics
     */
    private void handleForge(CommandSender sender, String[] args) {
        if (!sender.hasPermission("nuclearcraft.admin.forge")) {
            sender.sendMessage(ColorUtil.parse("<red>You don't have permission to use forge commands.</red>"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.parse("<yellow>Usage: /nc forge <give|energy|upgrade|stats></yellow>"));
            return;
        }
        switch (args[1].toLowerCase()) {
            case "give"   -> handleForgeGive(sender, args);
            case "energy" -> handleForgeEnergy(sender, args);
            case "upgrade"-> handleForgeUpgrade(sender, args);
            case "stats"  -> handleForgeStats(sender);
            default -> sender.sendMessage(ColorUtil.parse("<red>Unknown forge subcommand. Use: give|energy|upgrade|stats</red>"));
        }
    }

    private void handleForgeGive(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 3) {
            target = resolvePlayer(sender, args[2]);
            if (target == null) return;
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(ColorUtil.parse("<red>Specify a player: /nc forge give <player></red>"));
            return;
        }
        itemManager.getItem("nuclear-forge").ifPresentOrElse(
                item -> {
                    target.getInventory().addItem(item.build(1));
                    sender.sendMessage(ColorUtil.parse("<green>Gave a Nuclear Forge to <yellow>"
                            + target.getName() + "</yellow>.</green>"));
                },
                () -> sender.sendMessage(ColorUtil.parse("<red>Nuclear Forge item not found in registry.</red>"))
        );
    }

    private void handleForgeEnergy(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.parse("<red>You must be in-game and look at a forge block.</red>"));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(ColorUtil.parse("<yellow>Usage: /nc forge energy <set|add|clear> [amount]</yellow>"));
            return;
        }

        // Find the forge the player is looking at
        Block targetBlock = player.getTargetBlockExact(8);
        if (targetBlock == null || !nuclearForgeManager.isForge(targetBlock)) {
            sender.sendMessage(ColorUtil.parse("<red>Look at a Nuclear Forge block (within 8 blocks).</red>"));
            return;
        }

        ForgeData forge = nuclearForgeManager.getForgeAt(targetBlock).orElse(null);
        if (forge == null) {
            sender.sendMessage(ColorUtil.parse("<red>Could not locate forge data.</red>"));
            return;
        }

        switch (args[2].toLowerCase()) {
            case "set" -> {
                if (args.length < 4) { sender.sendMessage(ColorUtil.parse("<yellow>Usage: /nc forge energy set <amount></yellow>")); return; }
                int amount = parseAmount(sender, args[3]);
                if (amount < 0) return;
                nuclearForgeManager.setEnergy(forge, amount);
                sender.sendMessage(ColorUtil.parse("<green>Forge energy set to <yellow>" + amount + "</yellow>.</green>"));
            }
            case "add" -> {
                if (args.length < 4) { sender.sendMessage(ColorUtil.parse("<yellow>Usage: /nc forge energy add <amount></yellow>")); return; }
                int amount = parseAmount(sender, args[3]);
                if (amount < 0) return;
                forge.addEnergy(amount);
                sender.sendMessage(ColorUtil.parse("<green>Added <yellow>" + amount + "</yellow> energy to forge. Total: "
                        + (int) forge.getEnergy() + "</green>"));
            }
            case "clear" -> {
                forge.setEnergy(0);
                sender.sendMessage(ColorUtil.parse("<green>Forge energy cleared.</green>"));
            }
            default -> sender.sendMessage(ColorUtil.parse("<red>Unknown action. Use: set|add|clear</red>"));
        }
    }

    private void handleForgeUpgrade(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.parse("<red>Only players can use /nc forge upgrade.</red>"));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(ColorUtil.parse("<yellow>Usage: /nc forge upgrade <mk1|mk2|mk3|mk4></yellow>"));
            return;
        }

        UpgradeTier target;
        try {
            String tierStr = args[2].replace("mk", "mk_").toUpperCase();
            target = UpgradeTier.valueOf(tierStr);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ColorUtil.parse("<red>Invalid tier. Use: mk1, mk2, mk3, mk4</red>"));
            return;
        }

        var item = player.getInventory().getItemInMainHand();
        if (!upgradeManager.isUpgradeable(item)) {
            sender.sendMessage(ColorUtil.parse("<red>Hold a Plutonium or Hazmat item in your main hand.</red>"));
            return;
        }

        upgradeManager.applyTier(item, target);
        player.getInventory().setItemInMainHand(item);
        sender.sendMessage(ColorUtil.parse("<green>Applied <yellow>" + target.getDisplayName()
                + "</yellow> upgrade to your held item.</green>"));
    }

    private void handleForgeStats(CommandSender sender) {
        sender.sendMessage(ColorUtil.parse(
                "<dark_gray>── <gradient:#39ff14:#ffaa00>Nuclear Forge Stats</gradient> ──</dark_gray>"));
        sender.sendMessage(ColorUtil.parse(
                " <gray>Active Forges:</gray> <white>" + nuclearForgeManager.getMachineCount() + "</white>"));
        sender.sendMessage(ColorUtil.parse(
                " <gray>Total Upgrade Attempts:</gray> <white>" + nuclearForgeManager.getGlobalForgeUses() + "</white>"));
        sender.sendMessage(ColorUtil.parse(
                " <gray>Successes:</gray> <green>" + nuclearForgeManager.getGlobalSuccesses() + "</green>"
                + "  <gray>Failures:</gray> <red>" + nuclearForgeManager.getGlobalFailures() + "</red>"));
        sender.sendMessage(ColorUtil.parse(
                " <gray>MK-IV Creations:</gray> <gold>" + nuclearForgeManager.getGlobalMk4Creations() + "</gold>"));
        sender.sendMessage(ColorUtil.parse(
                " <gray>Overloads:</gray> <dark_red>" + nuclearForgeManager.getGlobalOverloads() + "</dark_red>"));

        for (Player p : plugin.getServer().getOnlinePlayers()) {
            playerDataManager.get(p).ifPresent(pd -> {
                if (pd.getForgeUses() > 0) {
                    sender.sendMessage(ColorUtil.parse(
                            " <aqua>" + p.getName() + "</aqua>"
                            + " — <gray>Uses:</gray> <white>" + pd.getForgeUses() + "</white>"
                            + " <gray>✔</gray> <green>" + pd.getSuccessfulUpgrades() + "</green>"
                            + " <gray>✘</gray> <red>" + pd.getFailedUpgrades() + "</red>"
                            + " <gray>MK-IV:</gray> <gold>" + pd.getMk4Creations() + "</gold>"));
                }
            });
        }
    }

    private void handleFarmingGrowall(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.parse("<red>Only players can use growall.</red>")); return;
        }
        int radius = 16;
        if (args.length >= 3) {
            try { radius = Math.max(1, Math.min(64, Integer.parseInt(args[2]))); }
            catch (NumberFormatException e) { /* use default */ }
        }
        farmingManager.getGrowthManager().forceGrowNearby(player.getLocation(), radius);
        sender.sendMessage(ColorUtil.parse(
                "<green>Force-grew all mutated crops within <yellow>" + radius + "</yellow> blocks.</green>"));
    }

    private void handleFarmingStats(CommandSender sender) {
        sender.sendMessage(ColorUtil.parse(
                "<dark_gray>── <gradient:#39ff14:#00ff88>Farming Stats</gradient> ──</dark_gray>"));
        sender.sendMessage(ColorUtil.parse(
                " <gray>Active Mutated Crops:</gray> <white>"
                + farmingManager.getCropManager().getCropCount() + "</white>"));
        sender.sendMessage(ColorUtil.parse(
                " <gray>Active Toxic Blooms:</gray> <white>"
                + farmingManager.getToxicBloomManager().getBloomCount() + "</white>"));
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            playerDataManager.get(p).ifPresent(pd ->
                    sender.sendMessage(ColorUtil.parse(
                            " <aqua>" + p.getName() + "</aqua>"
                            + " — <gray>Seeds:</gray> <white>" + pd.getSeedsPlanted() + "</white>"
                            + " <gray>Harvests:</gray> <white>" + pd.getPlantsHarvested() + "</white>"
                            + " <gray>Petals:</gray> <white>" + pd.getHealingPetalsCollected() + "</white>"
                            + " <gray>Cures:</gray> <white>" + pd.getRadiationCuresUsed() + "</white>"
                            + (farmingManager.getImmunityManager().isImmune(p)
                                ? " <gold>[IMMUNE: " + farmingManager.getImmunityManager().getRemainingSeconds(p) + "s]</gold>"
                                : ""))));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Phase 9 combat
    // ──────────────────────────────────────────────────────────────────────────

    private void handleCombat(CommandSender sender, String[] args) {
        if (!sender.hasPermission("nuclearcraft.admin.combat")) {
            sender.sendMessage(ColorUtil.parse(configManager.getMessage("general.no-permission")));
            return;
        }
        if (combatManager == null) {
            sender.sendMessage(ColorUtil.parse("<red>Combat system (Phase 9) is not initialized.</red>"));
            return;
        }
        String sub = args.length >= 2 ? args[1].toLowerCase() : "stats";
        switch (sub) {
            case "stats" -> {
                Player target = (sender instanceof Player p && args.length < 3) ? p
                        : args.length >= 3 ? resolvePlayer(sender, args[2]) : null;
                if (target == null) { sender.sendMessage(ColorUtil.parse("<red>Usage: /nc combat stats [player]</red>")); return; }
                playerDataManager.get(target.getUniqueId()).ifPresentOrElse(data -> {
                    sender.sendMessage(ColorUtil.parse(
                            "<gold>☢ Combat Stats — <white>" + target.getName() + "</white></gold>"));
                    sender.sendMessage(ColorUtil.parse(
                            "  <gray>PvP Kills:</gray> <white>" + data.getPvPKills() + "</white>"
                            + "  <gray>Radiation Kills:</gray> <white>" + data.getPvPRadiationKills() + "</white>"));
                    sender.sendMessage(ColorUtil.parse(
                            "  <gray>Arrow Kills:</gray> <white>" + data.getPvPArrowKills() + "</white>"
                            + "  <gray>Aura Kills:</gray> <white>" + data.getPvPAuraKills() + "</white>"));
                    sender.sendMessage(ColorUtil.parse(
                            "  <gray>Total PvP Radiation Inflicted:</gray> <green>" + data.getTotalPvPRadiationInflicted() + "</green>"));
                }, () -> sender.sendMessage(ColorUtil.parse("<red>No data found for " + target.getName() + ".</red>")));
            }
            case "mastery" -> {
                Player target = (sender instanceof Player p && args.length < 3) ? p
                        : args.length >= 3 ? resolvePlayer(sender, args[2]) : null;
                if (target == null) { sender.sendMessage(ColorUtil.parse("<red>Usage: /nc combat mastery [player]</red>")); return; }
                sender.sendMessage(combatManager.getMasteryManager().getSummary(target));
            }
            case "radiation" -> {
                Player target = (sender instanceof Player p && args.length < 3) ? p
                        : args.length >= 3 ? resolvePlayer(sender, args[2]) : null;
                if (target == null) { sender.sendMessage(ColorUtil.parse("<red>Usage: /nc combat radiation [player]</red>")); return; }
                playerDataManager.get(target.getUniqueId()).ifPresent(data -> {
                    sender.sendMessage(ColorUtil.parse(
                            "<gold>☢ Radiation Combat — <white>" + target.getName() + "</white></gold>"));
                    sender.sendMessage(ColorUtil.parse(
                            "  <gray>Arrow Hits:</gray> <white>" + data.getPvPArrowHits() + "</white>"));
                    sender.sendMessage(ColorUtil.parse(
                            "  <gray>Aura Damage Dealt:</gray> <green>" + data.getAuraDamageDealt() + "</green>"));
                });
            }
            default -> sender.sendMessage(ColorUtil.parse(
                    "<red>Unknown combat subcommand. Use: stats | mastery | radiation</red>"));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Phase 14 — Admin commands
    // ──────────────────────────────────────────────────────────────────────────

    private void handleAdminCmd(CommandSender sender, String subcommand) {
        if (!sender.hasPermission("nuclearcraft.admin")) {
            sender.sendMessage(ColorUtil.parse(configManager.getMessage("general.no-permission")));
            return;
        }
        if (adminManager == null) {
            sender.sendMessage(ColorUtil.parse("<red>[NuclearCraft] Admin system (Phase 14) is not initialized.</red>"));
            return;
        }
        adminManager.handleAdminCommand(sender, new String[]{subcommand});
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Phase 10 — Titan
    // ──────────────────────────────────────────────────────────────────────────

    private void handleTitan(CommandSender sender, String[] args) {
        if (!sender.hasPermission("nuclearcraft.admin.titan")) {
            sender.sendMessage(ColorUtil.parse(configManager.getMessage("general.no-permission")));
            return;
        }
        if (titanManager == null) {
            sender.sendMessage(ColorUtil.parse("<red>Titan system (Phase 10) is not initialized.</red>"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.parse("<gold>☢ /nc titan</gold> <gray>spawn|kill|phase|stats</gray>"));
            return;
        }
        switch (args[1].toLowerCase()) {
            case "spawn" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage(ColorUtil.parse("<red>Only players can use titan spawn from console (no location).</red>"));
                    return;
                }
                if (titanManager.isTitanAlive()) {
                    sender.sendMessage(ColorUtil.parse("<red>A Plutonium Titan is already active!</red>"));
                    return;
                }
                titanManager.spawnTitan(p.getLocation(), p);
                sender.sendMessage(ColorUtil.parse("<green>☢ Summoned the Plutonium Titan at your location.</green>"));
            }
            case "kill" -> {
                if (!titanManager.isTitanAlive()) {
                    sender.sendMessage(ColorUtil.parse("<red>No active Titan to kill.</red>"));
                    return;
                }
                titanManager.adminKillTitan();
                sender.sendMessage(ColorUtil.parse("<green>☢ Titan forcibly killed.</green>"));
            }
            case "phase" -> {
                if (args.length < 3) {
                    sender.sendMessage(ColorUtil.parse("<red>Usage: /nc titan phase <1-4></red>"));
                    return;
                }
                int phaseNum = parseAmount(sender, args[2]);
                if (phaseNum < 1 || phaseNum > 4) {
                    sender.sendMessage(ColorUtil.parse("<red>Phase must be 1–4.</red>"));
                    return;
                }
                if (!titanManager.isTitanAlive()) {
                    sender.sendMessage(ColorUtil.parse("<red>No active Titan.</red>"));
                    return;
                }
                titanManager.getPhaseManager().forcePhase(phaseNum);
                sender.sendMessage(ColorUtil.parse("<green>☢ Titan forced to phase " + phaseNum + ".</green>"));
            }
            case "stats" -> {
                Player target = (sender instanceof Player p && args.length < 3) ? p
                        : args.length >= 3 ? resolvePlayer(sender, args[2]) : null;
                if (target == null) {
                    sender.sendMessage(ColorUtil.parse("<red>Usage: /nc titan stats [player]</red>"));
                    return;
                }
                playerDataManager.get(target.getUniqueId()).ifPresentOrElse(data -> {
                    sender.sendMessage(ColorUtil.parse(
                            "<gold>☢ Titan Stats — <white>" + target.getName() + "</white></gold>"));
                    sender.sendMessage(ColorUtil.parse(
                            "  <gray>Summons:</gray> <white>" + data.getTitanSummons()
                            + "</white>  <gray>Kills:</gray> <white>" + data.getTitanKills()
                            + "</white>  <gray>Deaths:</gray> <white>" + data.getTitanDeaths() + "</white>"));
                    sender.sendMessage(ColorUtil.parse(
                            "  <gray>Damage Dealt:</gray> <green>" + data.getTitanDamageDealt()
                            + "</green>  <gray>Damage Taken:</gray> <red>" + data.getTitanDamageTaken() + "</red>"));
                    sender.sendMessage(ColorUtil.parse(
                            "  <gray>Catastrophes Survived:</gray> <yellow>" + data.getCatastrophesSurvived()
                            + "</yellow>  <gray>Cores Obtained:</gray> <aqua>" + data.getTitanCoresObtained() + "</aqua>"));
                }, () -> sender.sendMessage(ColorUtil.parse("<red>No data found for " + target.getName() + ".</red>")));
            }
            default -> sender.sendMessage(ColorUtil.parse(
                    "<red>Unknown titan subcommand. Use: spawn | kill | phase | stats</red>"));
        }
    }

    private void handleTitanTech(CommandSender sender, String[] args) {
        if (!sender.hasPermission("nuclearcraft.admin.titan")) {
            sender.sendMessage(ColorUtil.parse(configManager.getMessage("general.no-permission")));
            return;
        }
        if (titanTechManager == null) {
            sender.sendMessage(ColorUtil.parse("<red>Titan Technology (Phase 11) is not initialized.</red>"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.parse("<gold>☢ /nc titantech</gold> <gray>give|stats|aura|setbonusinfo</gray>"));
            return;
        }
        switch (args[1].toLowerCase()) {
            case "give" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage(ColorUtil.parse("<red>Only players can use titantech give.</red>")); return;
                }
                if (args.length < 3) {
                    sender.sendMessage(ColorUtil.parse("<red>Usage: /nc titantech give <item> [amount]</red>")); return;
                }
                String itemId = args[2].toLowerCase();
                int amount = args.length >= 4 ? parseAmount(sender, args[3]) : 1;
                if (amount < 1) { sender.sendMessage(ColorUtil.parse("<red>Amount must be at least 1.</red>")); return; }
                itemManager.getItem(itemId).ifPresentOrElse(ci -> {
                    org.bukkit.inventory.ItemStack item = ci.build(amount);
                    p.getInventory().addItem(item).forEach((k, v) -> p.getWorld().dropItemNaturally(p.getLocation(), v));
                    sender.sendMessage(ColorUtil.parse("<green>☢ Gave " + amount + "x <white>" + itemId + "</white> to " + p.getName() + ".</green>"));
                }, () -> sender.sendMessage(ColorUtil.parse("<red>Unknown item: " + itemId + "</red>")));
            }
            case "stats" -> {
                Player target = (sender instanceof Player p && args.length < 3) ? p
                        : args.length >= 3 ? resolvePlayer(sender, args[2]) : null;
                if (target == null) {
                    sender.sendMessage(ColorUtil.parse("<red>Usage: /nc titantech stats [player]</red>")); return;
                }
                playerDataManager.get(target.getUniqueId()).ifPresentOrElse(data -> {
                    sender.sendMessage(ColorUtil.parse(
                            "<gold>☢ Titan Tech Stats — <white>" + target.getName() + "</white></gold>"));
                    sender.sendMessage(ColorUtil.parse(
                            "  <gray>Equipment Crafted:</gray> <aqua>" + data.getTitanEquipmentCrafted() + "</aqua>"));
                    sender.sendMessage(ColorUtil.parse(
                            "  <gray>Radiation Reflected:</gray> <light_purple>" + data.getRadiationReflected() + "</light_purple>"));
                    sender.sendMessage(ColorUtil.parse(
                            "  <gray>Sword Hits:</gray> <yellow>" + data.getSwordHits() + "</yellow>"
                            + "  <gray>Arrows Fired:</gray> <yellow>" + data.getArrowsFired() + "</yellow>"));
                    boolean fullSet = target.isOnline() && titanTechManager.getSetBonusManager().hasFullSet(target);
                    sender.sendMessage(ColorUtil.parse(
                            "  <gray>Full Set Active:</gray> " + (fullSet ? "<green>YES</green>" : "<red>NO</red>")));
                }, () -> sender.sendMessage(ColorUtil.parse("<red>No data for " + target.getName() + ".</red>")));
            }
            case "aura" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage(ColorUtil.parse("<red>Only players can test aura.</red>")); return;
                }
                sender.sendMessage(ColorUtil.parse(
                        "<light_purple>☢ Titan Aura fires if you hold a Titan weapon in main hand. " +
                        "Radius: 5 blocks, interval: 40 ticks.</light_purple>"));
            }
            case "setbonusinfo" -> {
                sender.sendMessage(ColorUtil.parse("<gold>☢ Titan Set Bonus Effects:</gold>"));
                sender.sendMessage(ColorUtil.parse("  <gray>Full set:</gray> Radiation Immunity, Speed II, Jump II, Resistance, Fire Resistance"));
                sender.sendMessage(ColorUtil.parse("  <gray>+16 max HP</gray> (chestplate +4, set +8 = +12 hearts)"));
                sender.sendMessage(ColorUtil.parse("  <gray>30% radiation reflection back to attacker</gray>"));
                sender.sendMessage(ColorUtil.parse("  <gray>Auto-cure 100 radiation every 20s</gray>"));
            }
            default -> sender.sendMessage(ColorUtil.parse(
                    "<red>Unknown titantech subcommand. Use: give | stats | aura | setbonusinfo</red>"));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Tab completion
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return filter(TOP_SUBCOMMANDS, args[0]);
        }
        return switch (args[0].toLowerCase()) {
            case "radiation" -> args.length == 2 ? filter(RADIATION_SUBCOMMANDS, args[1]) : List.of();
            case "zombie"    -> args.length == 2 ? filter(ZOMBIE_SUBCOMMANDS, args[1])
                              : args.length == 3 && "spawn".equals(args[1]) ? filter(ZOMBIE_SPAWN_TYPES, args[2])
                              : args.length == 3 && "surge".equals(args[1]) ? filter(ZOMBIE_SURGE_ACTIONS, args[2])
                              : List.of();
            case "ore"       -> args.length == 2 ? filter(ORE_SUBCOMMANDS, args[1])
                              : args.length == 3 && "spawn".equals(args[1]) ? filter(ORE_SPAWN_TYPES, args[2])
                              : args.length == 3 && "give".equals(args[1])  ? filter(ORE_GIVE_TYPES, args[2])
                              : List.of();
            case "smelter"   -> args.length == 2 ? filter(SMELTER_SUBCOMMANDS, args[1]) : List.of();
            case "equipment" -> args.length == 2 ? filter(EQUIPMENT_SUBCOMMANDS, args[1])
                              : args.length == 3 && "give".equals(args[1]) ? filter(EQUIPMENT_GIVE_TYPES, args[2])
                              : List.of();
            case "farming"   -> args.length == 2 ? filter(FARMING_SUBCOMMANDS, args[1])
                              : args.length == 3 && "give".equals(args[1]) ? filter(FARMING_GIVE_TYPES, args[2])
                              : List.of();
            case "forge"     -> args.length == 2 ? filter(FORGE_SUBCOMMANDS, args[1])
                              : args.length == 3 && "energy".equals(args[1])  ? filter(FORGE_ENERGY_ACTIONS, args[2])
                              : args.length == 3 && "upgrade".equals(args[1]) ? filter(FORGE_UPGRADE_TIERS, args[2])
                              : List.of();
            case "combat"    -> args.length == 2 ? filter(COMBAT_SUBCOMMANDS, args[1]) : List.of();
            case "titan"     -> args.length == 2 ? filter(TITAN_SUBCOMMANDS, args[1])
                              : args.length == 3 && "phase".equals(args[1]) ? List.of("1", "2", "3", "4")
                              : List.of();
            case "titantech" -> args.length == 2 ? filter(TITANTECH_SUBCOMMANDS, args[1])
                              : args.length == 3 && "give".equals(args[1]) ? filter(TITANTECH_GIVE_TYPES, args[2])
                              : List.of();
            case "give"      -> args.length == 2
                    ? plugin.getServer().getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList())
                    : List.of();
            case "health", "diagnostics", "performance", "cleanup",
                    "fixdata", "dumpdata", "serverreport" -> List.of();
            default -> List.of();
        };
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Utilities
    // ──────────────────────────────────────────────────────────────────────────

    private Player resolvePlayer(CommandSender sender, String name) {
        var target = plugin.getServer().getPlayer(name);
        if (target == null) {
            sender.sendMessage(ColorUtil.parse(
                    configManager.getMessage("general.player-not-found").replace("{player}", name)));
        }
        return target;
    }

    private int parseAmount(CommandSender sender, String raw) {
        try { return Math.max(0, Integer.parseInt(raw)); }
        catch (NumberFormatException e) {
            sender.sendMessage(ColorUtil.parse(
                    configManager.getMessage("general.invalid-number").replace("{input}", raw)));
            return -1;
        }
    }

    private List<String> filter(List<String> list, String prefix) {
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
}
