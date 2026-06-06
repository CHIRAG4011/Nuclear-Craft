package com.nuclearcraft.boss;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.data.PlayerData;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.radiation.RadiationManager;
import com.nuclearcraft.radiation.RadiationSource;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.attribute.Attribute;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Giant;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Handles melee attacks, the constant radiation aura, and incoming/outgoing
 * damage scaling for the Plutonium Titan.
 */
public class TitanCombatManager {

    private final JavaPlugin        plugin;
    private final ConfigManager     configManager;
    private final RadiationManager  radiationManager;
    private final PlayerDataManager playerDataManager;
    private final TitanPhaseManager phaseManager;
    private final TitanDamageTracker damageTracker;

    private Giant      titan;
    private BukkitTask auraTick;
    private BukkitTask meleeTick;

    // Aura config
    private double auraRadius    = 10.0;
    private double auraRadiation = 3.0;
    private long   auraInterval  = 20L;

    // Melee config
    private double meleeDamage    = 18.0;
    private double meleeRange     = 3.5;
    private long   meleeInterval  = 24L;
    private long   lastMeleeMs    = 0L;

    public TitanCombatManager(JavaPlugin plugin, ConfigManager configManager,
                               RadiationManager radiationManager,
                               PlayerDataManager playerDataManager,
                               TitanPhaseManager phaseManager,
                               TitanDamageTracker damageTracker) {
        this.plugin           = plugin;
        this.configManager    = configManager;
        this.radiationManager = radiationManager;
        this.playerDataManager = playerDataManager;
        this.phaseManager     = phaseManager;
        this.damageTracker    = damageTracker;
    }

    public void initialize() {
        loadConfig();
        NCLogger.debug("TitanCombatManager initialized.");
    }

    public void startCombat(Giant titan) {
        this.titan = titan;
        stopCombat();
        startAuraTask();
        startMeleeTask();
    }

    public void stopCombat() {
        if (auraTick  != null) { auraTick.cancel();  auraTick  = null; }
        if (meleeTick != null) { meleeTick.cancel(); meleeTick = null; }
    }

    public void shutdown() {
        stopCombat();
    }

    // ── Radiation Aura ────────────────────────────────────────────────────────

    private void startAuraTask() {
        auraTick = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (titan == null || titan.isDead() || !titan.isValid()) return;
            Location loc = titan.getLocation();

            // Emit aura particles
            titan.getWorld().spawnParticle(Particle.ENTITY_EFFECT, loc.clone().add(0, 3, 0),
                    15, auraRadius * 0.4, 1.0, auraRadius * 0.4, 0.02);

            // Apply radiation to nearby players
            for (Player player : getNearbyPlayers(auraRadius)) {
                double exposure = computeAuraExposure(player);
                radiationManager.addRadiation(player, (int) exposure, RadiationSource.BOSS_ATTACK);
                PlayerData pd = playerDataManager.get(player.getUniqueId()).orElse(null);
                if (pd != null) {
                    pd.incrementTitanDamageTaken((int) exposure);
                }
            }
        }, 0L, auraInterval);
    }

    private double computeAuraExposure(Player player) {
        double dist    = player.getLocation().distance(titan.getLocation());
        double falloff = Math.max(0.1, 1.0 - (dist / auraRadius) * 0.6);
        return auraRadiation * falloff * phaseManager.getDamageMultiplier();
    }

    // ── Melee Attack ──────────────────────────────────────────────────────────

    private void startMeleeTask() {
        meleeTick = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (titan == null || titan.isDead() || !titan.isValid()) return;
            long now = System.currentTimeMillis();
            if (now - lastMeleeMs < meleeInterval * 50L) return;

            for (Player player : getNearbyPlayers(meleeRange)) {
                performMeleeAttack(player);
                lastMeleeMs = now;
                break; // one target per swing
            }
        }, 0L, 4L);
    }

    private void performMeleeAttack(Player player) {
        double dmg = meleeDamage * phaseManager.getDamageMultiplier();
        player.damage(dmg, titan);
        radiationManager.addRadiation(player, 8, RadiationSource.BOSS_ATTACK);

        PlayerData pd = playerDataManager.get(player.getUniqueId()).orElse(null);
        if (pd != null) pd.incrementTitanDamageTaken((int) dmg);

        Location loc = player.getLocation();
        loc.getWorld().spawnParticle(Particle.CRIT, loc, 10, 0.3, 0.3, 0.3, 0.1);
        loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.5f, 0.7f);
    }

    // ── Incoming damage handling ──────────────────────────────────────────────

    /**
     * Called when a player deals damage to the Titan.
     * Records contribution and applies resistance.
     */
    public void handleIncomingDamage(Player attacker, double rawDamage) {
        damageTracker.recordDamage(attacker, rawDamage);
        PlayerData pd = playerDataManager.get(attacker.getUniqueId()).orElse(null);
        if (pd != null) pd.incrementTitanDamageDealt((int) rawDamage);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<Player> getNearbyPlayers(double radius) {
        if (titan == null || titan.getWorld() == null) return List.of();
        Location loc = titan.getLocation();
        List<Player> result = new ArrayList<>();
        for (Player p : titan.getWorld().getPlayers()) {
            if (!p.isDead() && p.getGameMode() != org.bukkit.GameMode.SPECTATOR
                    && p.getLocation().distanceSquared(loc) <= radius * radius) {
                result.add(p);
            }
        }
        return result;
    }

    private void loadConfig() {
        var cfg       = configManager.getTitan();
        auraRadius    = cfg.getDouble("titan.abilities.radiation-aura.radius",    10.0);
        auraRadiation = cfg.getDouble("titan.abilities.radiation-aura.radiation", 3.0);
        auraInterval  = cfg.getLong("titan.abilities.radiation-aura.interval-ticks", 20L);
        meleeDamage   = cfg.getDouble("titan.stats.damage",   18.0);
        meleeRange    = cfg.getDouble("titan.stats.melee-range", 3.5);
        meleeInterval = cfg.getLong("titan.stats.melee-interval-ticks", 24L);
    }

    public double getMaxHealth(int playerCount) {
        var cfg = configManager.getTitan();
        if (playerCount <= 3)  return cfg.getDouble("titan.scaling.hp-1-3",  5000.0);
        if (playerCount <= 6)  return cfg.getDouble("titan.scaling.hp-4-6",  6500.0);
        if (playerCount <= 10) return cfg.getDouble("titan.scaling.hp-7-10", 8500.0);
        return cfg.getDouble("titan.scaling.hp-10plus", 10000.0);
    }
}
