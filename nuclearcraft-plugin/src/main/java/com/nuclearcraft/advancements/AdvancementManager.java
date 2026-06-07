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
 *   - A sound plays.
 *   - XP is awarded.
 *   - The achievement is persisted via PlayerData.
 *
 * Advancement keys (stored as "adv:<key>" in unlockedUpgrades):
 *   Phase 2/3:
 *     first_exposure    — first radiation gained
 *     mutant_hunter     — first irradiated zombie killed
 *     core_collector    — first radioactive core obtained
 *     alpha_slayer      — first alpha zombie killed
 *
 *   Phase 4:
 *     nuclear_discovery — found Plutonium Ore for the first time
 *     unsafe_miner      — attempted to mine without Radiation Drill
 *     safe_extraction   — successfully mined Plutonium Ore with Drill
 *     radioactive_hoarder — carried 64+ Plutonium Fragments
 *
 *   Phase 5:
 *     industrial_age    — built first Nuclear Smelter
 *     first_refinement  — refined first Plutonium Ingot
 *     master_refiner    — produced 100 Refined Plutonium Ingots
 *
 *   Phase 6:
 *     nuclear_warrior   — crafted first Plutonium Sword
 *     protected_worker  — assembled complete Hazmat Suit
 *     nuclear_knight    — equipped full Plutonium Armor
 *     radioactive_arsenal — crafted all five Plutonium Tools
 *
 *   Phase 7:
 *     first_harvest     — harvested first Mutated Healing Plant
 *     nuclear_farmer    — created first Radioactive Farm
 *     master_botanist   — harvested 100 Mutated Healing Plants
 *     radiation_survivor — used first Radiation Antidote or Serum
 *     cured_at_last     — cured Stage 4 Critical Radiation Poisoning
 */
public class AdvancementManager {

    public enum Advancement {
        // ── Phase 2 / 3 ──────────────────────────────────────────────────────

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
                Sound.UI_TOAST_CHALLENGE_COMPLETE),

        // ── Phase 4: Plutonium Ore ────────────────────────────────────────────

        NUCLEAR_DISCOVERY("nuclear_discovery",
                "☢ Nuclear Discovery",
                "Plutonium Ore detected! A new age of power begins...",
                200,
                Sound.BLOCK_BEACON_ACTIVATE),

        UNSAFE_MINER("unsafe_miner",
                "☢ Unsafe Miner",
                "You tried mining Plutonium Ore without a Radiation Drill!",
                25,
                Sound.ENTITY_CREEPER_PRIMED),

        SAFE_EXTRACTION("safe_extraction",
                "☢ Safe Extraction",
                "Successfully extracted Plutonium Ore with the Radiation Drill.",
                300,
                Sound.BLOCK_AMETHYST_CLUSTER_PLACE),

        RADIOACTIVE_HOARDER("radioactive_hoarder",
                "☢ Radioactive Hoarder",
                "Carrying 64 or more Raw Plutonium Fragments — use a Lead Crate!",
                50,
                Sound.BLOCK_ANVIL_LAND),

        // ── Phase 5: Nuclear Smelter ─────────────────────────────────────────

        INDUSTRIAL_AGE("industrial_age",
                "☢ The Industrial Age",
                "You built your first Nuclear Smelter!",
                500,
                Sound.BLOCK_BEACON_ACTIVATE),

        FIRST_REFINEMENT("first_refinement",
                "☢ First Refinement",
                "You refined your first Plutonium Ingot in the Nuclear Smelter.",
                300,
                Sound.BLOCK_AMETHYST_CLUSTER_PLACE),

        MASTER_REFINER("master_refiner",
                "☢ Master Refiner",
                "You have produced 100 Refined Plutonium Ingots — true industrial power!",
                1000,
                Sound.UI_TOAST_CHALLENGE_COMPLETE),

        // ── Phase 6: Plutonium Equipment ─────────────────────────────────────

        NUCLEAR_WARRIOR("nuclear_warrior",
                "☢ Nuclear Warrior",
                "You crafted your first Plutonium Sword — a weapon of mass irradiation.",
                500,
                Sound.ENTITY_PLAYER_LEVELUP),

        PROTECTED_WORKER("protected_worker",
                "☢ Protected Worker",
                "You assembled a complete Hazmat Suit — stay safe out there.",
                400,
                Sound.BLOCK_AMETHYST_CLUSTER_PLACE),

        NUCLEAR_KNIGHT("nuclear_knight",
                "☢ Nuclear Knight",
                "Full Plutonium Armor equipped — environmentally untouchable.",
                750,
                Sound.UI_TOAST_CHALLENGE_COMPLETE),

        RADIOACTIVE_ARSENAL("radioactive_arsenal",
                "☢ Radioactive Arsenal",
                "You crafted all five Plutonium Tools — the complete nuclear toolkit.",
                1000,
                Sound.UI_TOAST_CHALLENGE_COMPLETE),

        // ── Phase 7: Radioactive Farming & Cure ──────────────────────────────

