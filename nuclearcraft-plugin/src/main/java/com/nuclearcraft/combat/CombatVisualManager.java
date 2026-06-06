package com.nuclearcraft.combat;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;

/**
 * Manages all visual and audio effects for Phase 9 combat events.
 *
 * All methods are no-ops if their respective config flag is disabled.
 * Effects are designed to be lightweight and safe to call per-hit.
 */
public class CombatVisualManager {

    private final ConfigManager configManager;

    private boolean weaponHitParticles;
    private boolean comboParticles;
    private boolean surgeParticles;
    private boolean auraParticles;
    private boolean arrowImpactParticles;
    private int hitCount;
    private int comboCount;
    private int surgeCount;
    private int auraCount;

    public CombatVisualManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void initialize() {
        loadConfig();
        NCLogger.info("CombatVisualManager initialized.");
    }

    public void shutdown() {}

    private void loadConfig() {
        var cfg = configManager.getCombat();
        weaponHitParticles  = cfg.getBoolean("visuals.weapon-hit-particles", true);
        comboParticles      = cfg.getBoolean("visuals.combo-particles", true);
        surgeParticles      = cfg.getBoolean("visuals.surge-particles", true);
        auraParticles       = cfg.getBoolean("visuals.aura-particles", true);
        arrowImpactParticles= cfg.getBoolean("visuals.arrow-impact-particles", true);
        hitCount    = cfg.getInt("visuals.hit-particle-count", 12);
        comboCount  = cfg.getInt("visuals.combo-particle-count", 20);
        surgeCount  = cfg.getInt("visuals.surge-particle-count", 60);
        auraCount   = cfg.getInt("visuals.aura-particle-count", 5);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Weapon hit
    // ──────────────────────────────────────────────────────────────────────────

    /** Spawns a small green dust burst at the hit location. */
    public void spawnWeaponHitEffect(Location loc) {
        if (!weaponHitParticles) return;
        World w = loc.getWorld();
        if (w == null) return;
        w.spawnParticle(Particle.DUST, loc.clone().add(0, 1, 0),
                hitCount, 0.3, 0.3, 0.3, 0,
                new Particle.DustOptions(Color.LIME, 1.4f));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Combo escalation
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Spawns an escalating ring of particles when a combo hit lands.
     *
     * @param loc        location of the effect
     * @param comboLevel current combo count (≥ 2)
     */
    public void spawnComboEffect(Location loc, int comboLevel) {
        if (!comboParticles) return;
        World w = loc.getWorld();
        if (w == null) return;

        int count = Math.min(comboCount + (comboLevel - 2) * 4, 50);
        float size = Math.min(1.0f + comboLevel * 0.2f, 2.5f);

        w.spawnParticle(Particle.DUST, loc.clone().add(0, 1, 0),
                count, 0.5, 0.5, 0.5, 0,
                new Particle.DustOptions(Color.fromRGB(0, 200, 80), size));
        w.spawnParticle(Particle.CRIT, loc.clone().add(0, 1, 0),
                comboLevel * 2, 0.3, 0.3, 0.3, 0.1);

        if (comboLevel >= 4) {
            w.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, 0.6f, 1.5f + comboLevel * 0.1f);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Radiation surge
    // ──────────────────────────────────────────────────────────────────────────

    /** Spawns the large green explosion for a radiation surge event. */
    public void spawnSurgeEffect(Location loc) {
        if (!surgeParticles) return;
        World w = loc.getWorld();
        if (w == null) return;

        w.spawnParticle(Particle.DUST, loc.clone().add(0, 1, 0),
                surgeCount, 2.0, 2.0, 2.0, 0,
                new Particle.DustOptions(Color.LIME, 2.5f));
        w.spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1, 0, 0, 0, 0);
        w.spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, 1, 0),
                20, 1.0, 1.0, 1.0, 0.05);

        w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.5f);
        w.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.6f);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Aura
    // ──────────────────────────────────────────────────────────────────────────

    /** Spawns subtle green aura particles around the aura source. */
    public void spawnAuraEffect(Location loc) {
        if (!auraParticles) return;
        World w = loc.getWorld();
        if (w == null) return;
        w.spawnParticle(Particle.DUST, loc.clone().add(0, 1, 0),
                auraCount, 0.8, 0.8, 0.8, 0,
                new Particle.DustOptions(Color.fromRGB(0, 255, 80), 1.2f));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Arrow impact
    // ──────────────────────────────────────────────────────────────────────────

    /** Spawns the arrow impact dust + explosion effect. */
    public void spawnArrowImpactEffect(Location loc) {
        if (!arrowImpactParticles) return;
        World w = loc.getWorld();
        if (w == null) return;
        w.spawnParticle(Particle.DUST, loc, 25, 0.5, 0.5, 0.5, 0,
                new Particle.DustOptions(Color.LIME, 1.6f));
        w.spawnParticle(Particle.EXPLOSION, loc, 1, 0, 0, 0, 0);
        w.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_HIT, 1.0f, 0.7f);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Critical radiation explosion
    // ──────────────────────────────────────────────────────────────────────────

    /** Spawns a larger burst for a critical radiation hit. */
    public void spawnCriticalRadiationEffect(Location loc) {
        if (!weaponHitParticles) return;
        World w = loc.getWorld();
        if (w == null) return;
        w.spawnParticle(Particle.DUST, loc.clone().add(0, 1, 0),
                35, 0.7, 0.7, 0.7, 0,
                new Particle.DustOptions(Color.LIME, 2.2f));
        w.spawnParticle(Particle.EXPLOSION, loc, 1, 0, 0, 0, 0);
        w.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 1.8f);
    }
}
