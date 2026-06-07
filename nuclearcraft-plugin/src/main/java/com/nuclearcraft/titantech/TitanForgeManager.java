package com.nuclearcraft.titantech;

import com.nuclearcraft.advancements.AdvancementManager;
import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.radiation.RadiationManager;
import com.nuclearcraft.radiation.RadiationSource;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central manager for all Titan Reactor Forge machines.
 */
public class TitanForgeManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final ItemManager itemManager;
    private final TitanForgeRecipeManager recipeManager;
    private final PlayerDataManager playerDataManager;
    private final AdvancementManager advancementManager;
    private final RadiationManager radiationManager;

    private final Map<String, TitanForgeData> machines = new ConcurrentHashMap<>();
    private final Map<UUID, TitanForgeGUI> openGUIs   = new ConcurrentHashMap<>();
    private final Map<String, Long> overloadCooldowns  = new ConcurrentHashMap<>();

    private BukkitTask tickTask;
    private BukkitTask guiRefreshTask;

    private int tickPeriod;
    private double maxEnergy;
    private double energyPerCore;
    private double energyDecayPerTick;
    private int overloadCooldownTicks;
    private double overloadBurstRadius;
    private int overloadBurstAmount;
    private String guiTitle;
    private int guiRefreshTicks;

    public TitanForgeManager(JavaPlugin plugin, ConfigManager configManager,
                              ItemManager itemManager, TitanForgeRecipeManager recipeManager,
                              PlayerDataManager playerDataManager,
                              AdvancementManager advancementManager,
                              RadiationManager radiationManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.itemManager = itemManager;
        this.recipeManager = recipeManager;
        this.playerDataManager = playerDataManager;
        this.advancementManager = advancementManager;
        this.radiationManager = radiationManager;
    }

    public void initialize() {
        loadConfig();
        loadMachines();
        scheduleTick();
        scheduleGuiRefresh();
        NCLogger.info("TitanForgeManager initialized — " + machines.size() + " titan forge(s) loaded.");
    }

    public void shutdown() {
        if (tickTask != null)      { tickTask.cancel();      tickTask = null; }
        if (guiRefreshTask != null){ guiRefreshTask.cancel(); guiRefreshTask = null; }
        saveMachines();
        NCLogger.info("TitanForgeManager shut down — " + machines.size() + " titan forge(s) saved.");
    }

    // ── Config ───────────────────────────────────────────────────────────────

    private void loadConfig() {
        FileConfiguration cfg = configManager.getTitanItems();
        tickPeriod            = cfg.getInt("titan-forge.tick-period", 4);
        maxEnergy             = cfg.getDouble("titan-forge.max-energy", 15000.0);
        energyPerCore         = cfg.getDouble("titan-forge.energy-per-core", 5000.0);
        energyDecayPerTick    = cfg.getDouble("titan-forge.energy-decay-per-tick", 0.5);
        overloadCooldownTicks = cfg.getInt("titan-forge.overload-cooldown-ticks", 400);
        overloadBurstRadius   = cfg.getDouble("titan-forge.overload-burst-radius", 10.0);
        overloadBurstAmount   = cfg.getInt("titan-forge.overload-burst-amount", 300);
        guiTitle              = cfg.getString("titan-forge.gui-title", "§5☢ Titan Reactor Forge ☢");
        guiRefreshTicks       = cfg.getInt("titan-forge.gui-refresh-ticks", 10);
    }

    // ── Machine registration ─────────────────────────────────────────────────

    public TitanForgeData registerMachine(Block block, UUID placer) {
        String key = TitanForgeData.serializeLocation(block.getLocation());
        TitanForgeData data = new TitanForgeData(block.getLocation());
        data.setLastInteractUuid(placer);
        data.setState(TitanForgeState.READY);
        machines.put(key, data);
        return data;
    }

    public void unregisterMachine(Block block) {
        String key = TitanForgeData.serializeLocation(block.getLocation());
        TitanForgeData data = machines.remove(key);
        if (data != null && data.hasOutput()) {
            UUID who = data.getCrafterUuid() != null ? data.getCrafterUuid() : data.getLastInteractUuid();
            returnItemToPlayer(who, data.getPendingOutput());
        }
    }

    public boolean isTitanForge(Block block) {
        return machines.containsKey(TitanForgeData.serializeLocation(block.getLocation()));
    }

    public Optional<TitanForgeData> getForgeAt(Block block) {
        return Optional.ofNullable(machines.get(TitanForgeData.serializeLocation(block.getLocation())));
    }

    // ── GUI ──────────────────────────────────────────────────────────────────

    public void openGUI(Player player, TitanForgeData forge) {
        TitanForgeGUI gui = new TitanForgeGUI(guiTitle, forge, this, recipeManager, itemManager, maxEnergy);
        openGUIs.put(player.getUniqueId(), gui);
        forge.setLastInteractUuid(player.getUniqueId());
        gui.open(player);
    }

    public void onGUIClosed(UUID uuid) { openGUIs.remove(uuid); }

    // ── Craft logic ──────────────────────────────────────────────────────────

    public void tryStartCraft(Player player, TitanForgeData forge, Inventory inv, TitanForgeRecipe recipe) {
        if (forge.isCrafting()) {
            player.sendMessage("§c☢ The Titan Forge is already crafting!"); return;
        }
        if (forge.getState() == TitanForgeState.OVERLOADED) {
            player.sendMessage("§4☢ Forge is overloaded — wait for it to stabilize."); return;
        }
        if (forge.hasOutput()) {
            player.sendMessage("§e☢ Collect the output item first."); return;
        }

        ItemStack mat1     = inv.getItem(TitanForgeGUI.SLOT_MAT1);
        ItemStack mat2     = inv.getItem(TitanForgeGUI.SLOT_MAT2);
        ItemStack catalyst = inv.getItem(TitanForgeGUI.SLOT_CATALYST);

        int m1Count   = countCustomItem(mat1, recipe.getMaterial1Id());
        int m2Count   = countCustomItem(mat2, recipe.getMaterial2Id());
        int coreCount = countCustomItem(catalyst, "titan-core");

        if (m1Count < recipe.getMaterial1Amount()) {
            player.sendMessage("§c☢ Need " + recipe.getMaterial1Amount() + "x " + recipe.getMaterial1Id()
                    + " (have " + m1Count + ")."); return;
        }
        if (m2Count < recipe.getMaterial2Amount()) {
            player.sendMessage("§c☢ Need " + recipe.getMaterial2Amount() + "x " + recipe.getMaterial2Id()
                    + " (have " + m2Count + ")."); return;
        }
        if (coreCount < recipe.getCoresRequired()) {
            player.sendMessage("§c☢ Need " + recipe.getCoresRequired() + " Titan Core(s) in Catalyst slot (have " + coreCount + ")."); return;
        }

        // Consume materials and activate
        consumeItems(inv, TitanForgeGUI.SLOT_MAT1, recipe.getMaterial1Amount());
        consumeItems(inv, TitanForgeGUI.SLOT_MAT2, recipe.getMaterial2Amount());
        consumeItems(inv, TitanForgeGUI.SLOT_CATALYST, recipe.getCoresRequired());
        forge.setEnergy(forge.getEnergy() + recipe.getCoresRequired() * energyPerCore);

        forge.setActiveRecipe(recipe);
        forge.setCrafterUuid(player.getUniqueId());
        long now = plugin.getServer().getCurrentTick();
        forge.setCraftStartTick(now);
        forge.setCraftEndTick(now + recipe.getDurationTicks());
        forge.setCrafting(true);
        forge.setState(TitanForgeState.CRAFTING);
        forge.incrementTotalCrafts();

        playerDataManager.get(player.getUniqueId()).ifPresent(d -> {
            d.incrementTitanEquipmentCrafted();
            d.setDirty(true);
        });

        player.sendMessage("§5☢ Titan Reactor Forge activated! Crafting " + recipe.getDisplayName() + "...");
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.5f);
        refreshOpenGUI(player.getUniqueId());
    }

    // ── Tick loop ────────────────────────────────────────────────────────────

    private void scheduleTick() {
        tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (TitanForgeData forge : machines.values()) {
                try { tickMachine(forge); }
                catch (Exception e) {
                    NCLogger.severe("Error ticking Titan Forge at " + forge.getLocationKey(), e);
                    forge.setState(TitanForgeState.ERROR);
                }
            }
        }, tickPeriod, tickPeriod);
    }

    private void scheduleGuiRefresh() {
        guiRefreshTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () ->
                openGUIs.forEach((uuid, gui) -> {
                    if (gui.getInventory() != null) gui.refreshDisplay(gui.getInventory());
                }), guiRefreshTicks, guiRefreshTicks);
    }

    private void tickMachine(TitanForgeData forge) {
        if (forge.getEnergy() > 0) forge.consumeEnergy(energyDecayPerTick * tickPeriod);

        if (forge.getEnergy() > maxEnergy) {
            triggerOverload(forge); return;
        }

        if (forge.getState() == TitanForgeState.OVERLOADED) {
            long end = overloadCooldowns.getOrDefault(forge.getLocationKey(), 0L);
            if (plugin.getServer().getCurrentTick() >= end) {
                forge.setState(TitanForgeState.READY);
                overloadCooldowns.remove(forge.getLocationKey());
            }
            return;
        }

        if (forge.getState() == TitanForgeState.OFFLINE && forge.getEnergy() >= 100) {
            forge.setState(TitanForgeState.READY);
        }

        if (forge.isCrafting()) {
            spawnCraftParticles(forge);
            if (plugin.getServer().getCurrentTick() >= forge.getCraftEndTick()) {
                resolveCraft(forge);
            }
        }
    }

    private void triggerOverload(TitanForgeData forge) {
        forge.setState(TitanForgeState.OVERLOADED);
        forge.setEnergy(0);
        forge.incrementOverloads();

        Location loc = forge.getLocation().add(0.5, 0.5, 0.5);
        loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 120, 3, 3, 3, 0.15);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.4f);

        for (Player nearby : loc.getWorld().getNearbyPlayers(loc, overloadBurstRadius)) {
            radiationManager.addRadiation(nearby, overloadBurstAmount, RadiationSource.NUCLEAR_FORGE);
            nearby.sendMessage("§4☢ The Titan Forge overloaded! Absorbed " + overloadBurstAmount + " radiation!");
        }

        overloadCooldowns.put(forge.getLocationKey(),
                (long) plugin.getServer().getCurrentTick() + overloadCooldownTicks);

        if (forge.isCrafting()) {
            forge.setCrafting(false);
            forge.setActiveRecipe(null);
        }
    }

    private void resolveCraft(TitanForgeData forge) {
        TitanForgeRecipe recipe = forge.getActiveRecipe();
        if (recipe == null) { forge.setCrafting(false); forge.setState(TitanForgeState.READY); return; }

        boolean success = Math.random() * 100 < recipe.getSuccessChance();
        Player player   = forge.getCrafterUuid() != null
                ? plugin.getServer().getPlayer(forge.getCrafterUuid()) : null;

        if (success) {
            ItemStack result = itemManager.getItem(recipe.getOutputItemId())
                    .map(i -> i.build(1))
                    .orElse(new ItemStack(Material.NETHER_STAR));

            // Handle titan-arrow giving 8 at once
            if (recipe.getOutputItemId().equals("titan-arrow")) result.setAmount(8);

            forge.setPendingOutput(result);
            forge.setHasOutput(true);
            forge.incrementSuccessfulCrafts();

            if (player != null) {
                player.sendMessage("§5☢ Titan Forge SUCCESS! " + recipe.getDisplayName() + " is ready!");
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.8f);
                playerDataManager.get(player.getUniqueId()).ifPresent(d -> {
                    d.incrementTitanEquipmentCrafted();
                    d.setDirty(true);
                });
                checkAdvancements(player, recipe);
            }
            spawnSuccessParticles(forge);
        } else {
            forge.incrementFailedCrafts();
            if (player != null) {
                player.sendMessage("§c☢ Titan Forge FAILED. Materials consumed — the reactor destabilized!");
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 0.6f);
            }
            spawnFailParticles(forge);
        }

        forge.setCrafting(false);
        forge.setActiveRecipe(null);
        forge.setCrafterUuid(null);
        forge.setState(TitanForgeState.READY);
    }

    // ── Advancements ─────────────────────────────────────────────────────────

    private void checkAdvancements(Player player, TitanForgeRecipe recipe) {
        advancementManager.award(player, AdvancementManager.Advancement.TITAN_ENGINEER);
        String id = recipe.getOutputItemId();
        if (id.equals("titan-helmet") || id.equals("titan-chestplate")
                || id.equals("titan-leggings") || id.equals("titan-boots")) {
            advancementManager.award(player, AdvancementManager.Advancement.TITAN_WARRIOR);
        }
    }

    // ── Particles ────────────────────────────────────────────────────────────

    private void spawnCraftParticles(TitanForgeData forge) {
        Location loc = forge.getLocation().add(0.5, 1.0, 0.5);
        loc.getWorld().spawnParticle(Particle.WITCH, loc, 4, 0.3, 0.3, 0.3, 0.05);
        loc.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc, 2, 0.2, 0.2, 0.2, 0.01);
    }

    private void spawnSuccessParticles(TitanForgeData forge) {
        Location loc = forge.getLocation().add(0.5, 1.0, 0.5);
        loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 50, 0.6, 0.6, 0.6, 0.08);
        loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 20, 0.5, 0.5, 0.5, 0.05);
        loc.getWorld().playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.8f);
    }

    private void spawnFailParticles(TitanForgeData forge) {
        Location loc = forge.getLocation().add(0.5, 1.0, 0.5);
        loc.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc, 20, 0.5, 0.5, 0.5, 0.05);
        loc.getWorld().playSound(loc, Sound.BLOCK_ANVIL_LAND, 1.0f, 0.6f);
    }

    // ── Persistence ──────────────────────────────────────────────────────────

    private File getSaveFile() { return new File(plugin.getDataFolder(), "titan-forges.yml"); }

    private void loadMachines() {
        File f = getSaveFile();
        if (!f.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(f);
        for (String key : yaml.getKeys(false)) {
            try {
                String worldName = yaml.getString(key + ".world");
                int x = yaml.getInt(key + ".x");
                int y = yaml.getInt(key + ".y");
                int z = yaml.getInt(key + ".z");
                World world = plugin.getServer().getWorld(worldName);
                if (world == null) continue;
                Location loc  = new Location(world, x, y, z);
                TitanForgeData data = new TitanForgeData(loc);
                data.setEnergy(yaml.getDouble(key + ".energy", 0));
                String sn = yaml.getString(key + ".state", "READY");
                try { data.setState(TitanForgeState.valueOf(sn)); }
                catch (Exception ignored) { data.setState(TitanForgeState.READY); }
                machines.put(key, data);
            } catch (Exception e) {
                NCLogger.severe("Failed to load Titan Forge at: " + key, e);
            }
        }
    }

    private void saveMachines() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, TitanForgeData> entry : machines.entrySet()) {
            String key = entry.getKey();
            TitanForgeData data = entry.getValue();
            Location loc = data.getLocation();
            yaml.set(key + ".world", loc.getWorld().getName());
            yaml.set(key + ".x", loc.getBlockX());
            yaml.set(key + ".y", loc.getBlockY());
            yaml.set(key + ".z", loc.getBlockZ());
            yaml.set(key + ".energy", data.getEnergy());
            yaml.set(key + ".state", data.isCrafting() ? "READY" : data.getState().name());
        }
        try { yaml.save(getSaveFile()); }
        catch (IOException e) { NCLogger.severe("Failed to save Titan Forge data", e); }
    }

    // ── Utilities ────────────────────────────────────────────────────────────

    private int countCustomItem(ItemStack item, String customId) {
        if (item == null || item.getType().isAir()) return 0;
        return itemManager.getItem(customId)
                .map(ci -> ci.matches(item) ? item.getAmount() : 0)
                .orElse(0);
    }

    private void consumeItems(Inventory inv, int slot, int amount) {
        ItemStack item = inv.getItem(slot);
        if (item == null) return;
        if (item.getAmount() <= amount) inv.setItem(slot, null);
        else item.setAmount(item.getAmount() - amount);
    }

    private void returnItemToPlayer(UUID uuid, ItemStack item) {
        if (uuid == null || item == null) return;
        Player player = plugin.getServer().getPlayer(uuid);
        if (player != null && player.isOnline()) {
            player.getInventory().addItem(item).forEach((k, v) ->
                    player.getWorld().dropItemNaturally(player.getLocation(), v));
        }
    }

    private void refreshOpenGUI(UUID uuid) {
        TitanForgeGUI gui = openGUIs.get(uuid);
        if (gui != null && gui.getInventory() != null) gui.refreshDisplay(gui.getInventory());
    }

    public JavaPlugin getPlugin() { return plugin; }
    public Map<String, TitanForgeData> getMachines() { return Collections.unmodifiableMap(machines); }
}
