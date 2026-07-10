package com.usainsrht.elytratrails.trail;

import com.usainsrht.elytratrails.ElytraTrails;
import com.usainsrht.elytratrails.config.PlayerDataManager;
import com.usainsrht.elytratrails.config.TrailManager;
import com.usainsrht.elytratrails.config.WorldGuardHook;
import com.usainsrht.elytratrails.model.*;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Synchronous ticker that runs every tick and drives all emitters for every
 * gliding player (elytra trails), swimming players (swim trails), and all on-foot players (player trails).
 * Each emitter has its own interval so different parts of a trail can tick
 * at different rates.
 *
 * <p>Wing-tip positions are calculated from the player's yaw and an estimated
 * body-roll derived from lateral velocity, giving a realistic elytra look.
 */
public class ParticleTask extends BukkitRunnable {

    /* ── Wing geometry constants (blocks, relative to player centre) ── */
    private static final double WING_LENGTH     = 1.6;   // full wingspan from centre
    private static final double WING_BACK       = -0.3;  // how far behind the body
    private static final double WING_UP         = 0.15;  // slight upward tilt
    private static final double FEET_DOWN       = -0.8;  // feet below centre
    private static final double BEHIND_DIST     = 1.0;   // "behind" distance
    private static final double MOVING_THRESHOLD = 0.08; // velocity magnitude for "moving"

    private final ElytraTrails plugin;
    private final TrailManager trailManager;
    private final PlayerDataManager playerData;
    private final WorldGuardHook worldGuard;

    /** Global tick counter – increments every server tick. */
    private int tick = 0;

    /** Per-player tick counters (reset when they stop gliding). */
    private final Map<UUID, Integer> playerTicks = new HashMap<>();

    /** Per-player previous location for roll estimation. */
    private final Map<UUID, Location> prevLocations = new HashMap<>();

    /** Per-player tick counters for player trails. */
    private final Map<UUID, Integer> playerTrailTicks = new HashMap<>();

    /** Per-player tick counters for swim trails (reset when they stop swimming). */
    private final Map<UUID, Integer> swimTicks = new HashMap<>();

    /** Per-player previous location for swim roll estimation. */
    private final Map<UUID, Location> swimPrevLocations = new HashMap<>();

    public ParticleTask(ElytraTrails plugin, TrailManager trailManager,
                        PlayerDataManager playerData, WorldGuardHook worldGuard) {
        this.plugin = plugin;
        this.trailManager = trailManager;
        this.playerData = playerData;
        this.worldGuard = worldGuard;
    }

    /* ================================================================== */

