package io.mewb.fancyElytras.commands.subcommands;

import io.mewb.fancyElytras.FancyElytras;
import io.mewb.fancyElytras.commands.SubCommand;
import io.mewb.fancyElytras.gui.ParticleSelectionGUI;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ParticleCommand implements SubCommand {

    private final FancyElytras plugin;

    public ParticleCommand(FancyElytras plugin) {
        this.plugin = plugin;
    }

    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageUtil().sendMessage(sender, "general.player-only");
            return;
        }

        Player player = (Player) sender;

        // Find player's elytra
        ItemStack elytra = plugin.getElytraManager().getPlayerElytra(player);
        if (elytra == null) {
            plugin.getMessageUtil().sendMessage(player, "commands.particle.no-elytra");
            return;
        }

        if (!plugin.getElytraManager().isFancyElytra(elytra)) {
            plugin.getMessageUtil().sendMessage(player, "commands.particle.not-fancy");
            return;
        }

        // Add debug code here
        plugin.getLogger().info("=== PARTICLE DEBUG ===");
        plugin.getLogger().info("Total particles loaded: " + plugin.getParticleManager().getAvailableParticles().size());
        plugin.getLogger().info("Particle names: " + plugin.getParticleManager().getParticleTypeNames());

        // Check player permissions
        for (io.mewb.fancyElytras.data.ParticleType particle : plugin.getParticleManager().getAvailableParticles()) {
            boolean hasPermission = plugin.getParticleManager().hasParticlePermission(player, particle.getName());
            plugin.getLogger().info("Player " + player.getName() + " permission for " + particle.getName() + ": " + hasPermission);
        }

        // Open particle selection GUI
        ParticleSelectionGUI gui = new ParticleSelectionGUI(plugin, player, elytra);
        gui.open();

        plugin.getMessageUtil().sendMessage(player, "commands.particle.gui-opened");
    }


    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("fancyelytra.change_particle");
    }

    @Override
    public String getName() {
        return "particle";
    }

    @Override
    public String getDescription() {
        return "Open particle selection menu";
    }

    @Override
    public String getUsage() {
        return "/fancyelytra particle";
    }
}