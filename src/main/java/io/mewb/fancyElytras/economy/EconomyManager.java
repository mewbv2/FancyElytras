
package io.mewb.fancyElytras.economy;

import io.mewb.fancyElytras.FancyElytras;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.logging.Level;

public class EconomyManager {

    private final FancyElytras plugin;
    private Economy economy = null;
    private boolean economyEnabled = false;

    public EconomyManager(FancyElytras plugin) {
        this.plugin = plugin;
    }

    public boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().info("Vault not found - economy features disabled");
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager()
                .getRegistration(Economy.class);

        if (rsp == null) {
            plugin.getLogger().info("No economy plugin found - economy features disabled");
            return false;
        }

        economy = rsp.getProvider();
        economyEnabled = economy != null &&
                plugin.getConfigManager().getEconomyConfig().getBoolean("economy.enabled", true);

        if (economyEnabled) {
            plugin.getLogger().info("Economy integration enabled with " + economy.getName());
        }

        return economyEnabled;
    }

    public boolean isEconomyEnabled() {
        return economyEnabled && economy != null;
    }

    public double getBalance(Player player) {
        if (!isEconomyEnabled()) {
            return 0.0;
        }

        try {
            return economy.getBalance(player);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error getting balance for " + player.getName(), e);
            return 0.0;
        }
    }

    public boolean hasEnoughMoney(Player player, double amount) {
        if (!isEconomyEnabled()) {
            return false;
        }

        return getBalance(player) >= amount;
    }

    public boolean withdrawMoney(Player player, double amount) {
        if (!isEconomyEnabled()) {
            return false;
        }

        try {
            return economy.withdrawPlayer(player, amount).transactionSuccess();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error withdrawing money from " + player.getName(), e);
            return false;
        }
    }

    public boolean depositMoney(Player player, double amount) {
        if (!isEconomyEnabled()) {
            return false;
        }

        try {
            return economy.depositPlayer(player, amount).transactionSuccess();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error depositing money to " + player.getName(), e);
            return false;
        }
    }

    public double getFuelPrice() {
        return plugin.getConfigManager().getEconomyConfig()
                .getDouble("fuel-shop.price-per-fuel", 0.10);
    }

    public double calculateFuelCost(int fuelAmount) {
        double basePrice = getFuelPrice();
        double totalCost = basePrice * fuelAmount;

        // Apply bulk discounts
        if (plugin.getConfigManager().getEconomyConfig()
                .getBoolean("fuel-shop.bulk-discounts.enabled", true)) {

            totalCost = applyBulkDiscount(fuelAmount, totalCost);
        }

        return totalCost;
    }

    private double applyBulkDiscount(int fuelAmount, double totalCost) {
        var discountSection = plugin.getConfigManager().getEconomyConfig()
                .getConfigurationSection("fuel-shop.bulk-discounts.tiers");

        if (discountSection == null) {
            return totalCost;
        }

        double bestDiscount = 0.0;

        // Find the best applicable discount
        for (String tierKey : discountSection.getKeys(false)) {
            try {
                int tierAmount = Integer.parseInt(tierKey);
                if (fuelAmount >= tierAmount) {
                    double discount = discountSection.getDouble(tierKey + ".discount-percent", 0.0);
                    bestDiscount = Math.max(bestDiscount, discount);
                }
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid bulk discount tier: " + tierKey);
            }
        }

        if (bestDiscount > 0) {
            return totalCost * (1.0 - (bestDiscount / 100.0));
        }

        return totalCost;
    }

    public boolean purchaseFuel(Player player, int fuelAmount) {
        if (!isEconomyEnabled()) {
            plugin.getMessageUtil().sendMessage(player, "economy.not-available");
            return false;
        }

        double cost = calculateFuelCost(fuelAmount);

        if (!hasEnoughMoney(player, cost)) {
            String message = plugin.getMessageUtil().getMessage("economy.insufficient-funds")
                    .replace("{required}", formatMoney(cost))
                    .replace("{current}", formatMoney(getBalance(player)));
            plugin.getMessageUtil().sendMessage(player, message);
            return false;
        }

        if (!withdrawMoney(player, cost)) {
            plugin.getMessageUtil().sendMessage(player, "economy.fuel-shop.purchase-failed");
            return false;
        }

        // Success message
        String message = plugin.getMessageUtil().getMessage("economy.fuel-shop.purchase-success")
                .replace("{amount}", String.valueOf(fuelAmount))
                .replace("{cost}", formatMoney(cost));
        plugin.getMessageUtil().sendMessage(player, message);

        return true;
    }

    public String formatMoney(double amount) {
        String symbol = plugin.getConfigManager().getEconomyConfig()
                .getString("economy.currency-symbol", "$");

        if (economy != null) {
            return economy.format(amount);
        }

        return symbol + String.format("%.2f", amount);
    }

    public String getCurrencyName() {
        if (economy != null) {
            return economy.currencyNamePlural();
        }

        return plugin.getConfigManager().getEconomyConfig()
                .getString("economy.currency-name", "dollars");
    }
}