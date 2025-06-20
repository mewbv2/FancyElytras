package io.mewb.fancyElytras.commands.subcommands;

import io.mewb.fancyElytras.FancyElytras;
import io.mewb.fancyElytras.commands.SubCommand;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class ReloadCommand implements SubCommand {

    private final FancyElytras plugin;

    public ReloadCommand(FancyElytras plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage("§e[FancyElytra] Reloading configuration...");

        boolean success = plugin.reloadPlugin();

        if (success) {
            plugin.getMessageUtil().sendMessage(sender, "plugin.reloaded");
        } else {
            plugin.getMessageUtil().sendMessage(sender, "plugin.reload-failed");
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("fancyelytra.admin.reload");
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getDescription() {
        return "Reload plugin configuration";
    }

    @Override
    public String getUsage() {
        return "/fancyelytra reload";
    }
}