package com.usainsrht.elytratrails.listener;

import com.usainsrht.elytratrails.ElytraTrails;
import com.usainsrht.elytratrails.config.PlayerDataManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Loads player data on join and saves/unloads on quit.
 */
public class PlayerListener implements Listener {

    private final ElytraTrails plugin;
    private final PlayerDataManager playerData;

    public PlayerListener(ElytraTrails plugin, PlayerDataManager playerData) {
        this.plugin = plugin;
        this.playerData = playerData;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        playerData.load(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        playerData.unload(uuid);
        if (plugin.getParticleTask() != null) {
            plugin.getParticleTask().cleanup(uuid);
        }
    }
}


