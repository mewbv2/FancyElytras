package io.mewb.fancyElytras.tasks;

import io.mewb.fancyElytras.FancyElytras;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Periodic save task to ensure data persistence
 */
public class SaveTask extends BukkitRunnable {

    private final FancyElytras plugin;

    public SaveTask(FancyElytras plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        try {
            // Save all player data
            plugin.getPlayerDataManager().saveAllPlayerData();

            if (plugin.getConfigManager().getGeneralConfig().getBoolean("general.debug", false)) {
                plugin.getLogger().info("Periodic save completed");
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Error during periodic save: " + e.getMessage());
        }
    }
}