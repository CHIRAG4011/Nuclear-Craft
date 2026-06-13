package com.nuclearcraft.resourcepack;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.utils.NCLogger;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
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
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Manages server-side resource pack delivery for NuclearCraft.
 *
 * Reads pack URL, SHA-1 hash, required flag and prompt message from
 * resourcepack.yml and applies the pack to every player on join.
 * Uses a stable UUID derived from the pack hash so clients do not
 * re-download the pack on every join.
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
        applyPack(event.getPlayer());
    }

    @EventHandler
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        if (!logStatus) return;
        Player player = event.getPlayer();
        switch (event.getStatus()) {
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
     * Uses a UUID derived from the hash so the Minecraft client recognises
     * the same pack across sessions and does not re-download it.
     */
    public void applyPack(Player player) {
        if (!enabled || packUrl == null || packUrl.isBlank()) return;
        try {
            UUID stableId = UUID.nameUUIDFromBytes(
                    (packUrl + ":" + packHash).getBytes(StandardCharsets.UTF_8));

            ResourcePackInfo info = ResourcePackInfo.resourcePackInfo()
                    .id(stableId)
                    .uri(URI.create(packUrl))
                    .hash(packHash)
                    .build();

            Component prompt = Component.text(promptMessage, NamedTextColor.GREEN);

            ResourcePackRequest request = ResourcePackRequest.resourcePackRequest()
                    .packs(info)
                    .prompt(prompt)
                    .required(required)
                    .build();

            player.sendResourcePacks(request);
        } catch (Exception e) {
            NCLogger.warn("[ResourcePackManager] Failed to send resource pack to "
                    + player.getName() + ": " + e.getMessage());
        }
    }

    public boolean isEnabled() { return enabled; }
    public String getPackUrl()  { return packUrl; }
    public String getPackHash() { return packHash; }
    public boolean isRequired() { return required; }
}
