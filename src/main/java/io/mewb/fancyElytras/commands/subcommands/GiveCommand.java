
package io.mewb.fancyElytras.commands.subcommands;

import io.mewb.fancyElytras.FancyElytras;
import io.mewb.fancyElytras.commands.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class GiveCommand implements SubCommand {

    private final FancyElytras plugin;

    public GiveCommand(FancyElytras plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            String usage = plugin.getMessageUtil().getMessage("general.invalid-arguments")
                    .replace("{usage}", getUsage());
            plugin.getMessageUtil().sendMessage(sender, usage);
            return;
        }

        String playerName = args[0];
        Player target = Bukkit.getPlayer(playerName);

        if (target == null) {
            String message = plugin.getMessageUtil().getMessage("general.player-not-found")
                    .replace("{player}", playerName);
            plugin.getMessageUtil().sendMessage(sender, message);
            return;
        }

        // Check if target's inventory has space
        if (target.getInventory().firstEmpty() == -1) {
            String message = plugin.getMessageUtil().getMessage("commands.give.inventory-full")
                    .replace("{player}", target.getName());
            plugin.getMessageUtil().sendMessage(sender, message);
            return;
        }

        // Create fancy elytra
        String defaultParticle = plugin.getParticleManager().getDefaultParticle();
        int startingFuel = plugin.getFuelManager().getStartingFuel();

        ItemStack fancyElytra = plugin.getElytraManager().createFancyElytra(defaultParticle, startingFuel);

        // Give to player
        target.getInventory().addItem(fancyElytra);

        // Send messages
        String senderMessage = plugin.getMessageUtil().getMessage("commands.give.success")
                .replace("{player}", target.getName());
        plugin.getMessageUtil().sendMessage(sender, senderMessage);

        String receiverMessage = plugin.getMessageUtil().getMessage("commands.give.received");
        plugin.getMessageUtil().sendMessage(target, receiverMessage);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partial)) {
                    completions.add(player.getName());
                }
            }
        }

        return completions;
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("fancyelytra.give");
    }

    @Override
    public String getName() {
        return "give";
    }

    @Override
    public String getDescription() {
        return "Give a fancy elytra to a player";
    }

    @Override
    public String getUsage() {
        return "/fancyelytra give <player>";
    }
}