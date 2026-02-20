package com.usainsrht.elytratrails.config;

import com.usainsrht.elytratrails.ElytraTrails;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages per-player data: active trail and list of unlocked trails.
 * Persisted to players.yml in the plugin data folder.
 */
public class PlayerDataManager {

    private final ElytraTrails plugin;
    private final File file;
    private YamlConfiguration config;

    /** UUID → active trail id (null = none) */
    private final Map<UUID, String> activeTrails = new HashMap<>();
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
        if (config.contains(path)) {
            activeTrails.put(uuid, config.getString(path + ".active", null));
            unlockedTrails.put(uuid, new HashSet<>(config.getStringList(path + ".unlocked")));
        } else {
            activeTrails.put(uuid, null);
            unlockedTrails.put(uuid, new HashSet<>());
        }
    }

    public void save(UUID uuid) {
        String path = uuid.toString();
        config.set(path + ".active", activeTrails.get(uuid));
        config.set(path + ".unlocked", new ArrayList<>(getUnlockedTrails(uuid)));
        saveFile();
    }

    public void saveAll() {
        for (UUID uuid : activeTrails.keySet()) {
            String path = uuid.toString();
            config.set(path + ".active", activeTrails.get(uuid));
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

    public String getActiveTrail(UUID uuid) {
        return activeTrails.get(uuid);
    }

    public void setActiveTrail(UUID uuid, String trailId) {
        activeTrails.put(uuid, trailId);
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