        FIRST_HARVEST("first_harvest",
                "☢ First Harvest",
                "You harvested your first Mutated Healing Plant.",
                300,
                Sound.BLOCK_AMETHYST_CLUSTER_PLACE),

        NUCLEAR_FARMER("nuclear_farmer",
                "☢ Nuclear Farmer",
                "You planted your first Mutated Seed — the cure begins here.",
                200,
                Sound.BLOCK_GRASS_PLACE),

        MASTER_BOTANIST("master_botanist",
                "☢ Master Botanist",
                "Harvested 100 Mutated Healing Plants — a true nuclear botanist!",
                1500,
                Sound.UI_TOAST_CHALLENGE_COMPLETE),

        RADIATION_SURVIVOR("radiation_survivor",
                "☢ Radiation Survivor",
                "You used your first Radiation Antidote or Serum.",
                400,
                Sound.ENTITY_GENERIC_DRINK),

        CURED_AT_LAST("cured_at_last",
                "☢ Cured At Last",
                "You cured Stage 4 Critical Radiation Poisoning. Against all odds!",
                1000,
                Sound.UI_TOAST_CHALLENGE_COMPLETE),

        // ── Phase 8: Nuclear Forge & Equipment Upgrades ───────────────────────

        MASTER_BLACKSMITH("master_blacksmith",
                "☢ Master Blacksmith",
                "You built your first Nuclear Forge — the age of MK upgrades begins!",
                750,
                Sound.BLOCK_BEACON_ACTIVATE),

        ENHANCED_ARSENAL("enhanced_arsenal",
                "☢ Enhanced Arsenal",
                "You successfully upgraded a piece of equipment using the Nuclear Forge.",
                500,
                Sound.BLOCK_AMETHYST_CLUSTER_PLACE),

        NUCLEAR_ENGINEER("nuclear_engineer",
                "☢ Nuclear Engineer",
                "You achieved the pinnacle: an MK-IV Nuclear Forge upgrade!",
                2000,
                Sound.UI_TOAST_CHALLENGE_COMPLETE),

        PERFECTED_TECHNOLOGY("perfected_technology",
                "☢ Perfected Technology",
                "All equipment slots now carry MK-IV upgrades. The radiation aura surrounds you.",
                5000,
                Sound.UI_TOAST_CHALLENGE_COMPLETE),

        // ── Phase 9: Advanced Combat ──────────────────────────────────────────

        RADIOACTIVE_WARRIOR("radioactive_warrior",
                "☢ Radioactive Warrior",
                "You applied radiation to another player in PvP for the first time.",
                300,
                Sound.ENTITY_PLAYER_LEVELUP),

        CONTAMINATOR("contaminator",
                "☢ Contaminator",
                "You raised another player's radiation stage through PvP combat.",
                500,
                Sound.BLOCK_BEACON_ACTIVATE),

        NUCLEAR_ARCHER("nuclear_archer",
                "☢ Nuclear Archer",
                "You eliminated a player with a Plutonium Arrow.",
                750,
                Sound.UI_TOAST_CHALLENGE_COMPLETE),

        RADIATION_MASTER("radiation_master",
                "☢ Radiation Master",
                "You have inflicted 10,000 radiation through PvP combat — a true nuclear menace!",
                2500,
                Sound.UI_TOAST_CHALLENGE_COMPLETE),

        // ── Phase 11: Titan Technology ────────────────────────────────────────

        TITAN_ENGINEER("titan_engineer",
                "☢ Titan Engineer",
                "You crafted your first piece of equipment in the Titan Reactor Forge!",
                2000,
                Sound.BLOCK_BEACON_ACTIVATE),

        TITAN_WARRIOR("titan_warrior",
                "☢ Titan Warrior",
                "You forged Titan Armor — radiation can no longer stop you.",
                3000,
                Sound.UI_TOAST_CHALLENGE_COMPLETE),

        MASTER_OF_RADIATION("master_of_radiation",
                "☢ Master of Radiation",
                "Your Titan Set reflected radiation back at an attacker!",
                1500,
                Sound.ENTITY_PLAYER_LEVELUP),

        ULTIMATE_TECHNOLOGY("ultimate_technology",
                "☢ Ultimate Technology",
                "You are wearing the full Titan Set — the pinnacle of nuclear engineering!",
                5000,
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
        player.sendTitle(
                "§6§l" + advancement.getTitle(),
                "§7" + advancement.getDescription(),
                10, 70, 20
        );

        player.sendMessage(Component.text()
                .append(Component.text("[", NamedTextColor.DARK_GRAY))
                .append(Component.text("Advancement Unlocked!", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text("] ", NamedTextColor.DARK_GRAY))
                .append(Component.text(advancement.getTitle(), NamedTextColor.YELLOW))
                .build());

        player.sendMessage(Component.text("  " + advancement.getDescription(), NamedTextColor.GRAY));

        player.playSound(player.getLocation(), advancement.getSound(), 1.0f, 1.0f);
    }

    private void giveXp(Player player, int xp) {
        player.giveExp(xp, true);
    }
}
