package com.nuclearcraft.boss;

import com.nuclearcraft.advancements.AdvancementManager;
import com.nuclearcraft.boss.events.TitanDeathEvent;
import com.nuclearcraft.boss.events.TitanSpawnEvent;
import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.data.PlayerData;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.radiation.RadiationManager;
import com.nuclearcraft.utils.ColorUtil;
import com.nuclearcraft.utils.NCLogger;
import com.nuclearcraft.zombies.IrradiatedZombieManager;
import com.nuclearcraft.zombies.ZombieSpawnManager;
import org.bukkit.attribute.Attribute;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Giant;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * Top-level orchestrator for the Plutonium Titan boss system.
 *
 * Owns and wires together:
 *   TitanPhaseManager, TitanAIManager, TitanCombatManager,
 *   TitanAbilityManager, TitanArenaManager, TitanRewardManager,
 *   TitanSummoningManager, TitanDamageTracker
 *
 * Entry points:
 *   spawnTitan(Location, Player)  – spawns the boss
 *   handleDamage(Giant, Player, double) – called by TitanListener
 *   handleDeath(Giant, Player)    – called by TitanListener
 */
public class TitanManager {

    // ── Core dependencies ─────────────────────────────────────────────────────
    private final JavaPlugin          plugin;
    private final ConfigManager       configManager;
    private final RadiationManager    radiationManager;
    private final PlayerDataManager   playerDataManager;
    private final ItemManager         itemManager;
    private final AdvancementManager  advancementManager;
    private final IrradiatedZombieManager zombieManager;
    private final ZombieSpawnManager  zombieSpawnManager;

    // ── Sub-managers ──────────────────────────────────────────────────────────
    private TitanPhaseManager     phaseManager;
    private TitanAIManager        aiManager;
    private TitanCombatManager    combatManager;
    private TitanAbilityManager   abilityManager;
    private TitanArenaManager     arenaManager;
    private TitanRewardManager    rewardManager;
    private TitanSummoningManager summoningManager;
    private TitanDamageTracker    damageTracker;

    // ── State ─────────────────────────────────────────────────────────────────
    private Giant  activeTitan = null;
    private static final NamespacedKey TITAN_KEY =
            new NamespacedKey("nuclearcraft", "is_plutonium_titan");

    public TitanManager(JavaPlugin plugin, ConfigManager configManager,
                         RadiationManager radiationManager,
                         PlayerDataManager playerDataManager,
                         ItemManager itemManager,
                         AdvancementManager advancementManager,
                         IrradiatedZombieManager zombieManager,
                         ZombieSpawnManager zombieSpawnManager) {
        this.plugin           = plugin;
        this.configManager    = configManager;
        this.radiationManager = radiationManager;
        this.playerDataManager = playerDataManager;
        this.itemManager      = itemManager;
        this.advancementManager = advancementManager;
        this.zombieManager    = zombieManager;
        this.zombieSpawnManager = zombieSpawnManager;
    }

    public void initialize() {
        damageTracker    = new TitanDamageTracker();
        phaseManager     = new TitanPhaseManager(plugin, configManager);
        aiManager        = new TitanAIManager(plugin, configManager, playerDataManager, damageTracker);
        combatManager    = new TitanCombatManager(plugin, configManager, radiationManager,
                                                   playerDataManager, phaseManager, damageTracker);
        abilityManager   = new TitanAbilityManager(plugin, configManager, radiationManager,
                                                    playerDataManager, phaseManager,
                                                    zombieManager, zombieSpawnManager);
        arenaManager     = new TitanArenaManager(plugin, configManager, radiationManager);
        rewardManager    = new TitanRewardManager(plugin, configManager, itemManager, playerDataManager);

        // Summoning manager needs a reference back to this — created after
        summoningManager = new TitanSummoningManager(plugin, configManager, itemManager, this);

        phaseManager.initialize();
        aiManager.initialize();
        combatManager.initialize();
        abilityManager.initialize();
        arenaManager.initialize();
        rewardManager.initialize();
        summoningManager.initialize();

        NCLogger.info("TitanManager initialized (Phase 10).");
    }

    public void shutdown() {
        despawnTitan();
        if (summoningManager != null) summoningManager.shutdown();
        if (arenaManager     != null) arenaManager.shutdown();
        if (phaseManager     != null) phaseManager.shutdown();
        if (aiManager        != null) aiManager.shutdown();
        if (combatManager    != null) combatManager.shutdown();
        if (abilityManager   != null) abilityManager.shutdown();
        if (rewardManager    != null) rewardManager.shutdown();
        NCLogger.info("TitanManager shut down.");
    }

    // ── Spawn / Despawn ───────────────────────────────────────────────────────

