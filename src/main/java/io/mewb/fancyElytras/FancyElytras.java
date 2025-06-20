package io.mewb.fancyElytras;

import io.mewb.fancyElytras.commands.FancyElytraCommand;
import io.mewb.fancyElytras.config.ConfigManager;
import io.mewb.fancyElytras.data.DatabaseManager;
import io.mewb.fancyElytras.data.PlayerDataManager;
import io.mewb.fancyElytras.economy.EconomyManager;
import io.mewb.fancyElytras.listeners.ElytraListener;
import io.mewb.fancyElytras.listeners.PlayerListener;
import io.mewb.fancyElytras.managers.ElytraManager;
import io.mewb.fancyElytras.managers.ParticleManager;
import io.mewb.fancyElytras.managers.FuelManager;
import io.mewb.fancyElytras.utils.MessageUtil;
import io.mewb.fancyElytras.utils.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class FancyElytras extends JavaPlugin {

    private static FancyElytras instance;

    // Core managers
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private PlayerDataManager playerDataManager;
    private EconomyManager economyManager;

    // Feature managers
    private ElytraManager elytraManager;
    private ParticleManager particleManager;
    private FuelManager fuelManager;

    // Utilities
    private MessageUtil messageUtil;
    private UpdateChecker updateChecker;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize plugin
        if (!initializePlugin()) {
            getLogger().severe("Failed to initialize FancyElytra plugin!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }



        getLogger().info("FancyElytra has been enabled successfully!");

        // Check for updates if enabled
        if (configManager.getGeneralConfig().getBoolean("general.check-updates", true)) {
            updateChecker.checkForUpdates();
        }
    }



    @Override
    public void onDisable() {
        // Clean shutdown
        shutdownPlugin();
        getLogger().info("FancyElytra has been disabled.");
    }

    private boolean initializePlugin() {
        try {
            // Initialize configuration first
            this.configManager = new ConfigManager(this);
            if (!configManager.loadConfigurations()) {
                return false;
            }

            // Initialize message utility
            this.messageUtil = new MessageUtil(this);

            // Initialize database
            this.databaseManager = new DatabaseManager(this);
            if (!databaseManager.initialize()) {
                return false;
            }

            // Initialize player data manager
            this.playerDataManager = new PlayerDataManager(this);

            // Initialize economy (optional)
            this.economyManager = new EconomyManager(this);
            economyManager.setupEconomy();

            // Initialize feature managers
            this.particleManager = new ParticleManager(this);
            this.fuelManager = new FuelManager(this);
            this.elytraManager = new ElytraManager(this);

            // Initialize update checker
            this.updateChecker = new UpdateChecker(this, 12345); // Replace with actual resource ID

            // Register events
            registerEvents();

            // Register commands
            registerCommands();

            return true;

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error during plugin initialization", e);
            return false;
        }
    }

    private void registerEvents() {
        Bukkit.getPluginManager().registerEvents(new ElytraListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
    }

    private void registerCommands() {
        FancyElytraCommand commandExecutor = new FancyElytraCommand(this);
        getCommand("fancyelytra").setExecutor(commandExecutor);
        getCommand("fancyelytra").setTabCompleter(commandExecutor);
    }

    private void shutdownPlugin() {
        try {
            // Cancel all scheduled tasks
            Bukkit.getScheduler().cancelTasks(this);

            // Save all player data
            if (playerDataManager != null) {
                playerDataManager.saveAllPlayerData();
            }

            // Close database connections
            if (databaseManager != null) {
                databaseManager.close();
            }

        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Error during plugin shutdown", e);
        }
    }

    public boolean reloadPlugin() {
        try {
            // Reload configurations
            if (!configManager.reloadConfigurations()) {
                return false;
            }

            // Reload message utility
            messageUtil.reload();

            // Reload managers
            particleManager.reload();
            fuelManager.reload();
            elytraManager.reload();

            getLogger().info("FancyElytra configuration reloaded successfully!");
            return true;

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error during plugin reload", e);
            return false;
        }
    }

    // Static instance getter
    public static FancyElytras getInstance() {
        return instance;
    }

    // Getters for managers
    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public ElytraManager getElytraManager() {
        return elytraManager;
    }

    public ParticleManager getParticleManager() {
        return particleManager;
    }

    public FuelManager getFuelManager() {
        return fuelManager;
    }

    public MessageUtil getMessageUtil() {
        return messageUtil;
    }

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }
}