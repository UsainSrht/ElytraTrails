package com.usainsrht.elytratrails.command;

import com.usainsrht.elytratrails.ElytraTrails;
import com.usainsrht.elytratrails.config.PlayerDataManager;
import com.usainsrht.elytratrails.config.TrailManager;
import com.usainsrht.elytratrails.gui.CosmeticsGUI;
import com.usainsrht.elytratrails.gui.TrailGUI;
import com.usainsrht.elytratrails.model.Trail;
import com.usainsrht.elytratrails.model.TrailCategory;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.bukkit.configuration.file.FileConfiguration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles /elytra commands:
 *   /elytra               – opens the Cosmetics hub GUI
 *   /elytra gui           – opens the Cosmetics hub GUI
 *   /elytra elytra        – opens the Elytra Trails sub-GUI directly
 *   /elytra player        – opens the Player Trails sub-GUI directly
 *   /elytra arrow         – opens the Arrow Trails sub-GUI directly
 *   /elytra reload        – reloads trails.yml and config.yml
 *   /elytra give <player> <trail-id> – unlocks a trail for a player
 */
public class ElytraCommand implements TabExecutor {

    private final ElytraTrails plugin;
    private final TrailManager trailManager;
    private final PlayerDataManager playerData;
    private final CosmeticsGUI cosmeticsGUI;
    private final TrailGUI trailGUI;

    private final Map<String, String> subcommandMapping = new HashMap<>();
    private final Map<String, String> subcommandNames = new HashMap<>();
    private final Map<String, List<String>> subcommandAliases = new HashMap<>();

    public ElytraCommand(ElytraTrails plugin, TrailManager trailManager,
                         PlayerDataManager playerData,
                         CosmeticsGUI cosmeticsGUI, TrailGUI trailGUI) {
        this.plugin = plugin;
        this.trailManager = trailManager;
        this.playerData = playerData;
        this.cosmeticsGUI = cosmeticsGUI;
        this.trailGUI = trailGUI;
        loadSubcommands();
    }

    public void loadSubcommands() {
        subcommandMapping.clear();
        subcommandNames.clear();
        subcommandAliases.clear();

        FileConfiguration config = plugin.getConfig();
        String[] defaultSubcommands = {"gui", "elytra", "player", "arrow", "reload", "give"};

        for (String sub : defaultSubcommands) {
            String path = "command.subcommands." + sub;
            String name = config.getString(path + ".name", sub).toLowerCase();
            List<String> aliases = config.getStringList(path + ".aliases");
            if (aliases == null) {
                aliases = new ArrayList<>();
            }

            subcommandNames.put(sub, name);
            subcommandAliases.put(sub, aliases);

            subcommandMapping.put(name, sub);
            for (String alias : aliases) {
                subcommandMapping.put(alias.toLowerCase(), sub);
            }
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            return handleHub(sender);
        }

        String inputSub = args[0].toLowerCase();
        String standardSub = subcommandMapping.get(inputSub);

        if (standardSub == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("unknown-command"));
            return true;
        }

        switch (standardSub) {
            case "gui"    -> { return handleHub(sender); }
            case "elytra" -> { return handleSubGUI(sender, TrailCategory.ELYTRA); }
            case "player" -> { return handleSubGUI(sender, TrailCategory.PLAYER); }
            case "arrow"  -> { return handleSubGUI(sender, TrailCategory.ARROW);  }
            case "reload" -> { return handleReload(sender); }
            case "give"   -> { return handleGive(sender, args); }
            default -> {
                sender.sendMessage(plugin.getConfigManager().getMessage("unknown-command"));
                return true;
            }
        }
    }

    // ── Sub-commands ────────────────────────────────────────

    private boolean handleHub(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("only-players"));
            return true;
        }
        if (!player.hasPermission("elytratrails.gui")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission-cosmetics"));
            return true;
        }
        cosmeticsGUI.open(player);
        return true;
    }

    private boolean handleSubGUI(CommandSender sender, TrailCategory category) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("only-players-gui"));
            return true;
        }
        if (!player.hasPermission("elytratrails.gui")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission-trail"));
            return true;
        }
        trailGUI.open(player, 0, category);
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("elytratrails.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission-reload"));
            return true;
        }
        plugin.getConfigManager().reload();
        trailManager.loadConfig();
        plugin.registerCommands();
        trailManager.loadTrails();
        sender.sendMessage(plugin.getConfigManager().getMessage("config-reloaded"));
        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("elytratrails.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission-give"));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(plugin.getConfigManager().getMessage("give-usage"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-not-online", "%player%", args[1]));
            return true;
        }

        String trailId = args[2].toLowerCase();
        Trail trail = trailManager.getTrail(trailId);
        if (trail == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("trail-not-found", "%trail%", trailId));
            return true;
        }

        playerData.unlockTrail(target.getUniqueId(), trailId);
        sender.sendMessage(plugin.getConfigManager().getMessage("trail-given-sender",
                "%trail%", trail.getDisplayName(),
                "%player%", target.getName()));
        target.sendMessage(plugin.getConfigManager().getMessage("trail-given-receiver",
                "%trail%", trail.getDisplayName()));
        return true;
    }

    // ── Tab completion ──────────────────────────────────────

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            subs.add(subcommandNames.get("gui"));
            subs.add(subcommandNames.get("elytra"));
            subs.add(subcommandNames.get("player"));
            subs.add(subcommandNames.get("arrow"));
            if (sender.hasPermission("elytratrails.admin")) {
                subs.add(subcommandNames.get("reload"));
                subs.add(subcommandNames.get("give"));
            }
            return filterCompletions(subs, args[0]);
        }

        if (args.length == 2 && sender.hasPermission("elytratrails.admin")) {
            String standardSub = subcommandMapping.get(args[0].toLowerCase());
            if ("give".equals(standardSub)) {
                return filterCompletions(
                        Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()),
                        args[1]
                );
            }
        }

        if (args.length == 3 && sender.hasPermission("elytratrails.admin")) {
            String standardSub = subcommandMapping.get(args[0].toLowerCase());
            if ("give".equals(standardSub)) {
                return filterCompletions(new ArrayList<>(trailManager.getTrailIds()), args[2]);
            }
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
