package com.nuclearcraft.radiation;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.data.PlayerData;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.events.*;
import com.nuclearcraft.utils.MathHelper;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Optional;

/**
 * Core manager for the NuclearCraft radiation system.
 *
 * Handles:
 *  - Applying and removing radiation points
 *  - Recalculating and broadcasting stage changes
 *  - Applying per-stage potion effects every tick cycle
 *  - Radiation progression (worsening over time)
 *  - Radiation decay (slow recovery when clean)
 *  - Radiation death detection and custom death messages
 *
 * All Bukkit API calls are made on the main thread.
 * All data reads/writes go through PlayerDataManager.
 */
public class RadiationManager {

    /** Minimum radiation value per stage. Index = stage number. */
    public static final int[] STAGE_MIN = {0, 100, 250, 500, 750};

    /** Maximum radiation value per stage (inclusive). */
    public static final int[] STAGE_MAX = {99, 249, 499, 749, 1000};

    public static final int MAX_RADIATION = 1000;
    public static final int NUM_STAGES = 5;

    /** Radiation is considered "received recently" within this window. */
    public static final long DECAY_GRACE_PERIOD_MS = 10L * 60L * 1000L; // 10 minutes

    private final NuclearCraftPlugin plugin;
    private final ConfigManager configManager;
    private final PlayerDataManager playerDataManager;

    public RadiationManager(NuclearCraftPlugin plugin, ConfigManager configManager,
                             PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.playerDataManager = playerDataManager;
    }

    public void initialize() {
        scheduleEffectTask();
        scheduleProgressionTask();
        scheduleDecayTask();
        NCLogger.info("RadiationManager initialized.");
    }

