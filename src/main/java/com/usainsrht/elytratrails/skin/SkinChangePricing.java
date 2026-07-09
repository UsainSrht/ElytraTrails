package com.usainsrht.elytratrails.skin;

import com.usainsrht.elytratrails.ElytraTrails;
import com.usainsrht.elytratrails.economy.VaultHook;
import me.usainsrht.limitapi.api.LimitAPI;
import me.usainsrht.limitapi.api.LimitCalculationStrategy;
import me.usainsrht.limitapi.api.LimitService;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.MetaNode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Resolves skin-change cost with optional VIP discounts from LimitAPI / LuckPerms meta.
 */
public class SkinChangePricing {

    private final ElytraTrails plugin;
    private final boolean luckPermsEnabled;

    public SkinChangePricing(ElytraTrails plugin) {
        this.plugin = plugin;
        this.luckPermsEnabled = Bukkit.getPluginManager().isPluginEnabled("LuckPerms");
    }

    public double getBaseCost() {
        return plugin.getConfig().getDouble("skin-change.cost", 100.0);
    }

    public boolean isDiscountEnabled() {
        return plugin.getConfig().getBoolean("skin-change.discount.enabled", true);
    }

    public int getDiscountPercent(Player player) {
        if (!isDiscountEnabled()) {
            return 0;
        }

        int defaultPercent = plugin.getConfig().getInt("skin-change.discount.default-percent", 0);

        if (Bukkit.getPluginManager().isPluginEnabled("LimitAPI")) {
            try {
                return getDiscountFromLimitApi(player, defaultPercent);
            } catch (NoClassDefFoundError | Exception ignored) {
                // Fall through to LuckPerms
            }
        }

        if (luckPermsEnabled) {
            try {
                return getDiscountFromLuckPerms(player, defaultPercent);
            } catch (Exception ignored) {
                // Fall through to default
            }
        }

        return defaultPercent;
    }

    public double getEffectiveCost(Player player) {
        double baseCost = getBaseCost();
        if (baseCost <= 0) {
            return 0;
        }

        int percent = clampPercent(getDiscountPercent(player));
        if (percent <= 0) {
            return baseCost;
        }

        return baseCost * (100 - percent) / 100.0;
    }

    public PriceDisplay formatPrices(Player player, VaultHook vaultHook) {
        double baseCost = getBaseCost();
        int percent = clampPercent(getDiscountPercent(player));
        double effectiveCost = percent > 0 ? baseCost * (100 - percent) / 100.0 : baseCost;

        String formattedBase = formatAmount(vaultHook, baseCost);
        String formattedEffective = formatAmount(vaultHook, effectiveCost);

        return new PriceDisplay(baseCost, effectiveCost, percent, formattedBase, formattedEffective);
    }

    public String getDiscountSource() {
        return plugin.getConfig().getString("skin-change.discount.source", "VIP");
    }

    private int getDiscountFromLimitApi(Player player, int defaultPercent) {
        LimitService limitService = LimitAPI.get();
        if (limitService == null) {
            return defaultPercent;
        }

        String key = plugin.getConfig().getString("skin-change.discount.key", "skin-discount");
        String strategyStr = plugin.getConfig().getString("skin-change.discount.strategy", "META");
        LimitCalculationStrategy strategy = LimitCalculationStrategy.fromString(strategyStr);
        if (strategy == null) {
            strategy = LimitCalculationStrategy.META;
        }

        return clampPercent(limitService.getLimit(player, key, strategy, defaultPercent));
    }

    private int getDiscountFromLuckPerms(Player player, int defaultPercent) {
        String key = plugin.getConfig().getString("skin-change.discount.key", "skin-discount");
        User user = LuckPermsProvider.get().getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            return defaultPercent;
        }

        int max = 0;
        for (MetaNode node : user.getNodes(NodeType.META)) {
            if (node.getMetaKey().equalsIgnoreCase(key)) {
                try {
                    max = Math.max(max, Integer.parseInt(node.getMetaValue()));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return clampPercent(max > 0 ? max : defaultPercent);
    }

    private static int clampPercent(int percent) {
        return Math.max(0, Math.min(100, percent));
    }

    private static String formatAmount(VaultHook vaultHook, double amount) {
        return vaultHook.isEnabled() ? vaultHook.format(amount) : String.format("$%.2f", amount);
    }

    public record PriceDisplay(
            double baseCost,
            double effectiveCost,
            int discountPercent,
            String formattedBase,
            String formattedEffective
    ) {
        public boolean hasDiscount() {
            return discountPercent > 0 && baseCost > 0;
        }
    }
}
