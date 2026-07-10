package com.usainsrht.elytratrails.gui;

import com.usainsrht.elytratrails.ElytraTrails;
import com.usainsrht.elytratrails.config.ConfigManager;
import com.usainsrht.elytratrails.model.TrailCategory;
import com.usainsrht.elytratrails.skin.SkinChangePricing;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The main Cosmetics hub GUI — a 3-row (27-slot) inventory that gives players
 * buttons to navigate to the Elytra, Player, and Arrow trail sub-GUIs.
 */
public class CosmeticsGUI {

    private final ElytraTrails plugin;
    private final TrailGUI trailGUI;

    public CosmeticsGUI(ElytraTrails plugin, TrailGUI trailGUI) {
        this.plugin = plugin;
        this.trailGUI = trailGUI;
    }

    // ── Open ────────────────────────────────────────────────

    public void open(Player player) {
        ConfigManager cm = plugin.getConfigManager();
        int size = cm.getGuiConfig().getInt("cosmetics-gui.size", 27);
        Component title = cm.getGuiComponent("cosmetics-gui.title", "<purple><bold>✦ </bold><light_purple>Cosmetics</light_purple><bold> ✦</bold>");

        CosmeticsGUIHolder holder = new CosmeticsGUIHolder();
        Inventory inv = Bukkit.createInventory(holder, size, title);

        // Fill all slots with filler
        Material fillerMat = getMaterial(cm.getGuiConfig().getString("cosmetics-gui.items.filler.material"), Material.BLACK_STAINED_GLASS_PANE);
        String fillerName = cm.getGuiConfig().getString("cosmetics-gui.items.filler.display-name", " ");
        boolean fillerHideTooltip = cm.getGuiConfig().getBoolean("cosmetics-gui.items.filler.hide-tooltip", true);
        ItemStack filler = makeFiller(fillerMat, fillerName, fillerHideTooltip);
        for (int i = 0; i < size; i++) {
            inv.setItem(i, filler);
        }

        // ── Info item ─────────────────────────────────────
        int infoSlot = cm.getGuiConfig().getInt("cosmetics-gui.items.info.slot", 10);
        if (infoSlot >= 0 && infoSlot < size) {
            inv.setItem(infoSlot, makeItemFromConfig(cm, "cosmetics-gui.items.info", Material.BOOK,
                    "<light_purple><bold>ℹ How Cosmetics Work</bold>", Arrays.asList(
                        "<gray>Cosmetics are purely visual effects",
                        "<gray>and do not affect gameplay.",
                        "",
                        "<white><bold>Elytra Trails</bold>",
                        "<gray>  Play while you glide with an elytra.",
                        "",
                        "<white><bold>Player Trails</bold>",
                        "<gray>  Ambient effects around your body",
                        "<gray>  while on foot. Three modes:",
                        "<yellow>  Standby <dark_gray>— only when still",
                        "<yellow>  Moving  <dark_gray>— only when walking/running",
                        "<yellow>  Normal  <dark_gray>— always active",
                        "",
                        "<white><bold>Swimming Trails</bold>",
                        "<gray>  Play while you swim in water.",
                        "",
                        "<white><bold>Arrow Trails</bold>",
                        "<gray>  Follow any projectile you throw.",
                        "",
                        "<dark_gray>Trails can be locked to regions",
                        "<dark_gray>by the server administrator."
                    ), null, null));
        }

        // ── Player Trails ─────────────────────────────────
        int playerSlot = cm.getGuiConfig().getInt("cosmetics-gui.items.player-trails.slot", 12);
        if (playerSlot >= 0 && playerSlot < size) {
            inv.setItem(playerSlot, makeItemFromConfig(cm, "cosmetics-gui.items.player-trails", Material.FEATHER,
                    "<green><bold>✦ Player Trails</bold>", Arrays.asList(
                        "<gray>Ambient effects that follow",
                        "<gray>you while on foot.",
                        "",
                        "<yellow>Modes: <white>Standby, Moving, Normal",
                        "",
                        "<yellow>» <green>Click to open!"
                    ), null, null));
        }

        // ── Swimming Trails ───────────────────────────────
        int swimSlot = cm.getGuiConfig().getInt("cosmetics-gui.items.swim-trails.slot", 13);
        if (swimSlot >= 0 && swimSlot < size) {
            inv.setItem(swimSlot, makeItemFromConfig(cm, "cosmetics-gui.items.swim-trails", Material.PRISMARINE_SHARD,
                    "<blue><bold>✦ Swimming Trails</bold>", Arrays.asList(
                        "<gray>Spectacular effects while",
                        "<gray>swimming through water.",
                        "",
                        "<gray>Wake trails, wing streams,",
                        "<gray>spirals and more!",
                        "",
                        "<yellow>» <blue>Click to open!"
                    ), null, null));
        }

        // ── Arrow Trails ──────────────────────────────────
        int arrowSlot = cm.getGuiConfig().getInt("cosmetics-gui.items.arrow-trails.slot", 14);
        if (arrowSlot >= 0 && arrowSlot < size) {
            inv.setItem(arrowSlot, makeItemFromConfig(cm, "cosmetics-gui.items.arrow-trails", Material.ARROW,
                    "<gold><bold>✦ Arrow Trails</bold>", Arrays.asList(
                        "<gray>Particle effects that trail",
                        "<gray>behind any thrown projectile.",
                        "",
                        "<gray>Works with: <white>arrows, tridents,",
                        "<white>snowballs, eggs, potions & more.",
                        "",
                        "<yellow>» <gold>Click to open!"
                    ), null, null));
        }

        // ── Elytra Trails ─────────────────────────────────
        int elytraSlot = cm.getGuiConfig().getInt("cosmetics-gui.items.elytra-trails.slot", 16);
        if (elytraSlot >= 0 && elytraSlot < size) {
            inv.setItem(elytraSlot, makeItemFromConfig(cm, "cosmetics-gui.items.elytra-trails", Material.ELYTRA,
                    "<aqua><bold>✦ Elytra Trails</bold>", Arrays.asList(
                        "<gray>Spectacular effects while",
                        "<gray>gliding with an elytra.",
                        "",
                        "<gray>Wing tips, body spirals,",
                        "<gray>butterflies and more!",
                        "",
                        "<yellow>» <aqua>Click to open!"
                    ), null, null));
        }

        // ── Skin Change ───────────────────────────────────
        int skinSlot = cm.getGuiConfig().getInt("cosmetics-gui.items.skin-change.slot", 22);
        if (skinSlot >= 0 && skinSlot < size) {
            inv.setItem(skinSlot, makeSkinChangeItem(cm, player));
        }

        player.openInventory(inv);
    }

