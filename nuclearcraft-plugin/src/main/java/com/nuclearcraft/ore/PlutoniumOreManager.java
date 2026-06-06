package com.nuclearcraft.ore;

import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.data.PlayerData;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks every known Plutonium Ore block location on the server.
 *
 * Storage model:
 *   - In-memory: {@code Set<String>} keyed by {@code "worldName,x,y,z"} for O(1) lookup.
 *   - Persistent: {@code plugins/NuclearCraft/ore_data.yml} — loaded on enable,
 *     flushed on disable and after each batch of changes.
 *
 * Handles:
 *   - Ore registration on chunk generation (via OreGenerationManager).
 *   - Ore removal on successful mine or block destruction.
 *   - First-discovery notification per player (title + sound + stat).
 */
public class PlutoniumOreManager {

    private static final String DATA_FILE = "ore_data.yml";

    private final NuclearCraftPlugin plugin;
    private final PlayerDataManager playerDataManager;

    /** Live set of all tracked ore locations. */
    private final Set<String> knownOreLocations = ConcurrentHashMap.newKeySet();

    /** Players who have already discovered ore this session (avoids DB round-trip). */
    private final Set<UUID> discoveredThisSession = ConcurrentHashMap.newKeySet();

    private File dataFile;
    private boolean dirty = false;

    public PlutoniumOreManager(NuclearCraftPlugin plugin, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
    }

    public void initialize() {
        dataFile = new File(plugin.getDataFolder(), DATA_FILE);
        load();
        NCLogger.info("PlutoniumOreManager initialized — " + knownOreLocations.size() + " ore locations loaded.");
    }

    public void shutdown() {
        if (dirty) save();
        knownOreLocations.clear();
        NCLogger.info("PlutoniumOreManager shut down.");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Ore tracking
    // ──────────────────────────────────────────────────────────────────────────

    /** Registers a newly placed/generated ore block. */
    public void registerOre(Location location) {
        knownOreLocations.add(key(location));
        dirty = true;
    }

    /** Removes a mined or destroyed ore block from tracking. */
    public void removeOre(Location location) {
        knownOreLocations.remove(key(location));
        dirty = true;
    }

    /** Returns true if the given location is a tracked Plutonium Ore block. */
    public boolean isOre(Location location) {
        return knownOreLocations.contains(key(location));
    }

    public int getTrackedCount() { return knownOreLocations.size(); }

    // ──────────────────────────────────────────────────────────────────────────
    // Discovery detection
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Checks if this is the player's first-ever encounter with Plutonium Ore.
     * Returns true and marks discovery if so.
     */
    public boolean checkFirstDiscovery(Player player) {
        UUID uid = player.getUniqueId();
        if (discoveredThisSession.contains(uid)) return false;

        PlayerData data = playerDataManager.get(uid).orElse(null);
        if (data == null) return false;

        if (data.getPlutoniumOreFound() > 0) {
            discoveredThisSession.add(uid); // already found before, skip future checks
            return false;
        }

        // First ever discovery
        discoveredThisSession.add(uid);
        data.incrementPlutoniumOreFound();
        return true;
    }

    /** Increments the ore-found counter without the first-discovery check. */
    public void recordOreFound(Player player) {
        PlayerData data = playerDataManager.get(player.getUniqueId()).orElse(null);
        if (data != null) data.incrementPlutoniumOreFound();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Persistence
    // ──────────────────────────────────────────────────────────────────────────

    private void load() {
        if (!dataFile.exists()) {
            NCLogger.debug("No ore_data.yml found — starting fresh.");
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
        List<String> keys = yaml.getStringList("ore-locations");
        knownOreLocations.addAll(keys);
        NCLogger.debug("Loaded %d ore locations from disk.", keys.size());
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("ore-locations", new ArrayList<>(knownOreLocations));
        try {
            yaml.save(dataFile);
            dirty = false;
            NCLogger.debug("Saved %d ore locations to disk.", knownOreLocations.size());
        } catch (IOException e) {
            NCLogger.severe("Failed to save ore_data.yml", e);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────

    private static String key(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }
}
