package com.nuclearcraft.boss;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.data.PlayerData;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.Location;
import org.bukkit.entity.Giant;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Advanced AI for the Plutonium Titan.
 *
 * Targeting priority (in order):
 *  1. Player with highest cumulative damage dealt to the Titan
 *  2. Most irradiated player (within range)
 *  3. Nearest player (fallback)
 *
 * Movement is handled via velocity-based steering since Giants
 * do not have vanilla AI pathfinding enabled.
 */
public class TitanAIManager {

    private final JavaPlugin         plugin;
    private final ConfigManager      configManager;
    private final PlayerDataManager  playerDataManager;
    private final TitanDamageTracker damageTracker;

    private Giant      titan;
    private Player     currentTarget;
    private BukkitTask aiTask;
    private BukkitTask stuckTask;

    private Location   lastPosition;
    private int        stuckTicks    = 0;
    private long       targetSwitchCooldown = 0;

    private static final long   AI_INTERVAL_TICKS   = 4L;
    private static final long   STUCK_CHECK_TICKS   = 60L;
    private static final double STUCK_THRESHOLD      = 0.5;
    private static final long   TARGET_SWITCH_MS     = 5000L;
    private static final double MELEE_RANGE          = 3.5;
    private static final double DETECTION_RANGE      = 80.0;

    public TitanAIManager(JavaPlugin plugin, ConfigManager configManager,
                           PlayerDataManager playerDataManager, TitanDamageTracker damageTracker) {
        this.plugin           = plugin;
        this.configManager    = configManager;
        this.playerDataManager = playerDataManager;
        this.damageTracker    = damageTracker;
    }

    public void initialize() {
        NCLogger.debug("TitanAIManager initialized.");
    }

    public void startAI(Giant titan) {
        this.titan = titan;
        stopAI();
        aiTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickAI,
                10L, AI_INTERVAL_TICKS);
        stuckTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::checkStuck,
                60L, STUCK_CHECK_TICKS);
        NCLogger.debug("Titan AI started.");
    }

    public void stopAI() {
        if (aiTask   != null) { aiTask.cancel();   aiTask   = null; }
        if (stuckTask != null) { stuckTask.cancel(); stuckTask = null; }
        currentTarget = null;
        NCLogger.debug("Titan AI stopped.");
    }

    public void shutdown() {
        stopAI();
    }

    // ── AI tick ───────────────────────────────────────────────────────────────

    private void tickAI() {
        if (titan == null || titan.isDead() || !titan.isValid()) return;

        selectTarget();
        if (currentTarget == null || !currentTarget.isOnline()
                || currentTarget.isDead()
                || currentTarget.getWorld() != titan.getWorld()) {
            currentTarget = null;
            return;
        }

        moveTowardTarget();
    }

    private void selectTarget() {
        long now = System.currentTimeMillis();
        if (now - targetSwitchCooldown < TARGET_SWITCH_MS && currentTarget != null
                && currentTarget.isOnline() && !currentTarget.isDead()) {
            return;
        }

        List<Player> candidates = getNearbyPlayers();
        if (candidates.isEmpty()) { currentTarget = null; return; }

        // Priority 1 — highest damage contributor
        Optional<UUID> topDamager = damageTracker.getTopDamager();
        if (topDamager.isPresent()) {
            for (Player p : candidates) {
                if (p.getUniqueId().equals(topDamager.get())) {
                    setTarget(p, now);
                    return;
                }
            }
        }

        // Priority 2 — most irradiated
        Player mostIrradiated = null;
        double maxRad = -1;
        for (Player p : candidates) {
            PlayerData pd = playerDataManager.get(p.getUniqueId()).orElse(null);
            if (pd != null && pd.getRadiationLevel() > maxRad) {
                maxRad        = pd.getRadiationLevel();
                mostIrradiated = p;
            }
        }
        if (mostIrradiated != null && maxRad > 100) {
            setTarget(mostIrradiated, now);
            return;
        }

        // Priority 3 — nearest player
        Player nearest  = null;
        double nearDist = Double.MAX_VALUE;
        Location tLoc   = titan.getLocation();
        for (Player p : candidates) {
            double d = p.getLocation().distanceSquared(tLoc);
            if (d < nearDist) { nearDist = d; nearest = p; }
        }
        if (nearest != null) setTarget(nearest, now);
    }

    private void setTarget(Player p, long now) {
        if (currentTarget != p) {
            NCLogger.debug("Titan target → " + p.getName());
        }
        currentTarget        = p;
        targetSwitchCooldown = now;
    }

    private void moveTowardTarget() {
        if (currentTarget == null) return;
        Location tLoc    = titan.getLocation();
        Location pLoc    = currentTarget.getLocation();
        if (tLoc.getWorld() == null || !tLoc.getWorld().equals(pLoc.getWorld())) return;

        double dist = tLoc.distance(pLoc);
        if (dist <= MELEE_RANGE) return;

        double speed  = configManager.getTitan().getDouble("titan.stats.speed", 0.22);
        Vector dir    = pLoc.toVector().subtract(tLoc.toVector()).normalize().multiply(speed * 3.5);
        dir.setY(0);

        titan.setVelocity(dir);

        // Face the target
        Location lookAt = pLoc.clone();
        lookAt.setY(tLoc.getY());
        Vector facing = lookAt.toVector().subtract(tLoc.toVector());
        float yaw     = (float)(Math.toDegrees(Math.atan2(-facing.getX(), facing.getZ())));
        tLoc.setYaw(yaw);
        titan.teleport(tLoc);
    }

    // ── Stuck detection ───────────────────────────────────────────────────────

    private void checkStuck() {
        if (titan == null || titan.isDead()) return;
        Location cur = titan.getLocation();
        if (lastPosition != null && cur.getWorld() != null
                && cur.getWorld().equals(lastPosition.getWorld())) {
            double moved = cur.distanceSquared(lastPosition);
            if (moved < STUCK_THRESHOLD && currentTarget != null) {
                stuckTicks++;
                if (stuckTicks >= 3) {
                    resolveStuck();
                    stuckTicks = 0;
                }
            } else {
                stuckTicks = 0;
            }
        }
        lastPosition = cur.clone();
    }

    private void resolveStuck() {
        if (titan == null || currentTarget == null) return;
        NCLogger.debug("Titan stuck — teleporting toward target.");
        Location target = currentTarget.getLocation().clone();
        target.setY(target.getY() + 1);
        Vector toTarget = target.toVector().subtract(titan.getLocation().toVector()).normalize();
        Location resolved = titan.getLocation().add(toTarget.multiply(3));
        resolved.setY(titan.getLocation().getY());
        titan.teleport(resolved);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<Player> getNearbyPlayers() {
        if (titan == null || titan.getWorld() == null) return List.of();
        Location loc = titan.getLocation();
        List<Player> result = new ArrayList<>();
        for (Player p : titan.getWorld().getPlayers()) {
            if (!p.isDead() && p.getGameMode() != org.bukkit.GameMode.SPECTATOR
                    && p.getLocation().distanceSquared(loc) <= DETECTION_RANGE * DETECTION_RANGE) {
                result.add(p);
            }
        }
        return result;
    }

    public Player getCurrentTarget() { return currentTarget; }

    public boolean isInMeleeRange(Player player) {
        if (titan == null || player == null) return false;
        return titan.getLocation().distanceSquared(player.getLocation()) <= MELEE_RANGE * MELEE_RANGE;
    }
}