    // ── Click handling ──────────────────────────────────────

    public void handleClick(Player player, int slot) {
        ConfigManager cm = plugin.getConfigManager();
        int elytraSlot = cm.getGuiConfig().getInt("cosmetics-gui.items.elytra-trails.slot", 16);
        int playerSlot = cm.getGuiConfig().getInt("cosmetics-gui.items.player-trails.slot", 12);
        int swimSlot = cm.getGuiConfig().getInt("cosmetics-gui.items.swim-trails.slot", 13);
        int arrowSlot = cm.getGuiConfig().getInt("cosmetics-gui.items.arrow-trails.slot", 14);
        int skinSlot = cm.getGuiConfig().getInt("cosmetics-gui.items.skin-change.slot", 22);

        if (slot == elytraSlot) {
            trailGUI.open(player, 0, TrailCategory.ELYTRA);
        } else if (slot == playerSlot) {
            trailGUI.open(player, 0, TrailCategory.PLAYER);
        } else if (slot == swimSlot) {
            trailGUI.open(player, 0, TrailCategory.SWIM);
        } else if (slot == arrowSlot) {
            trailGUI.open(player, 0, TrailCategory.ARROW);
        } else if (slot == skinSlot) {
            handleSkinChangeClick(player);
        }
    }

    private void handleSkinChangeClick(Player player) {
        ConfigManager cm = plugin.getConfigManager();
        if (!player.hasPermission("elytratrails.use.skin")) {
            player.sendMessage(cm.getMessage("no-permission"));
            return;
        }
        if (!plugin.getSkinsRestorerHook().isEnabled()) {
            player.sendMessage(cm.getMessage("skins-not-enabled"));
            return;
        }

        double cost = plugin.getSkinChangePricing().getEffectiveCost(player);
        if (cost > 0) {
            if (!plugin.getVaultHook().isEnabled()) {
                player.sendMessage(cm.getMessage("economy-not-available-skin"));
                return;
            }
            if (!plugin.getVaultHook().has(player, cost)) {
                player.sendMessage(cm.getMessage("skin-change-cost", "%price%", plugin.getVaultHook().format(cost)));
                return;
            }
        }

        // Close the inventory first so the player can interact with the dialog
        player.closeInventory();

        Component dialogTitle = cm.getGuiComponent("cosmetics-gui.skin-change-dialog.title", "Change Skin");
        Component dialogBody = cm.getGuiComponent("cosmetics-gui.skin-change-dialog.body", "Enter a Minecraft username to copy their skin:");
        Component inputLabel = cm.getGuiComponent("cosmetics-gui.skin-change-dialog.input-label", "Minecraft Username");
        Component confirmText = cm.getGuiComponent("cosmetics-gui.skin-change-dialog.confirm-button", "Confirm");
        Component cancelText = cm.getGuiComponent("cosmetics-gui.skin-change-dialog.cancel-button", "Cancel");

        // Create Dialog input
        DialogInput usernameInput = DialogInput.text("skin_username", inputLabel)
                .maxLength(16)
                .build();

        // Action for Confirm button
        ActionButton confirmButton = ActionButton.builder(confirmText)
                .action(DialogAction.customClick((response, audience) -> {
                    if (!(audience instanceof Player audiencePlayer)) return;

                    String username = response.getText("skin_username");
                    if (username == null || username.trim().isEmpty()) {
                        audiencePlayer.sendMessage(cm.getMessage("skin-username-empty"));
                        return;
                    }

                    // Double check cost before setting
                    if (cost > 0) {
                        if (!plugin.getVaultHook().has(audiencePlayer, cost)) {
                            audiencePlayer.sendMessage(cm.getMessage("skin-not-enough-money"));
                            return;
                        }
                    }

                    audiencePlayer.sendMessage(cm.getMessage("skin-fetching", "%username%", username));

                    plugin.getSkinsRestorerHook().changeSkin(
                            audiencePlayer,
                            username.trim(),
                            () -> {
                                // On Success
                                if (cost > 0) {
                                    if (plugin.getVaultHook().withdraw(audiencePlayer, cost)) {
                                        audiencePlayer.sendMessage(cm.getMessage("skin-changed-charged", "%price%", plugin.getVaultHook().format(cost)));
                                    } else {
                                        audiencePlayer.sendMessage(cm.getMessage("skin-changed-free"));
                                    }
                                } else {
                                    audiencePlayer.sendMessage(cm.getMessage("skin-changed-free"));
                                }
                            },
                            (errorMsg) -> {
                                // On Failure
                                audiencePlayer.sendMessage(cm.getMessage("skin-change-failed", "%error%", errorMsg));
                            }
                    );
                }, ClickCallback.Options.builder()
                        .lifetime(Duration.ofMinutes(5))
                        .uses(1)
                        .build()))
                .build();

        // Action for Cancel button
        ActionButton cancelButton = ActionButton.builder(cancelText)
                .action(DialogAction.customClick((response, audience) -> {
                    if (audience instanceof Player audiencePlayer) {
                        audiencePlayer.sendMessage(cm.getMessage("skin-change-cancelled"));
                    }
                }, ClickCallback.Options.builder()
                        .lifetime(Duration.ofMinutes(5))
                        .uses(1)
                        .build()))
                .build();

        // Build the dialog
        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(dialogTitle)
                        .body(List.of(DialogBody.plainMessage(dialogBody)))
                        .inputs(List.of(usernameInput))
                        .build())
                .type(DialogType.confirmation(confirmButton, cancelButton))
        );

        player.showDialog(dialog);
    }

    // ── Item builders ───────────────────────────────────────

    private ItemStack makeSkinChangeItem(ConfigManager cm, Player player) {
        String path = "cosmetics-gui.items.skin-change";
        Material mat = getMaterial(cm.getGuiConfig().getString(path + ".material"), Material.PLAYER_HEAD);
        String nameRaw = cm.getGuiConfig().getString(path + ".display-name", "<light_purple><bold>✦ Change Skin</bold>");
        List<String> rawLore = cm.getGuiConfig().contains(path + ".lore")
                ? cm.getGuiConfig().getStringList(path + ".lore")
                : Arrays.asList(
                        "<gray>Change your skin to match any",
                        "<gray>other Minecraft player's name.",
                        "",
                        "%price_line%",
                        "%discount_line%",
                        "",
                        "<yellow>» <light_purple>Click to change!");

        SkinChangePricing pricing = plugin.getSkinChangePricing();
        SkinChangePricing.PriceDisplay prices = pricing.formatPrices(player, plugin.getVaultHook());

        String priceLine;
        String discountLine = "";
        if (prices.hasDiscount()) {
            String priceTemplate = cm.getGuiConfig().getString(
                    path + ".discounted-price-lore",
                    "<strikethrough><gray>%old_price%</strikethrough> <green>%new_price%");
            String discountTemplate = cm.getGuiConfig().getString(
                    path + ".discount-info-lore",
                    "<gray>%percent%% discount from %source%");
            priceLine = priceTemplate
                    .replace("%old_price%", prices.formattedBase())
                    .replace("%new_price%", prices.formattedEffective());
            discountLine = discountTemplate
                    .replace("%percent%", String.valueOf(prices.discountPercent()))
                    .replace("%source%", pricing.getDiscountSource());
        } else {
            String priceTemplate = cm.getGuiConfig().getString(path + ".price-lore", "<yellow>Cost: <white>%price%");
            String formattedPrice = prices.formattedEffective();
            priceLine = priceTemplate.replace("%price%", formattedPrice);
        }

        Component displayName = MiniMessage.miniMessage().deserialize(nameRaw);
        List<Component> lore = new ArrayList<>();
        for (String line : rawLore) {
            if ("%discount_line%".equals(line.trim())) {
                if (discountLine.isEmpty()) {
                    continue;
                }
                line = discountLine;
            } else {
                if (line.contains("%price_line%")) {
                    line = line.replace("%price_line%", priceLine);
                }
                if (line.contains("%discount_line%")) {
                    if (discountLine.isEmpty()) {
                        continue;
                    }
                    line = line.replace("%discount_line%", discountLine);
                }
            }
            lore.add(MiniMessage.miniMessage().deserialize(line));
        }

        return makeItem(mat, displayName, lore);
    }

    private ItemStack makeItemFromConfig(ConfigManager cm, String path, Material defaultMaterial, String defaultName, List<String> defaultLore, String placeholderTarget, String placeholderReplacement) {
        Material mat = getMaterial(cm.getGuiConfig().getString(path + ".material"), defaultMaterial);
        String nameRaw = cm.getGuiConfig().getString(path + ".display-name", defaultName);
        List<String> rawLore = cm.getGuiConfig().contains(path + ".lore") ? cm.getGuiConfig().getStringList(path + ".lore") : defaultLore;

        Component displayName = MiniMessage.miniMessage().deserialize(nameRaw);
        List<Component> lore = new ArrayList<>();
        for (String line : rawLore) {
            if (placeholderTarget != null && placeholderReplacement != null) {
                line = line.replace(placeholderTarget, placeholderReplacement);
            }
            lore.add(MiniMessage.miniMessage().deserialize(line));
        }

        return makeItem(mat, displayName, lore);
    }

    private ItemStack makeFiller(Material mat, String name, boolean hideTooltip) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize(name).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            meta.setHideTooltip(hideTooltip);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack makeItem(Material mat, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
            if (lore != null && !lore.isEmpty()) {
                List<Component> noItalicLore = new ArrayList<>();
                for (Component comp : lore) {
                    noItalicLore.add(comp.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
                }
                meta.lore(noItalicLore);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(meta);
        }
        return item;
    }

    private Material getMaterial(String name, Material def) {
        if (name == null) return def;
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return def;
        }
    }
}

