package com.nuclearcraft.blocks;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Represents a type of NuclearCraft custom block.
 * Blocks are tracked via chunk-persistent data keyed by their location.
 */
public class CustomBlock {

    public static final String PDC_KEY_BLOCK_ID = "nuclearcraft_block_id";

    private final String id;
    private final String displayName;
    private final Material material;
    private final boolean dropOnBreak;
    private final boolean trackInChunk;

    private final NamespacedKey namespacedKey;

    public CustomBlock(JavaPlugin plugin, String id, String displayName,
                       Material material, boolean dropOnBreak, boolean trackInChunk) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.dropOnBreak = dropOnBreak;
        this.trackInChunk = trackInChunk;
        this.namespacedKey = new NamespacedKey(plugin, id);
    }

    /**
     * Tags a block at the given location as this custom block type
     * using the chunk's PersistentDataContainer.
     */
    public void place(Block block) {
        if (!trackInChunk) return;
        String locationKey = serializeLocation(block.getLocation());
        block.getChunk().getPersistentDataContainer()
                .set(new NamespacedKey(namespacedKey.namespace(), locationKey),
                        PersistentDataType.STRING, id);
    }

    /**
     * Removes the custom block tag from the chunk's PersistentDataContainer.
     */
    public void remove(Block block) {
        if (!trackInChunk) return;
        String locationKey = serializeLocation(block.getLocation());
        block.getChunk().getPersistentDataContainer()
                .remove(new NamespacedKey(namespacedKey.namespace(), locationKey));
    }

    /**
     * Returns the custom block ID stored at the given block location, or null.
     */
    public static String getBlockId(JavaPlugin plugin, Block block) {
        String locationKey = serializeLocation(block.getLocation());
        return block.getChunk().getPersistentDataContainer()
                .get(new NamespacedKey(plugin, locationKey), PersistentDataType.STRING);
    }

    public static String serializeLocation(Location loc) {
        return loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Material getMaterial() { return material; }
    public boolean isDropOnBreak() { return dropOnBreak; }
    public boolean isTrackInChunk() { return trackInChunk; }
    public NamespacedKey getNamespacedKey() { return namespacedKey; }

    @Override
    public String toString() {
        return "CustomBlock{id='" + id + "', material=" + material + "}";
    }
}
