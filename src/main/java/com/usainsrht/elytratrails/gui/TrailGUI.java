package com.usainsrht.elytratrails.gui;

import com.usainsrht.elytratrails.ElytraTrails;
import com.usainsrht.elytratrails.config.ConfigManager;
import com.usainsrht.elytratrails.config.PlayerDataManager;
import com.usainsrht.elytratrails.config.TrailManager;
import com.usainsrht.elytratrails.economy.VaultHook;
import com.usainsrht.elytratrails.model.Trail;
import com.usainsrht.elytratrails.model.TrailCategory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Builds and opens paginated trail-selection GUIs for players.
 * Filters trails by {@link TrailCategory} so each sub-GUI only shows relevant trails.
 */
public class TrailGUI {

    private final ElytraTrails plugin;
    private final TrailManager trailManager;
    private final PlayerDataManager playerData;
    private final VaultHook vaultHook;
    private CosmeticsGUI cosmeticsGUI;

    public TrailGUI(ElytraTrails plugin, TrailManager trailManager,
                    PlayerDataManager playerData, VaultHook vaultHook,
                    CosmeticsGUI cosmeticsGUI) {
        this.plugin = plugin;
        this.trailManager = trailManager;
        this.playerData = playerData;
        this.vaultHook = vaultHook;
        this.cosmeticsGUI = cosmeticsGUI;
    }

    /**
     * Post-construction injection of the CosmeticsGUI reference
     * (needed to break the TrailGUI ↔ CosmeticsGUI circular dependency).
     */
    public void setCosmeticsGUI(CosmeticsGUI cosmeticsGUI) {
        this.cosmeticsGUI = cosmeticsGUI;
    }

    // ── Category display names & colors ─────────────────────

    private String categoryTitle(TrailCategory category) {
        ConfigManager cm = plugin.getConfigManager();
        return cm.getGuiConfig().getString("trail-gui.titles." + category.name(), switch (category) {
            case ELYTRA -> "<aqua><bold>Elytra Trails";
            case PLAYER -> "<green><bold>Player Trails";
            case ARROW -> "<gold><bold>Arrow Trails";
        });
    }

