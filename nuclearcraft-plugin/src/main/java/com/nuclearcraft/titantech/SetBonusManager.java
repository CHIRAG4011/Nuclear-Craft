package com.nuclearcraft.titantech;

import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.radiation.RadiationManager;
import com.nuclearcraft.radiation.RadiationSource;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Manages the full Titan Armor set bonus.
 *
 * Full Set Effects:
 *  - Radiation immunity (blocks all passive radiation gain)
 *  - +8 max health (on top of chestplate's +4)
 *  - Speed II, Jump Boost II, Resistance I, Fire Resistance
 *  - Radiation Reflection (30% of incoming radiation bounced back to attacker)
 *  - Auto radiation cure every 20 s
 */
public class SetBonusManager {

    private static final UUID MOD_SET_HEALTH_UUID = UUID.fromString("f3a1d204-77cb-4f1e-b11a-89c1c22cf712");

    private final JavaPlugin plugin;
    private final TitanArmorManager armorManager;
    private final RadiationManager radiationManager;
    private final PlayerDataManager playerDataManager;
    private final FileConfiguration cfg;

    private final Set<UUID> fullSetPlayers = new HashSet<>();
    private BukkitTask task;

    private int bonusIntervalTicks;
    private double reflectPercent;
    private boolean reflectEnabled;
    private int autoCureIntervalTicks;
    private long lastAutoCureTick = 0L;

    public SetBonusManager(JavaPlugin plugin, TitanArmorManager armorManager,
                           RadiationManager radiationManager,
                           PlayerDataManager playerDataManager,
                           FileConfiguration cfg) {
        this.plugin = plugin;
        this.armorManager = armorManager;
        this.radiationManager = radiationManager;
        this.playerDataManager = playerDataManager;
        this.cfg = cfg;
    }

    public void initialize() {
        bonusIntervalTicks   = cfg.getInt("set-bonus.effect-interval-ticks", 40);
        reflectPercent       = cfg.getDouble("set-bonus.radiation-reflect-percent", 30.0);
        reflectEnabled       = cfg.getBoolean("set-bonus.radiation-reflect-enabled", true);
        autoCureIntervalTicks= cfg.getInt("set-bonus.auto-cure-interval-ticks", 400);

        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickAll,
                bonusIntervalTicks, bonusIntervalTicks);
        NCLogger.info("SetBonusManager initialized.");
    }

    public void shutdown() {
        if (task != null) { task.cancel(); task = null; }
        for (Player p : plugin.getServer().getOnlinePlayers()) removeSetBonus(p);
    }

    private void tickAll() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            boolean hasSet = armorManager.hasFullSet(player);
            UUID uuid = player.getUniqueId();

            if (hasSet) {
                if (!fullSetPlayers.contains(uuid)) {
                    fullSetPlayers.add(uuid);
                    onSetEquipped(player);
                }
                applySetBonusEffects(player);
                attemptAutoCure(player);
            } else {
                if (fullSetPlayers.remove(uuid)) {
                    removeSetBonus(player);
                    onSetUnequipped(player);
                }
            }
        }
    }

    private void onSetEquipped(Player player) {
        player.sendMessage("§5§l☢ TITAN SET BONUS ACTIVATED ☢");
        player.sendMessage("§7Complete radiation immunity and empowerment active.");
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.6f);
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING,
                player.getLocation().add(0, 1, 0), 60, 1.0, 1.0, 1.0, 0.1);
        applySetHealthBonus(player, 8.0);
    }

    private void onSetUnequipped(Player player) {
        player.sendMessage("§c☢ Titan Set Bonus deactivated.");
        removeSetHealthBonus(player);
    }

    private void applySetBonusEffects(Player player) {
        int dur = bonusIntervalTicks + 20;
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, dur, 1, true, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, dur, 1, true, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, dur, 0, true, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, dur, 0, true, false, false));
    }

    private void attemptAutoCure(Player player) {
        long tick = plugin.getServer().getCurrentTick();
        if (tick - lastAutoCureTick < autoCureIntervalTicks) return;
        lastAutoCureTick = tick;

        playerDataManager.get(player.getUniqueId()).ifPresent(data -> {
            if (data.getRadiationLevel() > 0) {
                data.setRadiationLevel(Math.max(0, data.getRadiationLevel() - 100.0));
                if (data.getRadiationLevel() == 0) data.setRadiationStage(0);
                data.setDirty(true);
                player.sendMessage("§5☢ Titan Set: Reactor shielding purged radiation.");
            }
        });
    }

    /**
     * Attempts to reflect radiation when the player with full Titan set is attacked.
     * @return the reflected amount (0 if reflection did not trigger)
     */
    public double tryReflect(Player defender, Entity attacker, double amount) {
        if (!reflectEnabled) return 0;
        if (!isRadiationImmune(defender)) return 0;

        double reflected = amount * (reflectPercent / 100.0);

        if (attacker instanceof Player attackPlayer && !attackPlayer.equals(defender)) {
            radiationManager.addRadiation(attackPlayer, (int) reflected, RadiationSource.UNKNOWN);
            attackPlayer.sendMessage("§5☢ Your radiation was reflected by the Titan Set!");
        }

        playerDataManager.get(defender.getUniqueId()).ifPresent(d -> {
            d.incrementRadiationReflected((int) reflected);
            d.setDirty(true);
        });

        return reflected;
    }

    public boolean isRadiationImmune(Player player) {
        return fullSetPlayers.contains(player.getUniqueId());
    }

    public boolean hasFullSet(Player player) {
        return fullSetPlayers.contains(player.getUniqueId()) || armorManager.hasFullSet(player);
    }

    private void applySetHealthBonus(Player player, double extraHearts) {
        var attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) return;
        boolean has = attr.getModifiers().stream().anyMatch(m -> m.getUniqueId().equals(MOD_SET_HEALTH_UUID));
        if (!has) {
            attr.addModifier(new AttributeModifier(MOD_SET_HEALTH_UUID, "titan-set-health-bonus",
                    extraHearts * 2.0, AttributeModifier.Operation.ADD_NUMBER));
        }
    }

    private void removeSetHealthBonus(Player player) {
        var attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) return;
        attr.getModifiers().stream()
                .filter(m -> m.getUniqueId().equals(MOD_SET_HEALTH_UUID))
                .findFirst().ifPresent(attr::removeModifier);
    }

    private void removeSetBonus(Player player) { removeSetHealthBonus(player); }

    public void onPlayerQuit(Player player) {
        fullSetPlayers.remove(player.getUniqueId());
        removeSetBonus(player);
    }
}
