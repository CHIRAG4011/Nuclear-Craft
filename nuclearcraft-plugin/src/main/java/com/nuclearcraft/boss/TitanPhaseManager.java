package com.nuclearcraft.boss;

import com.nuclearcraft.boss.events.TitanPhaseChangeEvent;
import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.utils.ColorUtil;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Giant;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;

/**
 * Tracks the Titan's health percentage and manages phase transitions.
 * Each phase unlocks new abilities and may modify combat stats.
 */
public class TitanPhaseManager {

    private final JavaPlugin     plugin;
    private final ConfigManager  configManager;

    private TitanPhase currentPhase = TitanPhase.PHASE_1;
    private BossBar    bossBar;
    private Giant      titan;

    // Per-phase multipliers (loaded from config)
    private double[] speedMultipliers  = {1.0, 1.1, 1.25, 1.5};
    private double[] damageMultipliers = {1.0, 1.2, 1.5,  2.0};

    public TitanPhaseManager(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin        = plugin;
        this.configManager = configManager;
    }

    public void initialize() {
        loadMultipliers();
        NCLogger.debug("TitanPhaseManager initialized.");
    }

    public void shutdown() {
        destroyBossBar();
    }

    // ── Boss bar ──────────────────────────────────────────────────────────────

    public void createBossBar(Giant titan) {
        this.titan       = titan;
        this.currentPhase = TitanPhase.PHASE_1;
        destroyBossBar();
        bossBar = plugin.getServer().createBossBar(
                "☢ PLUTONIUM TITAN ☢  [Phase I]",
                BarColor.GREEN,
                BarStyle.SEGMENTED_10);
        bossBar.setProgress(1.0);
    }

    public void destroyBossBar() {
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
    }

    public void addPlayerToBossBar(Player player) {
        if (bossBar != null) bossBar.addPlayer(player);
    }

    public void removePlayerFromBossBar(Player player) {
        if (bossBar != null) bossBar.removePlayer(player);
    }

    public void addAllPlayers(Collection<? extends Player> players) {
        if (bossBar == null) return;
        players.forEach(bossBar::addPlayer);
    }

    // ── Tick update ───────────────────────────────────────────────────────────

    /**
     * Called every ~5 ticks to update the boss bar and check for phase transitions.
     * @return true if the phase changed this tick.
     */
    public boolean tick() {
        if (titan == null || titan.isDead() || bossBar == null) return false;

        double maxHp  = getMaxHp();
        double curHp  = titan.getHealth();
        double pct    = maxHp > 0 ? curHp / maxHp : 0.0;

        updateBossBar(pct);

        TitanPhase needed = TitanPhase.fromHealthPercent(pct);
        if (needed != currentPhase) {
            transitionTo(needed);
            return true;
        }
        return false;
    }

    public void updateWarningTitle(String message) {
        if (bossBar == null) return;
        bossBar.setTitle("☢ " + message + " ☢");
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (bossBar != null && titan != null && !titan.isDead()) {
                bossBar.setTitle("☢ PLUTONIUM TITAN ☢  [" + currentPhase.getDisplayName() + "]");
            }
        }, 60L);
    }

    // ── Phase transitions ─────────────────────────────────────────────────────

    private void transitionTo(TitanPhase newPhase) {
        TitanPhase old = currentPhase;
        currentPhase   = newPhase;

        NCLogger.info("Titan phase transition: " + old + " → " + newPhase);

        updateBossBarColor(newPhase);
        applyPhaseModifiers(newPhase);

        // Fire API event
        plugin.getServer().getPluginManager().callEvent(
                new TitanPhaseChangeEvent(titan, old, newPhase));

        // Announce to nearby players
        String message = switch (newPhase) {
            case PHASE_2 -> "<yellow>☢ The Titan grows more powerful! Phase II!</yellow>";
            case PHASE_3 -> "<red>☢ Critical phase — The Titan RAGES! Phase III!</red>";
            case PHASE_4 -> "<dark_red>☢ FINAL PHASE — NUCLEAR CATASTROPHE IMMINENT! ☢</dark_red>";
            default      -> "";
        };
        if (!message.isEmpty() && titan.getWorld() != null) {
            for (Player p : titan.getWorld().getPlayers()) {
                if (p.getLocation().distanceSquared(titan.getLocation()) <= 160 * 160) {
                    p.sendMessage(ColorUtil.parse(message));
                }
            }
        }
    }

    private void applyPhaseModifiers(TitanPhase phase) {
        if (titan == null || titan.isDead()) return;
        int idx = phase.ordinal();
        double speedMul = speedMultipliers[Math.min(idx, speedMultipliers.length - 1)];
        var speedAttr = titan.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) {
            double base = configManager.getTitan().getDouble("titan.stats.speed", 0.22);
            speedAttr.setBaseValue(base * speedMul);
        }
    }

    private void updateBossBar(double pct) {
        if (bossBar == null) return;
        bossBar.setProgress(Math.max(0.0, Math.min(1.0, pct)));
        bossBar.setTitle("☢ PLUTONIUM TITAN ☢  [" + currentPhase.getDisplayName() + "]"
                + "  " + (int)(pct * 100) + "%");
    }

    private void updateBossBarColor(TitanPhase phase) {
        if (bossBar == null) return;
        bossBar.setColor(switch (phase) {
            case PHASE_1 -> BarColor.GREEN;
            case PHASE_2 -> BarColor.YELLOW;
            case PHASE_3 -> BarColor.RED;
            case PHASE_4 -> BarColor.PURPLE;
        });
    }

    // ── Config ────────────────────────────────────────────────────────────────

    private void loadMultipliers() {
        var cfg = configManager.getTitan();
        speedMultipliers  = new double[]{
            cfg.getDouble("titan.phases.phase-1.speed-multiplier",  1.00),
            cfg.getDouble("titan.phases.phase-2.speed-multiplier",  1.10),
            cfg.getDouble("titan.phases.phase-3.speed-multiplier",  1.25),
            cfg.getDouble("titan.phases.phase-4.speed-multiplier",  1.50)
        };
        damageMultipliers = new double[]{
            cfg.getDouble("titan.phases.phase-1.damage-multiplier", 1.00),
            cfg.getDouble("titan.phases.phase-2.damage-multiplier", 1.20),
            cfg.getDouble("titan.phases.phase-3.damage-multiplier", 1.50),
            cfg.getDouble("titan.phases.phase-4.damage-multiplier", 2.00)
        };
    }

    private double getMaxHp() {
        if (titan == null) return 1.0;
        var attr = titan.getAttribute(Attribute.MAX_HEALTH);
        return attr != null ? attr.getValue() : 5000.0;
    }

    public TitanPhase getCurrentPhase() { return currentPhase; }

    public double getDamageMultiplier() {
        return damageMultipliers[currentPhase.ordinal()];
    }

    public BossBar getBossBar() { return bossBar; }

    /** Admin command: force-set the titan to a specific phase (1–4). */
    public void forcePhase(int phaseNumber) {
        TitanPhase target = switch (phaseNumber) {
            case 1  -> TitanPhase.PHASE_1;
            case 2  -> TitanPhase.PHASE_2;
            case 3  -> TitanPhase.PHASE_3;
            default -> TitanPhase.PHASE_4;
        };
        if (target != currentPhase) {
            transitionTo(target);
        }
    }
}