    private Material categoryFillerMaterial(TrailCategory category) {
        ConfigManager cm = plugin.getConfigManager();
        String matName = cm.getGuiConfig().getString("trail-gui.filler-materials." + category.name());
        if (matName != null) {
            try {
                return Material.valueOf(matName.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        return switch (category) {
            case ELYTRA -> Material.CYAN_STAINED_GLASS_PANE;
            case PLAYER -> Material.GREEN_STAINED_GLASS_PANE;
            case ARROW  -> Material.ORANGE_STAINED_GLASS_PANE;
        };
    }

    /**
     * Open the trail GUI for a player at a given page (0-indexed) for a specific category.
     */
    public void open(Player player, int page, TrailCategory category) {
        ConfigManager cm = plugin.getConfigManager();
        List<Trail> trails = trailManager.getTrailsByCategory(category);

        int size = cm.getGuiConfig().getInt("trail-gui.size", 54);
        int trailSlotsCount = size - 9;

        int totalPages = Math.max(1, (int) Math.ceil((double) trails.size() / trailSlotsCount));
        page = Math.max(0, Math.min(page, totalPages - 1));

        String rawTitle = categoryTitle(category);
        Component title = MiniMessage.miniMessage().deserialize(
                rawTitle.replace("%page%", String.valueOf(page + 1))
                        .replace("%total_pages%", String.valueOf(totalPages))
        );

        TrailGUIHolder holder = new TrailGUIHolder(page, category);
        Inventory inv = Bukkit.createInventory(holder, size, title);

        UUID uuid = player.getUniqueId();
        String activeTrailId = playerData.getActiveTrail(uuid, category);

        int startIndex = page * trailSlotsCount;
        int endIndex = Math.min(startIndex + trailSlotsCount, trails.size());

        for (int i = startIndex; i < endIndex; i++) {
            Trail trail = trails.get(i);
            int slot = i - startIndex;
            inv.setItem(slot, createTrailItem(trail, uuid, activeTrailId, player));
        }

        // Fill non-trail slots (from trailSlotsCount to size) with filler
        String fillerName = cm.getGuiConfig().getString("trail-gui.items.filler.display-name", " ");
        ItemStack filler = createFiller(categoryFillerMaterial(category), fillerName);
        for (int i = trailSlotsCount; i < size; i++) {
            inv.setItem(i, filler);
        }

        // Back button
        int backSlot = cm.getGuiConfig().getInt("trail-gui.items.back.slot", 45);
        if (backSlot >= 0 && backSlot < size) {
            inv.setItem(backSlot, createNavItem(cm, "back", Material.ARROW, "<light_purple><bold>← Back to Cosmetics</bold>"));
        }

        // Previous page button
        if (page > 0) {
            int prevSlot = cm.getGuiConfig().getInt("trail-gui.items.previous-page.slot", 46);
            if (prevSlot >= 0 && prevSlot < size) {
                inv.setItem(prevSlot, createNavItem(cm, "previous-page", Material.PAPER, "<yellow>← Previous Page"));
            }
        }

        // Close button
        int closeSlot = cm.getGuiConfig().getInt("trail-gui.items.close.slot", 49);
        if (closeSlot >= 0 && closeSlot < size) {
            inv.setItem(closeSlot, createNavItem(cm, "close", Material.BARRIER, "<red>Close"));
        }

        // Next page button
        if (page < totalPages - 1) {
            int nextSlot = cm.getGuiConfig().getInt("trail-gui.items.next-page.slot", 52);
            if (nextSlot >= 0 && nextSlot < size) {
                inv.setItem(nextSlot, createNavItem(cm, "next-page", Material.PAPER, "<yellow>Next Page →"));
            }
        }

        // Deselect trail button
        if (activeTrailId != null) {
            int deselectSlot = cm.getGuiConfig().getInt("trail-gui.items.deselect.slot", 47);
            if (deselectSlot >= 0 && deselectSlot < size) {
                inv.setItem(deselectSlot, createNavItem(cm, "deselect", Material.MILK_BUCKET, "<yellow>Deselect Trail"));
            }
        }

        player.openInventory(inv);
    }

    /**
     * Handle a click inside the trail GUI.
     */
    public void handleClick(Player player, int slot, TrailGUIHolder holder) {
        int page = holder.getPage();
        TrailCategory category = holder.getCategory();
        ConfigManager cm = plugin.getConfigManager();

        int backSlot = cm.getGuiConfig().getInt("trail-gui.items.back.slot", 45);
        int prevSlot = cm.getGuiConfig().getInt("trail-gui.items.previous-page.slot", 46);
        int deselectSlot = cm.getGuiConfig().getInt("trail-gui.items.deselect.slot", 47);
        int closeSlot = cm.getGuiConfig().getInt("trail-gui.items.close.slot", 49);
        int nextSlot = cm.getGuiConfig().getInt("trail-gui.items.next-page.slot", 52);

        // Back button
        if (slot == backSlot) {
            cosmeticsGUI.open(player);
            return;
        }
        // Previous page
        if (slot == prevSlot) {
            if (page > 0) open(player, page - 1, category);
            return;
        }
        // Close
        if (slot == closeSlot) {
            player.closeInventory();
            return;
        }
        // Next page
        if (slot == nextSlot) {
            List<Trail> trails = trailManager.getTrailsByCategory(category);
            int size = cm.getGuiConfig().getInt("trail-gui.size", 54);
            int trailSlotsCount = size - 9;
            int totalPages = Math.max(1, (int) Math.ceil((double) trails.size() / trailSlotsCount));
            if (page < totalPages - 1) open(player, page + 1, category);
            return;
        }
        // Deselect
        if (slot == deselectSlot) {
            playerData.clearActiveTrail(player.getUniqueId(), category);
            player.sendMessage(cm.getMessage("trail-deselected-general"));
            open(player, page, category);
            return;
        }

        // Trail slot click
        int size = cm.getGuiConfig().getInt("trail-gui.size", 54);
        int trailSlotsCount = size - 9;
        if (slot < 0 || slot >= trailSlotsCount) return;

        List<Trail> trails = trailManager.getTrailsByCategory(category);
        int index = page * trailSlotsCount + slot;
        if (index >= trails.size()) return;

        Trail trail = trails.get(index);
        UUID uuid = player.getUniqueId();

        String typePerm = "elytratrails.use." + category.name().toLowerCase();
        if (!player.hasPermission(typePerm)) {
            player.sendMessage(cm.getMessage("no-trail-permission"));
            return;
        }

        // Check if already active → deselect
        if (trail.getId().equals(playerData.getActiveTrail(uuid, category))) {
            playerData.clearActiveTrail(uuid, category);
            player.sendMessage(cm.getMessage("trail-deselected", "%trail%", trail.getDisplayName()));
            open(player, page, category);
            return;
        }

        if (playerData.hasTrailAccess(player, trail)) {
            // Select the trail
            playerData.setActiveTrail(uuid, category, trail.getId());
            player.sendMessage(cm.getMessage("trail-selected", "%trail%", trail.getDisplayName()));
            open(player, page, category);
        } else if (trail.getPrice() > 0 && player.hasPermission("elytratrails.buy")) {
            // Attempt purchase
            attemptPurchase(player, trail, page, category);
        } else {
            player.sendMessage(cm.getMessage("no-trail-permission"));
        }
    }

    // ── Private helpers ─────────────────────────────────────

    private void attemptPurchase(Player player, Trail trail, int page, TrailCategory category) {
        ConfigManager cm = plugin.getConfigManager();
        if (!vaultHook.isEnabled()) {
            player.sendMessage(cm.getMessage("economy-not-available"));
            return;
        }

        if (!vaultHook.has(player, trail.getPrice())) {
            player.sendMessage(cm.getMessage("not-enough-money", "%price%", vaultHook.format(trail.getPrice())));
            return;
        }

        if (vaultHook.withdraw(player, trail.getPrice())) {
            playerData.unlockTrail(player.getUniqueId(), trail.getId());
            playerData.setActiveTrail(player.getUniqueId(), category, trail.getId());
            player.sendMessage(cm.getMessage("purchase-success", "%trail%",
                    trail.getDisplayName(),
                    "%price%", vaultHook.format(trail.getPrice())));
            open(player, page, category);
        } else {
            player.sendMessage(cm.getMessage("transaction-failed"));
        }
    }

    private ItemStack createTrailItem(Trail trail, UUID uuid, String activeTrailId, Player player) {
        boolean isActive = trail.getId().equals(activeTrailId);
        boolean isUnlocked = playerData.hasTrailAccess(player, trail);
        boolean hasTrailPermission = player.hasPermission("elytratrails.trail.*")
                || player.hasPermission(trail.getPermission());
        boolean isFree = trail.getPrice() <= 0;

        ItemStack item = new ItemStack(trail.getIcon());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        ConfigManager cm = plugin.getConfigManager();
        String displayNameRaw = trail.getDisplayName();
        String nameFormat;
        if (isActive) {
            nameFormat = cm.getGuiConfig().getString("trail-item.display-names.active", "<green>★ %display_name% <green>★");
        } else if (isUnlocked) {
            nameFormat = cm.getGuiConfig().getString("trail-item.display-names.unlocked", "<white>%display_name%");
        } else {
            nameFormat = cm.getGuiConfig().getString("trail-item.display-names.locked", "<red>✖ %display_name%");
        }

        Component displayName = MiniMessage.miniMessage().deserialize(nameFormat.replace("%display_name%", displayNameRaw))
                .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
        meta.displayName(displayName);

        // Build Status lines first
        List<String> statusLines = new ArrayList<>();
        if (isActive) {
            statusLines = cm.getGuiConfig().getStringList("trail-item.status-lines.active");
            if (statusLines.isEmpty()) {
                statusLines = Arrays.asList(
                    "<green>▶ Currently Active",
                    "<yellow>Click to deselect."
                );
            }
        } else if (isUnlocked) {
            statusLines = cm.getGuiConfig().getStringList("trail-item.status-lines.unlocked");
            if (statusLines.isEmpty()) {
                statusLines = Arrays.asList(
                    "<green>✔ Unlocked",
                    "<yellow>Click to select."
                );
            }
        } else {
            // Locked
            if (isFree) {
                List<String> rawStatus = cm.getGuiConfig().getStringList("trail-item.status-lines.locked-free");
                if (rawStatus.isEmpty()) {
                    rawStatus = new ArrayList<>();
                    rawStatus.add("<red>✖ Locked");
                    rawStatus.add("%permission_line%");
                }
                for (String line : rawStatus) {
                    if (line.contains("%permission_line%")) {
                        if (!hasTrailPermission) {
                            String permLine = cm.getGuiConfig().getString("trail-item.permission-line-format", "<dark_gray>Permission: %permission%")
                                    .replace("%permission%", trail.getPermission());
                            statusLines.add(permLine);
                        }
                    } else {
                        statusLines.add(line);
                    }
                }
            } else {
                List<String> rawStatus = cm.getGuiConfig().getStringList("trail-item.status-lines.locked-paid");
                if (rawStatus.isEmpty()) {
                    rawStatus = new ArrayList<>();
                    rawStatus.add("<red>✖ Locked");
                    rawStatus.add("<gold>Price: <white>%price%");
                    rawStatus.add("%buy_prompt%");
                    rawStatus.add("%permission_line%");
                }
                String formattedPrice = vaultHook.isEnabled() ? vaultHook.format(trail.getPrice()) : String.format("$%.2f", trail.getPrice());
                for (String line : rawStatus) {
                    if (line.contains("%permission_line%")) {
                        if (!hasTrailPermission) {
                            String permLine = cm.getGuiConfig().getString("trail-item.permission-line-format", "<dark_gray>Permission: %permission%")
                                    .replace("%permission%", trail.getPermission());
                            statusLines.add(permLine);
                        }
                    } else if (line.contains("%buy_prompt%")) {
                        if (player.hasPermission("elytratrails.buy")) {
                            String buyPrompt = cm.getGuiConfig().getString("trail-item.buy-prompt-line", "<yellow>Click to purchase.");
                            statusLines.add(buyPrompt);
                        }
                    } else {
                        statusLines.add(line.replace("%price%", formattedPrice));
                    }
                }
            }
        }

        // Build status Components
        List<Component> componentStatus = new ArrayList<>();
        for (String line : statusLines) {
            componentStatus.add(MiniMessage.miniMessage().deserialize(line));
        }

        // Build complete Lore from template
        List<String> loreTemplate = cm.getGuiConfig().getStringList("trail-item.lore");
        if (loreTemplate.isEmpty()) {
            loreTemplate = Arrays.asList(
                "<gray>Type: <white>%type%",
                "%mode_line%",
                "",
                "%status%"
            );
        }

        List<Component> finalLore = new ArrayList<>();
        for (String templateLine : loreTemplate) {
            if (templateLine.contains("%mode_line%")) {
                if (trail.getCategory() == TrailCategory.PLAYER) {
                    String modeLine = cm.getGuiConfig().getString("trail-item.mode-line-format", "<gray>Mode: <white>%mode%")
                            .replace("%mode%", trail.getPlayerTrailMode().name());
                    finalLore.add(MiniMessage.miniMessage().deserialize(modeLine));
                }
            } else if (templateLine.contains("%status%")) {
                finalLore.addAll(componentStatus);
            } else {
                finalLore.add(MiniMessage.miniMessage().deserialize(templateLine
                        .replace("%type%", trail.getTrailType().name())
                        .replace("%permission%", trail.getPermission())
                ));
            }
        }

        List<Component> noItalicLore = new ArrayList<>();
        for (Component comp : finalLore) {
            noItalicLore.add(comp.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        }
        meta.lore(noItalicLore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

        // Enchant glow for active trail
        if (isActive) {
            meta.setEnchantmentGlintOverride(true);
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createNavItem(ConfigManager cm, String key, Material defaultMaterial, String defaultName) {
        String path = "trail-gui.items." + key;
        Material mat = defaultMaterial;
        String matStr = cm.getGuiConfig().getString(path + ".material");
        if (matStr != null) {
            try {
                mat = Material.valueOf(matStr.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        String nameRaw = cm.getGuiConfig().getString(path + ".display-name", defaultName);
        return createItem(mat, MiniMessage.miniMessage().deserialize(nameRaw));
    }

    private ItemStack createItem(Material material, Component name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createFiller(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize(name).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(meta);
        }
        return item;
    }
}

