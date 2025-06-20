package io.mewb.fancyElytras.commands.subcommands;

import io.mewb.fancyElytras.FancyElytras;
import io.mewb.fancyElytras.commands.SubCommand;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class HelpCommand implements SubCommand {

    private final FancyElytras plugin;

    public HelpCommand(FancyElytras plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // Send header
        sender.sendMessage(plugin.getMessageUtil().getMessage("commands.help.header"));
        
        // Show available commands based on permissions
        if (sender.hasPermission("fancyelytra.give")) {
            sender.sendMessage("§e/fe give <player> §7- Give a fancy elytra to a player");
        }

        if (sender.hasPermission("fancyelytra.change_particle")) {
            sender.sendMessage("§e/fe particle §7- Open particle selection menu");
        }

        if (sender.hasPermission("fancyelytra.fuel")) {
            sender.sendMessage("§e/fe fuel §7- Manage elytra fuel");
        }

        if (sender.hasPermission("fancyelytra.admin.reload")) {
            sender.sendMessage("§e/fe reload §7- Reload plugin configuration");
        }

        if (sender.hasPermission("fancyelytra.admin")) {
            sender.sendMessage("§e/fe stats §7- View plugin statistics");
            sender.sendMessage("§e/fe version §7- View plugin version");
            sender.sendMessage("§e/fe update §7- Check for updates");
        }
        
        // Send footer
        sender.sendMessage(plugin.getMessageUtil().getMessage("commands.help.footer"));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return true; // Help is available to everyone
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "Show help information";
    }

    @Override
    public String getUsage() {
        return "/fancyelytra help";
    }
}