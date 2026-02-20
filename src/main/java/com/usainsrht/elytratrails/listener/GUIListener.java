package com.usainsrht.elytratrails.listener;

import com.usainsrht.elytratrails.gui.TrailGUI;
import com.usainsrht.elytratrails.gui.TrailGUIHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Handles inventory interaction events for the trail GUI.
 */
public class GUIListener implements Listener {

    private final TrailGUI trailGUI;

    public GUIListener(TrailGUI trailGUI) {
        this.trailGUI = trailGUI;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof TrailGUIHolder holder)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Only handle clicks in the top inventory
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;

        trailGUI.handleClick(player, event.getSlot(), holder);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof TrailGUIHolder) {
            event.setCancelled(true);
        }
    }
}

