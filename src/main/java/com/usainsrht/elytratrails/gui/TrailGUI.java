package com.usainsrht.elytratrails.gui;

import com.usainsrht.elytratrails.ElytraTrails;
import com.usainsrht.elytratrails.config.PlayerDataManager;
import com.usainsrht.elytratrails.config.TrailManager;
import com.usainsrht.elytratrails.economy.VaultHook;
import com.usainsrht.elytratrails.model.Trail;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Builds and opens paginated trail-selection GUIs for players.
 */
public class TrailGUI {

    private static final int ROWS = 6;
    private static final int SIZE = ROWS * 9;            // 54 slots
    private static final int TRAIL_SLOTS = (ROWS - 1) * 9; // 45 slots for trails
    private static final String TITLE_PREFIX = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Elytra Trails";

    private final ElytraTrails plugin;
    private final TrailManager trailManager;
    private final PlayerDataManager playerData;
    private final VaultHook vaultHook;

    public TrailGUI(ElytraTrails plugin, TrailManager trailManager,
                    PlayerDataManager playerData, VaultHook vaultHook) {
        this.plugin = plugin;
        this.trailManager = trailManager;
        this.playerData = playerData;
        this.vaultHook = vaultHook;
    }

    /**
     * Open the trail GUI for a player at a given page (0-indexed).
     */
    public void open(Player player, int page) {
        List<Trail> trails = new ArrayList<>(trailManager.getTrails());
        int totalPages = Math.max(1, (int) Math.ceil((double) trails.size() / TRAIL_SLOTS));
        page = Math.max(0, Math.min(page, totalPages - 1));

        String title = TITLE_PREFIX + ChatColor.GRAY + " (" + (page + 1) + "/" + totalPages + ")";
        TrailGUIHolder holder = new TrailGUIHolder(page);
        Inventory inv = Bukkit.createInventory(holder, SIZE, title);

        UUID uuid = player.getUniqueId();
        String activeTrailId = playerData.getActiveTrail(uuid);
        Set<String> unlocked = playerData.getUnlockedTrails(uuid);

        int startIndex = page * TRAIL_SLOTS;
        int endIndex = Math.min(startIndex + TRAIL_SLOTS, trails.size());

        for (int i = startIndex; i < endIndex; i++) {
            Trail trail = trails.get(i);
            int slot = i - startIndex;
            inv.setItem(slot, createTrailItem(trail, uuid, activeTrailId, unlocked, player));
        }

        // ── Navigation bar (row 6, slots 45-53) ─────────────
        // Fill bottom row with glass panes
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = TRAIL_SLOTS; i < SIZE; i++) {
            inv.setItem(i, filler);
        }

        // Previous page button
        if (page > 0) {
            inv.setItem(45, createItem(Material.ARROW, ChatColor.YELLOW + "← Previous Page"));
        }

        // Close button
        inv.setItem(49, createItem(Material.BARRIER, ChatColor.RED + "Close"));

        // Next page button
        if (page < totalPages - 1) {
            inv.setItem(53, createItem(Material.ARROW, ChatColor.YELLOW + "Next Page →"));
        }

        // Deselect trail button
        if (activeTrailId != null) {
            inv.setItem(47, createItem(Material.MILK_BUCKET, ChatColor.YELLOW + "Deselect Trail"));
        }

