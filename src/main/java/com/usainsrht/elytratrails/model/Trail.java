package com.usainsrht.elytratrails.model;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable data class representing a trail definition loaded from trails.yml.
 * Each trail contains one or more {@link Emitter}s that define where and how
 * particles are spawned.
 */
public class Trail {

    private final String id;
    private final String displayName;
    private final List<String> lore;
    private final TrailType trailType;
    private final TrailCategory category;
    private final PlayerTrailMode playerTrailMode;
    private final List<Emitter> emitters;
    private final double price;
    private final Material icon;
    private final String permission;

    public Trail(String id, String displayName, List<String> lore, TrailType trailType,
                 TrailCategory category, PlayerTrailMode playerTrailMode,
                 List<Emitter> emitters, double price, Material icon) {
        this.id = id;
        this.displayName = displayName;
        this.lore = lore != null ? Collections.unmodifiableList(new ArrayList<>(lore)) : Collections.emptyList();
        this.trailType = trailType;
        this.category = category;
        this.playerTrailMode = playerTrailMode;
        this.emitters = emitters != null ? Collections.unmodifiableList(emitters) : Collections.emptyList();
        this.price = price;
        this.icon = icon;
        this.permission = "elytratrails.trail." + id;
    }

    public Trail(String id, String displayName, TrailType trailType,
                 TrailCategory category, PlayerTrailMode playerTrailMode,
                 List<Emitter> emitters, double price, Material icon) {
        this(id, displayName, Collections.emptyList(), trailType, category, playerTrailMode, emitters, price, icon);
    }

    /**
     * Parse a Trail from a ConfigurationSection keyed by its id.
     */
    public static Trail fromConfig(String id, ConfigurationSection section) {
        String displayName = section.getString("display-name", id);
        List<String> lore = section.contains("lore") ? section.getStringList("lore") : Collections.emptyList();

        TrailType trailType;
        try {
            trailType = TrailType.valueOf(section.getString("type", "STATIC").toUpperCase());
        } catch (IllegalArgumentException e) {
            trailType = TrailType.STATIC;
        }

        TrailCategory category;
        try {
            category = TrailCategory.valueOf(section.getString("category", "ELYTRA").toUpperCase());
        } catch (IllegalArgumentException e) {
            category = TrailCategory.ELYTRA;
        }

        PlayerTrailMode playerTrailMode;
        try {
            playerTrailMode = PlayerTrailMode.valueOf(section.getString("player-mode", "NORMAL").toUpperCase());
        } catch (IllegalArgumentException e) {
            playerTrailMode = PlayerTrailMode.NORMAL;
        }

        double price = section.getDouble("price", 0);

        Material icon;
        try {
            icon = Material.valueOf(section.getString("icon", "PAPER").toUpperCase());
        } catch (IllegalArgumentException e) {
            icon = Material.PAPER;
        }

        // Parse emitters list
        List<Emitter> emitters = new ArrayList<>();
        ConfigurationSection emittersSec = section.getConfigurationSection("emitters");
        if (emittersSec != null) {
            for (String key : emittersSec.getKeys(false)) {
                ConfigurationSection emSec = emittersSec.getConfigurationSection(key);
                if (emSec != null) {
                    emitters.add(Emitter.fromConfig(emSec));
                }
            }
        }

        return new Trail(id, displayName, lore, trailType, category, playerTrailMode, emitters, price, icon);
    }

    // ── Getters ──────────────────────────────────────────────

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public List<String> getLore() { return lore; }
    public TrailType getTrailType() { return trailType; }
    public TrailCategory getCategory() { return category; }
    public PlayerTrailMode getPlayerTrailMode() { return playerTrailMode; }
    public List<Emitter> getEmitters() { return emitters; }
    public double getPrice() { return price; }
    public Material getIcon() { return icon; }
    public String getPermission() { return permission; }
}
