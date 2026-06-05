package com.nuclearcraft.commands;

import com.nuclearcraft.advancements.AdvancementManager;
import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.data.PlayerData;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.radiation.RadiationManager;
import com.nuclearcraft.radiation.RadiationSource;
import com.nuclearcraft.utils.ColorUtil;
import com.nuclearcraft.utils.MathHelper;
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
 */
public class NuclearCraftCommand implements CommandExecutor, TabCompleter {

    private static final List<String> TOP_SUBCOMMANDS =
            List.of("help", "reload", "info", "debug", "give", "version", "radiation", "zombie");
    private static final List<String> RADIATION_SUBCOMMANDS =
            List.of("check", "add", "remove", "set", "clear");
    private static final List<String> ZOMBIE_SUBCOMMANDS =
            List.of("spawn", "stats", "surge");
    private static final List<String> ZOMBIE_SPAWN_TYPES =
            List.of("irradiated", "alpha");
    private static final List<String> ZOMBIE_SURGE_ACTIONS =
            List.of("start", "stop");

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

    public NuclearCraftCommand(NuclearCraftPlugin plugin, ConfigManager configManager,
                                PlayerDataManager playerDataManager, ItemManager itemManager,
                                RadiationManager radiationManager,
                                IrradiatedZombieManager irradiatedZombieManager,
                                ZombieSpawnManager zombieSpawnManager,
                                RadiationCloudManager radiationCloudManager,
                                RadiationNightManager radiationNightManager,
                                AdvancementManager advancementManager) {
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
        String stageName  = stageNames[Math.min(stage, 4)];
        boolean contagious = radiationManager.isContagious(target);
        long immunityLeft = Math.max(0, data.getImmunityTimerEndMs() - System.currentTimeMillis()) / 1000;

        sender.sendMessage(ColorUtil.parse(
                "<dark_gray>── <gradient:#39ff14:#00ff88>Radiation Status: " + target.getName() + "</gradient> ──</dark_gray>"));
        sender.sendMessage(ColorUtil.parse(
                " <gray>Radiation:</gray> <white>" + (int) data.getRadiationLevel() + " / 1000</white>"));
        sender.sendMessage(ColorUtil.parse(
                " <gray>Stage:</gray> " + stageColor + stage + " — " + stageName + "</" + stageColor.substring(1)));
        sender.sendMessage(ColorUtil.parse(
                " <gray>Contagious:</gray> " + (contagious ? "<red>YES</red>" : "<green>NO</green>")));
        sender.sendMessage(ColorUtil.parse(
                " <gray>Immunity:</gray> " + (immunityLeft > 0 ? "<aqua>" + immunityLeft + "s remaining</aqua>" : "<gray>None</gray>")));
        sender.sendMessage(ColorUtil.parse(
                " <gray>Last Source:</gray> <white>" + data.getLastRadiationSource() + "</white>"));
        sender.sendMessage(ColorUtil.parse(
                " <gray>Zombies Killed:</gray> <white>" + data.getIrradiatedZombiesKilled()
                + " (" + data.getAlphaZombiesKilled() + " Alpha)</white>"));
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
        String name = iz.getZombieLevel().getDisplayName();
        sender.sendMessage(ColorUtil.parse("<green>Spawned <yellow>" + name + " [L" + level.getLevel() + "]</yellow> near you.</green>"));
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
        }

        if (args.length == 3) {
            if ("zombie".equalsIgnoreCase(args[0])) {
                if ("spawn".equalsIgnoreCase(args[1])) return filter(ZOMBIE_SPAWN_TYPES, args[2]);
                if ("surge".equalsIgnoreCase(args[1])) return filter(ZOMBIE_SURGE_ACTIONS, args[2]);
            }
            if ("radiation".equalsIgnoreCase(args[0]) && !args[1].equalsIgnoreCase("check")) {
                return onlinePlayers(args[2]);
            }
        }

        if ("give".equalsIgnoreCase(args[0])) {
            if (args.length == 2) return onlinePlayers(args[1]);
            if (args.length == 3) return itemManager.getRegistry().getIds().stream()
                    .filter(id -> id.startsWith(args[2].toLowerCase())).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private List<String> filter(List<String> list, String partial) {
        return list.stream()
                .filter(s -> s.startsWith(partial.toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> onlinePlayers(String partial) {
        return plugin.getServer().getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(partial.toLowerCase()))
                .collect(Collectors.toList());
    }

    private Player resolvePlayer(CommandSender sender, String name) {
        Player p = plugin.getServer().getPlayer(name);
        if (p == null) {
            sender.sendMessage(ColorUtil.parse(
                    configManager.getMessage("general.player-not-found").replace("{player}", name)));
        }
        return p;
    }

    private int parseAmount(CommandSender sender, String input) {
        try {
            int v = Integer.parseInt(input);
            if (v < 0) {
                sender.sendMessage(ColorUtil.parse("<red>Amount must be a positive integer.</red>"));
                return -1;
            }
            return (int) MathHelper.clamp(v, 0, RadiationManager.MAX_RADIATION);
        } catch (NumberFormatException e) {
            sender.sendMessage(ColorUtil.parse(
                    configManager.getMessage("general.invalid-number").replace("{input}", input)));
            return -1;
        }
    }
}
