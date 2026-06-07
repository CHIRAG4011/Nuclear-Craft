package com.nuclearcraft.balance;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Economy protection for NuclearCraft's multiplayer environment.
 *
 * Enforces:
 *   1. Boss respawn cooldown per-player (prevent Titan farming).
 *   2. AFK farming detection (player must interact to keep farms active).
 *   3. Machine placement rate limiting (anti-spam).
 *   4. Max machines per player (prevent TPS abuse via machine floods).
 *
 * Phase 13 addition.
 */
public class EconomyBalanceManager implements Listener {

    private final NuclearCraftPlugin plugin;
    private final ConfigManager configManager;

    private FileConfiguration cfg;

    // ── In-memory state ───────────────────────────────────────────────────────

    /** UUID → timestamp (ms) of last Titan kill. */
    private final Map<UUID, Long> lastBossKillTime = new HashMap<>();

    /** UUID → timestamp (ms) of last farming interaction. */
    private final Map<UUID, Long> lastFarmInteraction = new HashMap<>();

    /** UUID → timestamp (ms) of last machine placement. */
    private final Map<UUID, Long> lastMachinePlacement = new HashMap<>();

    /** UUID → count of machines placed this session. */
    private final Map<UUID, Integer> machineCount = new HashMap<>();

    public EconomyBalanceManager(NuclearCraftPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void initialize() {
        cfg = configManager.getConfig(ConfigManager.ConfigFile.BALANCE);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        NCLogger.info("[EconomyBalanceManager] Economy protections active.");
    }

    public void reload() {
        cfg = configManager.getConfig(ConfigManager.ConfigFile.BALANCE);
    }

    public void shutdown() {
        lastBossKillTime.clear();
        lastFarmInteraction.clear();
        lastMachinePlacement.clear();
        machineCount.clear();
    }

    // ── Boss Cooldown ─────────────────────────────────────────────────────────

    /**
     * Returns true if this player must wait before participating in another Titan fight.
     */
    public boolean isBossOnCooldown(UUID playerId) {
        Long last = lastBossKillTime.get(playerId);
        if (last == null) return false;
        long cooldownMs = getBossRespawnCooldownMinutes() * 60_000L;
        return (System.currentTimeMillis() - last) < cooldownMs;
    }

    /**
     * Returns remaining cooldown in seconds, or 0 if not on cooldown.
     */
    public long getBossCooldownRemainingSeconds(UUID playerId) {
        Long last = lastBossKillTime.get(playerId);
        if (last == null) return 0;
        long cooldownMs = getBossRespawnCooldownMinutes() * 60_000L;
        long remainingMs = cooldownMs - (System.currentTimeMillis() - last);
        return Math.max(0, remainingMs / 1000);
    }

    /**
     * Records a Titan kill for a player. Call on Titan death for each participant.
     */
    public void recordBossKill(UUID playerId) {
        lastBossKillTime.put(playerId, System.currentTimeMillis());
        NCLogger.debug("[EconomyBalanceManager] Boss kill recorded for %s", playerId);
    }

    // ── AFK Farming ───────────────────────────────────────────────────────────

    /**
     * Returns true if the player has been inactive for longer than the AFK timeout.
     * Systems should stop awarding loot to AFK farmers.
     */
    public boolean isAfkFarming(UUID playerId) {
        Long last = lastFarmInteraction.get(playerId);
        if (last == null) return true; // never interacted → treat as AFK
        long timeoutMs = getAfkTimeoutSeconds() * 1000L;
        return (System.currentTimeMillis() - last) > timeoutMs;
    }

    /**
     * Records a farm-relevant interaction. Call when a player actively farms crops.
     */
    public void recordFarmInteraction(UUID playerId) {
        lastFarmInteraction.put(playerId, System.currentTimeMillis());
    }

    // ── Machine Placement ─────────────────────────────────────────────────────

    /**
     * Returns true if the player is allowed to place another machine.
     * Checks both the rate limit (placement cooldown) and the per-player machine cap.
     */
    public boolean canPlaceMachine(Player player) {
        UUID id = player.getUniqueId();

        // Rate limiting
        Long last = lastMachinePlacement.get(id);
        if (last != null) {
            long cooldownMs = getMachinePlacementCooldownSeconds() * 1000L;
            if ((System.currentTimeMillis() - last) < cooldownMs) return false;
        }

        // Machine cap
        int count = machineCount.getOrDefault(id, 0);
        return count < getMaxMachinesPerPlayer();
    }

    /**
     * Records a machine placement for a player.
     */
    public void recordMachinePlacement(UUID playerId) {
        lastMachinePlacement.put(playerId, System.currentTimeMillis());
        machineCount.merge(playerId, 1, Integer::sum);
    }

    /**
     * Records machine removal (player broke their machine).
     */
    public void recordMachineRemoval(UUID playerId) {
        machineCount.compute(playerId, (k, v) -> (v == null || v <= 1) ? 0 : v - 1);
    }

    /**
     * Returns how many machines this player currently has placed.
     */
    public int getMachineCountForPlayer(UUID playerId) {
        return machineCount.getOrDefault(playerId, 0);
    }

    // ── Config getters ────────────────────────────────────────────────────────

    public int getBossRespawnCooldownMinutes() {
        return cfg.getInt("economy.boss-respawn-cooldown-minutes", 60);
    }

    public int getAfkTimeoutSeconds() {
        return cfg.getInt("economy.afk-farming-timeout-seconds", 300);
    }

    public int getMaxMachinesPerPlayer() {
        return cfg.getInt("economy.max-machines-per-player", 5);
    }

    public int getMachinePlacementCooldownSeconds() {
        return cfg.getInt("economy.machine-placement-cooldown-seconds", 5);
    }

    public int getMaxCoresPerPlayer() {
        return cfg.getInt("economy.max-cores-per-player", 64);
    }

    public boolean isDuplicateDetectionEnabled() {
        return cfg.getBoolean("economy.duplicate-detection", true);
    }

    public int getMaxTitanLootsPerKill() {
        return cfg.getInt("economy.max-titan-loots-per-kill", 1);
    }

    // ── Listeners ─────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK
                || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            recordFarmInteraction(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        lastFarmInteraction.remove(id);
        lastMachinePlacement.remove(id);
    }
}
