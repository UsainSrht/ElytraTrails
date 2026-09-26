package com.usainsrht.elytratrails.trail;

import com.destroystokyo.paper.ParticleBuilder;
import com.usainsrht.elytratrails.ElytraTrails;
import com.usainsrht.elytratrails.model.Emitter;
import com.usainsrht.elytratrails.model.Trail;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Synchronous ticker that spawns particle trails on active projectiles.
 * A projectile entry is added by {@link com.usainsrht.elytratrails.listener.ProjectileListener}
 * on launch and removed on hit/land/expire.
 */
public class ProjectileTrailTask extends BukkitRunnable {

    private static final Pattern DURATION_PATTERN = Pattern.compile(
            "(\\d+(?:\\.\\d+)?)\\s*(m(?:in(?:ute)?s?)?|s(?:ec(?:ond)?s?)?|t(?:ick?s?)?|h(?:(?:ou)?rs?)?)",
            Pattern.CASE_INSENSITIVE
    );

    /** Maps projectile UUID → the Trail to render */
    private final Map<UUID, Trail> activeProjectiles = new ConcurrentHashMap<>();
    /** Per-projectile tick counter for interval tracking */
    private final Map<UUID, Integer> projectileTicks  = new ConcurrentHashMap<>();
    /** Maps projectile UUID → shooter player UUID */
    private final Map<UUID, UUID> projectileShooters = new ConcurrentHashMap<>();

    private final ElytraTrails plugin;

    /** Ticks to wait before particles start spawning (first N ticks spawn nothing). Default 5. */
    private int delayTicks = 5;
    /** Maximum lifetime in ticks before the trail stops and unregisters. Default 1200 ticks (1 minute). */
    private int maxLifetimeTicks = 1200;

    public ProjectileTrailTask(ElytraTrails plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public ElytraTrails getPlugin() {
        return plugin;
    }

    /**
     * Loads/reloads delay and max-lifetime settings from config.yml.
     */
    public void loadConfig() {
        FileConfiguration config = plugin.getConfig();
        this.delayTicks = parseConfigTicks(config, "arrow-trails.delay", "arrow-trails.delay-ticks", 5);
        this.maxLifetimeTicks = parseConfigTicks(config, "arrow-trails.max-lifetime", "arrow-trails.max-lifetime-ticks", 1200);
    }

    private int parseConfigTicks(FileConfiguration config, String path, String altPath, int def) {
        Object val = config.get(path);
        if (val == null && altPath != null) {
            val = config.get(altPath);
        }
        if (val == null) {
            val = config.get(path.replace("arrow-trails.", "arrow-trail."));
        }
        if (val == null) {
            val = config.get(path.replace("arrow-trails.", "arrow."));
        }
        if (val == null) {
            return def;
        }
        if (val instanceof Number num) {
            return num.intValue();
        }
        return parseDuration(val.toString(), def);
    }

    public static int parseDuration(String str, int defaultTicks) {
        if (str == null || str.isBlank()) {
            return defaultTicks;
        }
        str = str.trim();
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException ignored) {}

        try {
            Matcher m = DURATION_PATTERN.matcher(str);
            double totalTicks = 0;
            boolean matched = false;
            while (m.find()) {
                matched = true;
                double val = Double.parseDouble(m.group(1));
                String unit = m.group(2).toLowerCase();
                if (unit.startsWith("m") && !unit.startsWith("ms")) {
                    totalTicks += val * 60 * 20; // 1 min = 1200 ticks
                } else if (unit.startsWith("s")) {
                    totalTicks += val * 20;      // 1 sec = 20 ticks
                } else if (unit.startsWith("h")) {
                    totalTicks += val * 3600 * 20;
                } else if (unit.startsWith("t")) {
                    totalTicks += val;           // ticks
                }
            }
            if (matched) {
                return (int) Math.round(totalTicks);
            }
        } catch (Exception ignored) {}

        return defaultTicks;
    }

    public int getDelayTicks() {
        return delayTicks;
    }

    public void setDelayTicks(int delayTicks) {
        this.delayTicks = delayTicks;
    }

    public int getMaxLifetimeTicks() {
        return maxLifetimeTicks;
    }

