package com.nuclearcraft.resourcepack;

import com.nuclearcraft.utils.NCLogger;

/**
 * Central asset registry for NuclearCraft.
 *
 * Validates and reports on all registered asset sub-registries
 * (models, sounds, textures) on startup.
 */
public final class AssetRegistry {

    private AssetRegistry() {}

    /**
     * Called during plugin startup to validate all asset registries
     * and print a summary to the console.
     */
    public static void validate() {
        NCLogger.info("=== NuclearCraft Asset Registry ===");
        ModelRegistry.validate();
        NCLogger.info("[AssetRegistry] " + SoundRegistry.totalSounds() + " sound events registered.");
        NCLogger.info("[AssetRegistry] " + TextureRegistry.totalTextures() + " texture entries registered.");
        NCLogger.info("[AssetRegistry] Asset validation complete.");
    }

    /**
     * Returns the resource pack namespace used by this plugin.
     */
    public static String namespace() {
        return "nuclearcraft";
    }

    /**
     * Constructs a namespaced resource key in the form "nuclearcraft:path".
     */
    public static String key(String path) {
        return namespace() + ":" + path;
    }
}
