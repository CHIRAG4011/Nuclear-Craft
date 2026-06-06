package com.nuclearcraft.forge;

import com.nuclearcraft.advancements.AdvancementManager;
import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.events.*;
import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.radiation.RadiationManager;
import com.nuclearcraft.radiation.RadiationSource;
import com.nuclearcraft.upgrade.UpgradeManager;
import com.nuclearcraft.upgrade.UpgradeTier;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central manager for all Nuclear Forge machines.
 *
 * Responsibilities:
 * - Machine registration/removal on block place/break
 * - Persistent state loading/saving (forge-machines.yml)
 * - Tick-driven upgrade processing with success/fail RNG
 * - Energy management with overload detection
 * - GUI opening and display refresh
 * - Event dispatch (ForgeUpgradeStart/Success/Fail/Overload)
 * - Advancement and statistics tracking
 */
public class NuclearForgeManager {

    private final NuclearCraftPlugin plugin;
    private final ConfigManager configManager;
    private final ItemManager itemManager;
    private final UpgradeManager upgradeManager;
    private final PlayerDataManager playerDataManager;
    private final AdvancementManager advancementManager;
    private final RadiationManager radiationManager;

    private final Map<String, ForgeData> machines = new ConcurrentHashMap<>();
    /** Open GUIs keyed by player UUID so we can refresh display slots. */
    private final Map<UUID, NuclearForgeGUI> openGUIs = new ConcurrentHashMap<>();

    private BukkitTask tickTask;
    private BukkitTask guiRefreshTask;

    private int tickPeriod;
    private double maxEnergy;
    private double energyPerCore;
    private double energyDecayPerTick;
    private int overloadShutdownTicks;
    private double overloadBurstRadius;
    private int overloadBurstAmount;
    private String guiTitle;
    private int guiRefreshTicks;

    private final Map<String, Long> overloadCooldowns = new ConcurrentHashMap<>();

    public NuclearForgeManager(NuclearCraftPlugin plugin,
                                ConfigManager configManager,
                                ItemManager itemManager,
                                UpgradeManager upgradeManager,
                                PlayerDataManager playerDataManager,
                                AdvancementManager advancementManager,
                                RadiationManager radiationManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.itemManager = itemManager;
        this.upgradeManager = upgradeManager;
        this.playerDataManager = playerDataManager;
        this.advancementManager = advancementManager;
        this.radiationManager = radiationManager;
    }

    public void initialize() {
        loadConfig();
        loadMachines();
        scheduleTick();
        scheduleGuiRefresh();
        NCLogger.info("NuclearForgeManager initialized — " + machines.size() + " forge(s) loaded.");
    }

