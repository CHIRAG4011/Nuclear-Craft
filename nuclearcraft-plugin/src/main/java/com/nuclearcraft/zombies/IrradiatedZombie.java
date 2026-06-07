package com.nuclearcraft.zombies;

import com.nuclearcraft.utils.ColorUtil;
import com.nuclearcraft.utils.NCLogger;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Wrapper around a Bukkit {@link Zombie} entity that represents an Irradiated Zombie.
 *
 * All data is stored in the entity's PersistentDataContainer so it survives
 * chunk unloads, server restarts, and entity tracking gaps.
 *
 * PDC keys:
 *   nuclearcraft:is_irradiated    — byte 1
 *   nuclearcraft:zombie_level     — int (1-4)
 *   nuclearcraft:zombie_spawn_ms  — long (epoch ms)
 *   nuclearcraft:zombie_variant   — String (NORMAL | ALPHA)
 *   nuclearcraft:radiation_power  — int (radiation per hit)
 */
public class IrradiatedZombie {

    // Default zombie movement speed used as baseline for multiplier math
    private static final double BASE_ZOMBIE_SPEED    = 0.23;
    private static final double BASE_FOLLOW_RANGE    = 35.0;

    private static NamespacedKey keyIsIrradiated;
    private static NamespacedKey keyLevel;
    private static NamespacedKey keySpawnMs;
    private static NamespacedKey keyVariant;
    private static NamespacedKey keyRadiationPower;

    public static void initKeys(JavaPlugin plugin) {
        keyIsIrradiated   = new NamespacedKey(plugin, "is_irradiated");
        keyLevel          = new NamespacedKey(plugin, "zombie_level");
        keySpawnMs        = new NamespacedKey(plugin, "zombie_spawn_ms");
        keyVariant        = new NamespacedKey(plugin, "zombie_variant");
        keyRadiationPower = new NamespacedKey(plugin, "radiation_power");
    }

    // ──────────────────────────────────────────────────────────────────────────

    private final Zombie entity;
    private final ZombieLevel zombieLevel;
    private final String variant;  // "NORMAL" or "ALPHA"

