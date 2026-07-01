package com.usainsrht.elytratrails.skins;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Soft-dependency wrapper around SkinsRestorer API.
 * Safely changes a player's skin asynchronously, with callbacks on the main thread.
 */
public class SkinsRestorerHook {

    private final JavaPlugin plugin;
    private final boolean enabled;

    public SkinsRestorerHook(JavaPlugin plugin) {
        this.plugin = plugin;
        boolean srPresent = false;
        try {
            if (Bukkit.getPluginManager().getPlugin("SkinsRestorer") != null) {
                // Try class loading to make sure the API is present
                Class.forName("net.skinsrestorer.api.SkinsRestorerProvider");
                srPresent = true;
            }
        } catch (Exception e) {
            // SkinsRestorer class not found or other issues
        }
        this.enabled = srPresent;
        if (enabled) {
            plugin.getLogger().info("SkinsRestorer found — skin changing enabled.");
        } else {
            plugin.getLogger().info("SkinsRestorer not found — skin changing disabled.");
        }
    }

    /**
     * @return true if SkinsRestorer API is loaded and available.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Changes the skin of the player asynchronously.
     * Callbacks are executed on the primary Bukkit server thread.
     *
     * @param player    the player to change skin for
     * @param skinName  the username whose skin to fetch and apply
     * @param onSuccess runnable called on successful application
     * @param onFailure consumer called with an error message on failure
     */
    public void changeSkin(Player player, String skinName, Runnable onSuccess, Consumer<String> onFailure) {
        if (!enabled) {
            onFailure.accept("SkinsRestorer is not enabled!");
            return;
        }

        // Run the skin resolution asynchronously to prevent blocking the main server thread
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                net.skinsrestorer.api.SkinsRestorer skinsRestorerAPI = net.skinsrestorer.api.SkinsRestorerProvider.get();
                net.skinsrestorer.api.storage.SkinStorage skinStorage = skinsRestorerAPI.getSkinStorage();
                net.skinsrestorer.api.storage.PlayerStorage playerStorage = skinsRestorerAPI.getPlayerStorage();

                Optional<net.skinsrestorer.api.property.InputDataResult> result = skinStorage.findOrCreateSkinData(skinName);
                if (result.isPresent()) {
                    // Set and apply the skin synchronously on the main thread
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        try {
                            playerStorage.setSkinIdOfPlayer(player.getUniqueId(), result.get().getIdentifier());
                            skinsRestorerAPI.getSkinApplier(Player.class).applySkin(player);
                            onSuccess.run();
                        } catch (Exception e) {
                            onFailure.accept("Failed to apply skin: " + e.getMessage());
                        }
                    });
                } else {
                    Bukkit.getScheduler().runTask(plugin, () -> onFailure.accept("Skin username '" + skinName + "' could not be found."));
                }
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> onFailure.accept("An error occurred: " + e.getMessage()));
            }
        });
    }
}
