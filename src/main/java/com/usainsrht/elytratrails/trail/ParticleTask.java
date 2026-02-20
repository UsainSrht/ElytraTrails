package com.usainsrht.elytratrails.trail;

import com.usainsrht.elytratrails.ElytraTrails;
import com.usainsrht.elytratrails.config.PlayerDataManager;
import com.usainsrht.elytratrails.config.TrailManager;
import com.usainsrht.elytratrails.model.Trail;
import com.usainsrht.elytratrails.trail.shape.ShapeProvider;
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

/**
 * Synchronous ticker that spawns trail particles behind gliding elytra players.
 * Runs every 2 ticks (10 Hz) on the main thread.
 */
public class ParticleTask extends BukkitRunnable {

    private final ElytraTrails plugin;
    private final TrailManager trailManager;
    private final PlayerDataManager playerData;
    private final Map<String, ShapeProvider> shapes;

    /** Per-player tick counter for animated trails. */
    private final Map<UUID, Integer> tickCounters = new HashMap<>();

    public ParticleTask(ElytraTrails plugin, TrailManager trailManager,
                        PlayerDataManager playerData, Map<String, ShapeProvider> shapes) {
        this.plugin = plugin;
        this.trailManager = trailManager;
        this.playerData = playerData;
        this.shapes = shapes;
    }

    @Override
    public void run() {

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!isGlidingWithElytra(player)) {
                tickCounters.remove(player.getUniqueId());
                continue;
            }

            String trailId = playerData.getActiveTrail(player.getUniqueId());
            if (trailId == null) continue;

            Trail trail = trailManager.getTrail(trailId);
            if (trail == null) continue;

            // Permission check
            if (!player.hasPermission("elytratrails.trail.*")
                    && !player.hasPermission(trail.getPermission())) {
                continue;
            }

            int playerTick = tickCounters.merge(player.getUniqueId(), 1, Integer::sum);

            switch (trail.getTrailType()) {
                case STATIC -> spawnStatic(player, trail);
                case ANIMATED -> spawnAnimated(player, trail, playerTick);
                case COMPLEX -> spawnComplex(player, trail, playerTick);
            }
        }
    }

    // ── Spawn methods ───────────────────────────────────────

    private void spawnStatic(Player player, Trail trail) {
        Location loc = player.getLocation();
        if (trail.getParticle() == Particle.DUST && !trail.getColors().isEmpty()) {
            Color color = trail.getColors().get(0);
            Particle.DustOptions dust = new Particle.DustOptions(color, trail.getSize());
            player.getWorld().spawnParticle(
                    Particle.DUST, loc,
                    trail.getAmount(),
                    trail.getOffset().getX(), trail.getOffset().getY(), trail.getOffset().getZ(),
                    trail.getSpeed(), dust
            );
        } else {
            player.getWorld().spawnParticle(
                    trail.getParticle(), loc,
                    trail.getAmount(),
                    trail.getOffset().getX(), trail.getOffset().getY(), trail.getOffset().getZ(),
                    trail.getSpeed()
            );
        }
    }

    private void spawnAnimated(Player player, Trail trail, int tick) {
        Location loc = player.getLocation();
        List<Color> colors = trail.getColors();
        if (trail.getParticle() == Particle.DUST && !colors.isEmpty()) {
            // Cycle through the colour list
            Color color = colors.get(tick % colors.size());
            Particle.DustOptions dust = new Particle.DustOptions(color, trail.getSize());
            player.getWorld().spawnParticle(
                    Particle.DUST, loc,
                    trail.getAmount(),
                    trail.getOffset().getX(), trail.getOffset().getY(), trail.getOffset().getZ(),
                    trail.getSpeed(), dust
            );
        } else {
            // Fallback: treat as static
            spawnStatic(player, trail);
        }
    }

    private void spawnComplex(Player player, Trail trail, int tick) {
        ShapeProvider shape = shapes.get(trail.getShapeId());
        if (shape == null) {
            // Fallback to static if shape not found
            spawnStatic(player, trail);
            return;
        }

        Location loc = player.getLocation();
        List<Vector> offsets = shape.getOffsets(loc, tick);
        List<Color> colors = trail.getColors();

        for (int i = 0; i < offsets.size(); i++) {
            Location spawnLoc = loc.clone().add(offsets.get(i));
            if (trail.getParticle() == Particle.DUST && !colors.isEmpty()) {
                // Cycle colours across shape points for variety
                Color color = colors.get(i % colors.size());
                Particle.DustOptions dust = new Particle.DustOptions(color, trail.getSize());
                player.getWorld().spawnParticle(
                        Particle.DUST, spawnLoc,
                        trail.getAmount(),
                        0, 0, 0,
                        trail.getSpeed(), dust
                );
            } else {
                player.getWorld().spawnParticle(
                        trail.getParticle(), spawnLoc,
                        trail.getAmount(),
                        0, 0, 0,
                        trail.getSpeed()
                );
            }
        }
    }

    // ── Utility ─────────────────────────────────────────────

    private boolean isGlidingWithElytra(Player player) {
        if (!player.isGliding()) return false;
        ItemStack chestplate = player.getInventory().getChestplate();
        return chestplate != null && chestplate.getType() == Material.ELYTRA;
    }
}