    private IrradiatedZombie(Zombie entity, ZombieLevel level, String variant) {
        this.entity = entity;
        this.zombieLevel = level;
        this.variant = variant;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Factory
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Tags an existing zombie entity as an Irradiated Zombie and applies all stat modifiers.
     *
     * @param zombie     the entity to convert
     * @param level      the zombie's tier
     * @param surgeMult  true during a Radiation Surge (all zombies are irradiated)
     */
    public static IrradiatedZombie create(JavaPlugin plugin, Zombie zombie, ZombieLevel level, boolean surgeMult) {
        String variant = level.isAlpha() ? "ALPHA" : "NORMAL";
        IrradiatedZombie iz = new IrradiatedZombie(zombie, level, variant);
        iz.applyPdc(plugin, level, variant);
        iz.applyAttributes(level);
        iz.applyAppearance(level);
        return iz;
    }

    /**
     * Reconstructs an IrradiatedZombie from a zombie that already has PDC tags.
     * Returns null if the entity is not an irradiated zombie.
     */
    public static IrradiatedZombie fromExisting(Zombie zombie) {
        if (!isIrradiated(zombie)) return null;
        int levelInt = zombie.getPersistentDataContainer().getOrDefault(keyLevel, PersistentDataType.INTEGER, 1);
        String variant = zombie.getPersistentDataContainer().getOrDefault(keyVariant, PersistentDataType.STRING, "NORMAL");
        ZombieLevel level = ZombieLevel.fromInt(levelInt);
        return new IrradiatedZombie(zombie, level, variant);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // PDC utilities
    // ──────────────────────────────────────────────────────────────────────────

    private void applyPdc(JavaPlugin plugin, ZombieLevel level, String variant) {
        var pdc = entity.getPersistentDataContainer();
        pdc.set(keyIsIrradiated,   PersistentDataType.BYTE,    (byte) 1);
        pdc.set(keyLevel,          PersistentDataType.INTEGER,  level.getLevel());
        pdc.set(keySpawnMs,        PersistentDataType.LONG,     System.currentTimeMillis());
        pdc.set(keyVariant,        PersistentDataType.STRING,   variant);
        pdc.set(keyRadiationPower, PersistentDataType.INTEGER,  level.getRadiationOnHit());
    }

    /** Returns true if the given zombie has the nuclearcraft irradiated PDC tag. */
    public static boolean isIrradiated(Zombie zombie) {
        if (keyIsIrradiated == null) return false;
        Byte val = zombie.getPersistentDataContainer().get(keyIsIrradiated, PersistentDataType.BYTE);
        return val != null && val == 1;
    }

    /** Returns the radiation-per-hit stored in PDC, or 0 if not tagged. */
    public static int getRadiationPower(Zombie zombie) {
        if (keyRadiationPower == null) return 0;
        return zombie.getPersistentDataContainer().getOrDefault(keyRadiationPower, PersistentDataType.INTEGER, 0);
    }

    /** Returns the stored level integer (1-4), or 1 if not tagged. */
    public static int getLevel(Zombie zombie) {
        if (keyLevel == null) return 1;
        return zombie.getPersistentDataContainer().getOrDefault(keyLevel, PersistentDataType.INTEGER, 1);
    }

    public static boolean isAlpha(Zombie zombie) {
        if (keyVariant == null) return false;
        return "ALPHA".equals(zombie.getPersistentDataContainer().get(keyVariant, PersistentDataType.STRING));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Attribute application
    // ──────────────────────────────────────────────────────────────────────────

    private void applyAttributes(ZombieLevel level) {
        // Health
        AttributeInstance maxHealth = entity.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(level.getHealth());
            entity.setHealth(level.getHealth());
        }

        // Attack damage
        AttributeInstance damage = entity.getAttribute(Attribute.ATTACK_DAMAGE);
        if (damage != null) damage.setBaseValue(level.getAttackDamage());

        // Movement speed
        AttributeInstance speed = entity.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(BASE_ZOMBIE_SPEED * level.getSpeedMultiplier());

        // Knockback resistance
        AttributeInstance kbRes = entity.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (kbRes != null) kbRes.setBaseValue(level.getKnockbackResistance());

        // Follow range
        AttributeInstance followRange = entity.getAttribute(Attribute.FOLLOW_RANGE);
        if (followRange != null) followRange.setBaseValue(BASE_FOLLOW_RANGE * level.getFollowRangeMultiplier());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Appearance
    // ──────────────────────────────────────────────────────────────────────────

    private void applyAppearance(ZombieLevel level) {
        // Custom name
        entity.customName(ColorUtil.parse(buildName(level)));
        entity.setCustomNameVisible(level.isAlpha()); // Alpha shows name above head

        // All irradiated zombies glow green
        entity.setGlowing(true);

        // Green dyed leather armour — makes the zombie visually distinct in-game
        // without requiring OptiFine or shader mods
        applyGreenArmour(level);
    }

    /**
     * Equips green dyed leather armour scaled to zombie tier.
     * Pieces have 0% drop chance so players can't farm leather from them.
     *
     * Level 1 → helmet only
     * Level 2 → helmet + chestplate
     * Level 3 → full set (dim green)
     * Level 4 (Alpha) → full set (bright toxic green)
     */
    private void applyGreenArmour(ZombieLevel level) {
        EntityEquipment eq = entity.getEquipment();
        if (eq == null) return;

        // Neon green for Alpha, darker for normals
        Color colour = level.isAlpha()
                ? Color.fromRGB(100, 255, 30)  // toxic bright green (Alpha)
                : Color.fromRGB(40,  150, 20);  // dark radioactive green

        ItemStack helmet     = dyedLeather(Material.LEATHER_HELMET,     colour);
        ItemStack chestplate = dyedLeather(Material.LEATHER_CHESTPLATE, colour);
        ItemStack leggings   = dyedLeather(Material.LEATHER_LEGGINGS,   colour);
        ItemStack boots      = dyedLeather(Material.LEATHER_BOOTS,      colour);

        eq.setHelmet(helmet);
        eq.setHelmetDropChance(0f);

        if (level.getLevel() >= 2) {
            eq.setChestplate(chestplate);
            eq.setChestplateDropChance(0f);
        }
        if (level.getLevel() >= 3) {
            eq.setLeggings(leggings);
            eq.setLeggingsDropChance(0f);
            eq.setBoots(boots);
            eq.setBootsDropChance(0f);
        }
    }

    private ItemStack dyedLeather(Material material, Color colour) {
        ItemStack item = new ItemStack(material);
        if (item.getItemMeta() instanceof LeatherArmorMeta meta) {
            meta.setColor(colour);
            meta.setDisplayName(""); // hide name in tooltip
            item.setItemMeta(meta);
        }
        return item;
    }

    private String buildName(ZombieLevel level) {
        return switch (level.getLevel()) {
            case 2 -> "<yellow>☢ " + level.getDisplayName() + " II</yellow>";
            case 3 -> "<gold>☢ " + level.getDisplayName() + " III</gold>";
            case 4 -> "<gradient:#39ff14:#ff0000>☢ " + level.getDisplayName() + " ☢</gradient>";
            default -> "<green>☢ " + level.getDisplayName() + "</green>";
        };
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Accessors
    // ──────────────────────────────────────────────────────────────────────────

    public Zombie getEntity() { return entity; }
    public ZombieLevel getZombieLevel() { return zombieLevel; }
    public String getVariant() { return variant; }
    public boolean isAlphaZombie() { return "ALPHA".equals(variant); }
    public boolean isAlive() { return !entity.isDead() && entity.isValid(); }
}
