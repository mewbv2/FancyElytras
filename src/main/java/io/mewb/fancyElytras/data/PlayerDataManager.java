package io.mewb.fancyElytras.data;

import io.mewb.fancyElytras.FancyElytras;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {

    private final FancyElytras plugin;
    private final Map<UUID, PlayerData> playerDataCache;

    public PlayerDataManager(FancyElytras plugin) {
        this.plugin = plugin;
        this.playerDataCache = new ConcurrentHashMap<>();
    }

    public void loadPlayerData(Player player) {
        UUID playerUuid = player.getUniqueId();

        plugin.getDatabaseManager().loadPlayerData(playerUuid)
                .thenAccept(playerData -> {
                    playerDataCache.put(playerUuid, playerData);

                    if (plugin.getConfigManager().getGeneralConfig().getBoolean("general.debug", false)) {
                        plugin.getLogger().info("Loaded player data for " + player.getName());
                    }
                })
                .exceptionally(throwable -> {
                    plugin.getLogger().warning("Failed to load player data for " + player.getName() + ": " + throwable.getMessage());

                    // Create default data as fallback
                    PlayerData defaultData = new PlayerData(
                            playerUuid,
                            plugin.getParticleManager().getDefaultParticle(),
                            System.currentTimeMillis(),
                            System.currentTimeMillis()
                    );
                    playerDataCache.put(playerUuid, defaultData);

                    return null;
                });
    }

    public void savePlayerData(Player player) {
        UUID playerUuid = player.getUniqueId();
        PlayerData playerData = playerDataCache.get(playerUuid);

        if (playerData != null) {
            // Update last login time
            playerData.setLastLogin(System.currentTimeMillis());

            plugin.getDatabaseManager().savePlayerData(playerData)
                    .thenAccept(success -> {
                        if (plugin.getConfigManager().getGeneralConfig().getBoolean("general.debug", false)) {
                            plugin.getLogger().info("Saved player data for " + player.getName() + ": " + success);
                        }
                    })
                    .exceptionally(throwable -> {
                        plugin.getLogger().warning("Failed to save player data for " + player.getName() + ": " + throwable.getMessage());
                        return null;
                    });
        }
    }

    public void saveAllPlayerData() {
        plugin.getLogger().info("Saving all player data...");

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            savePlayerData(player);
        }
    }

    public PlayerData getPlayerData(UUID playerUuid) {
        return playerDataCache.get(playerUuid);
    }

    public void setPlayerData(UUID playerUuid, PlayerData playerData) {
        playerDataCache.put(playerUuid, playerData);
    }

    public void removePlayerData(UUID playerUuid) {
        playerDataCache.remove(playerUuid);
    }

    public void updatePlayerStatistic(Player player, String statType, int value) {
        plugin.getDatabaseManager().updatePlayerStatistic(player.getUniqueId(), statType, value)
                .exceptionally(throwable -> {
                    plugin.getLogger().warning("Failed to update statistic for " + player.getName() + ": " + throwable.getMessage());
                    return false;
                });
    }

    public void getPlayerStatistic(Player player, String statType, java.util.function.Consumer<Integer> callback) {
        plugin.getDatabaseManager().getPlayerStatistic(player.getUniqueId(), statType)
                .thenAccept(callback)
                .exceptionally(throwable -> {
                    plugin.getLogger().warning("Failed to get statistic for " + player.getName() + ": " + throwable.getMessage());
                    callback.accept(0);
                    return null;
                });
    }
}