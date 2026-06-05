package com.nuclearcraft.blocks;

import com.nuclearcraft.utils.NCLogger;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * Central registry for all NuclearCraft custom block types.
 */
public class BlockRegistry {

    private final Map<String, CustomBlock> registry = new LinkedHashMap<>();
    private final JavaPlugin plugin;

    public BlockRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void register(CustomBlock block) {
        if (registry.containsKey(block.getId())) {
            NCLogger.warn("Duplicate block registration attempted for ID: " + block.getId());
            return;
        }
        registry.put(block.getId(), block);
        NCLogger.debug("Registered custom block: %s", block.getId());
    }

    public Optional<CustomBlock> get(String id) {
        return Optional.ofNullable(registry.get(id));
    }

    public Optional<CustomBlock> get(Block block) {
        String id = CustomBlock.getBlockId(plugin, block);
        if (id == null) return Optional.empty();
        return get(id);
    }

    public boolean isCustomBlock(Block block) {
        return CustomBlock.getBlockId(plugin, block) != null;
    }

    public boolean isRegistered(String id) {
        return registry.containsKey(id);
    }

    public Collection<CustomBlock> getAll() {
        return Collections.unmodifiableCollection(registry.values());
    }

    public Set<String> getIds() {
        return Collections.unmodifiableSet(registry.keySet());
    }

    public void clear() {
        registry.clear();
    }

    public int size() {
        return registry.size();
    }
}
