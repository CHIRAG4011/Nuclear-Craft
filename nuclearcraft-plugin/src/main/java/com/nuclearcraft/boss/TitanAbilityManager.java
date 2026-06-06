package com.nuclearcraft.boss;

import com.nuclearcraft.boss.events.TitanAbilityEvent;
import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.data.PlayerData;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.radiation.RadiationManager;
import com.nuclearcraft.radiation.RadiationSource;
import com.nuclearcraft.utils.ColorUtil;
import com.nuclearcraft.utils.NCLogger;
import com.nuclearcraft.zombies.IrradiatedZombieManager;
import com.nuclearcraft.zombies.ZombieSpawnManager;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Giant;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Implements all Titan special abilities.
 *
 * Phase 1: Titan Slam, Radiation Aura (passive), Heavy Melee (via TitanCombatManager)
 * Phase 2: Radiation Wave, Mutant Summoning
 * Phase 3: Reactor Overload, Energy Beam
 * Phase 4: Nuclear Catastrophe, Final Frenzy
 */
public class TitanAbilityManager {

    private final JavaPlugin          plugin;
    private final ConfigManager       configManager;
    private final RadiationManager    radiationManager;
    private final PlayerDataManager   playerDataManager;
    private final TitanPhaseManager   phaseManager;
    private final IrradiatedZombieManager zombieManager;
    private final ZombieSpawnManager  zombieSpawnManager;

    private Giant      titan;
    private BukkitTask abilityLoopTask;
    private boolean    finalFrenzyActive = false;

    // Cooldown tracking (in ms)
    private final Map<String, Long> cooldowns = new HashMap<>();

    // Config
    private long   slamCooldownMs           = 10_000L;
    private double slamRadius               = 5.0;
    private double slamDamage               = 12.0;
    private long   waveCooldownMs           = 15_000L;
    private double waveRadius               = 20.0;
    private double waveRadiation            = 40.0;
    private long   summonCooldownMs         = 25_000L;
    private int    summonCount              = 3;
    private long   overloadCooldownMs       = 40_000L;
    private long   beamCooldownMs           = 20_000L;
    private double beamDamage               = 25.0;
    private double beamRadiation            = 60.0;
    private long   catastropheCooldownMs    = 90_000L;
    private double catastropheRadius        = 30.0;
    private double catastropheRadiation     = 80.0;
    private double catastropheMinSurviveHp  = 1.0;

    public TitanAbilityManager(JavaPlugin plugin, ConfigManager configManager,
                                RadiationManager radiationManager,
                                PlayerDataManager playerDataManager,
                                TitanPhaseManager phaseManager,
                                IrradiatedZombieManager zombieManager,
                                ZombieSpawnManager zombieSpawnManager) {
        this.plugin            = plugin;
        this.configManager     = configManager;
        this.radiationManager  = radiationManager;
        this.playerDataManager = playerDataManager;
        this.phaseManager      = phaseManager;
        this.zombieManager     = zombieManager;
        this.zombieSpawnManager = zombieSpawnManager;
    }

    public void initialize() {
        loadConfig();
        NCLogger.debug("TitanAbilityManager initialized.");
    }

    public void startAbilities(Giant titan) {
        this.titan = titan;
        stopAbilities();
        abilityLoopTask = plugin.getServer().getScheduler().runTaskTimer(
                plugin, this::abilityTick, 40L, 20L);
    }

    public void stopAbilities() {
        if (abilityLoopTask != null) { abilityLoopTask.cancel(); abilityLoopTask = null; }
        finalFrenzyActive = false;
    }

    public void shutdown() {
        stopAbilities();
    }

    // ── Ability loop ──────────────────────────────────────────────────────────

