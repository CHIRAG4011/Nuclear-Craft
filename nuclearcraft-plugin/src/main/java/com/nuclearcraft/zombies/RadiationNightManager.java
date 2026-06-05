package com.nuclearcraft.zombies;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.utils.NCLogger;
import com.nuclearcraft.utils.RandomUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the Radiation Surge night event.
 *
 * Mechanics:
 *   - Checks each world once per in-game night (when time crosses 13,000).
 *   - 5% chance (configurable) to activate a Radiation Surge.
 *   - During surge: 100% irradiated spawn rate, double radiation, double loot.
 *   - Ends automatically at sunrise (time 23,000).
 *   - Shows a BossBar to all players while active.
 *   - Broadcasts a message and plays a sound on start/end.
 *
 * One task runs every 100 ticks (5 seconds) to check world time.
 * No per-tick loop — only transitions are acted upon.
 */
public class RadiationNightManager {

    // Minecraft time constants
    private static final long NIGHT_START  = 13_000L;
    private static final long NIGHT_END    = 23_000L;

    private final NuclearCraftPlugin plugin;
    private final ConfigManager configManager;
    private final ZombieSpawnManager spawnManager;

    /** Per-world state — whether a surge is active this night. */
    private final Map<UUID, Boolean> worldSurgeActive = new HashMap<>();
    /** Per-world — whether we have already rolled for this night cycle. */
    private final Map<UUID, Boolean> nightRolled = new HashMap<>();

    private BossBar surgeBossBar;
    private BukkitTask timeCheckTask;

    public RadiationNightManager(NuclearCraftPlugin plugin, ConfigManager configManager,
                                  ZombieSpawnManager spawnManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.spawnManager = spawnManager;
    }

    public void initialize() {
        surgeBossBar = Bukkit.createBossBar(
                "☢ RADIATION SURGE ACTIVE ☢",
                BarColor.GREEN,
                BarStyle.SOLID
        );
        surgeBossBar.setVisible(false);

        startTimeCheckTask();
        NCLogger.info("RadiationNightManager initialized.");
    }

    public void shutdown() {
        if (timeCheckTask != null) timeCheckTask.cancel();
        if (surgeBossBar != null) {
            surgeBossBar.removeAll();
            surgeBossBar.setVisible(false);
        }
        spawnManager.setSurgeActive(false);
        NCLogger.info("RadiationNightManager shut down.");
    }

    // ──────────────────────────────────────────────────────────────────────────

    public boolean isSurgeActive() { return spawnManager.isSurgeActive(); }

    /** Force-start a surge (admin command). */
    public void forceSurge() {
        activateSurge();
    }

    /** Force-end a surge (admin command). */
    public void forceEndSurge() {
        endSurge();
    }

    // ──────────────────────────────────────────────────────────────────────────

    private void startTimeCheckTask() {
        timeCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (World world : Bukkit.getWorlds()) {
                    if (world.getEnvironment() != World.Environment.NORMAL) continue;
                    checkWorld(world);
                }
            }
        }.runTaskTimer(plugin, 100L, 100L);
    }

    private void checkWorld(World world) {
        UUID wid = world.getUID();
        long time = world.getTime();
        boolean currentlyNight = time >= NIGHT_START && time < NIGHT_END;

        if (currentlyNight) {
            // Roll for surge at the start of night (only once per night)
            if (!nightRolled.getOrDefault(wid, false)) {
                nightRolled.put(wid, true);
                boolean surge = rollSurge();
                worldSurgeActive.put(wid, surge);
                if (surge) activateSurge();
            }
        } else {
            // Daytime — reset roll flag and end any active surge
            if (nightRolled.getOrDefault(wid, false)) {
                nightRolled.put(wid, false);
                boolean wasSurge = worldSurgeActive.getOrDefault(wid, false);
                worldSurgeActive.put(wid, false);
                if (wasSurge) endSurge();
            }
        }
    }

    private boolean rollSurge() {
        double chance = configManager.getZombies().getDouble("night-event.surge-chance", 0.05);
        return RandomUtil.chance(chance);
    }

    private void activateSurge() {
        if (spawnManager.isSurgeActive()) return;
        spawnManager.setSurgeActive(true);

        // BossBar
        surgeBossBar.setVisible(true);
        for (Player player : Bukkit.getOnlinePlayers()) {
            surgeBossBar.addPlayer(player);
        }

        // Broadcast
        String message = configManager.getZombies().getString("night-event.start-message",
                "&4☢ &c&lRADIATION SURGE! &4☢ &cA wave of radioactive energy sweeps the world!");
        Bukkit.broadcast(Component.text(message.replace("&", "§")));

        // Sound
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
        }

        NCLogger.info("Radiation Surge activated.");
    }

    private void endSurge() {
        if (!spawnManager.isSurgeActive()) return;
        spawnManager.setSurgeActive(false);

        surgeBossBar.setVisible(false);
        surgeBossBar.removeAll();

        String message = configManager.getZombies().getString("night-event.end-message",
                "&2☢ &a&lRadiation Surge has ended. &2☢ &aThe air clears...");
        Bukkit.broadcast(Component.text(message.replace("&", "§")));

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.8f);
        }

        NCLogger.info("Radiation Surge ended.");
    }

    /** Called when a new player joins so they get the bossbar if active. */
    public void onPlayerJoin(Player player) {
        if (isSurgeActive()) surgeBossBar.addPlayer(player);
    }

    /** Called when a player quits to clean up bossbar. */
    public void onPlayerQuit(Player player) {
        surgeBossBar.removePlayer(player);
    }
}
