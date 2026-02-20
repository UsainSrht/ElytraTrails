package com.usainsrht.elytratrails.model;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A single particle emitter within a trail. A trail may contain many emitters,
 * each with its own spawn-point, interval, particle type, shape behaviour, etc.
 *
 * <p>Config keys (all under an emitter section):
 * <pre>
 *   spawn-point: WINGS | LEFT_WING | RIGHT_WING | FEET | BODY | BEHIND
 *   particle: DUST | FLAME | END_ROD | ...
 *   amount: 2
 *   interval: 1          # ticks between spawns (1 = every tick, 2 = every other, ...)
 *   speed: 0
 *   offset: {x, y, z}    # random spread
 *   size: 1.2             # dust size
 *   colors: ["#FF0000"]   # for DUST; multiple = cycle
 *   color-cycle-rate: 1   # how many ticks before next colour
 *   velocity: {x, y, z}   # directional velocity given to each particle (0,0,0 = none)
 *   random-direction: false  # give each particle a random unit-vector velocity
 *   random-direction-speed: 0.05
 *   shape: none | spiral | butterfly | wave  # special emitter shape
 *   # shape-specific:
 *   spiral-radius: 0.8
 *   spiral-speed: 0.5       # radians per tick
 *   spiral-points: 2        # points per tick
 *   spiral-expand: false     # pulsing radius
 *   spiral-expand-speed: 0.04
 *   spiral-expand-min: 0.3
 *   spiral-expand-max: 1.2
 *   butterfly-scale: 0.9
 *   butterfly-flap-speed: 0.4
 *   butterfly-points: 16
 *   wave-amplitude: 0.5
 *   wave-frequency: 0.3
 *   wing-coverage: 0.0     # 0 = tip only, 1 = full wing span (points along wing)
 * </pre>
 */
public class Emitter {

    // ── core ──
    private final SpawnPoint spawnPoint;
    private final Particle particle;
    private final int amount;
    private final int interval;
    private final double speed;
    private final Vector offset;
    private final float size;

    // ── colours ──
    private final List<Color> colors;
    private final int colorCycleRate;

    // ── velocity ──
    private final Vector velocity;
    private final boolean randomDirection;
    private final double randomDirectionSpeed;

    // ── shape ──
    private final String shape; // "none", "spiral", "butterfly", "wave"

    // ── spiral params ──
    private final double spiralRadius;
    private final double spiralSpeed;
    private final int spiralPoints;
    private final boolean spiralExpand;
    private final double spiralExpandSpeed;
    private final double spiralExpandMin;
    private final double spiralExpandMax;

    // ── butterfly params ──
    private final double butterflyScale;
    private final double butterflyFlapSpeed;
    private final int butterflyPoints;

    // ── wave params ──
    private final double waveAmplitude;
    private final double waveFrequency;

    // ── wing coverage ──
    private final double wingCoverage;

    @SuppressWarnings("ConstructorWithTooManyParameters")
    private Emitter(SpawnPoint spawnPoint, Particle particle, int amount, int interval,
                    double speed, Vector offset, float size, List<Color> colors, int colorCycleRate,
                    Vector velocity, boolean randomDirection, double randomDirectionSpeed,
                    String shape,
                    double spiralRadius, double spiralSpeed, int spiralPoints,
                    boolean spiralExpand, double spiralExpandSpeed, double spiralExpandMin, double spiralExpandMax,
                    double butterflyScale, double butterflyFlapSpeed, int butterflyPoints,
                    double waveAmplitude, double waveFrequency,
                    double wingCoverage) {
        this.spawnPoint = spawnPoint;
        this.particle = particle;
        this.amount = amount;
        this.interval = Math.max(1, interval);
        this.speed = speed;
        this.offset = offset;
        this.size = size;
        this.colors = colors != null ? Collections.unmodifiableList(colors) : Collections.emptyList();
        this.colorCycleRate = Math.max(1, colorCycleRate);
        this.velocity = velocity;
        this.randomDirection = randomDirection;
        this.randomDirectionSpeed = randomDirectionSpeed;
        this.shape = shape != null ? shape.toLowerCase() : "none";
        this.spiralRadius = spiralRadius;
        this.spiralSpeed = spiralSpeed;
        this.spiralPoints = spiralPoints;
        this.spiralExpand = spiralExpand;
        this.spiralExpandSpeed = spiralExpandSpeed;
        this.spiralExpandMin = spiralExpandMin;
        this.spiralExpandMax = spiralExpandMax;
        this.butterflyScale = butterflyScale;
        this.butterflyFlapSpeed = butterflyFlapSpeed;
        this.butterflyPoints = butterflyPoints;
        this.waveAmplitude = waveAmplitude;
        this.waveFrequency = waveFrequency;
        this.wingCoverage = wingCoverage;
    }

