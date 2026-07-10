package com.usainsrht.elytratrails.model;

/**
 * The category of a trail — determines which equipment slot / context it applies to.
 */
public enum TrailCategory {
    /** Plays while the player is gliding with an elytra. */
    ELYTRA,
    /** Plays around the player on foot (see {@link PlayerTrailMode} for sub-modes). */
    PLAYER,
    /** Plays while the player is swimming in water. */
    SWIM,
    /** Plays along the path of a thrown projectile. */
    ARROW
}
