package com.usainsrht.elytratrails.trail.shape;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Produces a butterfly-wing pattern behind the player.
 * Uses a polar rose curve mirrored on both sides.
 */
public class ButterflyShape implements ShapeProvider {

    private static final int POINTS = 16;
    private static final double SCALE = 0.9;

    @Override
    public List<Vector> getOffsets(Location origin, int tick) {
        List<Vector> offsets = new ArrayList<>();

        double yaw = Math.toRadians(origin.getYaw());

        // "right" vector (perpendicular to yaw in the horizontal plane)
        Vector right = new Vector(-Math.sin(yaw - Math.PI / 2), 0, Math.cos(yaw - Math.PI / 2)).normalize();
        Vector up = new Vector(0, 1, 0);
        Vector forward = origin.getDirection().normalize();

        // Flapping animation: oscillate wing spread over time
        double flapFactor = 0.7 + 0.3 * Math.sin(tick * 0.4);

        for (int i = 0; i < POINTS; i++) {
            double t = (double) i / POINTS * Math.PI;

            // Butterfly curve (simplified): r = sin(t) * (e^cos(t) - 2cos(4t))
            double r = Math.abs(Math.sin(t)) * (Math.exp(Math.cos(t)) - 2.0 * Math.cos(4 * t));
            r *= SCALE * flapFactor;

            double yOff = Math.sin(t) * r * 0.5; // vertical component

            // Right wing
            Vector rightWing = right.clone().multiply(Math.cos(t) * r)
                    .add(up.clone().multiply(yOff))
                    .add(forward.clone().multiply(-0.6));
            offsets.add(rightWing);

            // Left wing (mirror)
            Vector leftWing = right.clone().multiply(-Math.cos(t) * r)
                    .add(up.clone().multiply(yOff))
                    .add(forward.clone().multiply(-0.6));
            offsets.add(leftWing);
        }

        return offsets;
    }
}

