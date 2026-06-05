package com.nuclearcraft.commands;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.utils.ColorUtil;
import com.nuclearcraft.utils.NCLogger;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles the /nuclearcraft root command and all subcommands.
 * All permission checks are done at the method level for clarity.
 */
public class NuclearCraftCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("help", "reload", "info", "debug", "give", "version");
    private static final List<String> ADMIN_SUBCOMMANDS = List.of("reload", "debug", "give");

    private final NuclearCraftPlugin plugin;
    private final ConfigManager configManager;
    private final PlayerDataManager playerDataManager;
    private final ItemManager itemManager;

    public NuclearCraftCommand(NuclearCraftPlugin plugin, ConfigManager configManager,
                                PlayerDataManager playerDataManager, ItemManager itemManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.playerDataManager = playerDataManager;
        this.itemManager = itemManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "help" -> { sendHelp(sender); yield true; }
            case "version" -> { sendVersion(sender); yield true; }
            case "info" -> { sendInfo(sender); yield true; }
            case "reload" -> { handleReload(sender); yield true; }
            case "debug" -> { handleDebug(sender); yield true; }
            case "give" -> { handleGive(sender, args); yield true; }
            default -> {
                sender.sendMessage(ColorUtil.parse(configManager.getMessage("commands.unknown-command")));
                yield true;
            }
        };
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ColorUtil.parse(configManager.getRawMessage("commands.help-header")));
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("nuclearcraft help", "Show this help message");
        entries.put("nuclearcraft version", "Display plugin version");
        entries.put("nuclearcraft info", "Show plugin runtime info");
        if (sender.hasPermission("nuclearcraft.reload")) {
            entries.put("nuclearcraft reload", "Reload all configuration files");
        }
        if (sender.hasPermission("nuclearcraft.debug")) {
            entries.put("nuclearcraft debug", "Toggle debug mode");
        }
        if (sender.hasPermission("nuclearcraft.give")) {
            entries.put("nuclearcraft give <player> <item> [amount]", "Give a custom item to a player");
        }
        entries.forEach((cmd, desc) -> {
            String entry = configManager.getRawMessage("commands.help-entry")
                    .replace("{command}", cmd)
                    .replace("{description}", desc);
            sender.sendMessage(ColorUtil.parse(entry));
        });
    }

    private void sendVersion(CommandSender sender) {
        String msg = configManager.getRawMessage("commands.version")
                .replace("{version}", plugin.getDescription().getVersion())
                .replace("{server}", plugin.getServer().getName() + " " + plugin.getServer().getVersion());
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
        String msgKey = newState ? "commands.debug-enabled" : "commands.debug-disabled";
        sender.sendMessage(ColorUtil.parse(configManager.getMessage(msgKey)));
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

        String playerName = args[1];
        String itemId = args[2];
        int amount = 1;

        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
                amount = Math.max(1, Math.min(64, amount));
            } catch (NumberFormatException e) {
                String msg = configManager.getMessage("general.invalid-number")
                        .replace("{input}", args[3]);
                sender.sendMessage(ColorUtil.parse(msg));
                return;
            }
        }

        var target = plugin.getServer().getPlayer(playerName);
        if (target == null) {
            String msg = configManager.getMessage("general.player-not-found")
                    .replace("{player}", playerName);
            sender.sendMessage(ColorUtil.parse(msg));
            return;
        }

        var optItem = itemManager.getItem(itemId);
        if (optItem.isEmpty()) {
            String msg = configManager.getMessage("commands.give-invalid-item")
                    .replace("{item}", itemId);
            sender.sendMessage(ColorUtil.parse(msg));
            return;
        }

        ItemStack stack = optItem.get().build(amount);
        target.getInventory().addItem(stack);

        String msg = configManager.getMessage("commands.give-success")
                .replace("{amount}", String.valueOf(amount))
                .replace("{item}", itemId)
                .replace("{player}", target.getName());
        sender.sendMessage(ColorUtil.parse(msg));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && "give".equalsIgnoreCase(args[0]) && sender.hasPermission("nuclearcraft.give")) {
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && "give".equalsIgnoreCase(args[0]) && sender.hasPermission("nuclearcraft.give")) {
            return itemManager.getRegistry().getIds().stream()
                    .filter(id -> id.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
