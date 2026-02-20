package com.usainsrht.elytratrails.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Optional wrapper around the Vault Economy API.
 * All methods are safe to call even when Vault is not installed.
 */
public class VaultHook {

    private Economy economy;

    public VaultHook() {
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> rsp =
                    Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                economy = rsp.getProvider();
            }
        }
    }

    /**
     * @return true if Vault economy is available.
     */
    public boolean isEnabled() {
        return economy != null;
    }

    /**
     * Check if a player has at least the given amount.
     */
    public boolean has(Player player, double amount) {
        return isEnabled() && economy.has(player, amount);
    }

    /**
     * Withdraw the given amount from the player.
     * @return true if the transaction succeeded.
     */
    public boolean withdraw(Player player, double amount) {
        if (!isEnabled()) return false;
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    /**
     * Format a monetary amount using Vault's formatter.
     */
    public String format(double amount) {
        if (!isEnabled()) return String.format("$%.2f", amount);
        return economy.format(amount);
    }
}

