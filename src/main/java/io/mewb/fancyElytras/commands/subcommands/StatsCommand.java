package io.mewb.fancyElytras.commands.subcommands;

import io.mewb.fancyElytras.FancyElytras;
import io.mewb.fancyElytras.commands.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class StatsCommand implements SubCommand {

    private final FancyElytras plugin;

    public StatsCommand(FancyElytras plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageUtil().sendMessage(sender, "general.player-only");
            return;
        }

        Player player = (Player) sender;

        sender.sendMessage("§6=== FancyElytra Statistics ===");
        sender.sendMessage("§7Plugin Version: §e" + plugin.getDescription().getVersion());
        sender.sendMessage("§7Online Players: §e" + plugin.getServer().getOnlinePlayers().size());
        sender.sendMessage("§7Available Particles: §e" + plugin.getParticleManager().getAvailableParticles().size());
        sender.sendMessage("§7Cached Elytras: §e" + plugin.getElytraManager().getCachedElytraCount());
        sender.sendMessage("§7Active Particles: §e" + plugin.getParticleManager().getActiveParticleCount());
        sender.sendMessage("§7Fuel System: §e" + (plugin.getFuelManager().isFuelSystemEnabled() ? "Enabled" : "Disabled"));
        sender.sendMessage("§7Economy: §e" + (plugin.getEconomyManager().isEconomyEnabled() ? "Enabled" : "Disabled"));
        sender.sendMessage("§7Database: §e" + (plugin.getDatabaseManager().isConnected() ? "Connected" : "Disconnected"));
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
        return "stats";
    }

    @Override
    public String getDescription() {
        return "View plugin statistics";
    }

    @Override
    public String getUsage() {
        return "/fancyelytra stats";
    }
}