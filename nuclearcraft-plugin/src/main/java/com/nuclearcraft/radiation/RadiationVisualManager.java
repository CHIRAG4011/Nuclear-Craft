package com.nuclearcraft.radiation;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.utils.NCLogger;
import com.nuclearcraft.utils.ParticleUtil;
import com.nuclearcraft.utils.RandomUtil;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Manages all visual and audio feedback for the radiation system.
 *
 * Responsibilities:
 *   - Stage-specific particle effects rendered locally (only visible near the affected player)
 *   - Geiger-counter sound system with stage-scaled frequency
 *   - Scheduled tasks for both systems, optimized to avoid per-tick overhead
 */
public class RadiationVisualManager {

    private static final Color GREEN_GLOW = Color.fromRGB(57, 255, 20);
    private static final Color YELLOW_WARN = Color.fromRGB(255, 220, 0);
    private static final Color ORANGE_SEVERE = Color.fromRGB(255, 100, 0);
    private static final Color RED_CRITICAL = Color.fromRGB(200, 0, 0);

    private final NuclearCraftPlugin plugin;
    private final ConfigManager configManager;
    private final PlayerDataManager playerDataManager;
    private final RadiationManager radiationManager;

    public RadiationVisualManager(NuclearCraftPlugin plugin, ConfigManager configManager,
                                   PlayerDataManager playerDataManager,
                                   RadiationManager radiationManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.playerDataManager = playerDataManager;
        this.radiationManager = radiationManager;
    }

    public void initialize() {
        scheduleParticleTask();
        scheduleGeigerTask();
        NCLogger.info("RadiationVisualManager initialized.");
    }

    public void shutdown() {
        NCLogger.info("RadiationVisualManager shut down.");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Particles
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Particle task: runs every N ticks (configurable, default 10).
     * Only spawns particles for online players with stage >= 1.
     * Particles are spawned at the player's location — only nearby clients see them.
     */
    private void scheduleParticleTask() {
        long interval = configManager.getRadiation().getLong("visuals.particle-interval-ticks", 10L);
        plugin.getTaskManager().scheduleSync(() -> {
            if (!configManager.getRadiation().getBoolean("visuals.particles-enabled", true)) return;
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                int stage = radiationManager.getStage(player);
                if (stage == 0) continue;
                spawnStageParticles(player, stage);
            }
        }, interval, interval);
    }

    private void spawnStageParticles(Player player, int stage) {
        Location loc = player.getLocation().add(0, 1, 0);
        switch (stage) {
            case 1 -> {
                // Small green dust
                ParticleUtil.spawnDust(loc, GREEN_GLOW, 0.8f, 3);
            }
            case 2 -> {
                // Dust + spores
                ParticleUtil.spawnDust(loc, YELLOW_WARN, 1.0f, 5);
                ParticleUtil.spawn(loc, Particle.SPORE_BLOSSOM_AIR, 2, 0.4, 0.5, 0.4);
            }
            case 3 -> {
                // Heavy radioactive mist
                ParticleUtil.spawnDust(loc, ORANGE_SEVERE, 1.2f, 8);
                ParticleUtil.spawn(loc, Particle.SPORE_BLOSSOM_AIR, 4, 0.5, 0.8, 0.5);
                if (RandomUtil.chance(0.3)) {
                    ParticleUtil.spawn(loc, Particle.SMOKE, 3, 0.3, 0.5, 0.3);
                }
            }
            case 4 -> {
                // Dense toxic cloud
                ParticleUtil.spawnDust(loc, RED_CRITICAL, 1.5f, 12);
                ParticleUtil.spawn(loc, Particle.SPORE_BLOSSOM_AIR, 6, 0.6, 1.0, 0.6);
                ParticleUtil.spawn(loc, Particle.SMOKE, 5, 0.4, 0.8, 0.4);
                if (RandomUtil.chance(0.5)) {
                    ParticleUtil.spawn(loc, Particle.LARGE_SMOKE, 2, 0.3, 0.5, 0.3);
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Geiger Counter Audio
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Geiger task: runs every 10 ticks but rolls a chance per stage to reduce frequency.
     * Higher stages click more often and more intensely.
     */
    private void scheduleGeigerTask() {
        long interval = configManager.getRadiation().getLong("visuals.geiger-check-interval-ticks", 10L);
        plugin.getTaskManager().scheduleSync(() -> {
            if (!configManager.getRadiation().getBoolean("visuals.sounds-enabled", true)) return;
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                int stage = radiationManager.getStage(player);
                if (stage == 0) continue;
                playGeigerTick(player, stage);
            }
        }, interval, interval);
    }

    private void playGeigerTick(Player player, int stage) {
        // Stage 1: ~12.5% chance per check (slow clicks)
        // Stage 2: ~37.5% chance (moderate)
        // Stage 3: ~75%   (rapid)
        // Stage 4: ~100%  (continuous)
        double[] chances = {0, 0.125, 0.375, 0.75, 1.0};
        if (!RandomUtil.chance(chances[Math.min(stage, 4)])) return;

        float pitch = switch (stage) {
            case 1 -> 0.5f + RandomUtil.nextDouble(0, 0.2).floatValue();
            case 2 -> 0.8f + RandomUtil.nextDouble(0, 0.2).floatValue();
            case 3 -> 1.1f + RandomUtil.nextDouble(0, 0.2).floatValue();
            case 4 -> 1.5f + RandomUtil.nextDouble(0, 0.3).floatValue();
            default -> 1.0f;
        };

        float volume = switch (stage) {
            case 1 -> 0.3f;
            case 2 -> 0.5f;
            case 3 -> 0.7f;
            case 4 -> 1.0f;
            default -> 0.3f;
        };

        // Use BLOCK_NOTE_BLOCK_HAT as a Geiger-click approximation
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, volume, (float) pitch);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // On-demand visual triggers
    // ──────────────────────────────────────────────────────────────────────────

    /** Plays a radiation gain "zap" sound and burst of particles to the affected player only. */
    public void playExposureEffect(Player player, int stage) {
        Location loc = player.getLocation().add(0, 1, 0);
        ParticleUtil.spawnDust(loc, GREEN_GLOW, 1.5f, 15);
        player.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.4f, 2.0f);
    }

    /** Plays a recovery/cure sound and green particle burst. */
    public void playCureEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        ParticleUtil.spawnDust(loc, Color.fromRGB(0, 200, 255), 1.5f, 20);
        player.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.5f);
    }

    /** Plays a stage-escalation warning sound only to the affected player. */
    public void playStageEscalateEffect(Player player, int newStage) {
        float pitch = 0.5f + (newStage * 0.2f);
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.5f, pitch);
    }
}
