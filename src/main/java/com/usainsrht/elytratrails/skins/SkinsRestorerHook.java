package com.usainsrht.elytratrails.skins;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Consumer;

/**
 * Sends skin change requests to the SkinsRestorer Velocity proxy via plugin messaging.
 * This works in proxy mode without a backend database or SkinsRestorer API.
 */
public class SkinsRestorerHook {

    private final JavaPlugin plugin;
    private final boolean enabled;

    public SkinsRestorerHook(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, SkinRestorerMessages.MESSAGE_CHANNEL);
        this.enabled = true;
        plugin.getLogger().info("Skin changing enabled via SkinsRestorer proxy plugin messaging.");
    }

    public void disable() {
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, SkinRestorerMessages.MESSAGE_CHANNEL);
    }

    /**
     * @return true if skin changing via proxy plugin messaging is available.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Requests a skin change on the Velocity proxy for the given player.
     * Callbacks are executed on the primary Bukkit server thread.
     *
     * @param player    the player to change skin for
     * @param skinName  the username whose skin to fetch and apply
     * @param onSuccess runnable called after the request is sent to the proxy
     * @param onFailure consumer called with an error message on failure
     */
    public void changeSkin(Player player, String skinName, Runnable onSuccess, Consumer<String> onFailure) {
        if (!enabled) {
            onFailure.accept("SkinsRestorer is not enabled!");
            return;
        }

        if (skinName == null || skinName.isBlank()) {
            onFailure.accept("Username cannot be empty.");
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                onFailure.accept("Player is no longer online.");
                return;
            }

            try {
                byte[] payload = SkinRestorerMessages.createSetSkinPayload(skinName.trim());
                player.sendPluginMessage(plugin, SkinRestorerMessages.MESSAGE_CHANNEL, payload);
                onSuccess.run();
            } catch (Exception e) {
                onFailure.accept("Failed to send skin change request: " + e.getMessage());
            }
        });
    }
}
