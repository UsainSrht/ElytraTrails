package com.usainsrht.elytratrails.config;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Soft-depend wrapper around WorldGuard's region API.
 * All methods degrade gracefully if WorldGuard is not installed.
 */
public class WorldGuardHook {

    private final boolean enabled;
    private final Logger logger;

    public WorldGuardHook(Logger logger) {
        this.logger = logger;
        boolean wgPresent = false;
        try {
            // Check both WorldGuard and WorldEdit (required by WG 7.x)
            if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
                // Access WorldGuard API — this will throw if not properly loaded
                WorldGuard.getInstance();
                wgPresent = true;
                logger.info("WorldGuard found — region filtering enabled.");
            }
        } catch (Exception e) {
            logger.info("WorldGuard not found — region filtering disabled.");
        }
        this.enabled = wgPresent;
    }

    /**
     * @return true if WorldGuard is loaded and available.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Check if the player is currently inside any of the given region IDs.
     *
     * @param player  the player to check
     * @param regions the set of region IDs to look for
     * @return true if the player is in at least one of those regions
     */
    public boolean isPlayerInRegions(Player player, List<String> regions) {
        if (!enabled || regions == null || regions.isEmpty()) return false;
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager manager = container.get(BukkitAdapter.adapt(player.getWorld()));
            if (manager == null) return false;

            Set<ProtectedRegion> applicable = manager.getApplicableRegions(
                    BukkitAdapter.adapt(player.getLocation()).toVector().toBlockPoint()
            ).getRegions();

            for (ProtectedRegion region : applicable) {
                if (regions.contains(region.getId())) {
                    return true;
                }
            }
        } catch (Exception e) {
            // Silently fail if WG throws unexpectedly
        }
        return false;
    }
}
