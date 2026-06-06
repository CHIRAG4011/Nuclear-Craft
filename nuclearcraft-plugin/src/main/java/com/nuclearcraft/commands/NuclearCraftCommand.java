package com.nuclearcraft.commands;

import com.nuclearcraft.advancements.AdvancementManager;
import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.data.PlayerData;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.ore.OreMiningManager;
import com.nuclearcraft.ore.PlutoniumOreManager;
import com.nuclearcraft.ore.OreGenerationManager;
import com.nuclearcraft.radiation.RadiationManager;
import com.nuclearcraft.radiation.RadiationSource;
import com.nuclearcraft.smelter.NuclearSmelterManager;
import com.nuclearcraft.smelter.SmelterData;
import com.nuclearcraft.utils.ColorUtil;
import com.nuclearcraft.utils.NCLogger;
import com.nuclearcraft.zombies.*;
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
 */
public class NuclearCraftCommand implements CommandExecutor, TabCompleter {

    private static final List<String> TOP_SUBCOMMANDS =
            List.of("help", "reload", "info", "debug", "give", "version", "radiation", "zombie", "ore", "smelter");
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
                                NuclearSmelterManager nuclearSmelterManager) {
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
            case "debug"     -> { handleDebug(sender);             yield true; }
            case "give"      -> { handleGive(sender, args);        yield true; }
            case "radiation" -> { handleRadiation(sender, args);   yield true; }
            case "zombie"    -> { handleZombie(sender, args);      yield true; }
            case "ore"       -> { handleOre(sender, args);         yield true; }
            case "smelter"   -> { handleSmelter(sender, args);     yield true; }
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

    private void handleDebug(CommandSender sender) {
        if (!sender.hasPermission("nuclearcraft.debug")) {
            sender.sendMessage(ColorUtil.parse(configManager.getMessage("general.no-permission")));
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
            default -> sender.sendMessage(ColorUtil.parse(
                    "<red>Unknown zombie subcommand. Try: spawn, stats, surge</red>"));
        }
    }

    private void handleZombieSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.parse("<red>This command can only be used by players.</red>"));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(ColorUtil.parse("<red>Usage: /nc zombie spawn <irradiated|alpha></red>"));
            return;
        }
        ZombieLevel level = switch (args[2].toLowerCase()) {
            case "alpha" -> ZombieLevel.LEVEL_4;
            default      -> ZombieLevel.LEVEL_1;
        };
        IrradiatedZombie iz = zombieSpawnManager.spawnAt(player.getLocation().add(2, 0, 0), level);
        sender.sendMessage(ColorUtil.parse("<green>Spawned <yellow>" + iz.getZombieLevel().getDisplayName()
                + " [L" + level.getLevel() + "]</yellow> near you.</green>"));
    }

    private void handleZombieStats(CommandSender sender) {
        sender.sendMessage(ColorUtil.parse("<dark_gray>── <green>Irradiated Zombie System Stats</green> ──</dark_gray>"));
        sender.sendMessage(ColorUtil.parse(
                " <gray>Total Spawned:</gray> <white>" + irradiatedZombieManager.getTotalSpawned() + "</white>"));
        sender.sendMessage(ColorUtil.parse(
                " <gray>Active Zombies:</gray> <white>" + irradiatedZombieManager.getActiveCount() + "</white>"));
        sender.sendMessage(ColorUtil.parse(
                " <gray>Active Alphas:</gray> <white>" + irradiatedZombieManager.getActiveAlphaCount() + "</white>"));
        sender.sendMessage(ColorUtil.parse(
                " <gray>Total Alpha Spawned:</gray> <white>" + irradiatedZombieManager.getTotalAlphaSpawned() + "</white>"));
        sender.sendMessage(ColorUtil.parse(
                " <gray>Active Radiation Clouds:</gray> <white>" + radiationCloudManager.getActiveCount() + "</white>"));
        sender.sendMessage(ColorUtil.parse(
                " <gray>Radiation Surge:</gray> " + (radiationNightManager.isSurgeActive()
                        ? "<red>ACTIVE ☢</red>" : "<green>Inactive</green>")));
    }

    private void handleZombieSurge(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ColorUtil.parse("<red>Usage: /nc zombie surge <start|stop></red>"));
            return;
        }
        switch (args[2].toLowerCase()) {
            case "start" -> {
                radiationNightManager.forceSurge();
                sender.sendMessage(ColorUtil.parse("<green>Radiation Surge force-started.</green>"));
            }
            case "stop" -> {
                radiationNightManager.forceEndSurge();
                sender.sendMessage(ColorUtil.parse("<green>Radiation Surge force-stopped.</green>"));
            }
            default -> sender.sendMessage(ColorUtil.parse("<red>Use: start or stop</red>"));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Phase 4: /nc ore ...
    // ──────────────────────────────────────────────────────────────────────────

    private void handleOre(CommandSender sender, String[] args) {
        if (!sender.hasPermission("nuclearcraft.admin.ore")) {
            sender.sendMessage(ColorUtil.parse(configManager.getMessage("general.no-permission")));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.parse("<red>Usage: /nc ore <spawn|give|stats> [args...]</red>"));
            return;
        }
        switch (args[1].toLowerCase()) {
            case "spawn" -> handleOreSpawn(sender, args);
            case "give"  -> handleOreGive(sender, args);
            case "stats" -> handleOreStats(sender);
            default -> sender.sendMessage(ColorUtil.parse(
                    "<red>Unknown ore subcommand. Try: spawn, give, stats</red>"));
        }
    }

    private void handleOreSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.parse("<red>This command can only be used by players.</red>"));
            return;
        }
        if (args.length < 3 || !args[2].equalsIgnoreCase("plutonium")) {
            sender.sendMessage(ColorUtil.parse("<red>Usage: /nc ore spawn plutonium</red>"));
            return;
        }
        var block = player.getTargetBlockExact(5);
        var spawnLoc = (block != null) ? block.getLocation() : player.getLocation();
        spawnLoc.getBlock().setType(OreGenerationManager.ORE_MATERIAL);
        plutoniumOreManager.registerOre(spawnLoc);
        sender.sendMessage(ColorUtil.parse(
                "<green>☢ Plutonium Ore spawned at <white>"
                        + spawnLoc.getBlockX() + ", "
                        + spawnLoc.getBlockY() + ", "
                        + spawnLoc.getBlockZ() + "</white>.</green>"));
    }

    private void handleOreGive(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ColorUtil.parse("<red>Usage: /nc ore give <fragment|drill> [amount]</red>"));
            return;
        }
        Player target = (sender instanceof Player p) ? p : null;
        if (target == null) {
            sender.sendMessage(ColorUtil.parse("<red>Console cannot use ore give. Use /nc give instead.</red>"));
            return;
        }
        String itemId = switch (args[2].toLowerCase()) {
            case "fragment" -> "raw-plutonium-fragment";
            case "drill"    -> "radiation-drill";
            default -> null;
        };
        if (itemId == null) {
            sender.sendMessage(ColorUtil.parse("<red>Unknown item. Try: fragment, drill</red>"));
            return;
        }
        int amount = 1;
        if (args.length >= 4) {
            try { amount = Math.max(1, Math.min(64, Integer.parseInt(args[3]))); }
            catch (NumberFormatException ignored) {}
        }
        final int finalAmount = amount;
        itemManager.getItem(itemId).ifPresentOrElse(item -> {
            target.getInventory().addItem(item.build(finalAmount));
            sender.sendMessage(ColorUtil.parse(
                    "<green>Given <yellow>" + finalAmount + "x " + args[2] + "</yellow> to <aqua>"
                            + target.getName() + "</aqua>.</green>"));
        }, () -> sender.sendMessage(ColorUtil.parse("<red>Item '" + itemId + "' not found in registry.</red>")));
    }

    private void handleOreStats(CommandSender sender) {
        sender.sendMessage(ColorUtil.parse("<dark_gray>── <gradient:#39ff14:#00bfff>Plutonium Ore System Stats</gradient> ──</dark_gray>"));
        sender.sendMessage(ColorUtil.parse(
                " <gray>Tracked Ore Blocks:</gray> <white>" + plutoniumOreManager.getTrackedCount() + "</white>"));

        int totalFound = 0, totalMined = 0, totalFragments = 0, totalBursts = 0, totalDrillUses = 0, totalUnsafe = 0;
        for (var player : plugin.getServer().getOnlinePlayers()) {
            var dataOpt = playerDataManager.get(player.getUniqueId());
            if (dataOpt.isEmpty()) continue;
            var data = dataOpt.get();
            totalFound     += data.getPlutoniumOreFound();
            totalMined     += data.getPlutoniumOreMined();
            totalFragments += data.getFragmentsCollected();
            totalBursts    += data.getRadiationBurstsTriggered();
            totalDrillUses += data.getDrillUses();
            totalUnsafe    += data.getUnsafeMiningAttempts();
        }
        sender.sendMessage(ColorUtil.parse(" <gray>Ore Found (online):</gray> <white>" + totalFound + "</white>"));
        sender.sendMessage(ColorUtil.parse(" <gray>Ore Mined (online):</gray> <white>" + totalMined + "</white>"));
        sender.sendMessage(ColorUtil.parse(" <gray>Fragments Collected:</gray> <white>" + totalFragments + "</white>"));
        sender.sendMessage(ColorUtil.parse(" <gray>Radiation Bursts:</gray> <red>" + totalBursts + "</red>"));
        sender.sendMessage(ColorUtil.parse(" <gray>Drill Uses:</gray> <white>" + totalDrillUses + "</white>"));
        sender.sendMessage(ColorUtil.parse(" <gray>Unsafe Mining Attempts:</gray> <red>" + totalUnsafe + "</red>"));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Phase 5: /nc smelter ...
    // ──────────────────────────────────────────────────────────────────────────

    private void handleSmelter(CommandSender sender, String[] args) {
        if (!sender.hasPermission("nuclearcraft.admin.smelter")) {
            sender.sendMessage(ColorUtil.parse(configManager.getMessage("general.no-permission")));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.parse("<red>Usage: /nc smelter <give|stats|debug> [args...]</red>"));
            return;
        }
        switch (args[1].toLowerCase()) {
            case "give"  -> handleSmelterGive(sender, args);
            case "stats" -> handleSmelterStats(sender);
            case "debug" -> handleSmelterDebug(sender);
            default -> sender.sendMessage(ColorUtil.parse(
                    "<red>Unknown smelter subcommand. Try: give, stats, debug</red>"));
        }
    }

    private void handleSmelterGive(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 3) {
            target = plugin.getServer().getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(ColorUtil.parse(
                        configManager.getMessage("general.player-not-found").replace("{player}", args[2])));
                return;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage(ColorUtil.parse("<red>Console must specify a player: /nc smelter give <player></red>"));
            return;
        }

        itemManager.getItem("nuclear-smelter").ifPresentOrElse(item -> {
            target.getInventory().addItem(item.build());
            sender.sendMessage(ColorUtil.parse(
                    "<green>Given <gradient:#39ff14:#00bfff>Nuclear Smelter</gradient> to <aqua>"
                            + target.getName() + "</aqua>.</green>"));
        }, () -> sender.sendMessage(ColorUtil.parse(
                "<red>Nuclear Smelter item not found in registry — check ItemManager.</red>")));
    }

    private void handleSmelterStats(CommandSender sender) {
        sender.sendMessage(ColorUtil.parse(
                "<dark_gray>── <gradient:#39ff14:#00bfff>Nuclear Smelter System Stats</gradient> ──</dark_gray>"));
        sender.sendMessage(ColorUtil.parse(
                " <gray>Active Machines:</gray> <white>" + nuclearSmelterManager.getMachineCount() + "</white>"));

        int totalMachinesBuilt = 0, totalFragProcessed = 0, totalIngots = 0, totalOverheats = 0;
        for (var player : plugin.getServer().getOnlinePlayers()) {
            var dataOpt = playerDataManager.get(player.getUniqueId());
            if (dataOpt.isEmpty()) continue;
            var data = dataOpt.get();
            totalMachinesBuilt += data.getMachinesBuilt();
            totalFragProcessed += data.getFragmentsProcessed();
            totalIngots        += data.getIngotsProduced();
            totalOverheats     += data.getOverheatsTriggered();
        }
        sender.sendMessage(ColorUtil.parse(
                " <gray>Machines Built (online):</gray> <white>" + totalMachinesBuilt + "</white>"));
        sender.sendMessage(ColorUtil.parse(
                " <gray>Fragments Processed (online):</gray> <white>" + totalFragProcessed + "</white>"));
        sender.sendMessage(ColorUtil.parse(
                " <gray>Ingots Produced (online):</gray> <aqua>" + totalIngots + "</aqua>"));
        sender.sendMessage(ColorUtil.parse(
                " <gray>Overheats Triggered (online):</gray> <red>" + totalOverheats + "</red>"));

        // Live machine states summary
        int offline = 0, heating = 0, active = 0, cooling = 0, overheated = 0;
        for (SmelterData machine : nuclearSmelterManager.getAllMachines()) {
            switch (machine.getState()) {
                case OFFLINE    -> offline++;
                case HEATING    -> heating++;
                case ACTIVE     -> active++;
                case COOLING    -> cooling++;
                case OVERHEATED -> overheated++;
                default -> {}
            }
        }
        sender.sendMessage(ColorUtil.parse(
                " <gray>Machine States —</gray>"
                + " <green>Active: " + active + "</green>"
                + " <gold>Heating: " + heating + "</gold>"
                + " <aqua>Cooling: " + cooling + "</aqua>"
                + " <gray>Offline: " + offline + "</gray>"
                + " <red>Overheat: " + overheated + "</red>"));
    }

    private void handleSmelterDebug(CommandSender sender) {
        sender.sendMessage(ColorUtil.parse(
                "<dark_gray>── <gradient:#39ff14:#00bfff>Smelter Debug — " 
                + nuclearSmelterManager.getMachineCount() + " machine(s)</gradient> ──</dark_gray>"));
        if (nuclearSmelterManager.getMachineCount() == 0) {
            sender.sendMessage(ColorUtil.parse(" <gray>No active machines.</gray>"));
            return;
        }
        for (SmelterData machine : nuclearSmelterManager.getAllMachines()) {
            sender.sendMessage(ColorUtil.parse(
                    " <dark_gray>[</dark_gray><white>" + machine.getLocationKey() + "</white><dark_gray>]</dark_gray>"
                    + " <" + machine.getState().getColor() + ">" + machine.getState().getDisplayName() + "</>"
                    + " <gray>T:" + String.format("%.0f", machine.getTemperature()) + "°C"
                    + " F:" + machine.getFuelRemaining()
                    + " P:" + String.format("%.0f%%", machine.getProgressFraction() * 100) + "</gray>"));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Tab completion
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return TOP_SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if ("radiation".equalsIgnoreCase(args[0])) {
                return filter(RADIATION_SUBCOMMANDS, args[1]);
            }
            if ("zombie".equalsIgnoreCase(args[0]) && sender.hasPermission("nuclearcraft.admin.zombies")) {
                return filter(ZOMBIE_SUBCOMMANDS, args[1]);
            }
            if ("ore".equalsIgnoreCase(args[0]) && sender.hasPermission("nuclearcraft.admin.ore")) {
                return filter(ORE_SUBCOMMANDS, args[1]);
            }
            if ("smelter".equalsIgnoreCase(args[0]) && sender.hasPermission("nuclearcraft.admin.smelter")) {
                return filter(SMELTER_SUBCOMMANDS, args[1]);
            }
        }

        if (args.length == 3) {
            if ("zombie".equalsIgnoreCase(args[0])) {
                if ("spawn".equalsIgnoreCase(args[1])) return filter(ZOMBIE_SPAWN_TYPES, args[2]);
                if ("surge".equalsIgnoreCase(args[1])) return filter(ZOMBIE_SURGE_ACTIONS, args[2]);
            }
            if ("ore".equalsIgnoreCase(args[0])) {
                if ("spawn".equalsIgnoreCase(args[1])) return filter(ORE_SPAWN_TYPES, args[2]);
                if ("give".equalsIgnoreCase(args[1]))  return filter(ORE_GIVE_TYPES, args[2]);
            }
            if ("give".equalsIgnoreCase(args[0]) && sender.hasPermission("nuclearcraft.give")) {
                return plugin.getServer().getOnlinePlayers().stream()
                        .map(Player::getName).filter(n -> n.startsWith(args[2]))
                        .collect(Collectors.toList());
            }
            if ("smelter".equalsIgnoreCase(args[0]) && "give".equalsIgnoreCase(args[1])
                    && sender.hasPermission("nuclearcraft.admin.smelter")) {
                return plugin.getServer().getOnlinePlayers().stream()
                        .map(Player::getName).filter(n -> n.startsWith(args[2]))
                        .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Utility
    // ──────────────────────────────────────────────────────────────────────────

    private Player resolvePlayer(CommandSender sender, String name) {
        Player target = plugin.getServer().getPlayer(name);
        if (target == null) {
            sender.sendMessage(ColorUtil.parse(
                    configManager.getMessage("general.player-not-found").replace("{player}", name)));
        }
        return target;
    }

    private int parseAmount(CommandSender sender, String input) {
        try { return Integer.parseInt(input); }
        catch (NumberFormatException e) {
            sender.sendMessage(ColorUtil.parse(
                    configManager.getMessage("general.invalid-number").replace("{input}", input)));
            return -1;
        }
    }

    private List<String> filter(List<String> options, String prefix) {
        return options.stream()
                .filter(s -> s.startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
}