    public void setMaxLifetimeTicks(int maxLifetimeTicks) {
        this.maxLifetimeTicks = maxLifetimeTicks;
    }

    // ── Registration ────────────────────────────────────────

    public void register(Projectile projectile, Trail trail, UUID shooterUuid) {
        UUID uid = projectile.getUniqueId();
        activeProjectiles.put(uid, trail);
        projectileTicks.put(uid, 0);
        if (shooterUuid != null) {
            projectileShooters.put(uid, shooterUuid);
        }
    }

    public void register(Projectile projectile, Trail trail) {
        UUID shooterUuid = projectile.getShooter() instanceof Player p ? p.getUniqueId() : null;
        register(projectile, trail, shooterUuid);
    }

    public void unregister(UUID projectileUUID) {
        activeProjectiles.remove(projectileUUID);
        projectileTicks.remove(projectileUUID);
        projectileShooters.remove(projectileUUID);
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
            if (entity == null || entity.isDead() || !entity.isValid()
                    || (entity instanceof AbstractArrow arrow && arrow.isInBlock())) {
                it.remove();
                projectileTicks.remove(uid);
                projectileShooters.remove(uid);
                continue;
            }

            int pt = projectileTicks.merge(uid, 1, Integer::sum);

            // Max lifetime check (stops and unregisters old projectiles)
            if (maxLifetimeTicks > 0 && pt > maxLifetimeTicks) {
                it.remove();
                projectileTicks.remove(uid);
                projectileShooters.remove(uid);
                continue;
            }

            // Initial flight delay check (first N ticks do not spawn particles)
            if (delayTicks > 0 && pt <= delayTicks) {
                continue;
            }

            UUID shooterUuid = projectileShooters.get(uid);
            Player shooter = shooterUuid != null ? plugin.getServer().getPlayer(shooterUuid) : null;
            if (shooter == null && entity instanceof Projectile proj && proj.getShooter() instanceof Player p) {
                shooter = p;
            }

            if (plugin.isDisableInSpectator() && shooter != null && shooter.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }

            Location loc = entity.getLocation();
            Vector velocity = entity.getVelocity().normalize();

            for (Emitter emitter : trail.getEmitters()) {
                if (pt % emitter.getInterval() != 0) continue;
                spawnProjectileEmitter(loc, velocity, emitter, pt, shooter);
            }
        }
    }

    // ── Particle spawning ───────────────────────────────────

    private void spawnProjectileEmitter(Location loc, Vector velocity, Emitter emitter, int pt, Player shooter) {
        Color color = resolveColor(emitter, pt);
        Object data = ParticleTask.resolveParticleData(emitter, color);

        if (emitter.isRandomDirection()) {
            for (int i = 0; i < emitter.getAmount(); i++) {
                Vector dir = randomUnitVector().multiply(emitter.getRandomDirectionSpeed());
                spawnParticle(shooter, loc, emitter.getParticle(),
                        0, dir.getX(), dir.getY(), dir.getZ(), emitter.getRandomDirectionSpeed(), data);
            }
        } else {
            spawnParticle(shooter, loc, emitter.getParticle(),
                    emitter.getAmount(),
                    emitter.getOffset().getX(), emitter.getOffset().getY(), emitter.getOffset().getZ(),
                    emitter.getSpeed(), data);
        }
    }

    private void spawnParticle(Player shooter, Location loc, Particle particle,
                               int count, double ox, double oy, double oz,
                               double speed, Object data) {
        try {
            ParticleBuilder builder = new ParticleBuilder(particle)
                    .location(loc)
                    .count(count)
                    .offset(ox, oy, oz)
                    .extra(speed);
            if (plugin.isRespectVanish() && shooter != null) {
                builder.source(shooter);
            }
            if (data != null) {
                builder.data(data);
            } else if (particle.getDataType() == Float.class) {
                builder.data(1.0f);
            } else if (particle.getDataType() == Color.class) {
                builder.data(Color.WHITE);
            } else if (particle.getDataType() == Integer.class) {
                builder.data(0);
            }
            builder.spawn();
        } catch (Exception ignored) {
            // Guard against unexpected particle spawning issues
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
