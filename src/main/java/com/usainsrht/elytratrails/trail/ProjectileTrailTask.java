package com.usainsrht.elytratrails.trail;

import com.usainsrht.elytratrails.ElytraTrails;
import com.usainsrht.elytratrails.model.Emitter;
import com.usainsrht.elytratrails.model.Trail;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Projectile;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Synchronous ticker that spawns particle trails on active projectiles.
 * A projectile entry is added by {@link com.usainsrht.elytratrails.listener.ProjectileListener}
 * on launch and removed on hit/land/expire.
 */
public class ProjectileTrailTask extends BukkitRunnable {

    /** Maps projectile UUID → the Trail to render */
    private final Map<UUID, Trail> activeProjectiles = new ConcurrentHashMap<>();
    /** Per-projectile tick counter for interval tracking */
    private final Map<UUID, Integer> projectileTicks  = new ConcurrentHashMap<>();

    private final ElytraTrails plugin;

    public ProjectileTrailTask(ElytraTrails plugin) {
        this.plugin = plugin;
    }

    // ── Registration ────────────────────────────────────────

    public void register(Projectile projectile, Trail trail) {
        activeProjectiles.put(projectile.getUniqueId(), trail);
        projectileTicks.put(projectile.getUniqueId(), 0);
    }

    public void unregister(UUID projectileUUID) {
        activeProjectiles.remove(projectileUUID);
        projectileTicks.remove(projectileUUID);
    }

    public boolean isTracked(UUID projectileUUID) {
        return activeProjectiles.containsKey(projectileUUID);
    }

    // ── BukkitRunnable ──────────────────────────────────────

    @Override
    public void run() {
        if (activeProjectiles.isEmpty()) return;

        // Iterate all tracked projectiles
        Iterator<Map.Entry<UUID, Trail>> it = activeProjectiles.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Trail> entry = it.next();
            UUID uid = entry.getKey();
            Trail trail = entry.getValue();

            // Find the entity in the world
            org.bukkit.entity.Entity entity = null;
            for (org.bukkit.World world : plugin.getServer().getWorlds()) {
                entity = world.getEntity(uid);
                if (entity != null) break;
            }

            // Remove dead / landed projectiles
            if (entity == null || entity.isDead() || !entity.isValid()) {
                it.remove();
                projectileTicks.remove(uid);
                continue;
            }

            int pt = projectileTicks.merge(uid, 1, Integer::sum);
            Location loc = entity.getLocation();
            Vector velocity = entity.getVelocity().normalize();

            for (Emitter emitter : trail.getEmitters()) {
                if (pt % emitter.getInterval() != 0) continue;
                spawnProjectileEmitter(loc, velocity, emitter, pt);
            }
        }
    }

    // ── Particle spawning ───────────────────────────────────

    private void spawnProjectileEmitter(Location loc, Vector velocity, Emitter emitter, int pt) {
        Color color = resolveColor(emitter, pt);

        if (emitter.getParticle() == Particle.DUST && color != null) {
            Particle.DustOptions dust = new Particle.DustOptions(color, emitter.getSize());
            loc.getWorld().spawnParticle(Particle.DUST, loc,
                    emitter.getAmount(),
                    emitter.getOffset().getX(), emitter.getOffset().getY(), emitter.getOffset().getZ(),
                    emitter.getSpeed(), dust);
        } else if (emitter.isRandomDirection()) {
            for (int i = 0; i < emitter.getAmount(); i++) {
                Vector dir = randomUnitVector().multiply(emitter.getRandomDirectionSpeed());
                loc.getWorld().spawnParticle(emitter.getParticle(), loc,
                        0, dir.getX(), dir.getY(), dir.getZ(), emitter.getRandomDirectionSpeed());
            }
        } else {
            loc.getWorld().spawnParticle(emitter.getParticle(), loc,
                    emitter.getAmount(),
                    emitter.getOffset().getX(), emitter.getOffset().getY(), emitter.getOffset().getZ(),
                    emitter.getSpeed());
        }
    }

    private Color resolveColor(Emitter emitter, int tick) {
        List<Color> colors = emitter.getColors();
        if (colors.isEmpty()) return null;
        int idx = (tick / emitter.getColorCycleRate()) % colors.size();
        return colors.get(idx);
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
