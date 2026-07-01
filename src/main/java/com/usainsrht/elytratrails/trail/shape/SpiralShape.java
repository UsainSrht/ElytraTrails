package com.usainsrht.elytratrails.trail.shape;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Produces a helix / spiral pattern behind the player.
 */
public class SpiralShape implements ShapeProvider {

    private static final double RADIUS = 0.8;
    private static final int POINTS_PER_TICK = 4;
    private static final double ANGLE_STEP = Math.PI / 6; // 30°

    @Override
    public List<Vector> getOffsets(Location origin, int tick) {
        List<Vector> offsets = new ArrayList<>();

        double yaw = Math.toRadians(origin.getYaw());
        double pitch = Math.toRadians(origin.getPitch());

        // Direction the player is looking (normalised)
        Vector forward = origin.getDirection().normalize();
        // "right" vector (perpendicular to forward in the horizontal plane)
        Vector right = new Vector(-Math.sin(yaw - Math.PI / 2), 0, Math.cos(yaw - Math.PI / 2)).normalize();
        // "up" vector (perpendicular to forward and right)
        Vector up = right.clone().crossProduct(forward).normalize();

        for (int i = 0; i < POINTS_PER_TICK; i++) {
            double angle = (tick * POINTS_PER_TICK + i) * ANGLE_STEP;
            double x = Math.cos(angle) * RADIUS;
            double y = Math.sin(angle) * RADIUS;

            Vector offset = right.clone().multiply(x).add(up.clone().multiply(y));
            // Push the spiral slightly behind the player
            offset.add(forward.clone().multiply(-0.5 - (i * 0.15)));
            offsets.add(offset);
        }

        return offsets;
    }
}

