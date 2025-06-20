package io.mewb.fancyElytras.commands.subcommands;

import io.mewb.fancyElytras.FancyElytras;
import io.mewb.fancyElytras.commands.SubCommand;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class VersionCommand implements SubCommand {

    private final FancyElytras plugin;

    public VersionCommand(FancyElytras plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage("§6=== FancyElytra Version Information ===");
        sender.sendMessage("§7Plugin Version: §e" + plugin.getDescription().getVersion());
        sender.sendMessage("§7Authors: §e" + String.join(", ", plugin.getDescription().getAuthors()));
        sender.sendMessage("§7Server Version: §e" + plugin.getServer().getVersion());
        sender.sendMessage("§7Bukkit API: §e" + plugin.getServer().getBukkitVersion());

        if (plugin.getUpdateChecker().isUpdateAvailable()) {
            sender.sendMessage("§a§lUpdate Available!");
            sender.sendMessage("§7Current: §e" + plugin.getUpdateChecker().getCurrentVersion());
            sender.sendMessage("§7Latest: §a" + plugin.getUpdateChecker().getLatestVersion());
            sender.sendMessage("§7Download: §b" + plugin.getUpdateChecker().getDownloadUrl());
        } else {
            sender.sendMessage("§a§lPlugin is up to date!");
        }
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
        return "version";
    }

    @Override
    public String getDescription() {
        return "View plugin version";
    }

    @Override
    public String getUsage() {
        return "/fancyelytra version";
    }
}