package com.usainsrht.elytratrails.listener;

import com.usainsrht.elytratrails.config.PlayerDataManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Loads player data on join and saves/unloads on quit.
 */
public class PlayerListener implements Listener {

    private final PlayerDataManager playerData;

    public PlayerListener(PlayerDataManager playerData) {
        this.playerData = playerData;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        playerData.load(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerData.unload(event.getPlayer().getUniqueId());
    }
}

