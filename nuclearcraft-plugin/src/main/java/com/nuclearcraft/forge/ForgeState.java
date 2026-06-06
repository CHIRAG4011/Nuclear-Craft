package com.nuclearcraft.forge;

/**
 * Represents the current operational state of a Nuclear Forge machine.
 */
public enum ForgeState {

    /** Forge has no energy and cannot operate. */
    OFFLINE,

    /** Forge is powered and waiting for input. */
    READY,

    /** Forge is currently processing an upgrade. */
    UPGRADING,

    /** Upgrade has finished; result is waiting in the output slot. */
    COMPLETED,

    /** An internal error occurred; operator must inspect. */
    ERROR,

    /** Energy exceeded safe limits; temporary shutdown with radiation burst. */
    OVERLOADED;

    public String displayName() {
        return switch (this) {
            case OFFLINE   -> "§7● OFFLINE";
            case READY     -> "§a● READY";
            case UPGRADING -> "§e● UPGRADING...";
            case COMPLETED -> "§b● COMPLETED";
            case ERROR     -> "§c● ERROR";
            case OVERLOADED-> "§4● OVERLOADED";
        };
    }
}
