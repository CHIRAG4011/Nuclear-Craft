package com.nuclearcraft.equipment;

import com.nuclearcraft.config.ConfigManager;
import com.nuclearcraft.data.PlayerDataManager;
import com.nuclearcraft.items.CustomItem;
import com.nuclearcraft.items.ItemManager;
import com.nuclearcraft.radiation.RadiationManager;
import com.nuclearcraft.radiation.RadiationSource;
import com.nuclearcraft.utils.NCLogger;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Plutonium Arrows — a custom projectile that irradiates, poisons, and marks targets.
 *
 * <h3>Mechanics</h3>
 * <ol>
 *   <li>A player fires a bow while a Plutonium Arrow is their consumable item.</li>
 *   <li>The fired {@link Arrow} UUID is tracked in an in-memory set.</li>
 *   <li>On projectile hit:
 *     <ul>
 *       <li>If the target is a player: apply radiation, Poison, and Glowing.</li>
 *       <li>Green impact particles spawn at the hit location.</li>
 *     </ul>
 *   </li>
 *   <li>Arrows expire naturally; the UUID is cleaned up on hit.</li>
 * </ol>
 */
public class PlutoniumArrowManager {

    private final JavaPlugin plugin;
    private final ItemManager itemManager;
    private final ConfigManager configManager;
    private final RadiationManager radiationManager;
    private final PlayerDataManager playerDataManager;

    /** Set of Arrow UUIDs currently in flight as Plutonium Arrows. */
    private final Set<UUID> activeArrows =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    public PlutoniumArrowManager(JavaPlugin plugin, ItemManager itemManager,
                                 ConfigManager configManager,
                                 RadiationManager radiationManager,
                                 PlayerDataManager playerDataManager) {
        this.plugin             = plugin;
        this.itemManager        = itemManager;
        this.configManager      = configManager;
        this.radiationManager   = radiationManager;
        this.playerDataManager  = playerDataManager;
    }

    public void initialize() {
        registerItem();
        registerRecipe();
        NCLogger.info("PlutoniumArrowManager initialized.");
    }

    public void shutdown() {
        activeArrows.clear();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Item + Recipe
    // ──────────────────────────────────────────────────────────────────────────

    private void registerItem() {
        CustomItem arrow = new CustomItem(plugin,
                "plutonium-arrow",
                "<gradient:#39ff14:#00ff88>Plutonium Arrow</gradient>",
                java.util.List.of(
                        "Tipped with refined plutonium.",
                        "<red>☢ On hit: irradiates, poisons, and marks target</red>",
                        "<green>Creates toxic impact particles</green>"),
                Material.ARROW, 1314, true);
        itemManager.getRegistry().register(arrow);
    }

    private void registerRecipe() {
        ItemStack result = itemManager.getRegistry().get("plutonium-arrow")
                .map(ci -> ci.build(4)).orElse(null);
        if (result == null) return;
        // P = refined-plutonium-ingot (ECHO_SHARD), A = arrow, F = feather
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, "plutonium_arrow"), result);
        recipe.shape(" P ", " A ", " F ");
        recipe.setIngredient('P', Material.ECHO_SHARD);
        recipe.setIngredient('A', Material.ARROW);
        recipe.setIngredient('F', Material.FEATHER);
        plugin.getServer().addRecipe(recipe);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Event handlers (called from EquipmentListener)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Called when a player shoots a bow. If the consumed arrow is a Plutonium Arrow,
     * tracks the projectile UUID.
     */
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getProjectile() instanceof Arrow arrow)) return;

        // Check consumable (the arrow item being spent)
        ItemStack consumable = event.getConsumable();
        if (consumable == null) return;

        Optional<CustomItem> arrowItem = itemManager.getItem("plutonium-arrow");
        if (arrowItem.isEmpty() || !arrowItem.get().matches(consumable)) return;

        activeArrows.add(arrow.getUniqueId());

        playerDataManager.get(player.getUniqueId()).ifPresent(data ->
                data.setArrowsFired(data.getArrowsFired() + 1));

        NCLogger.debug("Plutonium arrow fired by %s, tracking UUID %s",
                player.getName(), arrow.getUniqueId());
    }

    /**
     * Called when a projectile hits something. If tracked, applies hit effects.
     */
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        UUID id = arrow.getUniqueId();
        if (!activeArrows.remove(id)) return; // not a plutonium arrow

        var cfg    = configManager.getEquipment();
        int radAmt = cfg.getInt("arrows.radiation-amount", 25);
        int poisonDur = cfg.getInt("arrows.poison-duration-ticks", 80);
        int poisonAmp = cfg.getInt("arrows.poison-amplifier", 0);
        int glowDur   = cfg.getInt("arrows.glowing-duration-ticks", 100);

        Entity hitEntity = event.getHitEntity();
        if (hitEntity instanceof Player target) {
            radiationManager.addRadiation(target, radAmt, RadiationSource.PLUTONIUM_ARROW);
            target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, poisonDur, poisonAmp));
            target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, glowDur, 0));
        }

        spawnImpactParticles(arrow.getLocation());
        arrow.getWorld().playSound(arrow.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 1.0f, 0.8f);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private void spawnImpactParticles(Location loc) {
        loc.getWorld().spawnParticle(Particle.DUST,
                loc, 20, 0.4, 0.4, 0.4, 0,
                new Particle.DustOptions(Color.LIME, 1.5f));
        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1, 0, 0, 0, 0);
    }
}
