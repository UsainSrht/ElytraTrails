package com.usainsrht.elytratrails.listener;

import com.usainsrht.elytratrails.config.PlayerDataManager;
import com.usainsrht.elytratrails.config.TrailManager;
import com.usainsrht.elytratrails.model.Trail;
import com.usainsrht.elytratrails.model.TrailCategory;
import com.usainsrht.elytratrails.trail.ProjectileTrailTask;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;

import java.util.UUID;

/**
 * Listens for projectile launch/land events to register and unregister
 * arrow (projectile) trails in the {@link ProjectileTrailTask}.
 *
 * <p>Any thrown projectile fired by a player counts — arrows, tridents,
 * snowballs, eggs, potions, etc.
 */
public class ProjectileListener implements Listener {

    private final PlayerDataManager playerData;
    private final TrailManager trailManager;
    private final ProjectileTrailTask projectileTask;

    public ProjectileListener(PlayerDataManager playerData,
                              TrailManager trailManager,
                              ProjectileTrailTask projectileTask) {
        this.playerData = playerData;
        this.trailManager = trailManager;
        this.projectileTask = projectileTask;
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();

        // Only care about player-fired projectiles
        if (!(projectile.getShooter() instanceof Player player)) return;

        UUID uuid = player.getUniqueId();
        String trailId = playerData.getActiveTrail(uuid, TrailCategory.ARROW);
        if (trailId == null) return;

        Trail trail = trailManager.getTrail(trailId);
        if (trail == null || trail.getCategory() != TrailCategory.ARROW) return;

        // Permission check
        if (!player.hasPermission("elytratrails.use.arrow")) {
            return;
        }

        if (!player.hasPermission("elytratrails.trail.*")
                && !player.hasPermission(trail.getPermission())) {
            return;
        }

        projectileTask.register(projectile, trail);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        UUID uid = event.getEntity().getUniqueId();
        if (projectileTask.isTracked(uid)) {
            projectileTask.unregister(uid);
        }
    }
}