    /**
     * Parse an emitter from a YAML section.
     */
    public static Emitter fromConfig(ConfigurationSection sec) {
        SpawnPoint sp;
        try {
            sp = SpawnPoint.valueOf(sec.getString("spawn-point", "WINGS").toUpperCase());
        } catch (IllegalArgumentException e) {
            sp = SpawnPoint.WINGS;
        }

        Particle particle;
        try {
            particle = Particle.valueOf(sec.getString("particle", "DUST").toUpperCase());
        } catch (IllegalArgumentException e) {
            particle = Particle.DUST;
        }

        int amount = sec.getInt("amount", 1);
        int interval = sec.getInt("interval", 1);
        double speed = sec.getDouble("speed", 0);

        ConfigurationSection offSec = sec.getConfigurationSection("offset");
        Vector offset = new Vector(
                offSec != null ? offSec.getDouble("x", 0) : 0,
                offSec != null ? offSec.getDouble("y", 0) : 0,
                offSec != null ? offSec.getDouble("z", 0) : 0
        );
        float size = (float) sec.getDouble("size", 1.0);

        List<Color> colors = new ArrayList<>();
        for (String hex : sec.getStringList("colors")) {
            colors.add(parseHex(hex));
        }
        int colorCycleRate = sec.getInt("color-cycle-rate", 1);

        ConfigurationSection velSec = sec.getConfigurationSection("velocity");
        Vector velocity = new Vector(
                velSec != null ? velSec.getDouble("x", 0) : 0,
                velSec != null ? velSec.getDouble("y", 0) : 0,
                velSec != null ? velSec.getDouble("z", 0) : 0
        );
        boolean randomDir = sec.getBoolean("random-direction", false);
        double randomDirSpeed = sec.getDouble("random-direction-speed", 0.05);

        String shape = sec.getString("shape", "none");

        double spiralRadius = sec.getDouble("spiral-radius", 0.8);
        double spiralSpeed = sec.getDouble("spiral-speed", 0.5);
        int spiralPoints = sec.getInt("spiral-points", 2);
        boolean spiralExpand = sec.getBoolean("spiral-expand", false);
        double spiralExpandSpeed = sec.getDouble("spiral-expand-speed", 0.04);
        double spiralExpandMin = sec.getDouble("spiral-expand-min", 0.3);
        double spiralExpandMax = sec.getDouble("spiral-expand-max", 1.2);

        double bfScale = sec.getDouble("butterfly-scale", 0.9);
        double bfFlap = sec.getDouble("butterfly-flap-speed", 0.4);
        int bfPoints = sec.getInt("butterfly-points", 16);

        double waveAmp = sec.getDouble("wave-amplitude", 0.5);
        double waveFreq = sec.getDouble("wave-frequency", 0.3);

        double wingCoverage = sec.getDouble("wing-coverage", 0.0);

        return new Emitter(sp, particle, amount, interval, speed, offset, size,
                colors, colorCycleRate, velocity, randomDir, randomDirSpeed,
                shape,
                spiralRadius, spiralSpeed, spiralPoints,
                spiralExpand, spiralExpandSpeed, spiralExpandMin, spiralExpandMax,
                bfScale, bfFlap, bfPoints,
                waveAmp, waveFreq,
                wingCoverage);
    }

    private static Color parseHex(String hex) {
        if (hex == null || hex.isEmpty()) return Color.WHITE;
        hex = hex.replace("#", "");
        try {
            int rgb = Integer.parseInt(hex, 16);
            return Color.fromRGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
        } catch (NumberFormatException e) {
            return Color.WHITE;
        }
    }

    // ── Getters ─────────────────────────────────────────────

    public SpawnPoint getSpawnPoint() { return spawnPoint; }
    public Particle getParticle() { return particle; }
    public int getAmount() { return amount; }
    public int getInterval() { return interval; }
    public double getSpeed() { return speed; }
    public Vector getOffset() { return offset.clone(); }
    public float getSize() { return size; }
    public List<Color> getColors() { return colors; }
    public int getColorCycleRate() { return colorCycleRate; }
    public Vector getVelocity() { return velocity.clone(); }
    public boolean isRandomDirection() { return randomDirection; }
    public double getRandomDirectionSpeed() { return randomDirectionSpeed; }
    public String getShape() { return shape; }
    public double getSpiralRadius() { return spiralRadius; }
    public double getSpiralSpeed() { return spiralSpeed; }
    public int getSpiralPoints() { return spiralPoints; }
    public boolean isSpiralExpand() { return spiralExpand; }
    public double getSpiralExpandSpeed() { return spiralExpandSpeed; }
    public double getSpiralExpandMin() { return spiralExpandMin; }
    public double getSpiralExpandMax() { return spiralExpandMax; }
    public double getButterflyScale() { return butterflyScale; }
    public double getButterflyFlapSpeed() { return butterflyFlapSpeed; }
    public int getButterflyPoints() { return butterflyPoints; }
    public double getWaveAmplitude() { return waveAmplitude; }
    public double getWaveFrequency() { return waveFrequency; }
    public double getWingCoverage() { return wingCoverage; }
}