    public void shutdown() {
        NCLogger.info("RadiationManager shut down.");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Core API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Adds radiation to the player from a specified source.
     * Fires {@link RadiationGainEvent}. Respects immunity timer and cancellation.
     * Armor reduces exposure based on configuration.
     *
     * @param player the affected player
     * @param amount positive radiation points to add
     * @param source the origin of the radiation
     */
    public void addRadiation(Player player, int amount, RadiationSource source) {
        Optional<PlayerData> opt = playerDataManager.get(player);
        if (opt.isEmpty()) return;
        PlayerData data = opt.get();

        if (data.isImmune()) return;

        // Armor reduction
        int effectiveAmount = applyArmorReduction(player, amount);

        RadiationGainEvent event = new RadiationGainEvent(player, effectiveAmount, source, data.getRadiationLevel());
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        effectiveAmount = event.getAmount();
        double newLevel = Math.min(MAX_RADIATION, data.getRadiationLevel() + effectiveAmount);
        data.setRadiationLevel(newLevel);
        data.addTotalRadiationExposure(effectiveAmount);
        data.setLastRadiationSource(source.name());
        data.setLastRadiationReceivedMs(System.currentTimeMillis());

        updateStage(player, data);
        NCLogger.debug("addRadiation: %s +%d from %s (total=%.0f)", player.getName(), effectiveAmount, source, newLevel);
    }

    /**
     * Removes radiation from the player (cure, healing item, etc.).
     * Fires {@link RadiationLossEvent}. Respects cancellation.
     *
     * @param player the affected player
     * @param amount positive radiation points to remove
     */
    public void removeRadiation(Player player, int amount) {
        Optional<PlayerData> opt = playerDataManager.get(player);
        if (opt.isEmpty()) return;
        PlayerData data = opt.get();

        RadiationLossEvent event = new RadiationLossEvent(player, amount, data.getRadiationLevel());
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        double newLevel = Math.max(0, data.getRadiationLevel() - event.getAmount());
        double cured = data.getRadiationLevel() - newLevel;
        data.setRadiationLevel(newLevel);
        data.addTotalRadiationCured(cured);

        updateStage(player, data);
        NCLogger.debug("removeRadiation: %s -%.0f (total=%.0f)", player.getName(), cured, newLevel);
    }

    /**
     * Sets the player's radiation to an absolute value.
     * Fires the appropriate Gain or Loss event.
     *
     * @param player the affected player
     * @param amount target radiation (clamped to 0–1000)
     */
    public void setRadiation(Player player, int amount) {
        int clamped = (int) MathHelper.clamp(amount, 0, MAX_RADIATION);
        Optional<PlayerData> opt = playerDataManager.get(player);
        if (opt.isEmpty()) return;
        PlayerData data = opt.get();
        double current = data.getRadiationLevel();
        if (clamped > current) {
            addRadiation(player, (int)(clamped - current), RadiationSource.COMMAND);
        } else if (clamped < current) {
            removeRadiation(player, (int)(current - clamped));
        }
    }

    /**
     * Returns the player's current radiation level, or 0 if not loaded.
     *
     * @param player the player to query
     * @return radiation points (0–1000)
     */
    public double getRadiation(Player player) {
        return playerDataManager.get(player)
                .map(PlayerData::getRadiationLevel)
                .orElse(0.0);
    }

    /**
     * Fully clears the player's radiation (sets to 0, resets stage).
     * Fires {@link RadiationLossEvent} with the full amount.
     *
     * @param player the player to cure
     */
    public void clearRadiation(Player player) {
        Optional<PlayerData> opt = playerDataManager.get(player);
        if (opt.isEmpty()) return;
        PlayerData data = opt.get();
        if (data.getRadiationLevel() <= 0) return;
        removeRadiation(player, (int) data.getRadiationLevel() + 1);
    }

    /**
     * Returns the current radiation stage (0–4) for the player.
     *
     * @param player the player to query
     * @return radiation stage, or 0 if not loaded
     */
    public int getStage(Player player) {
        return playerDataManager.get(player)
                .map(PlayerData::getRadiationStage)
                .orElse(0);
    }

    /**
     * Returns true if the player has any radiation (stage >= 1).
     *
     * @param player the player to check
     */
    public boolean isInfected(Player player) {
        return getStage(player) >= 1;
    }

    /**
     * Returns true if the player can spread radiation to others (stage >= 2).
     *
     * @param player the player to check
     */
    public boolean isContagious(Player player) {
        return getStage(player) >= 2;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Recalculates the stage from current radiation and fires a stage-change event
     * if the stage boundary was crossed.
     */
    public void updateStage(Player player, PlayerData data) {
        int newStage = calculateStage((int) data.getRadiationLevel());
        int oldStage = data.getRadiationStage();
        if (newStage == oldStage) return;

        RadiationStageChangeEvent event = new RadiationStageChangeEvent(
                player, oldStage, newStage, data.getRadiationLevel());
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        data.setRadiationStage(newStage);
        onStageChange(player, data, oldStage, newStage);
        NCLogger.debug("Stage change: %s %d -> %d", player.getName(), oldStage, newStage);
    }

    /** Returns the stage number for a given radiation value. */
    public static int calculateStage(int radiation) {
        for (int i = NUM_STAGES - 1; i >= 0; i--) {
            if (radiation >= STAGE_MIN[i]) return i;
        }
        return 0;
    }

    private void onStageChange(Player player, PlayerData data, int oldStage, int newStage) {
        var cfg = configManager.getRadiation();
        String msgPath = "radiation.stage-" + newStage;
        String stageName = cfg.getString("stages." + newStage + ".name", "Unknown");

        if (newStage > oldStage) {
            String warnKey = newStage >= 4 ? "radiation.critical" : "radiation.exposed";
            player.sendMessage(com.nuclearcraft.utils.ColorUtil.parse(
                    configManager.getMessage(warnKey)));
        }

        if (newStage == 0) {
            player.sendMessage(com.nuclearcraft.utils.ColorUtil.parse(
                    configManager.getMessage("radiation.stage-0")));
        }

        if (newStage >= 1 && oldStage < 1) {
            data.setTimesInfected(data.getTimesInfected() + 1);
        }
    }

    private int applyArmorReduction(Player player, int amount) {
        if (!configManager.getRadiation().getBoolean("armor-reduction.enabled", true)) return amount;
        double reduction = 0;
        int piecesWorn = 0;
        var inventory = player.getInventory();
        if (inventory.getHelmet() != null) piecesWorn++;
        if (inventory.getChestplate() != null) piecesWorn++;
        if (inventory.getLeggings() != null) piecesWorn++;
        if (inventory.getBoots() != null) piecesWorn++;
        double perPiece = configManager.getRadiation().getDouble("armor-reduction.per-piece-reduction", 0.15);
        double maxReduction = configManager.getRadiation().getDouble("armor-reduction.max-reduction", 0.60);
        reduction = Math.min(piecesWorn * perPiece, maxReduction);
        return (int) Math.max(0, amount * (1.0 - reduction));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Scheduled tasks
    // ──────────────────────────────────────────────────────────────────────────

    /** Applies potion effects and radiation damage every second (20 ticks). */
    private void scheduleEffectTask() {
        plugin.getTaskManager().scheduleSync(() -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                playerDataManager.get(player).ifPresent(data -> applyStageEffects(player, data));
            }
        }, 20L, 20L);
    }

    /** Worsens radiation every 60 seconds based on current stage. */
    private void scheduleProgressionTask() {
        long interval = configManager.getRadiation().getLong("progression.interval-ticks", 1200L);
        plugin.getTaskManager().scheduleSync(() -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                playerDataManager.get(player).ifPresent(data -> {
                    int stage = data.getRadiationStage();
                    if (stage <= 0) return;
                    int[] gains = {0,
                            configManager.getRadiation().getInt("progression.stage-1-gain", 5),
                            configManager.getRadiation().getInt("progression.stage-2-gain", 10),
                            configManager.getRadiation().getInt("progression.stage-3-gain", 15),
                            configManager.getRadiation().getInt("progression.stage-4-gain", 20)};
                    int gain = gains[Math.min(stage, gains.length - 1)];
                    if (gain > 0) {
                        double newLevel = Math.min(MAX_RADIATION, data.getRadiationLevel() + gain);
                        data.setRadiationLevel(newLevel);
                        updateStage(player, data);
                    }
                });
            }
        }, interval, interval);
    }

    /** Slowly removes radiation if the player has been clean for 10 minutes. */
    private void scheduleDecayTask() {
        long interval = configManager.getRadiation().getLong("decay.interval-ticks", 1200L);
        plugin.getTaskManager().scheduleSync(() -> {
            long now = System.currentTimeMillis();
            long graceMs = configManager.getRadiation().getLong("decay.grace-period-minutes", 10) * 60_000L;
            int decayAmount = configManager.getRadiation().getInt("decay.amount-per-interval", 2);
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                playerDataManager.get(player).ifPresent(data -> {
                    if (data.getRadiationLevel() <= 0) return;
                    if ((now - data.getLastRadiationReceivedMs()) >= graceMs) {
                        double newLevel = Math.max(0, data.getRadiationLevel() - decayAmount);
                        data.setRadiationLevel(newLevel);
                        updateStage(player, data);
                        NCLogger.debug("Radiation decay: %s -%.0f (total=%.0f)", player.getName(), (double)decayAmount, newLevel);
                    }
                });
            }
        }, interval, interval);
    }

    /** Applies stage-appropriate PotionEffects and damage. */
    private void applyStageEffects(Player player, PlayerData data) {
        int stage = data.getRadiationStage();
        if (stage == 0) return;

        int duration = 40; // 2 seconds, re-applied every second to prevent flicker

        switch (stage) {
            case 1 -> {
                applyEffect(player, PotionEffectType.WEAKNESS, 0, duration);
                applyEffect(player, PotionEffectType.NAUSEA, 0, duration);
            }
            case 2 -> {
                applyEffect(player, PotionEffectType.WEAKNESS, 0, duration);
                applyEffect(player, PotionEffectType.SLOWNESS, 0, duration);
                applyEffect(player, PotionEffectType.NAUSEA, 0, duration);
            }
            case 3 -> {
                applyEffect(player, PotionEffectType.WEAKNESS, 1, duration);
                applyEffect(player, PotionEffectType.SLOWNESS, 1, duration);
                applyEffect(player, PotionEffectType.HUNGER, 0, duration);
                // Periodic damage: ~every 4 seconds
                if ((System.currentTimeMillis() / 1000L) % 4 == 0) {
                    double dmg = configManager.getRadiation().getDouble("stages.3.damage-per-cycle", 0.5);
                    player.damage(dmg);
                }
            }
            case 4 -> {
                applyEffect(player, PotionEffectType.WEAKNESS, 2, duration);
                applyEffect(player, PotionEffectType.SLOWNESS, 2, duration);
                applyEffect(player, PotionEffectType.HUNGER, 1, duration);
                // Damage every 2 seconds
                if ((System.currentTimeMillis() / 1000L) % 2 == 0) {
                    double dmg = configManager.getRadiation().getDouble("stages.4.damage-per-cycle", 1.0);
                    player.damage(dmg);
                }
                // Random nausea burst
                if (com.nuclearcraft.utils.RandomUtil.chance(0.1)) {
                    applyEffect(player, PotionEffectType.NAUSEA, 1, 60);
                }
            }
        }
    }

    private void applyEffect(Player player, PotionEffectType type, int amplifier, int duration) {
        player.addPotionEffect(new PotionEffect(type, duration, amplifier, true, false, false));
    }
}
