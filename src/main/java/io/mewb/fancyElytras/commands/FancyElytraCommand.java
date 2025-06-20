package io.mewb.fancyElytras.commands;

import io.mewb.fancyElytras.FancyElytras;
import io.mewb.fancyElytras.commands.subcommands.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class FancyElytraCommand implements CommandExecutor, TabCompleter {

    private final FancyElytras plugin;
    private final Map<String, SubCommand> subCommands;

    public FancyElytraCommand(FancyElytras plugin) {
        this.plugin = plugin;
        this.subCommands = new HashMap<>();

        // Register subcommands
        registerSubCommands();
    }

    private void registerSubCommands() {
        subCommands.put("help", new HelpCommand(plugin));
        subCommands.put("give", new GiveCommand(plugin));
        subCommands.put("particle", new ParticleCommand(plugin));
        subCommands.put("fuel", new FuelCommand(plugin));
        subCommands.put("reload", new ReloadCommand(plugin));
        subCommands.put("stats", new StatsCommand(plugin));
        subCommands.put("version", new VersionCommand(plugin));
        subCommands.put("update", new UpdateCommand(plugin));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check basic permission
        if (!sender.hasPermission("fancyelytra.use")) {
            plugin.getMessageUtil().sendMessage(sender, "general.no-permission");
            return true;
        }

        // No arguments - show help
        if (args.length == 0) {
            subCommands.get("help").execute(sender, args);
            return true;
        }

        // Get subcommand
        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand == null) {
            plugin.getMessageUtil().sendMessage(sender, "general.invalid-arguments");
            return true;
        }

        // Check permission for subcommand
        if (!subCommand.hasPermission(sender)) {
            plugin.getMessageUtil().sendMessage(sender, "general.no-permission");
            return true;
        }

        // Execute subcommand
        try {
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
            subCommand.execute(sender, subArgs);
        } catch (Exception e) {
            plugin.getLogger().warning("Error executing command: " + e.getMessage());
            plugin.getMessageUtil().sendMessage(sender, "errors.general");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Tab complete subcommands
            String partial = args[0].toLowerCase();
            for (String subCommandName : subCommands.keySet()) {
                SubCommand subCommand = subCommands.get(subCommandName);
                if (subCommand.hasPermission(sender) && subCommandName.startsWith(partial)) {
                    completions.add(subCommandName);
                }
            }
        } else if (args.length > 1) {
            // Tab complete for subcommands
            String subCommandName = args[0].toLowerCase();
            SubCommand subCommand = subCommands.get(subCommandName);

            if (subCommand != null && subCommand.hasPermission(sender)) {
                String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
                completions = subCommand.tabComplete(sender, subArgs);
            }
        }

        return completions;
    }

    public Map<String, SubCommand> getSubCommands() {
        return subCommands;
    }
}