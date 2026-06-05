package com.nuclearcraft.items;

import com.nuclearcraft.utils.NCLogger;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * Central registry for all NuclearCraft custom items.
 * Items are registered by their string ID and looked up by ID or ItemStack.
 */
public class ItemRegistry {

    private final Map<String, CustomItem> registry = new LinkedHashMap<>();
    private final JavaPlugin plugin;

    public ItemRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void register(CustomItem item) {
        if (registry.containsKey(item.getId())) {
            NCLogger.warn("Duplicate item registration attempted for ID: " + item.getId());
            return;
        }
        registry.put(item.getId(), item);
        NCLogger.debug("Registered custom item: %s", item.getId());
    }

    public Optional<CustomItem> get(String id) {
        return Optional.ofNullable(registry.get(id));
    }

    public Optional<CustomItem> get(ItemStack item) {
        String id = CustomItem.getId(plugin, item);
        if (id == null) return Optional.empty();
        return get(id);
    }

    public boolean isCustomItem(ItemStack item) {
        return CustomItem.getId(plugin, item) != null;
    }

    public boolean isRegistered(String id) {
        return registry.containsKey(id);
    }

    public Collection<CustomItem> getAll() {
        return Collections.unmodifiableCollection(registry.values());
    }

    public Set<String> getIds() {
        return Collections.unmodifiableSet(registry.keySet());
    }

    public int size() {
        return registry.size();
    }

    public void clear() {
        registry.clear();
    }
}