    @Override
    public void run() {
        tick++;

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();

            // ── Elytra trails ───────────────────────────────
            if (isGlidingWithElytra(player)) {
                tickElytraTrail(player, uuid);
            } else {
                playerTicks.remove(uuid);
                prevLocations.remove(uuid);
            }

            // ── Swim trails ─────────────────────────────────
            if (isSwimming(player)) {
                tickSwimTrail(player, uuid);
            } else {
                swimTicks.remove(uuid);
                swimPrevLocations.remove(uuid);
            }

            // ── Player trails ───────────────────────────────
            if (!isGlidingWithElytra(player) && !isSwimming(player)) {
                tickPlayerTrail(player, uuid);
            }
        }
    }

    /* ================================================================== */
    /*  Elytra trail tick                                                  */
    /* ================================================================== */

    private void tickElytraTrail(Player player, UUID uuid) {
        String trailId = playerData.getActiveTrail(uuid, TrailCategory.ELYTRA);
        if (trailId == null) return;

        Trail trail = trailManager.getTrail(trailId);
        if (trail == null) return;

        if (!player.hasPermission("elytratrails.use.elytra")) {
            return;
        }

        if (!playerData.hasTrailAccess(player, trail)) {
            return;
        }

        int pt = playerTicks.merge(uuid, 1, Integer::sum);

        // ── Compute body vectors ─────────────────────────
        Location loc = player.getLocation();
        double yawRad = Math.toRadians(loc.getYaw());

        Vector forward = loc.getDirection().normalize();

        // "right" in the horizontal plane (perpendicular to yaw)
        Vector right = new Vector(-Math.cos(yawRad), 0, -Math.sin(yawRad)).normalize();

        // Estimate roll from lateral velocity
        double roll = estimateRoll(player, right, prevLocations);

        // Rotated up considering roll
        Vector up = new Vector(0, 1, 0);
        Vector rolledRight = right.clone().multiply(Math.cos(roll)).add(up.clone().multiply(Math.sin(roll)));
        Vector rolledUp    = up.clone().multiply(Math.cos(roll)).subtract(right.clone().multiply(Math.sin(roll)));

        // ── Compute anchor positions ─────────────────────
        Vector leftWingTip  = rolledRight.clone().multiply(-WING_LENGTH)
                .add(forward.clone().multiply(WING_BACK))
                .add(rolledUp.clone().multiply(WING_UP));
        Vector rightWingTip = rolledRight.clone().multiply(WING_LENGTH)
                .add(forward.clone().multiply(WING_BACK))
                .add(rolledUp.clone().multiply(WING_UP));
        Vector feet   = new Vector(0, FEET_DOWN, 0);
        Vector behind = forward.clone().multiply(-BEHIND_DIST);
        Vector body   = new Vector(0, 0, 0);

        prevLocations.put(uuid, loc.clone());

        // ── Tick each emitter ────────────────────────────
        for (Emitter emitter : trail.getEmitters()) {
            if (pt % emitter.getInterval() != 0) continue;

            List<Vector> anchors = resolveAnchors(emitter, leftWingTip, rightWingTip,
                    feet, body, behind, rolledRight, rolledUp, forward);

            for (Vector anchor : anchors) {
                spawnEmitter(player, loc, anchor, emitter, pt, forward, rolledRight, rolledUp);
            }
        }
    }

    /* ================================================================== */
    /*  Swim trail tick                                                    */
    /* ================================================================== */

    private void tickSwimTrail(Player player, UUID uuid) {
        String trailId = playerData.getActiveTrail(uuid, TrailCategory.SWIM);
        if (trailId == null) return;

        Trail trail = trailManager.getTrail(trailId);
        if (trail == null || trail.getCategory() != TrailCategory.SWIM) return;

        if (!player.hasPermission("elytratrails.use.swim")) {
            return;
        }

        if (!playerData.hasTrailAccess(player, trail)) {
            return;
        }

        int pt = swimTicks.merge(uuid, 1, Integer::sum);

        Location loc = player.getLocation();
        double yawRad = Math.toRadians(loc.getYaw());

        Vector forward = loc.getDirection().normalize();
        Vector right = new Vector(-Math.cos(yawRad), 0, -Math.sin(yawRad)).normalize();
        double roll = estimateRoll(player, right, swimPrevLocations);

        Vector up = new Vector(0, 1, 0);
        Vector rolledRight = right.clone().multiply(Math.cos(roll)).add(up.clone().multiply(Math.sin(roll)));
        Vector rolledUp    = up.clone().multiply(Math.cos(roll)).subtract(right.clone().multiply(Math.sin(roll)));

        Vector leftWingTip  = rolledRight.clone().multiply(-WING_LENGTH)
                .add(forward.clone().multiply(WING_BACK))
                .add(rolledUp.clone().multiply(WING_UP));
        Vector rightWingTip = rolledRight.clone().multiply(WING_LENGTH)
                .add(forward.clone().multiply(WING_BACK))
                .add(rolledUp.clone().multiply(WING_UP));
        Vector feet   = new Vector(0, FEET_DOWN, 0);
        Vector behind = forward.clone().multiply(-BEHIND_DIST);
        Vector body   = new Vector(0, 0, 0);

        swimPrevLocations.put(uuid, loc.clone());

        for (Emitter emitter : trail.getEmitters()) {
            if (pt % emitter.getInterval() != 0) continue;

            List<Vector> anchors = resolveAnchors(emitter, leftWingTip, rightWingTip,
                    feet, body, behind, rolledRight, rolledUp, forward);

            for (Vector anchor : anchors) {
                spawnEmitter(player, loc, anchor, emitter, pt, forward, rolledRight, rolledUp);
            }
        }
    }

    /* ================================================================== */
    /*  Player trail tick                                                  */
    /* ================================================================== */

    private void tickPlayerTrail(Player player, UUID uuid) {
        String trailId = playerData.getActiveTrail(uuid, TrailCategory.PLAYER);
        if (trailId == null) return;

        Trail trail = trailManager.getTrail(trailId);
        if (trail == null || trail.getCategory() != TrailCategory.PLAYER) return;

        if (!player.hasPermission("elytratrails.use.player")) {
            return;
        }

        if (!playerData.hasTrailAccess(player, trail)) {
            return;
        }

        // ── WorldGuard region check ──────────────────────
        if (worldGuard.isEnabled() && !trailManager.getRegionList().isEmpty()) {
            boolean inRegion = worldGuard.isPlayerInRegions(player, trailManager.getRegionList());
            if (trailManager.isRegionWhitelist()) {
                // Whitelist: must be in region to show trail
                if (!inRegion) return;
            } else {
                // Blacklist: must NOT be in region
                if (inRegion) return;
            }
        }

        // ── Movement check ───────────────────────────────
        double speed = player.getVelocity().clone().setY(0).length();
        boolean isMoving = speed > MOVING_THRESHOLD;
        PlayerTrailMode mode = trail.getPlayerTrailMode();
        if (mode == PlayerTrailMode.STANDBY && isMoving) return;
        if (mode == PlayerTrailMode.MOVING && !isMoving) return;

        int pt = playerTrailTicks.merge(uuid, 1, Integer::sum);

        Location loc = player.getLocation();
        double yawRad = Math.toRadians(loc.getYaw());
        Vector forward = loc.getDirection().normalize();
        Vector right   = new Vector(-Math.cos(yawRad), 0, -Math.sin(yawRad)).normalize();
        Vector up      = new Vector(0, 1, 0);

        for (Emitter emitter : trail.getEmitters()) {
            if (pt % emitter.getInterval() != 0) continue;

            List<Vector> anchors = resolvePlayerAnchors(emitter, right, up, forward, pt);

            for (Vector anchor : anchors) {
                spawnEmitter(player, loc, anchor, emitter, pt, forward, right, up);
            }
        }
    }

    /* ================================================================== */
    /*  Anchor resolution – elytra                                        */
    /* ================================================================== */

    private List<Vector> resolveAnchors(Emitter emitter,
                                        Vector leftWingTip, Vector rightWingTip,
                                        Vector feet, Vector body, Vector behind,
                                        Vector rolledRight, Vector rolledUp, Vector forward) {
        double coverage = emitter.getWingCoverage();
        SpawnPoint sp = emitter.getSpawnPoint();

        return switch (sp) {
            case LEFT_WING  -> wingPoints(leftWingTip, rolledRight, forward, coverage, true);
            case RIGHT_WING -> wingPoints(rightWingTip, rolledRight, forward, coverage, false);
            case WINGS -> {
                List<Vector> pts = new java.util.ArrayList<>(wingPoints(leftWingTip, rolledRight, forward, coverage, true));
                pts.addAll(wingPoints(rightWingTip, rolledRight, forward, coverage, false));
                yield pts;
            }
            case FEET   -> List.of(feet);
            case BODY   -> List.of(body);
            case BEHIND -> List.of(behind);
            // Player-trail spawn points used as fallback here
            case AROUND, ABOVE, BELOW -> resolvePlayerAnchors(emitter, rolledRight, rolledUp, forward, tick);
        };
    }

    /* ================================================================== */
    /*  Anchor resolution – player trail                                  */
    /* ================================================================== */

    /**
     * Resolve spawn anchors for AROUND, ABOVE, BELOW, FEET, BODY spawn points.
     */
    private List<Vector> resolvePlayerAnchors(Emitter emitter, Vector right, Vector up, Vector forward, int pt) {
        SpawnPoint sp = emitter.getSpawnPoint();
        return switch (sp) {
            case AROUND -> aroundPoints(emitter, right, up, pt);
            case ABOVE  -> List.of(new Vector(0, 1.8, 0));  // above head
            case BELOW  -> List.of(new Vector(0, -0.1, 0)); // just below feet
            case FEET   -> List.of(new Vector(0, 0.0, 0));
            case BODY   -> List.of(new Vector(0, 1.0, 0));  // torso height
            case BEHIND -> List.of(forward.clone().multiply(-0.5).add(new Vector(0, 1.0, 0)));
            // Elytra spawn points are ignored for player trails
            default -> List.of(new Vector(0, 1.0, 0));
        };
    }

    /**
     * Returns a ring of points orbiting around the player.
     * The number of points is based on emitter.getSpiralPoints() (reused for ring density).
     */
    private List<Vector> aroundPoints(Emitter emitter, Vector right, Vector up, int pt) {
        int count  = Math.max(1, emitter.getSpiralPoints());
        double radius = emitter.getSpiralRadius() > 0 ? emitter.getSpiralRadius() : 1.0;
        double angleStep = (2 * Math.PI) / count;
        double baseAngle = pt * emitter.getSpiralSpeed();
        double height = 1.0; // orbit at body height

        List<Vector> pts = new java.util.ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double angle = baseAngle + i * angleStep;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            // Use right and forward for the horizontal plane
            Vector forward = right.clone().crossProduct(up).normalize();
            pts.add(right.clone().multiply(x).add(forward.multiply(z)).add(new Vector(0, height, 0)));
        }
        return pts;
    }

    /**
     * Returns points along a wing. coverage 0 = tip only; 1 = full wing.
     */
    private List<Vector> wingPoints(Vector tip, Vector right, Vector forward,
                                    double coverage, boolean isLeft) {
        if (coverage <= 0.0) return List.of(tip);

        int steps = Math.max(1, (int) (coverage * 6)); // up to 6 points along the wing
        List<Vector> points = new java.util.ArrayList<>();
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps; // 0 = body, 1 = tip
            Vector pt = tip.clone().multiply(t); // linearly interpolate from centre to tip
            points.add(pt);
        }
        return points;
    }

    /* ================================================================== */
    /*  Emitter particle spawning                                         */
    /* ================================================================== */

    private void spawnEmitter(Player player, Location origin, Vector anchor,
                              Emitter emitter, int pt,
                              Vector forward, Vector right, Vector up) {

        String shape = emitter.getShape();
        switch (shape) {
            case "spiral"    -> spawnSpiral(player, origin, anchor, emitter, pt, forward, right, up);
            case "butterfly" -> spawnButterfly(player, origin, anchor, emitter, pt, right, up, forward);
            case "wave"      -> spawnWave(player, origin, anchor, emitter, pt, right, up, forward);
            default          -> spawnSimple(player, origin, anchor, emitter, pt);
        }
    }

    /* ── Simple (static / animated colour cycling) ────────────────────── */

    private void spawnSimple(Player player, Location origin, Vector anchor,
                             Emitter emitter, int pt) {
        Location spawnLoc = origin.clone().add(anchor);
        Color color = resolveColor(emitter, pt);

        if (emitter.getParticle() == Particle.DUST && color != null) {
            Particle.DustOptions dust = new Particle.DustOptions(color, emitter.getSize());
            player.getWorld().spawnParticle(Particle.DUST, spawnLoc,
                    emitter.getAmount(),
                    emitter.getOffset().getX(), emitter.getOffset().getY(), emitter.getOffset().getZ(),
                    emitter.getSpeed(), dust);
        } else {
            if (emitter.isRandomDirection()) {
                // Spawn one at a time with random velocity
                for (int i = 0; i < emitter.getAmount(); i++) {
                    Vector dir = randomUnitVector().multiply(emitter.getRandomDirectionSpeed());
                    player.getWorld().spawnParticle(emitter.getParticle(), spawnLoc,
                            0, dir.getX(), dir.getY(), dir.getZ(), emitter.getRandomDirectionSpeed());
                }
            } else if (!emitter.getVelocity().isZero()) {
                Vector v = emitter.getVelocity();
                player.getWorld().spawnParticle(emitter.getParticle(), spawnLoc,
                        0, v.getX(), v.getY(), v.getZ(), 1);
            } else {
                player.getWorld().spawnParticle(emitter.getParticle(), spawnLoc,
                        emitter.getAmount(),
                        emitter.getOffset().getX(), emitter.getOffset().getY(), emitter.getOffset().getZ(),
                        emitter.getSpeed());
            }
        }
    }

    /* ── Spiral shape ─────────────────────────────────────────────────── */

    private void spawnSpiral(Player player, Location origin, Vector anchor,
                             Emitter emitter, int pt,
                             Vector forward, Vector right, Vector up) {
        double radius = emitter.getSpiralRadius();
        if (emitter.isSpiralExpand()) {
            // Pulsing radius using sine
            double phase = pt * emitter.getSpiralExpandSpeed();
            double min = emitter.getSpiralExpandMin();
            double max = emitter.getSpiralExpandMax();
            radius = min + (max - min) * (0.5 + 0.5 * Math.sin(phase));
        }

        for (int i = 0; i < emitter.getSpiralPoints(); i++) {
            double angle = (pt * emitter.getSpiralPoints() + i) * emitter.getSpiralSpeed();
            double x = Math.cos(angle) * radius;
            double y = Math.sin(angle) * radius;

            Vector spiralOff = right.clone().multiply(x).add(up.clone().multiply(y));
            Location spawnLoc = origin.clone().add(anchor).add(spiralOff);

            Color color = resolveColor(emitter, pt + i);
            if (emitter.getParticle() == Particle.DUST && color != null) {
                Particle.DustOptions dust = new Particle.DustOptions(color, emitter.getSize());
                player.getWorld().spawnParticle(Particle.DUST, spawnLoc,
                        emitter.getAmount(), 0, 0, 0, 0, dust);
            } else {
                player.getWorld().spawnParticle(emitter.getParticle(), spawnLoc,
                        emitter.getAmount(), 0, 0, 0, emitter.getSpeed());
            }
        }
    }

    /* ── Butterfly shape ──────────────────────────────────────────────── */

    private void spawnButterfly(Player player, Location origin, Vector anchor,
                                Emitter emitter, int pt,
                                Vector right, Vector up, Vector forward) {
        double scale = emitter.getButterflyScale();
        double flapFactor = 0.7 + 0.3 * Math.sin(pt * emitter.getButterflyFlapSpeed());
        int points = emitter.getButterflyPoints();

        for (int i = 0; i < points; i++) {
            double t = (double) i / points * Math.PI;

            // Butterfly curve: r = |sin(t)| * (e^cos(t) - 2*cos(4t))
            double r = Math.abs(Math.sin(t)) * (Math.exp(Math.cos(t)) - 2.0 * Math.cos(4 * t));
            r *= scale * flapFactor;

            double yOff = Math.sin(t) * r * 0.5;

            // Right wing point
            Vector rwOff = right.clone().multiply(Math.cos(t) * r)
                    .add(up.clone().multiply(yOff))
                    .add(forward.clone().multiply(-0.4));
            spawnButterflyPoint(player, origin, anchor, rwOff, emitter, pt + i);

            // Left wing point (mirrored)
            Vector lwOff = right.clone().multiply(-Math.cos(t) * r)
                    .add(up.clone().multiply(yOff))
                    .add(forward.clone().multiply(-0.4));
            spawnButterflyPoint(player, origin, anchor, lwOff, emitter, pt + i);
        }
    }

    private void spawnButterflyPoint(Player player, Location origin, Vector anchor,
                                     Vector shapeOffset, Emitter emitter, int colorIdx) {
        Location spawnLoc = origin.clone().add(anchor).add(shapeOffset);
        Color color = resolveColor(emitter, colorIdx);
        if (emitter.getParticle() == Particle.DUST && color != null) {
            Particle.DustOptions dust = new Particle.DustOptions(color, emitter.getSize());
            player.getWorld().spawnParticle(Particle.DUST, spawnLoc,
                    1, 0, 0, 0, 0, dust);
        } else {
            player.getWorld().spawnParticle(emitter.getParticle(), spawnLoc,
                    1, 0, 0, 0, emitter.getSpeed());
        }
    }

    /* ── Wave shape (sine wave along the wing) ────────────────────────── */

    private void spawnWave(Player player, Location origin, Vector anchor,
                           Emitter emitter, int pt,
                           Vector right, Vector up, Vector forward) {
        double y = Math.sin(pt * emitter.getWaveFrequency()) * emitter.getWaveAmplitude();
        Vector waveOff = up.clone().multiply(y);
        Location spawnLoc = origin.clone().add(anchor).add(waveOff);

        Color color = resolveColor(emitter, pt);
        if (emitter.getParticle() == Particle.DUST && color != null) {
            Particle.DustOptions dust = new Particle.DustOptions(color, emitter.getSize());
            player.getWorld().spawnParticle(Particle.DUST, spawnLoc,
                    emitter.getAmount(),
                    emitter.getOffset().getX(), emitter.getOffset().getY(), emitter.getOffset().getZ(),
                    emitter.getSpeed(), dust);
        } else {
            player.getWorld().spawnParticle(emitter.getParticle(), spawnLoc,
                    emitter.getAmount(),
                    emitter.getOffset().getX(), emitter.getOffset().getY(), emitter.getOffset().getZ(),
                    emitter.getSpeed());
        }
    }

    /* ================================================================== */
    /*  Helpers                                                           */
    /* ================================================================== */

    /**
     * Resolve the current colour from the emitter's colour list (cycling).
     * Returns null if the list is empty.
     */
    private Color resolveColor(Emitter emitter, int tick) {
        List<Color> colors = emitter.getColors();
        if (colors.isEmpty()) return null;
        int idx = (tick / emitter.getColorCycleRate()) % colors.size();
        return colors.get(idx);
    }

    /**
     * Estimate body roll from lateral velocity.
     * Returns angle in radians; positive = tilting right.
     */
    private double estimateRoll(Player player, Vector right, Map<UUID, Location> prevLocMap) {
        Location prev = prevLocMap.get(player.getUniqueId());
        if (prev == null) return 0;

        Vector velocity = player.getLocation().toVector().subtract(prev.toVector());
        double lateral = velocity.dot(right); // positive = moving right
        // Clamp to a sensible roll angle (max ~35°)
        return Math.max(-0.6, Math.min(0.6, lateral * 3.0));
    }

    private boolean isSwimming(Player player) {
        return player.isSwimming() && player.isInWater();
    }

    private boolean isGlidingWithElytra(Player player) {
        if (!player.isGliding()) return false;
        ItemStack chestplate = player.getInventory().getChestplate();
        return chestplate != null && chestplate.getType() == Material.ELYTRA;
    }

    private Vector randomUnitVector() {
        double theta = ThreadLocalRandom.current().nextDouble(0, 2 * Math.PI);
        double phi = Math.acos(2 * ThreadLocalRandom.current().nextDouble() - 1);
        return new Vector(
                Math.sin(phi) * Math.cos(theta),
                Math.sin(phi) * Math.sin(theta),
                Math.cos(phi)
        );
    }
}