    private void abilityTick() {
        if (titan == null || titan.isDead() || !titan.isValid()) return;
        TitanPhase phase = phaseManager.getCurrentPhase();
        long now = System.currentTimeMillis();

        // Phase 1 abilities
        tryActivate("slam", slamCooldownMs, now, this::titanSlam);

        // Phase 2+ abilities
        if (phase.ordinal() >= TitanPhase.PHASE_2.ordinal()) {
            tryActivate("wave", waveCooldownMs, now, this::radiationWave);
            tryActivate("summon", summonCooldownMs, now, this::mutantSummoning);
        }

        // Phase 3+ abilities
        if (phase.ordinal() >= TitanPhase.PHASE_3.ordinal()) {
            tryActivate("overload", overloadCooldownMs, now, this::reactorOverload);
            tryActivate("beam", beamCooldownMs, now, this::energyBeam);
        }

        // Phase 4 abilities
        if (phase == TitanPhase.PHASE_4) {
            tryActivate("catastrophe", catastropheCooldownMs, now, this::nuclearCatastrophe);
            if (!finalFrenzyActive) activateFinalFrenzy();
        }
    }

    private void tryActivate(String key, long cooldownMs, long now, Runnable ability) {
        long lastUsed = cooldowns.getOrDefault(key, 0L);
        // Apply frenzy cooldown reduction in phase 4
        long effective = finalFrenzyActive ? (long)(cooldownMs * 0.6) : cooldownMs;
        if (now - lastUsed >= effective) {
            cooldowns.put(key, now);
            ability.run();
        }
    }

    // ── Phase 1: Titan Slam ───────────────────────────────────────────────────

