package com.nuclearcraft.titantech;

import com.nuclearcraft.advancements.AdvancementManager;
import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.radiation.RadiationManager;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Top-level orchestrator for Phase 11: Titan Technology System.
 *
 * Initializes all Phase 11 sub-managers in dependency order:
 *  1. TitanForgeRecipeManager  — no deps
 *  2. TitanForgeManager        — needs RecipeManager, RadiationManager
 *  3. TitanArmorManager        — needs ItemManager
 *  4. SetBonusManager          — needs ArmorManager, RadiationManager
 *  5. TitanWeaponManager       — needs RadiationManager
 *  6. TitanArrowManager        — needs RadiationManager
 *  7. TitanUpgradeManager      — needs ItemManager
 *  8. TitanAuraManager         — needs WeaponManager, RadiationManager
 *  9. TitanRepairManager       — needs ItemManager
 */
public class TitanTechManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final ItemManager itemManager;
    private final RadiationManager radiationManager;
    private final PlayerDataManager playerDataManager;
    private final AdvancementManager advancementManager;

    private TitanForgeRecipeManager forgeRecipeManager;
    private TitanForgeManager forgeManager;
    private TitanArmorManager armorManager;
    private SetBonusManager setBonusManager;
    private TitanWeaponManager weaponManager;
    private TitanArrowManager arrowManager;
    private TitanUpgradeManager upgradeManager;
    private TitanAuraManager auraManager;
    private TitanRepairManager repairManager;

    public TitanTechManager(JavaPlugin plugin, ConfigManager configManager,
                             ItemManager itemManager, RadiationManager radiationManager,
                             PlayerDataManager playerDataManager,
                             AdvancementManager advancementManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.itemManager = itemManager;
        this.radiationManager = radiationManager;
        this.playerDataManager = playerDataManager;
        this.advancementManager = advancementManager;
    }

    public void initialize() {
        NCLogger.info("Initializing Phase 11: Titan Technology System...");
        FileConfiguration cfg = configManager.getTitanItems();

        forgeRecipeManager = new TitanForgeRecipeManager(plugin, itemManager, cfg);
        forgeRecipeManager.initialize();

        forgeManager = new TitanForgeManager(plugin, configManager, itemManager,
                forgeRecipeManager, playerDataManager, advancementManager, radiationManager);
        forgeManager.initialize();

        armorManager = new TitanArmorManager(plugin, itemManager, cfg);
        armorManager.initialize();

        setBonusManager = new SetBonusManager(plugin, armorManager, radiationManager,
                playerDataManager, cfg);
        setBonusManager.initialize();

        weaponManager = new TitanWeaponManager(plugin, itemManager, radiationManager,
                playerDataManager, cfg);
        weaponManager.initialize();

        arrowManager = new TitanArrowManager(plugin, itemManager, radiationManager,
                playerDataManager, cfg);
        arrowManager.initialize();

        upgradeManager = new TitanUpgradeManager(plugin, itemManager, cfg);
        upgradeManager.initialize();

        auraManager = new TitanAuraManager(plugin, itemManager, radiationManager,
                playerDataManager, weaponManager, cfg);
        auraManager.initialize();

        repairManager = new TitanRepairManager(plugin, itemManager, cfg);
        repairManager.initialize();

        NCLogger.info("Phase 11 Titan Technology System fully initialized.");
    }

    public void shutdown() {
        if (auraManager         != null) auraManager.shutdown();
        if (arrowManager        != null) arrowManager.shutdown();
        if (setBonusManager     != null) setBonusManager.shutdown();
        if (armorManager        != null) armorManager.shutdown();
        if (weaponManager       != null) weaponManager.shutdown();
        if (upgradeManager      != null) upgradeManager.shutdown();
        if (repairManager       != null) repairManager.shutdown();
        if (forgeManager        != null) forgeManager.shutdown();
        if (forgeRecipeManager  != null) forgeRecipeManager.shutdown();
        NCLogger.info("Phase 11 Titan Technology System shut down.");
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public TitanForgeRecipeManager getForgeRecipeManager() { return forgeRecipeManager; }
    public TitanForgeManager       getForgeManager()       { return forgeManager; }
    public TitanArmorManager       getArmorManager()       { return armorManager; }
    public SetBonusManager         getSetBonusManager()    { return setBonusManager; }
    public TitanWeaponManager      getWeaponManager()      { return weaponManager; }
    public TitanArrowManager       getArrowManager()       { return arrowManager; }
    public TitanUpgradeManager     getUpgradeManager()     { return upgradeManager; }
    public TitanAuraManager        getAuraManager()        { return auraManager; }
    public TitanRepairManager      getRepairManager()      { return repairManager; }
}
