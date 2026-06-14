package com.nuclearcraft.smelter;

import com.nuclearcraft.advancements.AdvancementManager;
import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.core.NuclearCraftPlugin;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.events.NuclearSmelterCompleteEvent;
import com.nuclearcraft.events.NuclearSmelterOverheatEvent;
import com.nuclearcraft.events.NuclearSmelterStartEvent;
import com.nuclearcraft.items.CustomItem;
import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.utils.NCLogger;
import com.nuclearcraft.utils.ParticleUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central manager for all Nuclear Smelter machines.
 *
 * Responsibilities:
 *  - Registering and unregistering machines on place/break
 *  - Ticking all active machines every {@code tick-period} ticks
 *  - Managing the processing state machine (OFFLINE → HEATING → ACTIVE → …)
 *  - Consuming fuel, advancing recipe progress, producing output
 *  - Firing custom events (start, complete, overheat)
 *  - Persisting machine data to smelter_data.yml across restarts
 *  - Crediting the last-interacting player with statistics and advancements
 */
public class NuclearSmelterManager {

    private final NuclearCraftPlugin plugin;
    private final ConfigManager configManager;
    private final ItemManager itemManager;
    private final PlayerDataManager playerDataManager;
    private final AdvancementManager advancementManager;
    private final NuclearSmelterRecipeManager recipeManager;

    private final Map<String, SmelterData> machines = new ConcurrentHashMap<>();
    private BukkitTask tickTask;
    private int tickPeriod;

    public NuclearSmelterManager(NuclearCraftPlugin plugin, ConfigManager configManager,
                                  ItemManager itemManager, PlayerDataManager playerDataManager,
                                  AdvancementManager advancementManager,
                                  NuclearSmelterRecipeManager recipeManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.itemManager = itemManager;
        this.playerDataManager = playerDataManager;
        this.advancementManager = advancementManager;
        this.recipeManager = recipeManager;
    }

    public void initialize() {
        tickPeriod = configManager.getSmelter().getInt("machine.tick-period", 4);
        loadMachines();
        scheduleTick();
        NCLogger.info("NuclearSmelterManager initialized — " + machines.size() + " machine(s) loaded.");
    }