    public void shutdown() {
        if (tickTask != null && !tickTask.isCancelled()) { tickTask.cancel(); tickTask = null; }
        if (guiRefreshTask != null && !guiRefreshTask.isCancelled()) { guiRefreshTask.cancel(); guiRefreshTask = null; }
        saveMachines();
        NCLogger.info("NuclearForgeManager shut down — " + machines.size() + " forge(s) saved.");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Config
    // ──────────────────────────────────────────────────────────────────────────

    private void loadConfig() {
        FileConfiguration cfg = configManager.getForge();
        tickPeriod            = cfg.getInt("machine.tick-period", 4);
        maxEnergy             = cfg.getDouble("machine.max-energy", 10000.0);
        energyPerCore         = cfg.getDouble("machine.energy-per-core", 2000.0);
        energyDecayPerTick    = cfg.getDouble("machine.energy-decay-per-tick", 0.5);
        overloadShutdownTicks = cfg.getInt("machine.overload-shutdown-ticks", 200);
        overloadBurstRadius   = cfg.getDouble("overload.radiation-burst-radius", 8.0);
        overloadBurstAmount   = cfg.getInt("overload.radiation-burst-amount", 150);
        guiTitle              = cfg.getString("gui.title", "§a☢ Nuclear Forge ☢");
        guiRefreshTicks       = cfg.getInt("gui.refresh-ticks", 10);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Machine registration
    // ──────────────────────────────────────────────────────────────────────────

    public ForgeData registerMachine(Block block, UUID placerUuid) {
        String key = ForgeData.serializeLocation(block.getLocation());
        ForgeData data = new ForgeData(block.getLocation());
        data.setLastInteractingPlayerUuid(placerUuid);
        data.setState(ForgeState.READY);
        machines.put(key, data);
        NCLogger.debug("Nuclear Forge registered at " + key);
        return data;
    }

    public void unregisterMachine(Block block) {
        String key = ForgeData.serializeLocation(block.getLocation());
        ForgeData data = machines.remove(key);
        if (data != null) {
            // Return pending item to last interacting player
            if (data.getPendingEquipment() != null) {
                UUID uuid = data.getUpgraderUuid() != null ? data.getUpgraderUuid() : data.getLastInteractingPlayerUuid();
                returnItemToPlayer(uuid, data.getPendingEquipment());
            }
            NCLogger.debug("Nuclear Forge unregistered at " + key);
        }
    }

    public boolean isForge(Block block) {
        return machines.containsKey(ForgeData.serializeLocation(block.getLocation()));
    }

    public Optional<ForgeData> getForgeAt(Block block) {
        return Optional.ofNullable(machines.get(ForgeData.serializeLocation(block.getLocation())));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GUI
    // ──────────────────────────────────────────────────────────────────────────

    public void openGUI(Player player, ForgeData forge) {
        NuclearForgeGUI gui = new NuclearForgeGUI(guiTitle, forge, this, upgradeManager, maxEnergy);
        openGUIs.put(player.getUniqueId(), gui);
        forge.setLastInteractingPlayerUuid(player.getUniqueId());
        plugin.getGuiManager().open(player, gui);
    }

    public void onGUIClosed(UUID playerUuid) {
        openGUIs.remove(playerUuid);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Upgrade logic
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Called when the player clicks the Forge button in the GUI.
     * Validates materials, consumes them, and starts the upgrade timer.
     */
    public void tryStartUpgrade(Player player, ForgeData forge, Inventory guiInventory) {
        if (forge.isUpgrading()) {
            player.sendMessage(colorize("&c☢ The forge is already upgrading!"));
            return;
        }
        if (forge.getState() == ForgeState.OVERLOADED) {
            player.sendMessage(colorize("&4☢ The forge is overloaded! Wait for it to cool down."));
            return;
        }
        if (forge.hasOutput()) {
            player.sendMessage(colorize("&e☢ Collect the output item before starting a new upgrade."));
            return;
        }

        ItemStack equipment = guiInventory.getItem(NuclearForgeGUI.SLOT_EQUIPMENT);
        ItemStack material  = guiInventory.getItem(NuclearForgeGUI.SLOT_MATERIAL);
        ItemStack catalyst  = guiInventory.getItem(NuclearForgeGUI.SLOT_CATALYST);

        if (isAir(equipment) || !upgradeManager.isUpgradeable(equipment)) {
            player.sendMessage(colorize("&c☢ Place upgradeable Plutonium or Hazmat equipment in the input slot."));
            return;
        }

        UpgradeTier currentTier = upgradeManager.getTier(equipment);
        UpgradeTier targetTier  = currentTier.next();
        if (targetTier == null) {
            player.sendMessage(colorize("&c☢ This item is already at maximum upgrade tier (MK-IV)."));
            return;
        }

        // Load per-tier config overrides
        int ingotsReq  = configManager.getForge().getInt("upgrades." + tierConfigKey(targetTier) + ".ingots-required", targetTier.getIngotsRequired());
        int coresReq   = configManager.getForge().getInt("upgrades." + tierConfigKey(targetTier) + ".cores-required",  targetTier.getCoresRequired());
        int heartsReq  = configManager.getForge().getInt("upgrades." + tierConfigKey(targetTier) + ".hearts-required", targetTier.getHeartsRequired());
        int energyCost = configManager.getForge().getInt("upgrades." + tierConfigKey(targetTier) + ".energy-cost",     targetTier.getEnergyCost());

        // Count materials
        int ingotCount = countCustomItem(material, "refined-plutonium-ingot");
        int coreCount  = countCustomItem(catalyst,  "radioactive-core");
        int heartCount = countCustomItem(catalyst,  "irradiated-heart");

        if (ingotCount < ingotsReq) {
            player.sendMessage(colorize("&c☢ Need " + ingotsReq + " Refined Plutonium Ingots (have " + ingotCount + ")."));
            return;
        }
        if (coreCount < coresReq) {
            player.sendMessage(colorize("&c☢ Need " + coresReq + " Radioactive Cores in the catalyst slot (have " + coreCount + ")."));
            return;
        }
        if (heartsReq > 0 && heartCount < heartsReq) {
            player.sendMessage(colorize("&c☢ Need " + heartsReq + " Irradiated Hearts in the catalyst slot (have " + heartCount + ")."));
            return;
        }
        if (forge.getEnergy() < energyCost) {
            player.sendMessage(colorize("&c☢ Not enough forge energy (" + (int)forge.getEnergy() + " / " + energyCost + ")."));
            player.sendMessage(colorize("&7Insert Radioactive Cores into the catalyst slot to add energy."));
            return;
        }

        // Fire cancellable start event
        ForgeUpgradeStartEvent startEvent = new ForgeUpgradeStartEvent(player, forge, equipment, currentTier, targetTier);
        plugin.getServer().getPluginManager().callEvent(startEvent);
        if (startEvent.isCancelled()) return;

        // Consume materials
        consumeItems(guiInventory, NuclearForgeGUI.SLOT_MATERIAL, ingotsReq);
        consumeItems(guiInventory, NuclearForgeGUI.SLOT_CATALYST, coresReq + heartsReq);

        // Consume energy (cores in catalyst provide energy too — 1 core = energyPerCore)
        forge.consumeEnergy(energyCost);
        forge.addEnergyConsumed(energyCost);

        // Store the item being upgraded
        forge.setPendingEquipment(equipment.clone());
        forge.setPendingTargetTier(targetTier);
        forge.setUpgraderUuid(player.getUniqueId());
        forge.setLastInteractingPlayerUuid(player.getUniqueId());

        // Remove the equipment from the GUI slot
        guiInventory.setItem(NuclearForgeGUI.SLOT_EQUIPMENT, null);

        // Set timing
        int durationTicks = configManager.getForge().getInt(
                "upgrades." + tierConfigKey(targetTier) + ".duration-ticks",
                targetTier.getBaseDurationTicks());
        long currentTick = plugin.getServer().getCurrentTick();
        forge.setUpgradeStartTick(currentTick);
        forge.setUpgradeEndTick(currentTick + durationTicks);
        forge.setState(ForgeState.UPGRADING);

        // Track stats
        forge.incrementForgeUses();
        forge.addMaterialsConsumed(ingotsReq + coresReq + heartsReq);
        playerDataManager.get(player.getUniqueId()).ifPresent(d -> {
            d.incrementForgeUses();
            d.setDirty(true);
        });

        player.sendMessage(colorize("&a☢ Upgrade started! Upgrading to " + targetTier.getDisplayName() + "..."));
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.2f);

        // Refresh GUI immediately
        NuclearForgeGUI gui = openGUIs.get(player.getUniqueId());
        if (gui != null) gui.refreshDisplay(gui.getInventory());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Tick loop
    // ──────────────────────────────────────────────────────────────────────────

    private void scheduleTick() {
        tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (ForgeData forge : machines.values()) {
                try {
                    tickMachine(forge);
                } catch (Exception e) {
                    NCLogger.severe("Error ticking forge at " + forge.getLocationKey(), e);
                    forge.setState(ForgeState.ERROR);
                }
            }
        }, tickPeriod, tickPeriod);
    }

    private void scheduleGuiRefresh() {
        guiRefreshTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            openGUIs.forEach((uuid, gui) -> {
                if (gui.getInventory() != null) {
                    gui.refreshDisplay(gui.getInventory());
                }
            });
        }, guiRefreshTicks, guiRefreshTicks);
    }

