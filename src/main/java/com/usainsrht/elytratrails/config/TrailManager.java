package com.usainsrht.elytratrails.config;

import com.usainsrht.elytratrails.ElytraTrails;
import com.usainsrht.elytratrails.model.Trail;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

/**
 * Loads and manages all Trail definitions from trails.yml.
 */
public class TrailManager {

    private final ElytraTrails plugin;
    private final Map<String, Trail> trails = new LinkedHashMap<>();

    public TrailManager(ElytraTrails plugin) {
        this.plugin = plugin;
        loadTrails();
    }

    /**
     * (Re)load all trails from trails.yml.
     */
    public void loadTrails() {
        trails.clear();

        // Save default if not present
        plugin.saveResource("trails.yml", false);

        File file = new File(plugin.getDataFolder(), "trails.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection trailsSection = config.getConfigurationSection("trails");
        if (trailsSection == null) {
            plugin.getLogger().warning("No 'trails' section found in trails.yml!");
            return;
        }

        for (String key : trailsSection.getKeys(false)) {
            ConfigurationSection sec = trailsSection.getConfigurationSection(key);
            if (sec == null) continue;
            try {
                Trail trail = Trail.fromConfig(key, sec);
                trails.put(key, trail);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load trail '" + key + "': " + e.getMessage());
            }
        }

        plugin.getLogger().info("Loaded " + trails.size() + " trail(s).");
    }

    /**
     * Get a trail by its id.
     */
    public Trail getTrail(String id) {
        return trails.get(id);
    }

    /**
     * Get all loaded trails in definition order.
     */
    public Collection<Trail> getTrails() {
        return Collections.unmodifiableCollection(trails.values());
    }

    /**
     * Get all trail ids.
     */
    public Set<String> getTrailIds() {
        return Collections.unmodifiableSet(trails.keySet());
    }
}

