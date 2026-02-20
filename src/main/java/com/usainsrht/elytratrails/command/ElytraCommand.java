package com.usainsrht.elytratrails.command;

import com.usainsrht.elytratrails.ElytraTrails;
import com.usainsrht.elytratrails.config.PlayerDataManager;
import com.usainsrht.elytratrails.config.TrailManager;
import com.usainsrht.elytratrails.gui.TrailGUI;
import com.usainsrht.elytratrails.model.Trail;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles /elytra commands:
 *   /elytra          – opens the GUI
 *   /elytra gui      – opens the GUI
 *   /elytra reload   – reloads trails.yml
 *   /elytra give <player> <trail-id> – unlocks a trail for a player
 */
public class ElytraCommand implements TabExecutor {

    private final ElytraTrails plugin;
    private final TrailManager trailManager;
    private final PlayerDataManager playerData;
    private final TrailGUI trailGUI;

    public ElytraCommand(ElytraTrails plugin, TrailManager trailManager,
                         PlayerDataManager playerData, TrailGUI trailGUI) {
        this.plugin = plugin;
        this.trailManager = trailManager;
        this.playerData = playerData;
        this.trailGUI = trailGUI;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (args.length == 0 || args[0].equalsIgnoreCase("gui")) {
            return handleGUI(sender);
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                return handleReload(sender);
            }
            case "give" -> {
                return handleGive(sender, args);
            }
            default -> {
                sender.sendMessage(ChatColor.RED + "Unknown sub-command. Use: gui, reload, give");
                return true;
            }
        }
    }

    // ── Sub-commands ────────────────────────────────────────

    private boolean handleGUI(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can open the trail GUI.");
            return true;
        }
        if (!player.hasPermission("elytratrails.gui")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to open the trail menu.");
            return true;
        }
        trailGUI.open(player, 0);
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("elytratrails.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to reload.");
            return true;
        }
        trailManager.loadTrails();
        sender.sendMessage(ChatColor.GREEN + "ElytraTrails configuration reloaded!");
        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("elytratrails.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /elytra give <player> <trail-id>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[1] + "' is not online.");
            return true;
        }

        String trailId = args[2].toLowerCase();
        Trail trail = trailManager.getTrail(trailId);
        if (trail == null) {
            sender.sendMessage(ChatColor.RED + "Trail '" + trailId + "' not found.");
            return true;
        }

        playerData.unlockTrail(target.getUniqueId(), trailId);
        sender.sendMessage(ChatColor.GREEN + "Unlocked trail '"
                + ChatColor.translateAlternateColorCodes('&', trail.getDisplayName())
                + ChatColor.GREEN + "' for " + target.getName() + ".");
        target.sendMessage(ChatColor.GREEN + "You have been given the trail: "
                + ChatColor.translateAlternateColorCodes('&', trail.getDisplayName()) + ChatColor.GREEN + "!");
        return true;
    }

    // ── Tab completion ──────────────────────────────────────

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            subs.add("gui");
            if (sender.hasPermission("elytratrails.admin")) {
                subs.add("reload");
                subs.add("give");
            }
            return filterCompletions(subs, args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give") && sender.hasPermission("elytratrails.admin")) {
            return filterCompletions(
                    Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()),
                    args[1]
            );
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give") && sender.hasPermission("elytratrails.admin")) {
            return filterCompletions(new ArrayList<>(trailManager.getTrailIds()), args[2]);
        }

        return Collections.emptyList();
    }

    private List<String> filterCompletions(List<String> options, String input) {
        String lower = input.toLowerCase();
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(lower))
                .collect(Collectors.toList());
    }
}