    public void shutdown() {
        if (tickTask != null && !tickTask.isCancelled()) {
            tickTask.cancel();
            tickTask = null;
        }
        saveMachines();
        NCLogger.info("NuclearSmelterManager shut down — " + machines.size() + " machine(s) saved.");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Machine Registration
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Registers a newly-placed Nuclear Smelter at the given block location.
     */
    public SmelterData registerMachine(Block block, UUID placerUuid) {
        String key = SmelterData.serializeLocation(block.getLocation());
        SmelterData data = new SmelterData(block.getLocation());
        data.setLastInteractingPlayerUuid(placerUuid);
        machines.put(key, data);
        NCLogger.debug("Smelter registered at %s by %s", key, placerUuid);
        return data;
    }

    /**
     * Unregisters a Nuclear Smelter, dropping any items still inside, and
     * returns a collection of items to be dropped at the block location.
     */
    public List<ItemStack> unregisterMachine(Block block) {
        String key = SmelterData.serializeLocation(block.getLocation());
        SmelterData data = machines.remove(key);
        if (data == null) return List.of();

        List<ItemStack> drops = new ArrayList<>();
        addIfNotEmpty(drops, data.getInputItem());
        addIfNotEmpty(drops, data.getFuelItem());
        addIfNotEmpty(drops, data.getOutputItem());

        NCLogger.debug("Smelter unregistered at %s — %d item stack(s) dropped.", key, drops.size());
        return drops;
    }

    private void addIfNotEmpty(List<ItemStack> list, ItemStack item) {
        if (item != null && !item.getType().isAir()) list.add(item.clone());
    }

    public SmelterData getMachine(Block block) {
        return machines.get(SmelterData.serializeLocation(block.getLocation()));
    }

    public boolean isMachine(Block block) {
        return machines.containsKey(SmelterData.serializeLocation(block.getLocation()));
    }

    public Collection<SmelterData> getAllMachines() {
        return Collections.unmodifiableCollection(machines.values());
    }

    public int getMachineCount() { return machines.size(); }

    // ──────────────────────────────────────────────────────────────────────────
    // Tick
    // ──────────────────────────────────────────────────────────────────────────

    private void scheduleTick() {
        tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (SmelterData machine : machines.values()) {
                try {
                    tickMachine(machine);
                } catch (Exception e) {
                    NCLogger.severe("Error ticking smelter at " + machine.getLocationKey(), e);
                    machine.setState(SmelterState.ERROR);
                }
            }
        }, tickPeriod, tickPeriod);
    }

    private void tickMachine(SmelterData machine) {
        var cfg = configManager.getSmelter();
        double heatingRate      = cfg.getDouble("temperature.heating-rate",       5.0);
        double activeHeatRate   = cfg.getDouble("temperature.active-heating-rate", 2.5);
        double coolingRate      = cfg.getDouble("temperature.cooling-rate",        1.0);
        double overheatCool     = cfg.getDouble("temperature.overheat-cooling-rate", 2.0);
        double minProcessTemp   = cfg.getDouble("temperature.min-processing",     500.0);
        double maxTemp          = cfg.getDouble("temperature.max",               1500.0);
        long   overheatMs       = cfg.getLong(  "temperature.overheat-duration-ms", 10000L);
        double restartFraction  = cfg.getDouble("temperature.restart-threshold-fraction", 0.80);

        SmelterState state = machine.getState();

        // ── OVERHEATED ────────────────────────────────────────────────────────
        if (state == SmelterState.OVERHEATED) {
            machine.setTemperature(machine.getTemperature() - overheatCool);
            if (System.currentTimeMillis() >= machine.getOverheatedUntilMs()) {
                machine.setState(SmelterState.COOLING);
                machine.resetProgress();
                playSound(machine, Sound.BLOCK_FIRE_EXTINGUISH, 0.4f, 1.0f);
            } else {
                spawnOverheatParticles(machine);
            }
            machine.refreshDisplay();
            return;
        }

        // ── Resolve fuel & recipe ─────────────────────────────────────────────
        boolean hasFuel   = consumeFuelIfNeeded(machine);
        String  inputId   = getInputItemId(machine);
        Optional<SmelterRecipe> recipeOpt = recipeManager.findRecipe(inputId);
        boolean hasRecipe = recipeOpt.isPresent();
        boolean hasSpace  = hasOutputSpace(machine, recipeOpt.orElse(null));
        boolean canRun    = hasFuel && hasRecipe && hasSpace;

        // ── State machine ─────────────────────────────────────────────────────
        switch (state) {
            case OFFLINE -> {
                if (canRun) {
                    machine.setState(SmelterState.HEATING);
                    playSound(machine, Sound.BLOCK_FURNACE_FIRE_CRACKLE, 0.5f, 0.7f);
                }
            }

            case HEATING -> {
                if (!canRun) {
                    machine.setState(SmelterState.COOLING);
                    break;
                }
                machine.setTemperature(machine.getTemperature() + heatingRate);
                spawnHeatingParticles(machine);

                if (machine.getTemperature() >= minProcessTemp) {
                    SmelterRecipe recipe = recipeOpt.get();
                    machine.setState(SmelterState.ACTIVE);
                    machine.setCurrentRecipeTotalTicks(recipe.getProcessingTicks());
                    machine.setCurrentRecipeId(recipe.getId());
                    machine.setProgressTicks(0);
                    plugin.getServer().getPluginManager().callEvent(
                            new NuclearSmelterStartEvent(machine, recipe));
                    playSound(machine, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.2f);
                }
            }

            case ACTIVE -> {
                if (!canRun) {
                    machine.setState(SmelterState.COOLING);
                    break;
                }

                machine.setTemperature(machine.getTemperature() + activeHeatRate);

                if (machine.getTemperature() >= maxTemp) {
                    enterOverheat(machine, overheatMs);
                    break;
                }

                // Re-initialize recipe timing if it was cleared by a prior completion
                if (machine.getCurrentRecipeTotalTicks() <= 0 && recipeOpt.isPresent()) {
                    machine.setCurrentRecipeTotalTicks(recipeOpt.get().getProcessingTicks());
                    machine.setCurrentRecipeId(recipeOpt.get().getId());
                    machine.setProgressTicks(0);
                }

                machine.setProgressTicks(machine.getProgressTicks() + tickPeriod);
                spawnActiveParticles(machine);

                if (machine.isComplete()) {
                    completeRecipe(machine, recipeOpt.get());
                }
            }

            case COOLING -> {
                if (canRun && machine.getTemperature() >= minProcessTemp * restartFraction) {
                    machine.setState(SmelterState.HEATING);
                    break;
                }
                machine.setTemperature(machine.getTemperature() - coolingRate);
                if (machine.getTemperature() <= 20.0) {
                    machine.setState(SmelterState.OFFLINE);
                }
            }

            case ERROR -> { /* awaiting manual reset; do nothing */ }
        }

        machine.refreshDisplay();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Fuel
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Drains 1 unit from the machine's internal fuel buffer, or consumes a
     * new fuel item from the fuel slot if the buffer is empty.
     *
     * @return true if fuel is available, false if the machine is out of fuel
     */
    private boolean consumeFuelIfNeeded(SmelterData machine) {
        if (machine.getFuelRemaining() > 0) {
            machine.setFuelRemaining(machine.getFuelRemaining() - 1);
            return true;
        }
        ItemStack fuelItem = machine.getFuelItem();
        if (fuelItem == null || fuelItem.getType().isAir()) return false;

        int value = getFuelValue(fuelItem.getType());
        if (value <= 0) return false;

        if (fuelItem.getType() == Material.LAVA_BUCKET) {
            machine.setFuelItem(new ItemStack(Material.BUCKET));
        } else if (fuelItem.getAmount() > 1) {
            fuelItem.setAmount(fuelItem.getAmount() - 1);
            machine.setFuelItem(fuelItem);
        } else {
            machine.setFuelItem(null);
        }

        machine.setFuelRemaining(value - 1);
        return true;
    }

    public int getFuelValue(Material material) {
        var cfg = configManager.getSmelter();
        return cfg.getInt("fuel.values." + material.name(), 0);
    }

    public boolean isValidFuel(Material material) {
        return getFuelValue(material) > 0;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Recipe helpers
    // ──────────────────────────────────────────────────────────────────────────

    private String getInputItemId(SmelterData machine) {
        ItemStack input = machine.getInputItem();
        if (input == null || input.getType().isAir()) return null;
        return CustomItem.getId(plugin, input);
    }

    private boolean hasOutputSpace(SmelterData machine, SmelterRecipe recipe) {
        if (recipe == null) return false;
        ItemStack existing = machine.getOutputItem();
        if (existing == null || existing.getType().isAir()) return true;
        Optional<CustomItem> out = itemManager.getItem(recipe.getOutputItemId());
        if (out.isEmpty()) return false;
        ItemStack sample = out.get().build();
        return existing.isSimilar(sample) && existing.getAmount() < existing.getMaxStackSize();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Recipe completion
    // ──────────────────────────────────────────────────────────────────────────

    private void completeRecipe(SmelterData machine, SmelterRecipe recipe) {
        ItemStack input = machine.getInputItem();
        if (input == null || input.getType().isAir()) {
            machine.resetProgress();
            return;
        }

        Optional<CustomItem> outputOpt = itemManager.getItem(recipe.getOutputItemId());
        if (outputOpt.isEmpty()) {
            NCLogger.warn("Smelter recipe '" + recipe.getId() + "' references unknown output: " + recipe.getOutputItemId());
            machine.resetProgress();
            return;
        }

        if (input.getAmount() > 1) {
            input.setAmount(input.getAmount() - 1);
            machine.setInputItem(input);
        } else {
            machine.setInputItem(null);
        }

        ItemStack outputItem = outputOpt.get().build();
        ItemStack existing   = machine.getOutputItem();
        if (existing == null || existing.getType().isAir()) {
            machine.setOutputItem(outputItem);
        } else {
            existing.setAmount(existing.getAmount() + 1);
            machine.setOutputItem(existing);
        }

        plugin.getServer().getPluginManager().callEvent(
                new NuclearSmelterCompleteEvent(machine, recipe, 1));

        playSound(machine, Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 1.0f, 1.5f);
        machine.resetProgress();

        creditPlayer(machine, recipe);
    }

    private void creditPlayer(SmelterData machine, SmelterRecipe recipe) {
        UUID uuid = machine.getLastInteractingPlayerUuid();
        if (uuid == null) return;

        playerDataManager.get(uuid).ifPresent(data -> {
            data.incrementFragmentsProcessed();
            data.incrementIngotsProduced();

            var player = plugin.getServer().getPlayer(uuid);

            if (data.getIngotsProduced() == 1 && player != null) {
                advancementManager.award(player, AdvancementManager.Advancement.FIRST_REFINEMENT);
            }
            if (data.getIngotsProduced() >= 100 && player != null) {
                advancementManager.award(player, AdvancementManager.Advancement.MASTER_REFINER);
            }
        });
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Overheat
    // ──────────────────────────────────────────────────────────────────────────

    private void enterOverheat(SmelterData machine, long durationMs) {
        machine.setState(SmelterState.OVERHEATED);
        machine.setOverheatedUntilMs(System.currentTimeMillis() + durationMs);
        machine.resetProgress();

        plugin.getServer().getPluginManager().callEvent(
                new NuclearSmelterOverheatEvent(machine, machine.getTemperature()));

        playSound(machine, Sound.BLOCK_ANVIL_BREAK, 1.0f, 0.5f);
        spawnOverheatParticles(machine);

        UUID uuid = machine.getLastInteractingPlayerUuid();
        if (uuid != null) {
            playerDataManager.get(uuid).ifPresent(d -> d.incrementOverheatsTriggered());
        }
        NCLogger.debug("Smelter overheat at %s (%.0f°C)", machine.getLocationKey(), machine.getTemperature());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Sounds & Particles
    // ──────────────────────────────────────────────────────────────────────────

    private void playSound(SmelterData machine, Sound sound, float volume, float pitch) {
        Location loc = machine.getLocation();
        if (loc.getWorld() == null) return;
        loc.getWorld().playSound(loc, sound, volume, pitch);
    }

    private void spawnHeatingParticles(SmelterData machine) {
        if (!configManager.getSmelter().getBoolean("particles.enabled", true)) return;
        Location loc = machine.getLocation().add(0.5, 1.0, 0.5);
        ParticleUtil.spawn(loc, Particle.FLAME, 2, 0.2, 0.2, 0.2);
        ParticleUtil.spawn(loc, Particle.SMOKE, 1, 0.1, 0.1, 0.1);
    }

    private void spawnActiveParticles(SmelterData machine) {
        if (!configManager.getSmelter().getBoolean("particles.enabled", true)) return;
        Location loc = machine.getLocation().add(0.5, 1.2, 0.5);
        int steamCount  = configManager.getSmelter().getInt("particles.active.steam-count", 4);
        int energyCount = configManager.getSmelter().getInt("particles.active.energy-count", 2);
        ParticleUtil.spawn(loc, Particle.CLOUD, steamCount, 0.3, 0.2, 0.3);
        ParticleUtil.spawnDust(loc, Color.fromRGB(57, 255, 20), 1.2f, energyCount);
    }

    private void spawnOverheatParticles(SmelterData machine) {
        if (!configManager.getSmelter().getBoolean("particles.enabled", true)) return;
        Location loc = machine.getLocation().add(0.5, 1.0, 0.5);
        int smoke  = configManager.getSmelter().getInt("particles.overheat.smoke-count", 12);
        int sparks = configManager.getSmelter().getInt("particles.overheat.spark-count", 8);
        ParticleUtil.spawn(loc, Particle.LARGE_SMOKE, smoke, 0.4, 0.4, 0.4);
        ParticleUtil.spawn(loc, Particle.LAVA, sparks, 0.3, 0.3, 0.3);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Persistence
    // ──────────────────────────────────────────────────────────────────────────

    private void saveMachines() {
        File file = new File(plugin.getDataFolder(), "smelter_data.yml");
        var cfg = new org.bukkit.configuration.file.YamlConfiguration();

        int index = 0;
        for (SmelterData machine : machines.values()) {
            String path = "machines.m" + index++;
            cfg.set(path + ".location-key",      machine.getLocationKey());
            cfg.set(path + ".state",              machine.getState().name());
            cfg.set(path + ".temperature",        machine.getTemperature());
            cfg.set(path + ".fuel-remaining",     machine.getFuelRemaining());
            cfg.set(path + ".progress-ticks",     machine.getProgressTicks());
            cfg.set(path + ".recipe-total-ticks", machine.getCurrentRecipeTotalTicks());
            cfg.set(path + ".recipe-id",          machine.getCurrentRecipeId());
            cfg.set(path + ".overheat-until",     machine.getOverheatedUntilMs());
            cfg.set(path + ".placer",
                    machine.getLastInteractingPlayerUuid() != null
                            ? machine.getLastInteractingPlayerUuid().toString() : null);

            ItemStack input  = machine.getInputItem();
            ItemStack fuel   = machine.getFuelItem();
            ItemStack output = machine.getOutputItem();
            if (input  != null && !input.getType().isAir())  cfg.set(path + ".input",  input);
            if (fuel   != null && !fuel.getType().isAir())   cfg.set(path + ".fuel",   fuel);
            if (output != null && !output.getType().isAir()) cfg.set(path + ".output", output);
        }

        try {
            cfg.save(file);
            NCLogger.debug("Saved %d smelter(s) to smelter_data.yml", machines.size());
        } catch (IOException e) {
            NCLogger.severe("Failed to save smelter_data.yml", e);
        }
    }

    private void loadMachines() {
        File file = new File(plugin.getDataFolder(), "smelter_data.yml");
        if (!file.exists()) return;

        var cfg = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        var section = cfg.getConfigurationSection("machines");
        if (section == null) return;

        int loaded = 0;
        for (String key : section.getKeys(false)) {
            String path = "machines." + key;
            String locationKey = cfg.getString(path + ".location-key");
            if (locationKey == null || locationKey.isBlank()) {
                NCLogger.warn("Smelter entry '" + key + "' has no location-key — skipping.");
                continue;
            }

            Location loc = SmelterData.deserializeLocation(locationKey);
            if (loc == null) {
                NCLogger.warn("Could not deserialize smelter location: '" + locationKey + "' — skipping.");
                continue;
            }

            String stateName     = cfg.getString(path + ".state", "OFFLINE");
            double temperature   = cfg.getDouble(path + ".temperature", 20.0);
            int fuelRemaining    = cfg.getInt(path + ".fuel-remaining", 0);
            int progressTicks    = cfg.getInt(path + ".progress-ticks", 0);
            int recipeTotalTicks = cfg.getInt(path + ".recipe-total-ticks", 0);
            String recipeId      = cfg.getString(path + ".recipe-id");
            long overheatUntil   = cfg.getLong(path + ".overheat-until", 0L);
            String placerStr     = cfg.getString(path + ".placer");

            SmelterData data = new SmelterData(loc);

            try { data.setState(SmelterState.valueOf(stateName)); }
            catch (IllegalArgumentException e) { data.setState(SmelterState.OFFLINE); }

            data.setTemperature(temperature);
            data.setFuelRemaining(fuelRemaining);
            data.setProgressTicks(progressTicks);
            data.setCurrentRecipeTotalTicks(recipeTotalTicks);
            data.setCurrentRecipeId(recipeId);
            data.setOverheatedUntilMs(overheatUntil);

            if (placerStr != null) {
                try { data.setLastInteractingPlayerUuid(UUID.fromString(placerStr)); }
                catch (IllegalArgumentException ignored) {}
            }

            ItemStack input  = cfg.getItemStack(path + ".input");
            ItemStack fuel   = cfg.getItemStack(path + ".fuel");
            ItemStack output = cfg.getItemStack(path + ".output");
            if (input  != null) data.setInputItem(input);
            if (fuel   != null) data.setFuelItem(fuel);
            if (output != null) data.setOutputItem(output);

            machines.put(data.getLocationKey(), data);
            loaded++;
        }

        NCLogger.info("Loaded " + loaded + " smelter(s) from smelter_data.yml.");
    }
}
