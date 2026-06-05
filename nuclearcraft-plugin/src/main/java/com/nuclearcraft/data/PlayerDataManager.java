package com.nuclearcraft.data;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cache and lifecycle manager for PlayerData.
 * Handles async load on join and periodic dirty-write flushing.
 */
public class PlayerDataManager {

    private final NuclearCraftPlugin plugin;
    private final DatabaseManager databaseManager;
    private final ConfigManager configManager;

    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public PlayerDataManager(NuclearCraftPlugin plugin, DatabaseManager databaseManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.configManager = configManager;
    }

    public void initialize() {
        NCLogger.info("PlayerDataManager initialized.");
    }

    public void shutdown() {
        NCLogger.info("Flushing all player data before shutdown...");
        for (PlayerData data : cache.values()) {
            if (data.isDirty()) {
                databaseManager.savePlayerData(data);
            }
        }
        cache.clear();
        NCLogger.info("PlayerDataManager shut down.");
    }

    /**
     * Loads a player's data asynchronously on join.
     */
    public CompletableFuture<PlayerData> loadAsync(Player player) {
        UUID uuid = player.getUniqueId();
        if (cache.containsKey(uuid)) {
            return CompletableFuture.completedFuture(cache.get(uuid));
        }

        return CompletableFuture.supplyAsync(() -> {
            Optional<PlayerData> loaded = databaseManager.loadPlayerData(uuid);
            PlayerData data = loaded.orElseGet(() -> {
                NCLogger.debug("Creating new player data for %s", player.getName());
                return new PlayerData(uuid);
            });
            cache.put(uuid, data);
            return data;
        }).exceptionally(ex -> {
            NCLogger.severe("Failed to load data for player " + player.getName(), ex);
            PlayerData fallback = new PlayerData(uuid);
            cache.put(uuid, fallback);
            return fallback;
        });
    }

    /**
     * Unloads and saves a player's data on quit.
     */
    public void unloadAndSave(UUID uuid) {
        PlayerData data = cache.remove(uuid);
        if (data != null && data.isDirty()) {
            if (configManager.isAsyncDataOperations()) {
                CompletableFuture.runAsync(() -> databaseManager.savePlayerData(data))
                        .exceptionally(ex -> {
                            NCLogger.severe("Async save failed for " + uuid, ex);
                            return null;
                        });
            } else {
                databaseManager.savePlayerData(data);
            }
        }
    }

    /**
     * Flushes all dirty data. Called by TaskManager on a schedule.
     */
    public void flushDirty() {
        for (PlayerData data : cache.values()) {
            if (data.isDirty()) {
                if (configManager.isAsyncDataOperations()) {
                    final PlayerData snapshot = data;
                    CompletableFuture.runAsync(() -> databaseManager.savePlayerData(snapshot))
                            .exceptionally(ex -> {
                                NCLogger.severe("Async flush failed for " + snapshot.getUuid(), ex);
                                return null;
                            });
                } else {
                    databaseManager.savePlayerData(data);
                }
            }
        }
        NCLogger.debug("Flushed dirty player data entries.");
    }

    public Optional<PlayerData> get(UUID uuid) {
        return Optional.ofNullable(cache.get(uuid));
    }

    public Optional<PlayerData> get(Player player) {
        return get(player.getUniqueId());
    }

    public PlayerData getOrDefault(UUID uuid) {
        return cache.getOrDefault(uuid, new PlayerData(uuid));
    }

    public boolean isLoaded(UUID uuid) {
        return cache.containsKey(uuid);
    }

    public Collection<PlayerData> getAllLoaded() {
        return cache.values();
    }

    public int getCacheSize() {
        return cache.size();
    }
}
