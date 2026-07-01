package com.usainsrht.elytratrails.gui;

import com.usainsrht.elytratrails.model.TrailCategory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Marker InventoryHolder so we can identify our trail-selection GUI inventories
 * via {@code event.getInventory().getHolder() instanceof TrailGUIHolder}.
 */
public class TrailGUIHolder implements InventoryHolder {

    private final int page;
    private final TrailCategory category;

    public TrailGUIHolder(int page, TrailCategory category) {
        this.page = page;
        this.category = category;
    }

    public int getPage() {
        return page;
    }

    public TrailCategory getCategory() {
        return category;
    }

    @Override
    public @NotNull Inventory getInventory() {
        // Not used — the actual inventory is created by TrailGUI
        throw new UnsupportedOperationException();
    }
}
