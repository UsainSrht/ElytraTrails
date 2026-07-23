package com.usainsrht.elytratrails.config;

import com.usainsrht.elytratrails.ElytraTrails;
import com.usainsrht.elytratrails.model.Trail;
import com.usainsrht.elytratrails.model.TrailCategory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

/**
 * Loads and manages all Trail definitions from trails.yml,
 * and region-filter settings from config.yml.
 */
public class TrailManager {

    private final ElytraTrails plugin;
    private final Map<String, Trail> trails = new LinkedHashMap<>();

    // ── Region filter settings ──────────────────────────────
    /** True = WHITELIST (player trails allowed only in these regions).
     *  False = BLACKLIST (player trails blocked in these regions). */
    private boolean regionWhitelist = true;
    private List<String> regionList = new ArrayList<>();

    public TrailManager(ElytraTrails plugin) {
        this.plugin = plugin;
        loadConfig();
        loadTrails();
    }

    // ── Config.yml loading ──────────────────────────────────

    /**
     * Load or reload config.yml (region filter settings).
     */
    public void loadConfig() {
        plugin.saveDefaultConfigIfMissing();
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        String modeStr = cfg.getString("player-trails.region-mode", "WHITELIST").toUpperCase();
        regionWhitelist = modeStr.equals("WHITELIST");
        regionList = cfg.getStringList("player-trails.regions");
    }

    // ── Trails.yml loading ──────────────────────────────────

    /**
     * (Re)load all trails from trails.yml.
     */
    public void loadTrails() {
        trails.clear();

        // Save default if not present
        plugin.saveResourceIfMissing("trails.yml");

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

    // ── Trail access ────────────────────────────────────────

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
     * Get trails filtered by category.
     */
    public List<Trail> getTrailsByCategory(TrailCategory category) {
        List<Trail> result = new ArrayList<>();
        for (Trail t : trails.values()) {
            if (t.getCategory() == category) result.add(t);
        }
        return result;
    }

    /**
     * Get all trail ids.
     */
    public Set<String> getTrailIds() {
        return Collections.unmodifiableSet(trails.keySet());
    }

    // ── Region filter ───────────────────────────────────────

    public boolean isRegionWhitelist() {
        return regionWhitelist;
    }

    public List<String> getRegionList() {
        return Collections.unmodifiableList(regionList);
    }
}
