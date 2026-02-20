package com.usainsrht.elytratrails.trail.shape;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * Generates relative particle offsets for complex trail shapes.
 */
public interface ShapeProvider {

    /**
     * Compute particle offset positions relative to the player's location.
     *
     * @param origin the player's current location (includes yaw/pitch)
     * @param tick   a monotonically increasing tick counter for animation
     * @return list of relative offsets to spawn particles at
     */
    List<Vector> getOffsets(Location origin, int tick);
}