    private void tickMachine(ForgeData forge) {
        // Energy decay
        if (forge.getEnergy() > 0) {
            forge.consumeEnergy(energyDecayPerTick * tickPeriod);
        }

        // Overload check
        if (forge.getEnergy() > maxEnergy && forge.getState() != ForgeState.OVERLOADED) {
            triggerOverload(forge);
            return;
        }

        // Overload cooldown recovery
        if (forge.getState() == ForgeState.OVERLOADED) {
            String key = forge.getLocationKey();
            long end = overloadCooldowns.getOrDefault(key, 0L);
            if (plugin.getServer().getCurrentTick() >= end) {
                forge.setState(ForgeState.READY);
                overloadCooldowns.remove(key);
                Location loc = forge.getLocation();
                loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.add(0.5, 1, 0.5), 10, 0.3, 0.3, 0.3, 0.01);
            }
            return;
        }

        // Offline → Ready if energy is present
        if (forge.getState() == ForgeState.OFFLINE) {
            if (forge.getEnergy() >= 100) forge.setState(ForgeState.READY);
            return;
        }

        // Check if upgrading and done
        if (forge.isUpgrading()) {
            long currentTick = plugin.getServer().getCurrentTick();

            // Visual particles while upgrading
            spawnUpgradeParticles(forge);

            if (currentTick >= forge.getUpgradeEndTick()) {
                resolveUpgrade(forge);
            }
        }
    }

    private void triggerOverload(ForgeData forge) {
        forge.setState(ForgeState.OVERLOADED);
        forge.setEnergy(0);
        forge.incrementOverloadCount();

        Location loc = forge.getLocation().add(0.5, 0.5, 0.5);
        loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 80, 2, 2, 2, 0.1);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);

        // Radiation burst
        for (Player nearby : loc.getWorld().getNearbyPlayers(loc, overloadBurstRadius)) {
            radiationManager.addRadiation(nearby, overloadBurstAmount, RadiationSource.NUCLEAR_FORGE);
            nearby.sendMessage(colorize("&4☢ The Nuclear Forge overloaded! You absorbed " + overloadBurstAmount + " radiation!"));
        }

        overloadCooldowns.put(forge.getLocationKey(), (long) plugin.getServer().getCurrentTick() + overloadShutdownTicks);

        plugin.getServer().getPluginManager().callEvent(
                new ForgeOverloadEvent(forge, forge.getLocation(), forge.getEnergy()));

        NCLogger.info("Nuclear Forge at " + forge.getLocationKey() + " OVERLOADED!");

        // If an upgrade was in progress, return the item safely
        if (forge.getPendingEquipment() != null) {
            forge.setOutputItem(forge.getPendingEquipment());
            forge.setPendingEquipment(null);
            forge.setPendingTargetTier(null);
        }
    }

    private void resolveUpgrade(ForgeData forge) {
        UpgradeTier targetTier = forge.getPendingTargetTier();
        ItemStack equipment    = forge.getPendingEquipment();
        if (targetTier == null || equipment == null) {
            forge.setState(ForgeState.READY);
            return;
        }

        double successChance = configManager.getForge().getDouble(
                "upgrades." + tierConfigKey(targetTier) + ".success-chance",
                targetTier.getSuccessChance());

        boolean success = Math.random() * 100 < successChance;

        Player player = forge.getUpgraderUuid() != null
                ? plugin.getServer().getPlayer(forge.getUpgraderUuid()) : null;

        if (success) {
            // Apply upgrade
            ItemStack upgraded = upgradeManager.applyNextTier(equipment.clone());
            forge.setOutputItem(upgraded);
            forge.incrementSuccessfulUpgrades();

            if (targetTier == UpgradeTier.MK_IV) {
                forge.incrementMk4Creations();
            }

            if (player != null) {
                player.sendMessage(colorize("&a☢ Upgrade SUCCESS! Your item is now " + targetTier.getDisplayName() + "!"));
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
                playerDataManager.get(player.getUniqueId()).ifPresent(d -> {
                    d.incrementSuccessfulUpgrades();
                    d.setDirty(true);
                    if (targetTier == UpgradeTier.MK_IV) d.incrementMk4Creations();
                });
                checkAdvancements(player, targetTier);
                plugin.getServer().getPluginManager().callEvent(
                        new ForgeUpgradeSuccessEvent(player, forge, upgraded, targetTier));
            }

            spawnSuccessParticles(forge);

        } else {
            // Fail — materials already consumed; check for equipment downgrade at MK-IV
            boolean downgraded = false;
            double downgradeChance = configManager.getForge().getDouble(
                    "upgrades." + tierConfigKey(targetTier) + ".downgrade-chance-on-fail", 0.0);

            if (targetTier == UpgradeTier.MK_IV && downgradeChance > 0 && Math.random() * 100 < downgradeChance) {
                equipment = upgradeManager.applyPreviousTier(equipment.clone());
                downgraded = true;
            }

            forge.setOutputItem(equipment);
            forge.incrementFailedUpgrades();

            if (player != null) {
                String msg = downgraded
                        ? "&c☢ Upgrade FAILED and item downgraded to MK-III! Materials lost."
                        : "&c☢ Upgrade FAILED. Materials lost, equipment returned safely.";
                player.sendMessage(colorize(msg));
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 0.7f);
                playerDataManager.get(player.getUniqueId()).ifPresent(d -> {
                    d.incrementFailedUpgrades();
                    d.setDirty(true);
                });
                plugin.getServer().getPluginManager().callEvent(
                        new ForgeUpgradeFailEvent(player, forge, equipment, targetTier, downgraded));
            }

            spawnFailParticles(forge);
        }

        forge.setPendingEquipment(null);
        forge.setPendingTargetTier(null);
        forge.setUpgraderUuid(null);
        forge.setState(ForgeState.COMPLETED);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Energy
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Called when a Radioactive Core is inserted into the catalyst slot.
     * Returns the energy amount added, or 0 if the forge would overload.
     */
    public double insertCore(ForgeData forge) {
        double newEnergy = forge.getEnergy() + energyPerCore;
        forge.setEnergy(newEnergy);
        if (forge.getState() == ForgeState.OFFLINE && newEnergy >= 100) {
            forge.setState(ForgeState.READY);
        }
        return energyPerCore;
    }

    public void setEnergy(ForgeData forge, double energy) {
        forge.setEnergy(Math.min(energy, maxEnergy * 1.5)); // allow slight over for overload testing
        if (forge.getState() == ForgeState.OFFLINE && energy >= 100) forge.setState(ForgeState.READY);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Particles
    // ──────────────────────────────────────────────────────────────────────────

    private void spawnUpgradeParticles(ForgeData forge) {
        Location loc = forge.getLocation().add(0.5, 1, 0.5);
        loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 3, 0.3, 0.3, 0.3, 0.01);
    }

    private void spawnSuccessParticles(ForgeData forge) {
        Location loc = forge.getLocation().add(0.5, 1, 0.5);
        loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 30, 0.5, 0.5, 0.5, 0.1);
        loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
    }

    private void spawnFailParticles(ForgeData forge) {
        Location loc = forge.getLocation().add(0.5, 1, 0.5);
        loc.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc, 15, 0.4, 0.4, 0.4, 0.05);
        loc.getWorld().playSound(loc, Sound.BLOCK_ANVIL_LAND, 1.0f, 0.5f);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Advancements
    // ──────────────────────────────────────────────────────────────────────────

    private void checkAdvancements(Player player, UpgradeTier tier) {
        if (tier.getLevel() >= 1) {
            advancementManager.award(player, AdvancementManager.Advancement.ENHANCED_ARSENAL);
        }
        if (tier == UpgradeTier.MK_IV) {
            advancementManager.award(player, AdvancementManager.Advancement.NUCLEAR_ENGINEER);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Persistence
    // ──────────────────────────────────────────────────────────────────────────

    private File getMachinesFile() {
        return new File(plugin.getDataFolder(), "forge-machines.yml");
    }

    private void loadMachines() {
        File file = getMachinesFile();
        if (!file.exists()) return;

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        int loaded = 0;

        for (String key : cfg.getKeys(false)) {
            try {
                String[] parts = key.replace("__", ",").split(",");
                if (parts.length < 4) continue;

                World world = plugin.getServer().getWorld(parts[0]);
                if (world == null) continue;

                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                int z = Integer.parseInt(parts[3]);
                Location loc = new Location(world, x, y, z);

                ForgeData data = new ForgeData(loc);
                data.setState(ForgeState.valueOf(cfg.getString(key + ".state", "READY")));
                data.setEnergy(cfg.getDouble(key + ".energy", 0.0));

                // Stats
                data.setTotalForgeUses(cfg.getInt(key + ".stats.total-uses", 0));
                data.setSuccessfulUpgrades(cfg.getInt(key + ".stats.successes", 0));
                data.setFailedUpgrades(cfg.getInt(key + ".stats.failures", 0));
                data.setMk4Creations(cfg.getInt(key + ".stats.mk4", 0));
                data.setOverloadCount(cfg.getInt(key + ".stats.overloads", 0));
                data.setTotalEnergyConsumed(cfg.getLong(key + ".stats.energy-consumed", 0));
                data.setTotalMaterialsConsumed(cfg.getLong(key + ".stats.materials-consumed", 0));

                // Stored output item
                ItemStack outputItem = cfg.getItemStack(key + ".output-item");
                if (outputItem != null) data.setOutputItem(outputItem);

                // Stored pending equipment (server crash recovery)
                ItemStack pending = cfg.getItemStack(key + ".pending-equipment");
                if (pending != null) {
                    // On load, place pending item as output so player can collect
                    data.setOutputItem(pending);
                }

                String uuidStr = cfg.getString(key + ".last-player");
                if (uuidStr != null) {
                    try { data.setLastInteractingPlayerUuid(UUID.fromString(uuidStr)); }
                    catch (IllegalArgumentException ignored) {}
                }

                // If was UPGRADING on save, reset to READY (item is in output as recovery)
                if (data.getState() == ForgeState.UPGRADING) data.setState(ForgeState.READY);

                machines.put(data.getLocationKey(), data);
                loaded++;
            } catch (Exception e) {
                NCLogger.severe("Failed to load forge at key: " + key, e);
            }
        }

        NCLogger.info("Loaded " + loaded + " Nuclear Forge machine(s).");
    }

    private void saveMachines() {
        File file = getMachinesFile();
        FileConfiguration cfg = new YamlConfiguration();

        for (ForgeData data : machines.values()) {
            String key = data.getLocationKey().replace(",", "__");

            cfg.set(key + ".world", data.getLocation().getWorld().getName());
            cfg.set(key + ".x",     data.getLocation().getBlockX());
            cfg.set(key + ".y",     data.getLocation().getBlockY());
            cfg.set(key + ".z",     data.getLocation().getBlockZ());
            cfg.set(key + ".state", data.getState().name());
            cfg.set(key + ".energy", data.getEnergy());

            cfg.set(key + ".stats.total-uses",          data.getTotalForgeUses());
            cfg.set(key + ".stats.successes",           data.getSuccessfulUpgrades());
            cfg.set(key + ".stats.failures",            data.getFailedUpgrades());
            cfg.set(key + ".stats.mk4",                 data.getMk4Creations());
            cfg.set(key + ".stats.overloads",           data.getOverloadCount());
            cfg.set(key + ".stats.energy-consumed",     data.getTotalEnergyConsumed());
            cfg.set(key + ".stats.materials-consumed",  data.getTotalMaterialsConsumed());

            if (data.getOutputItem() != null) cfg.set(key + ".output-item", data.getOutputItem());
            if (data.getPendingEquipment() != null) cfg.set(key + ".pending-equipment", data.getPendingEquipment());
            if (data.getLastInteractingPlayerUuid() != null)
                cfg.set(key + ".last-player", data.getLastInteractingPlayerUuid().toString());
        }

        try {
            cfg.save(file);
            NCLogger.debug("Saved " + machines.size() + " forge machine(s) to disk.");
        } catch (IOException e) {
            NCLogger.severe("Failed to save forge machines!", e);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Utility helpers
    // ──────────────────────────────────────────────────────────────────────────

    private int countCustomItem(ItemStack stack, String itemId) {
        if (isAir(stack)) return 0;
        // Identify by PDC nuclearcraft_item_id key
        if (!stack.hasItemMeta()) return 0;
        NamespacedKey key = new NamespacedKey(plugin, "nuclearcraft_item_id");
        String id = stack.getItemMeta().getPersistentDataContainer()
                .get(key, org.bukkit.persistence.PersistentDataType.STRING);
        if (itemId.equals(id)) return stack.getAmount();
        return 0;
    }

    private void consumeItems(Inventory inv, int slot, int amount) {
        ItemStack item = inv.getItem(slot);
        if (item == null) return;
        if (item.getAmount() <= amount) {
            inv.setItem(slot, null);
        } else {
            item.setAmount(item.getAmount() - amount);
        }
    }

    private void returnItemToPlayer(UUID uuid, ItemStack item) {
        if (uuid == null || item == null) return;
        Player player = plugin.getServer().getPlayer(uuid);
        if (player != null) {
            player.getInventory().addItem(item.clone()).forEach((k, v) ->
                    player.getWorld().dropItemNaturally(player.getLocation(), v));
        }
    }

    private boolean isAir(ItemStack item) {
        return item == null || item.getType().isAir();
    }

    private String tierConfigKey(UpgradeTier tier) {
        return "mk" + tier.getLevel();
    }

    @SuppressWarnings("deprecation")
    private String colorize(String s) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', s);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Public accessors
    // ──────────────────────────────────────────────────────────────────────────

    public int getMachineCount()        { return machines.size(); }
    public Collection<ForgeData> getMachines() { return machines.values(); }
    public NuclearCraftPlugin getPlugin() { return plugin; }
    public double getMaxEnergy()        { return maxEnergy; }
    public double getEnergyPerCore()    { return energyPerCore; }

    /**
     * Global aggregate stats across all forges, for /nc forge stats.
     */
    public int getGlobalForgeUses()       { return machines.values().stream().mapToInt(ForgeData::getTotalForgeUses).sum(); }
    public int getGlobalSuccesses()       { return machines.values().stream().mapToInt(ForgeData::getSuccessfulUpgrades).sum(); }
    public int getGlobalFailures()        { return machines.values().stream().mapToInt(ForgeData::getFailedUpgrades).sum(); }
    public int getGlobalMk4Creations()   { return machines.values().stream().mapToInt(ForgeData::getMk4Creations).sum(); }
    public int getGlobalOverloads()      { return machines.values().stream().mapToInt(ForgeData::getOverloadCount).sum(); }
}
