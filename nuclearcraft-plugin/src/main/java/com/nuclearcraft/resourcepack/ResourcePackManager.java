package com.nuclearcraft.resourcepack;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.utils.NCLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

import java.net.URI;

/**
 * Manages server-side resource pack delivery for NuclearCraft.
 *
 * Reads pack URL, SHA-1 hash, required flag and prompt message from
 * resourcepack.yml and applies the pack to every player on join.
 * Optionally logs acceptance/rejection status.
 *
 * Phase 12 addition.
 */
public class ResourcePackManager implements Listener {

    private final NuclearCraftPlugin plugin;
    private final ConfigManager configManager;

    private boolean enabled;
    private String packUrl;
    private String packHash;
    private boolean required;
    private String promptMessage;
    private boolean logStatus;

    public ResourcePackManager(NuclearCraftPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void initialize() {
        loadConfig();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        if (enabled) {
            NCLogger.info("[ResourcePackManager] Resource pack delivery enabled.");
            NCLogger.info("[ResourcePackManager] URL: " + packUrl);
        } else {
            NCLogger.info("[ResourcePackManager] Resource pack delivery disabled (resourcepack.yml: enabled: false).");
        }
    }

    public void reload() {
        loadConfig();
    }

    public void shutdown() {
        PlayerJoinEvent.getHandlerList().unregister(this);
        PlayerResourcePackStatusEvent.getHandlerList().unregister(this);
    }

    private void loadConfig() {
        FileConfiguration cfg = configManager.getConfig(ConfigManager.ConfigFile.RESOURCEPACK);
        enabled       = cfg.getBoolean("resource-pack.enabled", false);
        packUrl       = cfg.getString("resource-pack.url", "");
        packHash      = cfg.getString("resource-pack.hash", "");
        required      = cfg.getBoolean("resource-pack.required", false);
        promptMessage = cfg.getString("resource-pack.prompt", "NuclearCraft requires a resource pack for custom models and sounds.");
        logStatus     = cfg.getBoolean("resource-pack.log-status", true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!enabled || packUrl == null || packUrl.isBlank()) return;
        Player player = event.getPlayer();
        applyPack(player);
    }

    @EventHandler
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        if (!logStatus) return;
        Player player = event.getPlayer();
        PlayerResourcePackStatusEvent.Status status = event.getStatus();
        switch (status) {
            case SUCCESSFULLY_LOADED ->
                NCLogger.debug("[ResourcePackManager] " + player.getName() + " accepted resource pack.");
            case DECLINED ->
                NCLogger.info("[ResourcePackManager] " + player.getName() + " declined resource pack.");
            case FAILED_DOWNLOAD, FAILED_RELOAD ->
                NCLogger.warn("[ResourcePackManager] " + player.getName() + " failed to download resource pack.");
            default -> {}
        }
    }

    /**
     * Sends the configured resource pack to a specific player.
     */
    public void applyPack(Player player) {
        if (!enabled || packUrl == null || packUrl.isBlank()) return;
        try {
            byte[] hashBytes = parseHex(packHash);
            Component prompt = Component.text(promptMessage, NamedTextColor.GREEN);
            player.setResourcePack(packUrl, hashBytes, prompt, required);
        } catch (Exception e) {
            NCLogger.warn("[ResourcePackManager] Failed to send resource pack to "
                    + player.getName() + ": " + e.getMessage());
        }
    }

    /** Converts a 40-char hex SHA-1 string to byte[20]. Returns empty array on blank/null. */
    private static byte[] parseHex(String hex) {
        if (hex == null || hex.isBlank()) return new byte[0];
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    public boolean isEnabled() { return enabled; }
    public String getPackUrl()  { return packUrl; }
    public String getPackHash() { return packHash; }
    public boolean isRequired() { return required; }
}
