package io.mewb.fancyElytras.listeners;

import io.mewb.fancyElytras.FancyElytras;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final FancyElytras plugin;

    public PlayerListener(FancyElytras plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Load player data asynchronously
        plugin.getPlayerDataManager().loadPlayerData(player);

        // Cache elytra data if player has one
        plugin.getElytraManager().cachePlayerElytra(player);

        // Check for updates (if player has permission)
        if (player.hasPermission("fancyelytra.admin") &&
                plugin.getConfigManager().getGeneralConfig().getBoolean("updates.notify-ops", true)) {

            plugin.getUpdateChecker().checkForUpdates().thenAccept(hasUpdate -> {
                if (hasUpdate) {
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        player.sendMessage("§a[FancyElytra] A new version is available!");
                        player.sendMessage("§7Use /fancyelytra update to get more information.");
                    }, 60L); // Delay 3 seconds
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Save player data asynchronously
        plugin.getPlayerDataManager().savePlayerData(player);

        // Remove cached elytra data
        plugin.getElytraManager().removeCachedElytra(player.getUniqueId());

        // Clean up any ongoing takeoff sequences
        // This is handled by ElytraListener, but we can add cleanup here if needed
    }
}