    private void titanSlam() {
        if (titan == null) return;
        Location loc = titan.getLocation();
        fireAbilityEvent(TitanAbilityEvent.Ability.TITAN_SLAM);

        // Visual warning
        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 5, 1.0, 0.5, 1.0, 0.0);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.6f);

        // Damage and knockback after 0.5s delay
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (titan == null || titan.isDead()) return;
            for (Player player : getPlayersInRadius(slamRadius)) {
                player.damage(slamDamage * phaseManager.getDamageMultiplier(), titan);
                radiationManager.addRadiation(player, 15, RadiationSource.BOSS_ATTACK);
                Vector kb = player.getLocation().toVector()
                        .subtract(titan.getLocation().toVector()).normalize()
                        .multiply(2.5).setY(0.8);
                player.setVelocity(kb);
                PlayerData pd = playerDataManager.get(player.getUniqueId()).orElse(null);
                if (pd != null) pd.incrementTitanDamageTaken((int)(slamDamage * phaseManager.getDamageMultiplier()));
            }
            loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 3, slamRadius * 0.3, 0.3, slamRadius * 0.3, 0.0);
            loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 0.5f);
        }, 10L);
    }

    // ── Phase 2: Radiation Wave ───────────────────────────────────────────────

    private void radiationWave() {
        if (titan == null) return;
        Location center = titan.getLocation();
        fireAbilityEvent(TitanAbilityEvent.Ability.RADIATION_WAVE);
        phaseManager.updateWarningTitle("RADIATION WAVE");

        broadcastNearby("<yellow>☢ The Titan charges a Radiation Wave — MOVE AWAY!</yellow>");

        // Expanding ring animation + damage
        final int[] tick = {0};
        plugin.getServer().getScheduler().runTaskTimer(plugin, task -> {
            if (titan == null || titan.isDead() || !titan.isValid()) { task.cancel(); return; }
            double radius = (tick[0] / 20.0) * waveRadius;
            spawnRingParticles(center, radius);
            if (tick[0] == 20) {
                for (Player player : getPlayersInRadius(waveRadius)) {
                    radiationManager.addRadiation(player, (int) waveRadiation, RadiationSource.BOSS_ATTACK);
                    PlayerData pd = playerDataManager.get(player.getUniqueId()).orElse(null);
                    if (pd != null) pd.incrementTitanDamageTaken((int) waveRadiation);
                }
                center.getWorld().playSound(center, Sound.ENTITY_WITHER_SHOOT, 2.0f, 0.5f);
                task.cancel();
            }
            tick[0]++;
        }, 0L, 1L);
    }

    private void spawnRingParticles(Location center, double radius) {
        int count = (int)(radius * 4);
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI * i) / count;
            double x = center.getX() + Math.cos(angle) * radius;
            double z = center.getZ() + Math.sin(angle) * radius;
            Location ring = new Location(center.getWorld(), x, center.getY() + 1, z);
            center.getWorld().spawnParticle(Particle.ENTITY_EFFECT, ring, 2, 0, 0.5, 0, 0.05);
            center.getWorld().spawnParticle(Particle.ENCHANTED_HIT, ring, 1, 0, 0.3, 0, 0.0);
        }
    }

    // ── Phase 2: Mutant Summoning ─────────────────────────────────────────────

    private void mutantSummoning() {
        if (titan == null) return;
        Location loc = titan.getLocation();
        fireAbilityEvent(TitanAbilityEvent.Ability.MUTANT_SUMMONING);

        loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_AMBIENT, 2.0f, 0.5f);
        loc.getWorld().spawnParticle(Particle.PORTAL, loc.clone().add(0, 2, 0),
                30, 2.0, 1.0, 2.0, 0.2);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (titan == null || titan.isDead()) return;
            var cfg       = configManager.getTitan();
            int alphaCount = cfg.getInt("titan.abilities.mutant-summon.alpha-count", 1);
            int regCount   = cfg.getInt("titan.abilities.mutant-summon.regular-count", summonCount);
            for (int i = 0; i < regCount; i++) zombieSpawnManager.spawnIrradiatedZombie(randomNear(loc, 6));
            for (int i = 0; i < alphaCount; i++) zombieSpawnManager.spawnAlphaZombie(randomNear(loc, 6));
        }, 20L);
    }

    // ── Phase 3: Reactor Overload ─────────────────────────────────────────────

    private void reactorOverload() {
        if (titan == null) return;
        Location loc = titan.getLocation();
        fireAbilityEvent(TitanAbilityEvent.Ability.REACTOR_OVERLOAD);
        phaseManager.updateWarningTitle("REACTOR OVERLOAD — MOVE AWAY!");

        broadcastNearby("<red>☢ REACTOR OVERLOAD — The Titan is charging! MOVE AWAY!</red>");

        // 5-second charge-up with visual
        final int[] seconds = {5};
        plugin.getServer().getScheduler().runTaskTimer(plugin, chargeTask -> {
            if (titan == null || titan.isDead()) { chargeTask.cancel(); return; }
            loc.getWorld().spawnParticle(Particle.FLASH, loc.clone().add(0, 2, 0),
                    1, 0, 0, 0, 0);
            loc.getWorld().spawnParticle(Particle.ENTITY_EFFECT, loc.clone().add(0, 2, 0),
                    20, 1.5, 1.0, 1.5, 0.05);
            loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.5f + (seconds[0] * 0.1f));
            seconds[0]--;
            if (seconds[0] <= 0) {
                chargeTask.cancel();
                detonateOverload(loc);
            }
        }, 0L, 20L);
    }

    private void detonateOverload(Location loc) {
        if (titan == null || titan.isDead()) return;
        double radius = configManager.getTitan().getDouble("titan.abilities.reactor-overload.radius", 18.0);
        double rad    = configManager.getTitan().getDouble("titan.abilities.reactor-overload.radiation", 70.0);

        loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 10, radius * 0.3, 1.0, radius * 0.3, 0.0);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 4.0f, 0.4f);

        for (Player player : getPlayersInRadius(radius)) {
            radiationManager.addRadiation(player, (int) rad, RadiationSource.BOSS_ATTACK);
            PlayerData pd = playerDataManager.get(player.getUniqueId()).orElse(null);
            if (pd != null) pd.incrementTitanDamageTaken((int) rad);
        }
    }

    // ── Phase 3: Energy Beam ──────────────────────────────────────────────────

    private void energyBeam() {
        if (titan == null) return;
        World world = titan.getWorld();
        if (world == null) return;

        // Pick random nearby player
        List<Player> candidates = getPlayersInRadius(60.0);
        if (candidates.isEmpty()) return;
        Player target = candidates.get(new Random().nextInt(candidates.size()));

        fireAbilityEvent(TitanAbilityEvent.Ability.ENERGY_BEAM);
        phaseManager.updateWarningTitle("ENERGY BEAM INCOMING!");
        target.sendMessage(ColorUtil.parse("<dark_red>☢ THE TITAN IS TARGETING YOU WITH AN ENERGY BEAM!</dark_red>"));

        // 3-second lock-on warning
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (titan == null || titan.isDead() || target.isDead()) return;

            Location src  = titan.getLocation().clone().add(0, 4, 0);
            Location dest = target.getLocation();

            // Trace beam particles
            Vector dir    = dest.toVector().subtract(src.toVector()).normalize();
            double length = src.distance(dest);
            for (double d = 0; d < length; d += 0.5) {
                Location beamLoc = src.clone().add(dir.clone().multiply(d));
                world.spawnParticle(Particle.ENCHANTED_HIT, beamLoc, 2, 0.1, 0.1, 0.1, 0.0);
                world.spawnParticle(Particle.ENTITY_EFFECT,  beamLoc, 1, 0.0, 0.0, 0.0, 0.05);
            }

            // Scorch the ground
            world.spawnParticle(Particle.LAVA, dest, 5, 0.5, 0.1, 0.5, 0.0);
            world.playSound(dest, Sound.ENTITY_BLAZE_SHOOT, 2.0f, 0.5f);

            // Damage
            target.damage(beamDamage * phaseManager.getDamageMultiplier(), titan);
            radiationManager.addRadiation(target, (int) beamRadiation, RadiationSource.BOSS_ATTACK);
            PlayerData pd = playerDataManager.get(target.getUniqueId()).orElse(null);
            if (pd != null) pd.incrementTitanDamageTaken((int)(beamDamage + beamRadiation));
        }, 60L);
    }

    // ── Phase 4: Nuclear Catastrophe ─────────────────────────────────────────

    private void nuclearCatastrophe() {
        if (titan == null) return;
        Location loc = titan.getLocation();
        fireAbilityEvent(TitanAbilityEvent.Ability.NUCLEAR_CATASTROPHE);
        phaseManager.updateWarningTitle("NUCLEAR CATASTROPHE!");

        broadcastNearby("<dark_red>☢ NUCLEAR CATASTROPHE — Take cover! ☢</dark_red>");

        // 10-second countdown with darkening effects
        final int[] seconds = {10};
        plugin.getServer().getScheduler().runTaskTimer(plugin, chargeTask -> {
            if (titan == null || titan.isDead()) { chargeTask.cancel(); return; }

            // Growing visual
            double radius = (10 - seconds[0]) * 3.0;
            spawnRingParticles(loc, radius);
            loc.getWorld().spawnParticle(Particle.ENTITY_EFFECT, loc.clone().add(0, 4, 0),
                    30, 2.0, 1.5, 2.0, 0.08);

            if (seconds[0] == 5) {
                for (Player p : getPlayersInRadius(catastropheRadius + 10)) {
                    p.sendMessage(ColorUtil.parse("<dark_red>☢ 5 SECONDS — TAKE COVER!</dark_red>"));
                }
            }
            loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_AMBIENT, 2.0f, 0.3f + (seconds[0] * 0.07f));

            seconds[0]--;
            if (seconds[0] <= 0) {
                chargeTask.cancel();
                detonateCatastrophe(loc);
            }
        }, 0L, 20L);
    }

    private void detonateCatastrophe(Location loc) {
        if (titan == null || titan.isDead()) return;

        loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 20,
                catastropheRadius * 0.4, 2.0, catastropheRadius * 0.4, 0.0);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 5.0f, 0.3f);
        loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_DEATH, 3.0f, 0.5f);

        for (Player player : getPlayersInRadius(catastropheRadius)) {
            double newHp = Math.max(catastropheMinSurviveHp, player.getHealth() - 15.0);
            player.setHealth(newHp);
            radiationManager.addRadiation(player, (int) catastropheRadiation, RadiationSource.BOSS_ATTACK);

            PlayerData pd = playerDataManager.get(player.getUniqueId()).orElse(null);
            if (pd != null) {
                pd.incrementTitanDamageTaken((int)(15.0 + catastropheRadiation));
                pd.incrementCatastrophesSurvived();
                pd.markDirty();
            }
            player.sendMessage(ColorUtil.parse("<dark_red>You survived the Nuclear Catastrophe!</dark_red>"));
        }
    }

    // ── Phase 4: Final Frenzy ─────────────────────────────────────────────────

    private void activateFinalFrenzy() {
        if (titan == null || finalFrenzyActive) return;
        finalFrenzyActive = true;
        fireAbilityEvent(TitanAbilityEvent.Ability.FINAL_FRENZY);
        broadcastNearby("<dark_red>☢ FINAL FRENZY — The Titan has gone berserk! ☢</dark_red>");

        // Speed boost handled by TitanPhaseManager via PHASE_4 transition
        // We add an additional attack-speed boost here
        var attrSpd = titan.getAttribute(Attribute.ATTACK_SPEED);
        if (attrSpd != null) {
            attrSpd.setBaseValue(attrSpd.getBaseValue() * 1.5);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<Player> getPlayersInRadius(double radius) {
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

    private Location randomNear(Location center, double spread) {
        Random rng = new Random();
        double angle  = rng.nextDouble() * Math.PI * 2;
        double dist   = spread * (0.5 + rng.nextDouble() * 0.5);
        return center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
    }

    private void broadcastNearby(String miniMessage) {
        if (titan == null || titan.getWorld() == null) return;
        for (Player p : titan.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(titan.getLocation()) <= 100 * 100) {
                p.sendMessage(ColorUtil.parse(miniMessage));
            }
        }
    }

    private void fireAbilityEvent(TitanAbilityEvent.Ability ability) {
        if (titan != null) {
            plugin.getServer().getPluginManager().callEvent(
                    new TitanAbilityEvent(titan, ability));
        }
    }

    private void loadConfig() {
        var cfg = configManager.getTitan();
        slamCooldownMs        = cfg.getLong("titan.abilities.titan-slam.cooldown-ms",     10_000L);
        slamRadius            = cfg.getDouble("titan.abilities.titan-slam.radius",          5.0);
        slamDamage            = cfg.getDouble("titan.abilities.titan-slam.damage",          12.0);
        waveCooldownMs        = cfg.getLong("titan.abilities.radiation-wave.cooldown-ms",  15_000L);
        waveRadius            = cfg.getDouble("titan.abilities.radiation-wave.radius",      20.0);
        waveRadiation         = cfg.getDouble("titan.abilities.radiation-wave.radiation",   40.0);
        summonCooldownMs      = cfg.getLong("titan.abilities.mutant-summon.cooldown-ms",   25_000L);
        summonCount           = cfg.getInt("titan.abilities.mutant-summon.regular-count",   3);
        overloadCooldownMs    = cfg.getLong("titan.abilities.reactor-overload.cooldown-ms", 40_000L);
        beamCooldownMs        = cfg.getLong("titan.abilities.energy-beam.cooldown-ms",     20_000L);
        beamDamage            = cfg.getDouble("titan.abilities.energy-beam.damage",         25.0);
        beamRadiation         = cfg.getDouble("titan.abilities.energy-beam.radiation",      60.0);
        catastropheCooldownMs = cfg.getLong("titan.abilities.nuclear-catastrophe.cooldown-ms", 90_000L);
        catastropheRadius     = cfg.getDouble("titan.abilities.nuclear-catastrophe.radius", 30.0);
        catastropheRadiation  = cfg.getDouble("titan.abilities.nuclear-catastrophe.radiation", 80.0);
        catastropheMinSurviveHp = cfg.getDouble("titan.abilities.nuclear-catastrophe.min-survive-hp", 1.0);
    }
}
