package io.mewb.fancyElytras.commands.subcommands;

import io.mewb.fancyElytras.FancyElytras;
import io.mewb.fancyElytras.commands.SubCommand;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class UpdateCommand implements SubCommand {

    private final FancyElytras plugin;

    public UpdateCommand(FancyElytras plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage("§6Checking for updates...");

        plugin.getUpdateChecker().checkForUpdates().thenAccept(hasUpdate -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (hasUpdate) {
                    sender.sendMessage("§a§lUpdate Available!");
                    sender.sendMessage("§7Current Version: §e" + plugin.getUpdateChecker().getCurrentVersion());
                    sender.sendMessage("§7Latest Version: §a" + plugin.getUpdateChecker().getLatestVersion());
                    sender.sendMessage("§7Download: §b" + plugin.getUpdateChecker().getDownloadUrl());
                    sender.sendMessage("§7Please download and install the new version manually.");
                } else {
                    sender.sendMessage("§a§lNo updates available!");
                    sender.sendMessage("§7You are running the latest version: §e" + plugin.getUpdateChecker().getCurrentVersion());
                }
            });
        }).exceptionally(throwable -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                sender.sendMessage("§c§lFailed to check for updates!");
                sender.sendMessage("§7Error: " + throwable.getMessage());
            });
            return null;
        });
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("fancyelytra.admin");
    }

    @Override
    public String getName() {
        return "update";
    }

    @Override
    public String getDescription() {
        return "Check for plugin updates";
    }

    @Override
    public String getUsage() {
        return "/fancyelytra update";
    }
}