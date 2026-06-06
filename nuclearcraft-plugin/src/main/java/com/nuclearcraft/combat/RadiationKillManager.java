package com.nuclearcraft.combat;

import com.nuclearcraft.advancements.AdvancementManager;
import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.radiation.RadiationManager;
import com.nuclearcraft.utils.NCLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Phase 9 kill tracking and custom radiation death messages.
 *
 * <p>When a player dies, this manager checks whether the death is attributable
 * to a PvP radiation attack (via {@link PvPRadiationManager} attribution) and,
 * if so:
 * <ul>
 *   <li>Sets a custom death message referencing the attacker and weapon type.</li>
 *   <li>Records the kill in the attacker's {@link com.nuclearcraft.data.PlayerData}.</li>
 *   <li>Awards advancements to the attacker.</li>
 *   <li>Broadcasts a brief chat notification.</li>
 * </ul>
 */
public class RadiationKillManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final PlayerDataManager playerDataManager;
    private final AdvancementManager advancementManager;
    private final PvPRadiationManager pvpManager;
    private final CombatStatisticsManager statsManager;

    private boolean customDeathMessages;

    /**
     * Maps victim UUID → the {@link RadiationDamageType} of the kill blow.
     * Set by RadiationCombatManager when radiation is applied; read on death.
     */
    private final Map<UUID, RadiationDamageType> lastDamageType = new ConcurrentHashMap<>();

    public RadiationKillManager(JavaPlugin plugin,
                                 ConfigManager configManager,
                                 PlayerDataManager playerDataManager,
                                 AdvancementManager advancementManager,
                                 PvPRadiationManager pvpManager,
                                 CombatStatisticsManager statsManager) {
        this.plugin              = plugin;
        this.configManager       = configManager;
        this.playerDataManager   = playerDataManager;
        this.advancementManager  = advancementManager;
        this.pvpManager          = pvpManager;
        this.statsManager        = statsManager;
    }

    public void initialize() {
        customDeathMessages = configManager.getCombat()
                .getBoolean("kills.custom-death-messages", true);
        NCLogger.info("RadiationKillManager initialized.");
    }

    public void shutdown() {
        lastDamageType.clear();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Called by RadiationCombatManager before applying PvP radiation
    // ──────────────────────────────────────────────────────────────────────────

    public void trackLastDamageType(Player victim, RadiationDamageType type) {
        lastDamageType.put(victim.getUniqueId(), type);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Death event
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Called from CombatListener on PlayerDeathEvent.
     * Handles custom death message generation and kill stat recording.
     */
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        UUID victimId = victim.getUniqueId();

        UUID attackerId = pvpManager.getLastAttacker(victim);
        if (attackerId == null) {
            // Not a PvP radiation kill — nothing to do
            lastDamageType.remove(victimId);
            return;
        }

        Player attacker = plugin.getServer().getPlayer(attackerId);
        RadiationDamageType dmgType = lastDamageType.getOrDefault(victimId, RadiationDamageType.WEAPON);
        lastDamageType.remove(victimId);
        pvpManager.clearAttacker(victim);

        // Determine kill type
        CombatStatisticsManager.KillType killType = switch (dmgType) {
            case ARROW -> CombatStatisticsManager.KillType.ARROW;
            case AURA  -> CombatStatisticsManager.KillType.AURA;
            default    -> CombatStatisticsManager.KillType.RADIATION;
        };

        if (attacker != null) {
            statsManager.recordPvPKill(attacker, killType);
        }

        if (!customDeathMessages) return;

        // Build custom death message
        if (attacker != null) {
            Component msg = buildDeathMessage(attacker, victim, dmgType);
            event.deathMessage(msg);

            // Broadcast brief notification
            plugin.getServer().broadcast(
                    Component.text("[", NamedTextColor.DARK_GRAY)
                             .append(Component.text("☢", NamedTextColor.GREEN))
                             .append(Component.text("] ", NamedTextColor.DARK_GRAY))
                             .append(Component.text(victim.getName(), NamedTextColor.RED))
                             .append(Component.text(" was irradiated by ", NamedTextColor.GRAY))
                             .append(Component.text(attacker.getName(), NamedTextColor.GOLD)));
        }
    }

    private Component buildDeathMessage(Player killer, Player victim, RadiationDamageType type) {
        String weaponDesc = switch (type) {
            case WEAPON -> "Plutonium Sword";
            case ARROW  -> "Plutonium Arrow";
            case AURA   -> "Radiation Aura";
            default     -> "radiation";
        };

        return Component.text(victim.getName(), NamedTextColor.RED, TextDecoration.BOLD)
                .append(Component.text(" was turned into nuclear waste by ", NamedTextColor.GRAY))
                .append(Component.text(killer.getName(), NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text("'s " + weaponDesc, NamedTextColor.GREEN));
    }
}
