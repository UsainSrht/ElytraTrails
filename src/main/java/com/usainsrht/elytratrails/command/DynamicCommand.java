package com.usainsrht.elytratrails.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DynamicCommand extends Command {
    private final ElytraCommand executor;

    public DynamicCommand(@NotNull String name, @NotNull String description, @NotNull String usageMessage, @NotNull List<String> aliases, @NotNull ElytraCommand executor) {
        super(name, description, usageMessage, aliases);
        this.executor = executor;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        return executor.onCommand(sender, this, commandLabel, args);
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        return executor.onTabComplete(sender, this, alias, args);
    }
}
