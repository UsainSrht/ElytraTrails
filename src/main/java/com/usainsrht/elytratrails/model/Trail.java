package com.usainsrht.elytratrails.model;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable data class representing a single trail definition loaded from trails.yml.
 */
public class Trail {

    private final String id;
    private final String displayName;
    private final Particle particle;
    private final int amount;
    private final double speed;
    private final Vector offset;
    private final float size;
    private final TrailType trailType;
    private final List<Color> colors;
    private final String shapeId;
    private final double price;
    private final Material icon;
    private final String permission;

    public Trail(String id, String displayName, Particle particle, int amount, double speed,
                 Vector offset, float size, TrailType trailType, List<Color> colors,
                 String shapeId, double price, Material icon) {
        this.id = id;
        this.displayName = displayName;
        this.particle = particle;
        this.amount = amount;
        this.speed = speed;
        this.offset = offset;
        this.size = size;
        this.trailType = trailType;
        this.colors = colors != null ? Collections.unmodifiableList(colors) : Collections.emptyList();
        this.shapeId = shapeId;
        this.price = price;
        this.icon = icon;
        this.permission = "elytratrails.trail." + id;
    }

    /**
     * Parse a Trail from a ConfigurationSection keyed by its id.
     */
    public static Trail fromConfig(String id, ConfigurationSection section) {
        String displayName = section.getString("display-name", id);

        String particleName = section.getString("particle", "DUST");
        Particle particle;
        try {
            particle = Particle.valueOf(particleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            particle = Particle.DUST;
        }

        int amount = section.getInt("amount", 5);
        double speed = section.getDouble("speed", 0);

        ConfigurationSection offSec = section.getConfigurationSection("offset");
        Vector offset = new Vector(
                offSec != null ? offSec.getDouble("x", 0.2) : 0.2,
                offSec != null ? offSec.getDouble("y", 0.2) : 0.2,
                offSec != null ? offSec.getDouble("z", 0.2) : 0.2
        );

        float size = (float) section.getDouble("size", 1.0);

        TrailType trailType;
        try {
            trailType = TrailType.valueOf(section.getString("type", "STATIC").toUpperCase());
        } catch (IllegalArgumentException e) {
            trailType = TrailType.STATIC;
        }

        List<Color> colors = new ArrayList<>();
        List<String> colorStrings = section.getStringList("colors");
        for (String hex : colorStrings) {
            colors.add(parseHexColor(hex));
        }

        String shapeId = section.getString("shape", null);
        double price = section.getDouble("price", 0);

        Material icon;
        try {
            icon = Material.valueOf(section.getString("icon", "PAPER").toUpperCase());
        } catch (IllegalArgumentException e) {
            icon = Material.PAPER;
        }

        return new Trail(id, displayName, particle, amount, speed, offset, size, trailType, colors, shapeId, price, icon);
    }

    /**
     * Parse a hex colour string like "#FF0000" into a Bukkit Color.
     */
    private static Color parseHexColor(String hex) {
        if (hex == null || hex.isEmpty()) return Color.WHITE;
        hex = hex.replace("#", "");
        try {
            int rgb = Integer.parseInt(hex, 16);
            return Color.fromRGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
        } catch (NumberFormatException e) {
            return Color.WHITE;
        }
    }

    // ── Getters ──────────────────────────────────────────────

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Particle getParticle() { return particle; }
    public int getAmount() { return amount; }
    public double getSpeed() { return speed; }
    public Vector getOffset() { return offset.clone(); }
    public float getSize() { return size; }
    public TrailType getTrailType() { return trailType; }
    public List<Color> getColors() { return colors; }
    public String getShapeId() { return shapeId; }
    public double getPrice() { return price; }
    public Material getIcon() { return icon; }
    public String getPermission() { return permission; }
}

