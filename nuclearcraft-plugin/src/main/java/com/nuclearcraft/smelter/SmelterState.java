package com.nuclearcraft.smelter;

/**
 * Represents all possible states for a Nuclear Smelter machine.
 *
 * State transitions:
 *   OFFLINE → HEATING (when fuel + input are present)
 *   HEATING → ACTIVE (once temperature >= minimum processing temp)
 *   ACTIVE → COOLING (when fuel or input runs out)
 *   ACTIVE → OVERHEATED (if temperature exceeds maximum)
 *   COOLING → HEATING (when fuel + input re-added while still warm)
 *   COOLING → OFFLINE (once temperature returns to ambient)
 *   OVERHEATED → COOLING (after timeout)
 *   ERROR → OFFLINE (manual reset)
 */
public enum SmelterState {

    /**
     * Machine has no fuel or input, temperature is ambient.
     */
    OFFLINE("Offline", "☁ No fuel or input.", "gray"),

    /**
     * Machine has fuel and input but hasn't reached processing temperature yet.
     */
    HEATING("Heating", "🔥 Warming up...", "gold"),

    /**
     * Machine is at processing temperature and actively smelting.
     */
    ACTIVE("Active", "⚙ Processing...", "green"),

    /**
     * Machine has lost fuel or input; temperature is dropping.
     */
    COOLING("Cooling", "❄ Cooling down...", "aqua"),

    /**
     * Temperature exceeded the maximum threshold; processing halted temporarily.
     */
    OVERHEATED("Overheated!", "☢ DANGER: Overheat!", "red"),

    /**
     * Machine encountered an error condition (e.g., corrupted data on load).
     */
    ERROR("Error", "✖ Machine error.", "dark_red");

    private final String displayName;
    private final String description;
    private final String color;

    SmelterState(String displayName, String description, String color) {
        this.displayName = displayName;
        this.description = description;
        this.color = color;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public String getColor() { return color; }

    public boolean isProcessing() {
        return this == ACTIVE;
    }

    public boolean isHeating() {
        return this == HEATING || this == ACTIVE;
    }
}
