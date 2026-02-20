package com.usainsrht.elytratrails.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Marker InventoryHolder so we can identify our GUI inventories
 * via {@code event.getInventory().getHolder() instanceof TrailGUIHolder}.
 */
public class TrailGUIHolder implements InventoryHolder {

    private final int page;

    public TrailGUIHolder(int page) {
        this.page = page;
    }

    public int getPage() {
        return page;
    }

    @Override
    public @NotNull Inventory getInventory() {
        // Not used — the actual inventory is created by TrailGUI
        throw new UnsupportedOperationException();
    }
}

