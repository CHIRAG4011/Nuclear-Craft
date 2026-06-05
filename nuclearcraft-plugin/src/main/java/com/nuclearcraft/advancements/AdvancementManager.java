package com.nuclearcraft.advancements;

import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.data.PlayerData;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.utils.NCLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Lightweight custom advancement system.
 *
 * Rather than requiring a data pack, advancements are tracked in PlayerData
 * using the existing {@code unlockedUpgrades} Set with an "adv:" prefix key.
 *
 * When unlocked:
 *   - A Minecraft-style toast title + subtitle is displayed.
 *   - A sound plays (ENTITY_PLAYER_LEVELUP or similar).
 *   - XP is awarded.
 *   - The achievement is persisted via PlayerData.
 *
 * This approach is fully functional without requiring server restarts,
 * data pack reloads, or client-side resource changes.
 *
 * Advancement keys (stored as "adv:<key>" in unlockedUpgrades):
 *   first_exposure    — first radiation gained
 *   mutant_hunter     — first irradiated zombie killed
 *   core_collector    — first radioactive core obtained
 *   alpha_slayer      — first alpha zombie killed
 */
public class AdvancementManager {

    public enum Advancement {
        FIRST_EXPOSURE("first_exposure",
                "☢ First Exposure",
                "You gained radiation for the first time.",
                50,
                Sound.ENTITY_EXPERIENCE_ORB_PICKUP),

        MUTANT_HUNTER("mutant_hunter",
                "☢ Mutant Hunter",
                "Killed your first Irradiated Zombie.",
                100,
                Sound.ENTITY_PLAYER_LEVELUP),

        CORE_COLLECTOR("core_collector",
                "☢ Core Collector",
                "Obtained a Radioactive Core.",
                100,
                Sound.ENTITY_PLAYER_LEVELUP),

        ALPHA_SLAYER("alpha_slayer",
                "☢ Alpha Slayer",
                "Defeated an Alpha Irradiated Zombie!",
                500,
                Sound.UI_TOAST_CHALLENGE_COMPLETE);

        private final String key;
        private final String title;
        private final String description;
        private final int xpReward;
        private final Sound sound;

        Advancement(String key, String title, String description, int xpReward, Sound sound) {
            this.key = key;
            this.title = title;
            this.description = description;
            this.xpReward = xpReward;
            this.sound = sound;
        }

        public String getKey()         { return "adv:" + key; }
        public String getTitle()       { return title; }
        public String getDescription() { return description; }
        public int getXpReward()       { return xpReward; }
        public Sound getSound()        { return sound; }
    }

    private final NuclearCraftPlugin plugin;
    private final PlayerDataManager playerDataManager;

    public AdvancementManager(NuclearCraftPlugin plugin, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
    }

    public void initialize() {
        NCLogger.info("AdvancementManager initialized (" + Advancement.values().length + " advancements registered).");
    }

    public void shutdown() {}

    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Awards an advancement to a player if they haven't already earned it.
     * Safe to call multiple times — idempotent.
     *
     * @param player      the player to award
     * @param advancement the advancement to check/award
     */
    public void award(Player player, Advancement advancement) {
        PlayerData data = playerDataManager.get(player.getUniqueId()).orElse(null);
        if (data == null) return;

        String key = advancement.getKey();
        if (data.getUnlockedUpgrades().contains(key)) return; // already earned

        data.unlockUpgrade(key); // marks dirty; TaskManager will flush on its schedule

        showToast(player, advancement);
        giveXp(player, advancement.getXpReward());

        NCLogger.info("Advancement unlocked: " + player.getName() + " -> " + advancement.getTitle());
    }

    /**
     * Returns true if the player has already earned the given advancement.
     */
    public boolean hasEarned(Player player, Advancement advancement) {
        PlayerData data = playerDataManager.get(player.getUniqueId()).orElse(null);
        if (data == null) return false;
        return data.getUnlockedUpgrades().contains(advancement.getKey());
    }

    // ──────────────────────────────────────────────────────────────────────────

    private void showToast(Player player, Advancement advancement) {
        // Title line (advancement name in gold)
        player.sendTitle(
                "§6§l" + advancement.getTitle(),
                "§7" + advancement.getDescription(),
                10, 70, 20
        );

        // Chat message
        player.sendMessage(Component.text()
                .append(Component.text("[", NamedTextColor.DARK_GRAY))
                .append(Component.text("Advancement Unlocked!", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text("] ", NamedTextColor.DARK_GRAY))
                .append(Component.text(advancement.getTitle(), NamedTextColor.YELLOW))
                .build());

        player.sendMessage(Component.text("  " + advancement.getDescription(), NamedTextColor.GRAY));

        // Sound
        player.playSound(player.getLocation(), advancement.getSound(), 1.0f, 1.0f);
    }

    private void giveXp(Player player, int xp) {
        player.giveExp(xp, true);
    }
}
