package com.nuclearcraft.combat;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/**
 * Phase 9 Weapon Mastery System.
 *
 * Tracks sword, axe, and bow mastery XP per player (stored in PlayerData).
 * Mastery levels unlock passive combat bonuses applied by {@link RadiationCombatManager}.
 *
 * <pre>
 * Level       XP threshold  Bonus (extra radiation multiplier)
 * Novice      0             0%
 * Experienced 100           2%
 * Veteran     500           5%
 * Elite       1500          7%
 * Master      4000          10%
 * </pre>
 */
public class WeaponMasteryManager {

    public enum WeaponType { SWORD, AXE, BOW }

    public enum MasteryLevel {
        NOVICE("Novice",         0,    0.00),
        EXPERIENCED("Experienced", 100,  0.02),
        VETERAN("Veteran",       500,  0.05),
        ELITE("Elite",           1500, 0.07),
        MASTER("Master",         4000, 0.10);

        public final String displayName;
        public final int threshold;
        /** Additional radiation multiplier (0.10 = +10%). */
        public final double radiationBonus;

        MasteryLevel(String displayName, int threshold, double radiationBonus) {
            this.displayName    = displayName;
            this.threshold      = threshold;
            this.radiationBonus = radiationBonus;
        }
    }

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final PlayerDataManager playerDataManager;

    private boolean enabled;
    private int swordHitXp;
    private int swordKillXp;
    private int axeHitXp;
    private int axeKillXp;
    private int bowHitXp;
    private int bowKillXp;

    // Threshold overrides from config (fall back to enum defaults)
    private int[] thresholds = new int[MasteryLevel.values().length];

    public WeaponMasteryManager(JavaPlugin plugin,
                                 ConfigManager configManager,
                                 PlayerDataManager playerDataManager) {
        this.plugin             = plugin;
        this.configManager      = configManager;
        this.playerDataManager  = playerDataManager;
    }

    public void initialize() {
        loadConfig();
        NCLogger.info("WeaponMasteryManager initialized (enabled=" + enabled + ").");
    }

    public void shutdown() {}

    private void loadConfig() {
        var cfg = configManager.getCombat();
        enabled   = cfg.getBoolean("mastery.enabled", true);
        swordHitXp  = cfg.getInt("mastery.xp.sword-hit",  1);
        swordKillXp = cfg.getInt("mastery.xp.sword-kill", 10);
        axeHitXp    = cfg.getInt("mastery.xp.axe-hit",    1);
        axeKillXp   = cfg.getInt("mastery.xp.axe-kill",   10);
        bowHitXp    = cfg.getInt("mastery.xp.bow-hit",    2);
        bowKillXp   = cfg.getInt("mastery.xp.bow-kill",   15);

        MasteryLevel[] levels = MasteryLevel.values();
        for (int i = 0; i < levels.length; i++) {
            thresholds[i] = levels[i].threshold;
        }
        thresholds[1] = cfg.getInt("mastery.levels.experienced", 100);
        thresholds[2] = cfg.getInt("mastery.levels.veteran",     500);
        thresholds[3] = cfg.getInt("mastery.levels.elite",       1500);
        thresholds[4] = cfg.getInt("mastery.levels.master",      4000);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // XP recording
    // ──────────────────────────────────────────────────────────────────────────

    public void recordHit(Player player, WeaponType type) {
        if (!enabled) return;
        int xp = switch (type) {
            case SWORD -> swordHitXp;
            case AXE   -> axeHitXp;
            case BOW   -> bowHitXp;
        };
        addMasteryXp(player, type, xp);
    }

    public void recordKill(Player player, WeaponType type) {
        if (!enabled) return;
        int xp = switch (type) {
            case SWORD -> swordKillXp;
            case AXE   -> axeKillXp;
            case BOW   -> bowKillXp;
        };
        addMasteryXp(player, type, xp);
    }

    private void addMasteryXp(Player player, WeaponType type, int xp) {
        playerDataManager.get(player.getUniqueId()).ifPresent(data -> {
            switch (type) {
                case SWORD -> data.addSwordMasteryXp(xp);
                case AXE   -> data.addAxeMasteryXp(xp);
                case BOW   -> data.addBowMasteryXp(xp);
            }
            data.setDirty(true);
        });
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Level & bonus queries
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Returns the current {@link MasteryLevel} for the player and weapon type.
     */
    public MasteryLevel getMasteryLevel(Player player, WeaponType type) {
        int xp = getMasteryXp(player, type);
        MasteryLevel result = MasteryLevel.NOVICE;
        for (int i = 0; i < MasteryLevel.values().length; i++) {
            if (xp >= thresholds[i]) result = MasteryLevel.values()[i];
        }
        return result;
    }

    /**
     * Returns the radiation bonus multiplier for this player's mastery.
     * 0.0 = no bonus, 0.10 = +10%.
     */
    public double getMasteryBonus(Player player, WeaponType type) {
        return getMasteryLevel(player, type).radiationBonus;
    }

    /**
     * Shorthand to check if player has MASTER level for the given weapon.
     */
    public boolean isMaster(Player player, WeaponType type) {
        return getMasteryLevel(player, type) == MasteryLevel.MASTER;
    }

    public int getMasteryXp(Player player, WeaponType type) {
        return playerDataManager.get(player.getUniqueId()).map(data -> switch (type) {
            case SWORD -> data.getSwordMasteryXp();
            case AXE   -> data.getAxeMasteryXp();
            case BOW   -> data.getBowMasteryXp();
        }).orElse(0);
    }

    /**
     * Formatted summary for the /nc combat mastery command.
     */
    public String getSummary(Player player) {
        return String.format(
                "§6☢ Weapon Mastery (%s):\n" +
                "  §eSword: §f%s §7(%d XP)\n" +
                "  §eAxe:   §f%s §7(%d XP)\n" +
                "  §eBow:   §f%s §7(%d XP)",
                player.getName(),
                getMasteryLevel(player, WeaponType.SWORD).displayName,
                getMasteryXp(player, WeaponType.SWORD),
                getMasteryLevel(player, WeaponType.AXE).displayName,
                getMasteryXp(player, WeaponType.AXE),
                getMasteryLevel(player, WeaponType.BOW).displayName,
                getMasteryXp(player, WeaponType.BOW));
    }
}
