package com.usainsrht.elytratrails.config;

import com.usainsrht.elytratrails.ElytraTrails;
import com.usainsrht.elytratrails.model.TrailCategory;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages per-player data: active trails (one per category) and list of unlocked trails.
 * Persisted to players.yml in the plugin data folder.
 *
 * <p>Active trail keys in the YAML file:
 * <pre>
 *   <uuid>:
 *     active-elytra: "trail_id"
 *     active-player: "trail_id"
 *     active-arrow:  "trail_id"
 *     unlocked:
 *       - "trail_id"
 * </pre>
 */
public class PlayerDataManager {

    private final ElytraTrails plugin;
    private final File file;
    private YamlConfiguration config;

    /** UUID → (category → active trail id) */
    private final Map<UUID, Map<TrailCategory, String>> activeTrails = new HashMap<>();
    /** UUID → set of unlocked trail ids */
    private final Map<UUID, Set<String>> unlockedTrails = new HashMap<>();

    public PlayerDataManager(ElytraTrails plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "players.yml");
        loadAll();
    }

    // ── Public API ──────────────────────────────────────────

    public void load(UUID uuid) {
        String path = uuid.toString();
        Map<TrailCategory, String> catMap = new EnumMap<>(TrailCategory.class);

        if (config.contains(path)) {
            // New multi-category format
            for (TrailCategory cat : TrailCategory.values()) {
                String key = path + ".active-" + cat.name().toLowerCase();
                String val = config.getString(key, null);
                if (val != null) catMap.put(cat, val);
            }
            // Legacy single-trail migration: if old "active" key exists and no elytra slot set
            String legacy = config.getString(path + ".active", null);
            if (legacy != null && !catMap.containsKey(TrailCategory.ELYTRA)) {
                catMap.put(TrailCategory.ELYTRA, legacy);
            }
            unlockedTrails.put(uuid, new HashSet<>(config.getStringList(path + ".unlocked")));
        } else {
            unlockedTrails.put(uuid, new HashSet<>());
        }

        activeTrails.put(uuid, catMap);
    }

    public void save(UUID uuid) {
        String path = uuid.toString();
        Map<TrailCategory, String> catMap = activeTrails.getOrDefault(uuid, Collections.emptyMap());
        for (TrailCategory cat : TrailCategory.values()) {
            String val = catMap.get(cat);
            config.set(path + ".active-" + cat.name().toLowerCase(), val);
        }
        // Remove legacy key if present
        config.set(path + ".active", null);
        config.set(path + ".unlocked", new ArrayList<>(getUnlockedTrails(uuid)));
        saveFile();
    }

    public void saveAll() {
        for (UUID uuid : activeTrails.keySet()) {
            String path = uuid.toString();
            Map<TrailCategory, String> catMap = activeTrails.getOrDefault(uuid, Collections.emptyMap());
            for (TrailCategory cat : TrailCategory.values()) {
                String val = catMap.get(cat);
                config.set(path + ".active-" + cat.name().toLowerCase(), val);
            }
            config.set(path + ".active", null);
            config.set(path + ".unlocked", new ArrayList<>(getUnlockedTrails(uuid)));
        }
        saveFile();
    }

    public void unload(UUID uuid) {
        save(uuid);
        activeTrails.remove(uuid);
        unlockedTrails.remove(uuid);
    }

    // ── Active trail ────────────────────────────────────────

    public String getActiveTrail(UUID uuid, TrailCategory category) {
        Map<TrailCategory, String> catMap = activeTrails.get(uuid);
        if (catMap == null) return null;
        return catMap.get(category);
    }

    /** Legacy accessor — returns ELYTRA slot. */
    public String getActiveTrail(UUID uuid) {
        return getActiveTrail(uuid, TrailCategory.ELYTRA);
    }

    public void setActiveTrail(UUID uuid, TrailCategory category, String trailId) {
        activeTrails.computeIfAbsent(uuid, k -> new EnumMap<>(TrailCategory.class))
                    .put(category, trailId);
    }

    /** Clears the active trail for a specific category. */
    public void clearActiveTrail(UUID uuid, TrailCategory category) {
        Map<TrailCategory, String> catMap = activeTrails.get(uuid);
        if (catMap != null) catMap.remove(category);
    }

    // ── Unlocked trails ─────────────────────────────────────

    public Set<String> getUnlockedTrails(UUID uuid) {
        return unlockedTrails.computeIfAbsent(uuid, k -> new HashSet<>());
    }

    public boolean hasUnlocked(UUID uuid, String trailId) {
        return getUnlockedTrails(uuid).contains(trailId);
    }

    public void unlockTrail(UUID uuid, String trailId) {
        getUnlockedTrails(uuid).add(trailId);
    }

    // ── Internal ────────────────────────────────────────────

    private void loadAll() {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create players.yml: " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(file);

        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                load(uuid);
            } catch (IllegalArgumentException ignored) {
                // skip invalid keys
            }
        }
    }

    private void saveFile() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save players.yml: " + e.getMessage());
        }
    }
}
