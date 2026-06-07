package com.nuclearcraft.titantech;

public enum TitanForgeState {
    OFFLINE("§8⬛ Offline"),
    READY("§a✔ Ready"),
    CRAFTING("§e⚙ Crafting"),
    POWERED("§b⚡ Powered"),
    OVERLOADED("§4☢ Overloaded"),
    ERROR("§c✘ Error");

    private final String displayName;

    TitanForgeState(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
