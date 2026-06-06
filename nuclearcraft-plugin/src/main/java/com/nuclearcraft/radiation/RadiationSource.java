package com.nuclearcraft.radiation;

/**
 * Identifies the origin of a radiation exposure event.
 * Used for logging, statistics, and future system hooks (achievements, boss events, etc.).
 */
public enum RadiationSource {

    /** Radiation received by being near or hit by an Irradiated Zombie. */
    IRRADIATED_ZOMBIE("Irradiated Zombie"),

    /** Radiation from mining or standing near Plutonium Ore. */
    PLUTONIUM_ORE("Plutonium Ore"),

    /** Radiation from standing near Radioactive Debris. */
    RADIOACTIVE_DEBRIS("Radioactive Debris"),

    /** Radiation from working on or near Radioactive Farmland. */
    RADIOACTIVE_FARMLAND("Radioactive Farmland"),

    /** Radiation from being hit with a Plutonium weapon. */
    PLUTONIUM_WEAPON("Plutonium Weapon"),

    /** Radiation received from proximity to or contact with an infected player. */
    RADIATED_PLAYER("Radiated Player"),

    /** Radiation from a Plutonium-tipped arrow. */
    PLUTONIUM_ARROW("Plutonium Arrow"),

    /** Radiation emitted by carrying Raw Plutonium Fragments in inventory. */
    PLUTONIUM_FRAGMENT("Plutonium Fragment"),

    /** Radiation from walking through a radiation cloud. */
    RADIATION_CLOUD("Radiation Cloud"),

    /** Radiation from a Plutonium Titan boss attack. */
    BOSS_ATTACK("Boss Attack"),

    /** Radiation emitted by an active Nuclear Smelter machine. */
    NUCLEAR_SMELTER("Nuclear Smelter"),

    /** Radiation applied directly via admin command. */
    COMMAND("Command"),

    /** Source could not be determined. */
    UNKNOWN("Unknown");

    private final String displayName;

    RadiationSource(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static RadiationSource fromString(String name) {
        if (name == null || name.isBlank()) return UNKNOWN;
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
