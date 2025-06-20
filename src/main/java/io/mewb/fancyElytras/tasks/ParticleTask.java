package io.mewb.fancyElytras.tasks;

import io.mewb.fancyElytras.FancyElytras;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * This is the VITAL task that runs the particle system!
 * Without this, particles won't spawn automatically during flight.
 */
public class ParticleTask extends BukkitRunnable {

    private final FancyElytras plugin;
    private int tickCounter = 0;

    public ParticleTask(FancyElytras plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        try {
            // Reset particle counts every tick for performance tracking
            plugin.getParticleManager().resetParticleCount();

            // Process all online players
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player.isGliding()) {
                    processGlidingPlayer(player);
                }
            }

            // Increment tick counter
            tickCounter++;

            // Perform cleanup tasks every 20 ticks (1 second)
            if (tickCounter % 20 == 0) {
                performPeriodicTasks();
            }

            // Perform heavy cleanup every 1200 ticks (1 minute)
            if (tickCounter % 1200 == 0) {
                performHeavyCleanup();
                tickCounter = 0; // Reset to prevent overflow
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Error in ParticleTask: " + e.getMessage());
            if (plugin.getConfigManager().getGeneralConfig().getBoolean("general.debug", false)) {
                e.printStackTrace();
            }
        }
    }

    private void processGlidingPlayer(Player player) {
        // Check if world is disabled
        if (isWorldDisabled(player)) {
            return;
        }

        // Check permission
        if (!player.hasPermission("fancyelytra.use")) {
            return;
        }

        // Get player's elytra
        var elytra = plugin.getElytraManager().getPlayerElytra(player);
        if (elytra == null || !plugin.getElytraManager().isFancyElytra(elytra)) {
            return;
        }

        // Get elytra data
        var elytraData = plugin.getElytraManager().getElytraData(elytra);
        if (elytraData == null) {
            return;
        }

        // Check if should spawn particles without fuel
        if (!elytraData.hasFuel() &&
                !plugin.getConfigManager().getGeneralConfig().getBoolean("elytra.show-particles-without-fuel", false)) {
            return;
        }

        // Spawn particle trail
        plugin.getParticleManager().spawnParticleTrail(player, elytraData.getParticleType());
    }

    private void performPeriodicTasks() {
        // Update cached elytra data for all online players
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            try {
                var elytra = plugin.getElytraManager().getPlayerElytra(player);
                if (elytra != null && plugin.getElytraManager().isFancyElytra(elytra)) {
                    var elytraData = plugin.getElytraManager().getElytraData(elytra);
                    if (elytraData != null && elytraData.isModified()) {
                        // Update the item if data was modified
                        plugin.getElytraManager().setElytraData(elytra, elytraData);
                        elytraData.setModified(false);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error updating elytra data for " + player.getName() + ": " + e.getMessage());
            }
        }
    }

    private void performHeavyCleanup() {
        // Log performance statistics if debug is enabled
        if (plugin.getConfigManager().getGeneralConfig().getBoolean("general.debug", false)) {
            int activeParticles = plugin.getParticleManager().getActiveParticleCount();
            int cachedElytras = plugin.getElytraManager().getCachedElytraCount();
            double avgParticles = plugin.getParticleManager().getAverageParticlesPerPlayer();

            plugin.getLogger().info(String.format("Performance Stats - Active Particles: %d, Cached Elytras: %d, Avg Particles/Player: %.2f",
                    activeParticles, cachedElytras, avgParticles));
        }

        // Clean up disconnected player data
        cleanupDisconnectedPlayers();
    }

    private void cleanupDisconnectedPlayers() {
        // This is handled by PlayerListener, but we can add additional cleanup here
        // For example, cleaning up any lingering data for players who disconnected unexpectedly
    }

    private boolean isWorldDisabled(Player player) {
        String worldName = player.getWorld().getName();
        return plugin.getConfigManager().getGeneralConfig()
                .getStringList("general.disabled-worlds").contains(worldName);
    }

    public int getTickCounter() {
        return tickCounter;
    }

    public void resetTickCounter() {
        this.tickCounter = 0;
    }
}