package com.usainsrht.elytratrails.trail.shape;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Produces a radial flame-burst pattern behind the player.
 */
public class FlamePointsShape implements ShapeProvider {

    private static final int POINTS = 8;
    private static final double MAX_RADIUS = 0.7;

    @Override
    public List<Vector> getOffsets(Location origin, int tick) {
        List<Vector> offsets = new ArrayList<>();
        Vector forward = origin.getDirection().normalize();

        for (int i = 0; i < POINTS; i++) {
            double angle = ThreadLocalRandom.current().nextDouble(0, 2 * Math.PI);
            double radius = ThreadLocalRandom.current().nextDouble(0.1, MAX_RADIUS);

            double x = Math.cos(angle) * radius;
            double y = Math.sin(angle) * radius;

            // Offset behind the player
            Vector offset = new Vector(x, y, 0);
            // Rotate offset to align with player's look direction (simplified: just push behind)
            offset.add(forward.clone().multiply(-0.5));
            offsets.add(offset);
        }

        return offsets;
    }
}