    @SuppressWarnings("deprecation")
    public void spawnTitan(Location loc, Player summoner) {
        if (isTitanAlive()) {
            if (summoner != null)
                summoner.sendMessage(ColorUtil.parse("<red>☢ A Titan is already active!</red>"));
            return;
        }
        if (loc.getWorld() == null) {
            NCLogger.warn("spawnTitan called with null world.");
            return;
        }

        // Count nearby players for health scaling
        int nearbyCount = (int) loc.getWorld().getPlayers().stream()
                .filter(p -> p.getLocation().distanceSquared(loc) <= 200 * 200)
                .count();
        double maxHp = combatManager.getMaxHealth(nearbyCount);

        // Spawn Giant
        Giant giant = (Giant) loc.getWorld().spawnEntity(loc, EntityType.GIANT);
        giant.customName(ColorUtil.parse("<gradient:#7700ff:#39ff14>☢ PLUTONIUM TITAN ☢</gradient>"));
        giant.setCustomNameVisible(true);
        giant.setRemoveWhenFarAway(false);
        giant.setSilent(false);

        // Mark via PDC
        giant.getPersistentDataContainer().set(TITAN_KEY, PersistentDataType.BYTE, (byte) 1);

        // Set attributes
        setAttr(giant, Attribute.MAX_HEALTH, maxHp);
        giant.setHealth(maxHp);
        setAttr(giant, Attribute.MOVEMENT_SPEED,
                configManager.getTitan().getDouble("titan.stats.speed", 0.22));
        setAttr(giant, Attribute.ATTACK_DAMAGE,
                configManager.getTitan().getDouble("titan.stats.damage", 18.0));
        setAttr(giant, Attribute.FOLLOW_RANGE,
                configManager.getTitan().getDouble("titan.stats.follow-range", 80.0));
        setAttr(giant, Attribute.KNOCKBACK_RESISTANCE, 1.0);

        activeTitan = giant;

        // Wire sub-managers
        phaseManager.createBossBar(giant);
        aiManager.startAI(giant);
        combatManager.startCombat(giant);
        abilityManager.startAbilities(giant);
        arenaManager.activateArena(loc);
        damageTracker.reset();

        // Add all nearby players to boss bar
        phaseManager.addAllPlayers(loc.getWorld().getPlayers());

        // Fire event
        plugin.getServer().getPluginManager().callEvent(
                new TitanSpawnEvent(giant, loc, summoner != null ? summoner.getName() : "ADMIN"));

        // Global broadcast
        plugin.getServer().broadcast(ColorUtil.parse(
                "<gradient:#7700ff:#ff0000>☢ THE PLUTONIUM TITAN HAS AWAKENED ☢</gradient>"));
        loc.getWorld().playSound(loc, org.bukkit.Sound.ENTITY_WITHER_SPAWN, 5.0f, 0.5f);

        // Update summoner stats
        if (summoner != null) {
            PlayerData pd = playerDataManager.get(summoner.getUniqueId()).orElse(null);
            if (pd != null) { pd.incrementTitanSummons(); pd.markDirty(); }
        }

        NCLogger.info("Plutonium Titan spawned at " + formatLoc(loc)
                + " with " + maxHp + " HP (players nearby: " + nearbyCount + ").");
    }

    public void despawnTitan() {
        if (activeTitan != null && !activeTitan.isDead()) {
            activeTitan.remove();
        }
        cleanupActiveFight();
    }

    /** Admin: force-kill the active titan (triggers normal death sequence). */
    public void adminKillTitan() {
        if (activeTitan == null || activeTitan.isDead()) return;
        activeTitan.setHealth(0.0);
    }

    // ── Event entry points (called from TitanListener) ────────────────────────

    /**
     * Called when a player damages the titan.
     */
    public void handleDamage(Giant titan, Player attacker, double finalDamage) {
        if (titan != activeTitan) return;
        combatManager.handleIncomingDamage(attacker, finalDamage);
        phaseManager.tick();
    }

    /**
     * Called when the titan entity dies.
     */
    public void handleDeath(Giant titan, Player lastHit) {
        if (titan != activeTitan) return;

        // Determine top contributor as kill credit
        Player killCredit = lastHit;
        Optional<UUID> topDamager = damageTracker.getTopDamager();
        if (topDamager.isPresent()) {
            Player top = plugin.getServer().getPlayer(topDamager.get());
            if (top != null && top.isOnline()) killCredit = top;
        }

        // Fire event
        plugin.getServer().getPluginManager().callEvent(
                new TitanDeathEvent(titan, titan.getLocation(), damageTracker.snapshot(), killCredit));

        // Rewards
        rewardManager.onTitanDeath(titan, damageTracker, killCredit);

        cleanupActiveFight();
        NCLogger.info("Plutonium Titan defeated. Kill credit: "
                + (killCredit != null ? killCredit.getName() : "unknown"));
    }

    private void cleanupActiveFight() {
        aiManager.stopAI();
        combatManager.stopCombat();
        abilityManager.stopAbilities();
        arenaManager.deactivateArena();
        phaseManager.destroyBossBar();
        activeTitan = null;
    }

    // ── Phase tick (called from TitanListener on entity tick) ─────────────────

    public void tickBossBar() {
        if (activeTitan == null || activeTitan.isDead()) return;
        phaseManager.tick();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public boolean isTitanAlive() {
        return activeTitan != null && !activeTitan.isDead() && activeTitan.isValid();
    }

    public boolean isTitan(org.bukkit.entity.Entity entity) {
        if (!(entity instanceof Giant g)) return false;
        return g.getPersistentDataContainer().has(TITAN_KEY, PersistentDataType.BYTE);
    }

    public Giant getActiveTitan()                    { return activeTitan; }
    public TitanSummoningManager getSummoningManager() { return summoningManager; }
    public TitanPhaseManager getPhaseManager()       { return phaseManager; }
    public TitanDamageTracker getDamageTracker()     { return damageTracker; }

    private void setAttr(Giant g, Attribute attr, double value) {
        AttributeInstance inst = g.getAttribute(attr);
        if (inst != null) inst.setBaseValue(value);
    }

    private String formatLoc(Location l) {
        return String.format("%.1f,%.1f,%.1f in %s",
                l.getX(), l.getY(), l.getZ(),
                l.getWorld() != null ? l.getWorld().getName() : "null");
    }
}
