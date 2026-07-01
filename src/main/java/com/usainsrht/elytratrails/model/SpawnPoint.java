package com.usainsrht.elytratrails.model;

/**
 * Where on the player's body an emitter spawns particles.
 */
public enum SpawnPoint {
    /** Tip of the left elytra wing. */
    LEFT_WING,
    /** Tip of the right elytra wing. */
    RIGHT_WING,
    /** Both wing tips (shorthand). */
    WINGS,
    /** At the player's feet. */
    FEET,
    /** Center of the player's body / behind. */
    BODY,
    /** Directly behind the player along their flight vector. */
    BEHIND,
    /** Ring/orbit around the player (player trails). */
    AROUND,
    /** Above the player's head (player trails). */
    ABOVE,
    /** Below the player's feet (player trails). */
    BELOW
}

