package io.mewb.fancyElytras.commands.subcommands;

import io.mewb.fancyElytras.FancyElytras;
import io.mewb.fancyElytras.commands.SubCommand;
import io.mewb.fancyElytras.data.ElytraData;
import io.mewb.fancyElytras.data.FuelItem;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class FuelCommand implements SubCommand {

    private final FancyElytras plugin;

    public FuelCommand(FancyElytras plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageUtil().sendMessage(sender, "general.player-only");
            return;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            // Show current fuel level
            showFuelLevel(player);
            return;
        }

        String subAction = args[0].toLowerCase();

        switch (subAction) {
            case "refill":
            case "refuel":
                handleRefuel(player, args);
                break;
            case "buy":
            case "purchase":
                handlePurchase(player, args);
                break;
            case "info":
                showFuelInfo(player);
                break;
            default:
                plugin.getMessageUtil().sendInvalidArguments(sender, getUsage());
        }
    }

    private void showFuelLevel(Player player) {
        ItemStack elytra = plugin.getElytraManager().getPlayerElytra(player);
        if (elytra == null) {
            plugin.getMessageUtil().sendMessage(player, "commands.fuel.no-elytra");
            return;
        }

        if (!plugin.getElytraManager().isFancyElytra(elytra)) {
            plugin.getMessageUtil().sendMessage(player, "commands.fuel.not-fancy");
            return;
        }

        ElytraData elytraData = plugin.getElytraManager().getElytraData(elytra);
        if (elytraData == null) {
            plugin.getMessageUtil().sendMessage(player, "errors.elytra-not-found");
            return;
        }

        String message = plugin.getMessageUtil().getMessage("commands.fuel.current-fuel")
                .replace("{fuel}", String.valueOf(elytraData.getFuel()))
                .replace("{max_fuel}", String.valueOf(elytraData.getMaxFuel()));

        plugin.getMessageUtil().sendRawMessage(player, message);
    }

    private void handleRefuel(Player player, String[] args) {
        if (!plugin.getFuelManager().isFuelSystemEnabled()) {
            plugin.getMessageUtil().sendMessage(player, "general.feature-disabled");
            return;
        }

        ItemStack elytra = plugin.getElytraManager().getPlayerElytra(player);
        if (elytra == null) {
            plugin.getMessageUtil().sendMessage(player, "commands.fuel.no-elytra");
            return;
        }

        ElytraData elytraData = plugin.getElytraManager().getElytraData(elytra);
        if (elytraData == null) {
            plugin.getMessageUtil().sendMessage(player, "errors.elytra-not-found");
            return;
        }

        // Find fuel items in inventory
        boolean refueled = false;
        for (FuelItem fuelItem : plugin.getFuelManager().getAllFuelItems()) {
            Material material = fuelItem.getMaterial();
            int available = countItems(player, material);

            if (available > 0) {
                // Calculate how much we can refuel
                int maxRefuel = (elytraData.getMaxFuel() - elytraData.getFuel()) / fuelItem.getFuelValue();
                int itemsToUse = Math.min(available, maxRefuel);

                if (itemsToUse > 0) {
                    if (plugin.getFuelManager().refuelElytra(player, elytraData, material, itemsToUse)) {
                        plugin.getElytraManager().setElytraData(elytra, elytraData);
                        refueled = true;
                        break;
                    }
                }
            }
        }

        if (!refueled) {
            plugin.getMessageUtil().sendMessage(player, "commands.fuel.no-fuel-items");
        }
    }

    private void handlePurchase(Player player, String[] args) {
        if (!plugin.getEconomyManager().isEconomyEnabled()) {
            plugin.getMessageUtil().sendMessage(player, "economy.not-available");
            return;
        }

        ItemStack elytra = plugin.getElytraManager().getPlayerElytra(player);
        if (elytra == null) {
            plugin.getMessageUtil().sendMessage(player, "commands.fuel.no-elytra");
            return;
        }

        ElytraData elytraData = plugin.getElytraManager().getElytraData(elytra);
        if (elytraData == null) {
            plugin.getMessageUtil().sendMessage(player, "errors.elytra-not-found");
            return;
        }

        int fuelAmount = 100; // Default amount

        if (args.length > 1) {
            try {
                fuelAmount = Integer.parseInt(args[1]);
                if (fuelAmount <= 0) {
                    plugin.getMessageUtil().sendMessage(player, "errors.invalid-fuel-amount");
                    return;
                }
            } catch (NumberFormatException e) {
                plugin.getMessageUtil().sendMessage(player, "errors.invalid-fuel-amount");
                return;
            }
        }

        // Calculate maximum fuel that can be added
        int maxFuelToAdd = elytraData.getMaxFuel() - elytraData.getFuel();
        fuelAmount = Math.min(fuelAmount, maxFuelToAdd);

        if (fuelAmount <= 0) {
            plugin.getMessageUtil().sendRawMessage(player, "&cYour elytra is already full!");
            return;
        }

        double cost = plugin.getEconomyManager().calculateFuelCost(fuelAmount);

        if (!plugin.getEconomyManager().hasEnoughMoney(player, cost)) {
            String message = plugin.getMessageUtil().getMessage("commands.fuel.insufficient-money")
                    .replace("{cost}", plugin.getEconomyManager().formatMoney(cost))
                    .replace("{amount}", String.valueOf(fuelAmount));
            plugin.getMessageUtil().sendRawMessage(player, message);
            return;
        }

        if (plugin.getEconomyManager().purchaseFuel(player, fuelAmount)) {
            elytraData.addFuel(fuelAmount);
            plugin.getElytraManager().setElytraData(elytra, elytraData);

            String message = plugin.getMessageUtil().getMessage("commands.fuel.purchased-fuel")
                    .replace("{amount}", String.valueOf(fuelAmount))
                    .replace("{cost}", plugin.getEconomyManager().formatMoney(cost));
            plugin.getMessageUtil().sendRawMessage(player, message);
        }
    }

    private void showFuelInfo(Player player) {
        player.sendMessage("§6=== Fuel System Information ===");
        player.sendMessage("§7Fuel items and their values:");

        for (FuelItem fuelItem : plugin.getFuelManager().getAllFuelItems()) {
            int available = countItems(player, fuelItem.getMaterial());
            player.sendMessage("§e" + fuelItem.getDisplayName() +
                    "§7: §a+" + fuelItem.getFuelValue() + " fuel §7(You have: §e" + available + "§7)");
        }

        if (plugin.getEconomyManager().isEconomyEnabled()) {
            double fuelPrice = plugin.getEconomyManager().getFuelPrice();
            player.sendMessage("§7Fuel price: §a" + plugin.getEconomyManager().formatMoney(fuelPrice) + "§7 per fuel unit");
            player.sendMessage("§7Your balance: §a" + plugin.getEconomyManager().formatMoney(plugin.getEconomyManager().getBalance(player)));
        }
    }

    private int countItems(Player player, Material material) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            List<String> subCommands = List.of("refill", "buy", "info");

            for (String subCommand : subCommands) {
                if (subCommand.startsWith(partial)) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("buy")) {
            // Suggest common fuel amounts
            List<String> amounts = List.of("50", "100", "250", "500", "1000");
            String partial = args[1];

            for (String amount : amounts) {
                if (amount.startsWith(partial)) {
                    completions.add(amount);
                }
            }
        }

        return completions;
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("fancyelytra.fuel");
    }

    @Override
    public String getName() {
        return "fuel";
    }

    @Override
    public String getDescription() {
        return "Manage elytra fuel";
    }

    @Override
    public String getUsage() {
        return "/fancyelytra fuel [refill|buy <amount>|info]";
    }
}