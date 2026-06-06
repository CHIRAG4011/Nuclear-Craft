package com.nuclearcraft.combat;

/**
 * Classifies the type of radiation damage for combat routing and statistics.
 *
 * Unlike {@link com.nuclearcraft.radiation.RadiationSource}, which identifies
 * the physical origin, RadiationDamageType categorises damage by mechanical context:
 * whether it came from a weapon swing, a projectile, a passive aura, etc.
 */
public enum RadiationDamageType {

    /** Direct melee weapon hit (Plutonium Sword, Plutonium Axe). */
    WEAPON("Weapon Hit"),

    /** Projectile hit from a Plutonium Arrow. */
    ARROW("Arrow Hit"),

    /** Passive radiation aura from MK-IV equipped player. */
    AURA("Radiation Aura"),

    /** Walking through an environmental radiation cloud. */
    CLOUD("Radiation Cloud"),

    /** Contagion spread from an infected player. */
    CONTAGION("Contagion"),

    /** Boss ability or attack. */
    BOSS("Boss Attack"),

    /** Environmental / passive exposure (ore, debris, farmland, etc.). */
    ENVIRONMENTAL("Environmental"),

    /** Admin command application. */
    ADMIN("Admin Command");

    private final String displayName;

    RadiationDamageType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
