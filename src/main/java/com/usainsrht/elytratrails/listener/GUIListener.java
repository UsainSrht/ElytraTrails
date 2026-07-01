package com.usainsrht.elytratrails.listener;

import com.usainsrht.elytratrails.gui.CosmeticsGUI;
import com.usainsrht.elytratrails.gui.CosmeticsGUIHolder;
import com.usainsrht.elytratrails.gui.TrailGUI;
import com.usainsrht.elytratrails.gui.TrailGUIHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Handles inventory interaction events for both the main Cosmetics GUI
 * and the individual trail-selection sub-GUIs.
 */
public class GUIListener implements Listener {

    private final CosmeticsGUI cosmeticsGUI;
    private final TrailGUI trailGUI;

    public GUIListener(CosmeticsGUI cosmeticsGUI, TrailGUI trailGUI) {
        this.cosmeticsGUI = cosmeticsGUI;
        this.trailGUI = trailGUI;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Only handle clicks in the top inventory
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;

        if (event.getInventory().getHolder() instanceof CosmeticsGUIHolder) {
            event.setCancelled(true);
            cosmeticsGUI.handleClick(player, event.getSlot());
        } else if (event.getInventory().getHolder() instanceof TrailGUIHolder holder) {
            event.setCancelled(true);
            trailGUI.handleClick(player, event.getSlot(), holder);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof CosmeticsGUIHolder
                || event.getInventory().getHolder() instanceof TrailGUIHolder) {
            event.setCancelled(true);
        }
    }
}