        player.openInventory(inv);
    }

    /**
     * Handle a click inside the trail GUI.
     */
    public void handleClick(Player player, int slot, TrailGUIHolder holder) {
        int page = holder.getPage();

        // Navigation
        if (slot == 45) {
            // Previous page
            if (page > 0) open(player, page - 1);
            return;
        }
        if (slot == 53) {
            // Next page
            open(player, page + 1);
            return;
        }
        if (slot == 49) {
            // Close
            player.closeInventory();
            return;
        }
        if (slot == 47) {
            // Deselect
            playerData.setActiveTrail(player.getUniqueId(), null);
            player.sendMessage(ChatColor.YELLOW + "Trail deselected.");
            open(player, page);
            return;
        }

        // Trail slot click
        if (slot < 0 || slot >= TRAIL_SLOTS) return;

        List<Trail> trails = new ArrayList<>(trailManager.getTrails());
        int index = page * TRAIL_SLOTS + slot;
        if (index >= trails.size()) return;

        Trail trail = trails.get(index);
        UUID uuid = player.getUniqueId();

        // Check if already active → deselect
        if (trail.getId().equals(playerData.getActiveTrail(uuid))) {
            playerData.setActiveTrail(uuid, null);
            player.sendMessage(ChatColor.YELLOW + "Trail " + ChatColor.translateAlternateColorCodes('&', trail.getDisplayName())
                    + ChatColor.YELLOW + " deselected.");
            open(player, page);
            return;
        }

        boolean hasPermission = player.hasPermission("elytratrails.trail.*")
                || player.hasPermission(trail.getPermission());
        boolean isUnlocked = playerData.hasUnlocked(uuid, trail.getId());

        if (hasPermission || isUnlocked) {
            // Select the trail
            playerData.setActiveTrail(uuid, trail.getId());
            player.sendMessage(ChatColor.GREEN + "Trail " + ChatColor.translateAlternateColorCodes('&', trail.getDisplayName())
                    + ChatColor.GREEN + " selected!");
            open(player, page);
        } else if (trail.getPrice() > 0 && player.hasPermission("elytratrails.buy")) {
            // Attempt purchase
            attemptPurchase(player, trail, page);
        } else {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this trail.");
        }
    }

    // ── Private helpers ─────────────────────────────────────

    private void attemptPurchase(Player player, Trail trail, int page) {
        if (!vaultHook.isEnabled()) {
            player.sendMessage(ChatColor.RED + "Economy is not available. Cannot purchase trails.");
            return;
        }

        if (!vaultHook.has(player, trail.getPrice())) {
            player.sendMessage(ChatColor.RED + "You need " + vaultHook.format(trail.getPrice()) + " to purchase this trail.");
            return;
        }

        if (vaultHook.withdraw(player, trail.getPrice())) {
            playerData.unlockTrail(player.getUniqueId(), trail.getId());
            playerData.setActiveTrail(player.getUniqueId(), trail.getId());
            player.sendMessage(ChatColor.GREEN + "Purchased and equipped "
                    + ChatColor.translateAlternateColorCodes('&', trail.getDisplayName())
                    + ChatColor.GREEN + " for " + vaultHook.format(trail.getPrice()) + "!");
            open(player, page);
        } else {
            player.sendMessage(ChatColor.RED + "Transaction failed. Please try again.");
        }
    }

    private ItemStack createTrailItem(Trail trail, UUID uuid, String activeTrailId,
                                      Set<String> unlocked, Player player) {
        boolean isActive = trail.getId().equals(activeTrailId);
        boolean hasPermission = player.hasPermission("elytratrails.trail.*")
                || player.hasPermission(trail.getPermission());
        boolean isUnlocked = unlocked.contains(trail.getId()) || hasPermission;
        boolean isFree = trail.getPrice() <= 0;

        ItemStack item = new ItemStack(trail.getIcon());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // Display name
        String name = ChatColor.translateAlternateColorCodes('&', trail.getDisplayName());
        if (isActive) {
            meta.setDisplayName(ChatColor.GREEN + "★ " + name + ChatColor.GREEN + " ★");
        } else if (isUnlocked) {
            meta.setDisplayName(ChatColor.WHITE + name);
        } else {
            meta.setDisplayName(ChatColor.RED + "✖ " + name);
        }

        // Lore
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Type: " + ChatColor.WHITE + trail.getTrailType().name());
        lore.add("");

        if (isActive) {
            lore.add(ChatColor.GREEN + "▶ Currently Active");
            lore.add(ChatColor.YELLOW + "Click to deselect.");
        } else if (isUnlocked) {
            lore.add(ChatColor.GREEN + "✔ Unlocked");
            lore.add(ChatColor.YELLOW + "Click to select.");
        } else {
            lore.add(ChatColor.RED + "✖ Locked");
            if (!isFree) {
                lore.add(ChatColor.GOLD + "Price: " + ChatColor.WHITE + vaultHook.format(trail.getPrice()));
                if (player.hasPermission("elytratrails.buy")) {
                    lore.add(ChatColor.YELLOW + "Click to purchase.");
                }
            }
            if (!hasPermission) {
                lore.add(ChatColor.DARK_GRAY + "Permission: " + trail.getPermission());
            }
        }

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);

        // Enchant glow for active trail
        if (isActive) {
            meta.setEnchantmentGlintOverride(true);
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }
}

