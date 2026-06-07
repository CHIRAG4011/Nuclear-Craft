package com.nuclearcraft.sound;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.resourcepack.SoundRegistry;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/**
 * Central manager for all NuclearCraft sound playback.
 *
 * When a resource pack is active, the custom nuclearcraft:* sound keys are used.
 * When no pack is present the system falls back to mapped vanilla sounds
 * defined in {@link SoundRegistry}.
 *
 * Volumes and pitches are configurable via resourcepack.yml under the
 * "sounds" section.
 *
 * Phase 12 addition.
 */
public class SoundManager {

    private final NuclearCraftPlugin plugin;
    private final ConfigManager configManager;

    private boolean useCustomSounds;
    private float masterVolume;
    private float masterPitch;

    public SoundManager(NuclearCraftPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void initialize() {
        loadConfig();
        NCLogger.info("[SoundManager] Initialized. Custom sounds: " + useCustomSounds);
    }

    public void reload() {
        loadConfig();
    }

    public void shutdown() {}

    private void loadConfig() {
        FileConfiguration cfg = configManager.getConfig(ConfigManager.ConfigFile.RESOURCEPACK);
        useCustomSounds = cfg.getBoolean("resource-pack.enabled", false);
        masterVolume    = (float) cfg.getDouble("sounds.master-volume", 1.0);
        masterPitch     = (float) cfg.getDouble("sounds.master-pitch", 1.0);
    }

    // ── Public play API ───────────────────────────────────────────────────────

    /**
     * Plays a sound for a single player using a SoundRegistry key.
     */
    public void play(Player player, String soundKey) {
        play(player, soundKey, masterVolume, masterPitch);
    }

    /**
     * Plays a sound for a single player with custom volume and pitch.
     */
    public void play(Player player, String soundKey, float volume, float pitch) {
        if (player == null) return;
        try {
            if (useCustomSounds) {
                player.playSound(player.getLocation(), soundKey, SoundCategory.MASTER, volume, pitch);
            } else {
                String fallback = SoundRegistry.getFallback(soundKey);
                playVanilla(player, player.getLocation(), fallback, volume, pitch);
            }
        } catch (Exception e) {
            NCLogger.debug("[SoundManager] Error playing sound '" + soundKey + "': " + e.getMessage());
        }
    }

    /**
     * Plays a sound at a world location for all nearby players.
     */
    public void playAtLocation(Location location, String soundKey, float volume, float pitch) {
        if (location == null || location.getWorld() == null) return;
        try {
            if (useCustomSounds) {
                location.getWorld().playSound(location, soundKey, SoundCategory.MASTER, volume, pitch);
            } else {
                String fallback = SoundRegistry.getFallback(soundKey);
                location.getWorld().playSound(location, parseFallback(fallback), SoundCategory.MASTER, volume, pitch);
            }
        } catch (Exception e) {
            NCLogger.debug("[SoundManager] Error playing location sound '" + soundKey + "': " + e.getMessage());
        }
    }

    public void playAtLocation(Location location, String soundKey) {
        playAtLocation(location, soundKey, masterVolume, masterPitch);
    }

    // ── Convenience sound methods ─────────────────────────────────────────────

    public void playRadiationGain(Player player) {
        play(player, SoundRegistry.RADIATION_GAIN, 0.6f, 1.2f);
    }

    public void playRadiationStageChange(Player player) {
        play(player, SoundRegistry.RADIATION_STAGE_CHANGE, 1.0f, 0.8f);
    }

    public void playRadiationCure(Player player) {
        play(player, SoundRegistry.RADIATION_CURE, 1.0f, 1.0f);
    }

    public void playRadiationSurge(Location location) {
        playAtLocation(location, SoundRegistry.RADIATION_SURGE, 1.5f, 0.7f);
    }

    public void playRadiationDeath(Location location) {
        playAtLocation(location, SoundRegistry.RADIATION_DEATH, 1.0f, 1.0f);
    }

    public void playAntidoteDrink(Player player) {
        play(player, SoundRegistry.ANTIDOTE_DRINK, 1.0f, 1.0f);
    }

    public void playSerumDrink(Player player) {
        play(player, SoundRegistry.SERUM_DRINK, 1.0f, 1.2f);
    }

    public void playOreMine(Location location) {
        playAtLocation(location, SoundRegistry.ORE_MINE, 1.0f, 1.0f);
    }

    public void playOreDiscover(Player player) {
        play(player, SoundRegistry.ORE_DISCOVER, 0.8f, 1.5f);
    }

    public void playOreDrill(Location location) {
        playAtLocation(location, SoundRegistry.ORE_DRILL, 1.0f, 1.0f);
    }

    public void playSmelterStart(Location location) {
        playAtLocation(location, SoundRegistry.SMELTER_START, 0.8f, 1.0f);
    }

    public void playSmelterComplete(Location location) {
        playAtLocation(location, SoundRegistry.SMELTER_COMPLETE, 1.0f, 1.0f);
    }

    public void playSmelterOverheat(Location location) {
        playAtLocation(location, SoundRegistry.SMELTER_OVERHEAT, 1.0f, 0.7f);
    }

    public void playForgeStart(Location location) {
        playAtLocation(location, SoundRegistry.FORGE_START, 1.0f, 1.0f);
    }

    public void playForgeComplete(Location location) {
        playAtLocation(location, SoundRegistry.FORGE_COMPLETE, 1.0f, 1.1f);
    }

    public void playForgeOverload(Location location) {
        playAtLocation(location, SoundRegistry.FORGE_OVERLOAD, 1.5f, 0.6f);
    }

    public void playTitanForgeStart(Location location) {
        playAtLocation(location, SoundRegistry.TITAN_FORGE_START, 1.0f, 0.9f);
    }

    public void playTitanForgeComplete(Location location) {
        playAtLocation(location, SoundRegistry.TITAN_FORGE_COMPLETE, 1.5f, 1.0f);
    }

    public void playUpgradeSuccess(Player player) {
        play(player, SoundRegistry.UPGRADE_SUCCESS, 1.0f, 1.2f);
    }

    public void playUpgradeFail(Player player) {
        play(player, SoundRegistry.UPGRADE_FAIL, 1.0f, 0.8f);
    }

    public void playWeaponRadiationHit(Player target) {
        play(target, SoundRegistry.WEAPON_RADIATION_HIT, 0.8f, 0.8f);
    }

    public void playTitanSpawn(Location location) {
        playAtLocation(location, SoundRegistry.TITAN_SPAWN, 2.0f, 0.6f);
    }

    public void playTitanDeath(Location location) {
        playAtLocation(location, SoundRegistry.TITAN_DEATH, 2.0f, 0.8f);
    }

    public void playTitanPhaseChange(Location location) {
        playAtLocation(location, SoundRegistry.TITAN_PHASE_CHANGE, 2.0f, 0.7f);
    }

    public void playTitanRoar(Location location) {
        playAtLocation(location, SoundRegistry.TITAN_ROAR, 2.0f, 0.5f);
    }

    public void playRadiationNightStart(Location location) {
        playAtLocation(location, SoundRegistry.RADIATION_NIGHT_START, 2.0f, 0.8f);
    }

    public void playRadiationNightEnd(Location location) {
        playAtLocation(location, SoundRegistry.RADIATION_NIGHT_END, 1.5f, 1.0f);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private void playVanilla(Player player, Location location, String soundName, float volume, float pitch) {
        try {
            Sound sound = Sound.valueOf(soundName
                    .replace("minecraft:", "")
                    .toUpperCase()
                    .replace(".", "_"));
            player.playSound(location, sound, SoundCategory.MASTER, volume, pitch);
        } catch (IllegalArgumentException ignored) {
            player.playSound(location, soundName, SoundCategory.MASTER, volume, pitch);
        }
    }

    private Sound parseFallback(String fallback) {
        try {
            return Sound.valueOf(fallback
                    .replace("minecraft:", "")
                    .toUpperCase()
                    .replace(".", "_"));
        } catch (Exception e) {
            return Sound.BLOCK_BEACON_AMBIENT;
        }
    }
}
