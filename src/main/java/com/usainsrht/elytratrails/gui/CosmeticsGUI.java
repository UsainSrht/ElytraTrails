package com.usainsrht.elytratrails.gui;

import com.usainsrht.elytratrails.model.TrailCategory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

/**
 * The main Cosmetics hub GUI — a 3-row (27-slot) inventory that gives players
 * buttons to navigate to the Elytra, Player, and Arrow trail sub-GUIs.
 *
 * <p>Layout (row/column notation, 0-indexed):
 * <pre>
 *  Row 0: [F][F][F][F][F][F][F][F][F]
 *  Row 1: [F][I][F][P][F][A][F][E][F]
 *  Row 2: [F][F][F][F][F][F][F][F][F]
 *
 *  I = Info item  (slot 10)
 *  P = Player Trails button (slot 12)
 *  A = Arrow Trails button  (slot 14)
 *  E = Elytra Trails button (slot 16)
 *  F = Black glass pane filler
 * </pre>
 */
public class CosmeticsGUI {

    private static final int SIZE = 27;
    private static final String TITLE =
            ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "✦ " +
            ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Cosmetics" +
            ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + " ✦";

    // Category button slots
    private static final int SLOT_INFO   = 10;
    private static final int SLOT_PLAYER = 12;
    private static final int SLOT_ARROW  = 14;
    private static final int SLOT_ELYTRA = 16;

    private final TrailGUI trailGUI;

    public CosmeticsGUI(TrailGUI trailGUI) {
        this.trailGUI = trailGUI;
    }

    // ── Open ────────────────────────────────────────────────

    public void open(Player player) {
        CosmeticsGUIHolder holder = new CosmeticsGUIHolder();
        Inventory inv = Bukkit.createInventory(holder, SIZE, TITLE);

        // Fill all slots with black glass pane filler
        ItemStack filler = makeFiller();
        for (int i = 0; i < SIZE; i++) {
            inv.setItem(i, filler);
        }

        // ── Info item ─────────────────────────────────────
        inv.setItem(SLOT_INFO, makeInfoItem());

        // ── Player Trails ─────────────────────────────────
        inv.setItem(SLOT_PLAYER, makeCategoryButton(
                Material.FEATHER,
                ChatColor.GREEN + "" + ChatColor.BOLD + "✦ Player Trails",
                List.of(
                    ChatColor.GRAY + "Ambient effects that follow",
                    ChatColor.GRAY + "you while on foot.",
                    "",
                    ChatColor.YELLOW + "Modes: " + ChatColor.WHITE + "Standby, Moving, Normal",
                    "",
                    ChatColor.YELLOW + "» " + ChatColor.GREEN + "Click to open!"
                )
        ));

        // ── Arrow Trails ──────────────────────────────────
        inv.setItem(SLOT_ARROW, makeCategoryButton(
                Material.ARROW,
                ChatColor.GOLD + "" + ChatColor.BOLD + "✦ Arrow Trails",
                List.of(
                    ChatColor.GRAY + "Particle effects that trail",
                    ChatColor.GRAY + "behind any thrown projectile.",
                    "",
                    ChatColor.GRAY + "Works with: " + ChatColor.WHITE + "arrows, tridents,",
                    ChatColor.WHITE + "snowballs, eggs, potions & more.",
                    "",
                    ChatColor.YELLOW + "» " + ChatColor.GOLD + "Click to open!"
                )
        ));

        // ── Elytra Trails ─────────────────────────────────
        inv.setItem(SLOT_ELYTRA, makeCategoryButton(
                Material.ELYTRA,
                ChatColor.AQUA + "" + ChatColor.BOLD + "✦ Elytra Trails",
                List.of(
                    ChatColor.GRAY + "Spectacular effects while",
                    ChatColor.GRAY + "gliding with an elytra.",
                    "",
                    ChatColor.GRAY + "Wing tips, body spirals,",
                    ChatColor.GRAY + "butterflies and more!",
                    "",
                    ChatColor.YELLOW + "» " + ChatColor.AQUA + "Click to open!"
                )
        ));

        player.openInventory(inv);
    }

    // ── Click handling ──────────────────────────────────────

    public void handleClick(Player player, int slot) {
        switch (slot) {
            case SLOT_ELYTRA -> trailGUI.open(player, 0, TrailCategory.ELYTRA);
            case SLOT_PLAYER -> trailGUI.open(player, 0, TrailCategory.PLAYER);
            case SLOT_ARROW  -> trailGUI.open(player, 0, TrailCategory.ARROW);
            // Clicks on filler or info item do nothing
        }
    }

    // ── Item builders ───────────────────────────────────────

    private ItemStack makeInfoItem() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "ℹ How Cosmetics Work");
            meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Cosmetics are purely visual effects",
                ChatColor.GRAY + "and do not affect gameplay.",
                "",
                ChatColor.WHITE + "" + ChatColor.BOLD + "Elytra Trails",
                ChatColor.GRAY + "  Play while you glide with an elytra.",
                "",
                ChatColor.WHITE + "" + ChatColor.BOLD + "Player Trails",
                ChatColor.GRAY + "  Ambient effects around your body",
                ChatColor.GRAY + "  while on foot. Three modes:",
                ChatColor.YELLOW + "  Standby " + ChatColor.DARK_GRAY + "— only when still",
                ChatColor.YELLOW + "  Moving  " + ChatColor.DARK_GRAY + "— only when walking/running",
                ChatColor.YELLOW + "  Normal  " + ChatColor.DARK_GRAY + "— always active",
                "",
                ChatColor.WHITE + "" + ChatColor.BOLD + "Arrow Trails",
                ChatColor.GRAY + "  Follow any projectile you throw.",
                "",
                ChatColor.DARK_GRAY + "Trails can be locked to regions",
                ChatColor.DARK_GRAY + "by the server administrator."
            ));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack makeCategoryButton(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack makeFiller() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(meta);
        }
        return item;
    }
}
