package com.usainsrht.elytratrails.model;

/**
 * Sub-mode for {@link TrailCategory#PLAYER} trails.
 * Determines under what movement conditions the trail plays.
 */
public enum PlayerTrailMode {
    /** Always plays, regardless of movement. */
    NORMAL,
    /** Only plays when the player is NOT moving (velocity below threshold). */
    STANDBY,
    /** Only plays when the player IS moving (velocity above threshold). */
    MOVING
}
