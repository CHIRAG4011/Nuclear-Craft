package com.nuclearcraft.combat;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the radiation combo system for Phase 9 PvP.
 *
 * Each time attacker A hits victim V within {@code reset-ms} of the previous hit,
 * the combo counter increments. Each additional stack adds {@code radiation-per-stack}
 * extra radiation (capped at {@code max-bonus}).
 *
 * <p>Combo state is stored in a nested map:
 * {@code Map<attackerUUID, Map<victimUUID, ComboData>>}
 *
 * A lightweight cleanup task runs every 10 seconds to expire stale entries.
 */
public class CombatInfectionManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;

    private boolean enabled;
    private int maxHits;
    private long resetMs;
    private int radiationPerStack;
    private int maxBonus;

    private final Map<UUID, Map<UUID, ComboData>> combos = new ConcurrentHashMap<>();
    private BukkitTask cleanupTask;

    public CombatInfectionManager(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin          = plugin;
        this.configManager   = configManager;
    }

    public void initialize() {
        loadConfig();
        scheduleCleanup();
        NCLogger.info("CombatInfectionManager initialized (combo enabled=" + enabled + ").");
    }

    public void shutdown() {
        if (cleanupTask != null && !cleanupTask.isCancelled()) cleanupTask.cancel();
        combos.clear();
    }

    private void loadConfig() {
        var cfg = configManager.getCombat();
        enabled           = cfg.getBoolean("combo.enabled", true);
        maxHits           = cfg.getInt("combo.max-hits", 8);
        resetMs           = cfg.getLong("combo.reset-ms", 6000L);
        radiationPerStack = cfg.getInt("combo.radiation-per-stack", 5);
        maxBonus          = cfg.getInt("combo.max-bonus", 35);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Core API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Called when {@code attacker} hits {@code victim} with a plutonium weapon.
     * Increments the combo counter and returns the BONUS radiation to apply
     * (on top of the base weapon radiation).
     *
     * @return bonus radiation points from this combo hit (0 on the first hit)
     */
    public int onHit(Player attacker, Player victim) {
        if (!enabled) return 0;

        UUID aId = attacker.getUniqueId();
        UUID vId = victim.getUniqueId();

        combos.computeIfAbsent(aId, k -> new ConcurrentHashMap<>());
        Map<UUID, ComboData> victimMap = combos.get(aId);

        ComboData data = victimMap.get(vId);
        long now = System.currentTimeMillis();

        if (data == null || (now - data.lastHitMs) > resetMs) {
            data = new ComboData();
        }

        data.hitCount    = Math.min(data.hitCount + 1, maxHits);
        data.lastHitMs   = now;
        victimMap.put(vId, data);

        if (data.hitCount <= 1) return 0; // first hit has no bonus
        int bonus = Math.min((data.hitCount - 1) * radiationPerStack, maxBonus);
        NCLogger.debug("Combo hit %d on %s by %s: +%d radiation bonus",
                data.hitCount, victim.getName(), attacker.getName(), bonus);
        return bonus;
    }

    /** Returns the current combo count (0 if no active combo). */
    public int getComboCount(Player attacker, Player victim) {
        Map<UUID, ComboData> victimMap = combos.get(attacker.getUniqueId());
        if (victimMap == null) return 0;
        ComboData data = victimMap.get(victim.getUniqueId());
        if (data == null) return 0;
        if (System.currentTimeMillis() - data.lastHitMs > resetMs) return 0;
        return data.hitCount;
    }

    /** Resets the combo between an attacker and victim. */
    public void resetCombo(Player attacker, Player victim) {
        Map<UUID, ComboData> victimMap = combos.get(attacker.getUniqueId());
        if (victimMap != null) victimMap.remove(victim.getUniqueId());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Cleanup
    // ──────────────────────────────────────────────────────────────────────────

    private void scheduleCleanup() {
        cleanupTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long now = System.currentTimeMillis();
            combos.forEach((aId, victimMap) ->
                    victimMap.entrySet().removeIf(e -> (now - e.getValue().lastHitMs) > resetMs * 2));
            combos.entrySet().removeIf(e -> e.getValue().isEmpty());
        }, 200L, 200L); // every 10 seconds
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Data holder
    // ──────────────────────────────────────────────────────────────────────────

    private static final class ComboData {
        int  hitCount  = 0;
        long lastHitMs = 0L;
    }
}
