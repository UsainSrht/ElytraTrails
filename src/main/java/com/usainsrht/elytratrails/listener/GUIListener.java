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

        boolean isCosmetics = event.getView().getTopInventory().getHolder() instanceof CosmeticsGUIHolder;
        boolean isTrail = event.getView().getTopInventory().getHolder() instanceof TrailGUIHolder;

        if (!isCosmetics && !isTrail) return;

        if (event.getClickedInventory() == event.getView().getTopInventory()) {
            event.setCancelled(true);
            if (isCosmetics) {
                cosmeticsGUI.handleClick(player, event.getSlot(), event);
            } else {
                TrailGUIHolder holder = (TrailGUIHolder) event.getView().getTopInventory().getHolder();
                trailGUI.handleClick(player, event.getSlot(), holder);
            }
        } else {
            // Prevent shift-clicking items from player inventory into the GUI
            if (event.isShiftClick()) {
                event.setCancelled(true);
            }
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
