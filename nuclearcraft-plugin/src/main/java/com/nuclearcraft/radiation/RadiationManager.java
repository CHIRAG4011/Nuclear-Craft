package com.nuclearcraft.radiation;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.data.PlayerData;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.events.*;
import com.nuclearcraft.utils.MathHelper;
import com.nuclearcraft.utils.NCLogger;
import com.nuclearcraft.utils.RandomUtil;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Optional;
import java.util.Set;

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
 * Phase 7 update: Immunity check is now source-aware.
 * {@link RadiationSource}s in {@link #IMMUNITY_BLOCKED} are skipped while the player
 * is immune (environmental/passive sources). Boss attacks, weapons, and admin
 * commands still apply radiation regardless of immunity status.
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

    /**
     * Radiation sources that are blocked when the player has immunity (e.g. from the Radiation Serum).
     * Boss attacks, admin commands, and weapon hits bypass immunity intentionally.
     */
    private static final Set<RadiationSource> IMMUNITY_BLOCKED = Set.of(
            RadiationSource.PLUTONIUM_ORE,
            RadiationSource.RADIOACTIVE_DEBRIS,
            RadiationSource.RADIOACTIVE_SOIL,
            RadiationSource.RADIOACTIVE_FARMLAND,
            RadiationSource.TOXIC_BLOOM,
            RadiationSource.PLUTONIUM_FRAGMENT,
            RadiationSource.RADIATION_CLOUD,
            RadiationSource.NUCLEAR_SMELTER,
            RadiationSource.IRRADIATED_ZOMBIE,
            RadiationSource.RADIATED_PLAYER
    );

    private final NuclearCraftPlugin plugin;
    private final ConfigManager configManager;
    private final PlayerDataManager playerDataManager;

    /** Injected by Phase 6 EquipmentManager after it initialises. May be null before Phase 6. */
    private com.nuclearcraft.equipment.RadiationResistanceManager resistanceManager;

    public RadiationManager(NuclearCraftPlugin plugin, ConfigManager configManager,
                             PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.playerDataManager = playerDataManager;
    }

    /**
     * Wires the Phase 6 resistance manager into the radiation pipeline.
     * Once set, all subsequent {@link #addRadiation} calls use the new
     * hazmat/plutonium armor logic instead of the generic piece-count fallback.
     */
    public void setResistanceManager(com.nuclearcraft.equipment.RadiationResistanceManager resistanceManager) {
        this.resistanceManager = resistanceManager;
        NCLogger.info("RadiationResistanceManager wired into RadiationManager.");
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
     * Fires {@link RadiationGainEvent}. Respects selective immunity and cancellation.
     * Armor reduces exposure based on configuration.
     *
     * <p>Phase 7 immunity logic: immunity only blocks sources in {@link #IMMUNITY_BLOCKED}
     * (environmental/passive). Boss attacks ({@link RadiationSource#BOSS_ATTACK}),
     * weapon hits ({@link RadiationSource#PLUTONIUM_WEAPON}, {@link RadiationSource#PLUTONIUM_ARROW}),
     * and admin commands ({@link RadiationSource#COMMAND}) are never blocked by immunity.
     *
     * @param player the affected player
     * @param amount positive radiation points to add
     * @param source the origin of the radiation
     */
    public void addRadiation(Player player, int amount, RadiationSource source) {
        Optional<PlayerData> opt = playerDataManager.get(player);
        if (opt.isEmpty()) return;
        PlayerData data = opt.get();

        // Selective immunity check — only block environmental/passive sources
        if (data.isImmune() && IMMUNITY_BLOCKED.contains(source)) return;

        // Armor reduction — use Phase 6 resistance manager when available,
        // otherwise fall back to the legacy generic piece-count logic
        int effectiveAmount;
        if (resistanceManager != null) {
            double multiplier = resistanceManager.getMultiplier(player, source);
            effectiveAmount = (int) Math.max(0, amount * multiplier);
        } else {
            effectiveAmount = applyArmorReduction(player, amount);
        }

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

        // ── Skin infection glow via scoreboard team ─────────────────────────
        updateInfectionGlow(player, newStage);
    }

    /**
     * Applies or removes the green scoreboard glow that visually signals infection.
     * Uses the main scoreboard's "nc_infected" team (created in initialize).
     */
    private void updateInfectionGlow(Player player, int stage) {
        try {
            Scoreboard sb = plugin.getServer().getScoreboardManager().getMainScoreboard();
            Team team = sb.getTeam("nc_infected");
            if (team == null) {
                team = sb.registerNewTeam("nc_infected");
                team.color(NamedTextColor.GREEN);
            }
            if (stage >= 1) {
                team.addPlayer(player);
                player.setGlowing(true);
            } else {
                team.removePlayer(player);
                player.setGlowing(false);
            }
        } catch (Exception e) {
            NCLogger.warn("Could not update infection glow for " + player.getName() + ": " + e.getMessage());
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

    /** Applies stage-appropriate PotionEffects, radiation damage, and special effects. */
    private void applyStageEffects(Player player, PlayerData data) {
        int stage = data.getRadiationStage();
        if (stage == 0) return;

        // Standard effects refreshed every second (duration 40 = 2s prevents flicker)
        int dur = 40;

        switch (stage) {
            case 1 -> {
                // DARKNESS fires immediately (no warmup unlike nausea).
                // Nausea warp ramps up over ~15 s in parallel.
                applyEffect(player, PotionEffectType.WEAKNESS, 0, dur);
                applyDarkness(player, 0);   // subtle flicker — immediate
                applyNausea(player, 0);     // screen warp — ramps in over time
            }
            case 2 -> {
                applyEffect(player, PotionEffectType.WEAKNESS, 0, dur);
                applyEffect(player, PotionEffectType.SLOWNESS, 0, dur);
                applyDarkness(player, 0);
                applyNausea(player, 0);
            }
            case 3 -> {
                applyEffect(player, PotionEffectType.WEAKNESS, 1, dur);
                applyEffect(player, PotionEffectType.SLOWNESS, 1, dur);
                applyEffect(player, PotionEffectType.HUNGER, 0, dur);
                applyDarkness(player, 1);   // stronger pulse
                applyNausea(player, 0);
                if (RandomUtil.chance(0.05)) randomTeleport(player, 2, 5);
                if ((System.currentTimeMillis() / 1000L) % 4 == 0) {
                    double dmg = configManager.getRadiation().getDouble("stages.3.damage-per-cycle", 0.5);
                    player.damage(dmg);
                }
            }
            case 4 -> {
                applyEffect(player, PotionEffectType.WEAKNESS, 2, dur);
                applyEffect(player, PotionEffectType.SLOWNESS, 2, dur);
                applyEffect(player, PotionEffectType.HUNGER, 1, dur);
                applyDarkness(player, 1);   // heavy blackout pulses
                applyNausea(player, 1);     // level-II warp
                if (RandomUtil.chance(0.15)) randomTeleport(player, 3, 8);
                if ((System.currentTimeMillis() / 1000L) % 2 == 0) {
                    double dmg = configManager.getRadiation().getDouble("stages.4.damage-per-cycle", 1.0);
                    player.damage(dmg);
                }
            }
        }
    }

    /**
     * Applies NAUSEA with a duration long enough for the screen-warp animation to trigger.
     *
     * <p>Minecraft's NAUSEA effect has an internal warmup of ~300 ticks before the
     * screen actually begins to warble. If we keep reapplying with duration=40 every
     * second the warmup never completes. So we apply once at 800 ticks (40 s) and
     * only refresh when fewer than 100 ticks remain — ensuring the animation always fires.
     */
    private void applyNausea(Player player, int amplifier) {
        PotionEffect current = player.getPotionEffect(PotionEffectType.NAUSEA);
        if (current != null && current.getDuration() > 100 && current.getAmplifier() >= amplifier) return;
        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 800, amplifier, true, false, false));
    }

    /**
     * Teleports the player a random horizontal distance.
     * Skips the teleport if the destination blocks are not air (safety check).
     */
    private void randomTeleport(Player player, int minBlocks, int maxBlocks) {
        double angle = Math.random() * 2 * Math.PI;
        double dist  = minBlocks + Math.random() * (maxBlocks - minBlocks);
        Location src = player.getLocation();
        Location dst = src.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);

        // Safety: only teleport if destination blocks are passable
        if (!dst.getBlock().isPassable() || !dst.clone().add(0, 1, 0).getBlock().isPassable()) return;

        // Play origin portal burst + teleport sound (enderman-style but lower pitch)
        src.getWorld().spawnParticle(Particle.PORTAL, src.clone().add(0, 1, 0), 40, 0.4, 0.8, 0.4, 0.15);
        player.playSound(src, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 0.4f);

        player.teleport(dst);

        // Play arrival burst
        dst.getWorld().spawnParticle(Particle.PORTAL, dst.clone().add(0, 1, 0), 40, 0.4, 0.8, 0.4, 0.15);
    }

    /**
     * Applies DARKNESS effect — pulses the screen to near-black immediately with no warmup.
     * Duration is kept just long enough to avoid flicker (reapplied every second).
     * Amplifier 0 = subtle flicker, 1 = heavy blackout pulses.
     */
    private void applyDarkness(Player player, int amplifier) {
        PotionEffect current = player.getPotionEffect(PotionEffectType.DARKNESS);
        if (current != null && current.getDuration() > 20 && current.getAmplifier() >= amplifier) return;
        player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, amplifier, true, false, false));
    }

    private void applyEffect(Player player, PotionEffectType type, int amplifier, int duration) {
        player.addPotionEffect(new PotionEffect(type, duration, amplifier, true, false, false));
    }
}
