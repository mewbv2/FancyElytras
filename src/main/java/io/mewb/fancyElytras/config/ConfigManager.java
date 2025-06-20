package io.mewb.fancyElytras.config;

import io.mewb.fancyElytras.FancyElytras;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Level;

public class ConfigManager {

    private final FancyElytras plugin;

    // Configuration objects
    private FileConfiguration generalConfig;
    private FileConfiguration particleConfig;
    private FileConfiguration fuelConfig;
    private FileConfiguration economyConfig;
    private FileConfiguration messagesConfig;

    // Configuration files
    private File generalFile;
    private File particleFile;
    private File fuelFile;
    private File economyFile;
    private File messagesFile;

    public ConfigManager(FancyElytras plugin) {
        this.plugin = plugin;
    }

    public boolean loadConfigurations() {
        try {
            // Create plugin data folder if it doesn't exist
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }

            // Initialize all config files
            initializeConfigFiles();

            // Load all configurations
            loadAllConfigs();

            return true;

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load configurations", e);
            return false;
        }
    }

    private void initializeConfigFiles() throws IOException {
        // General config (main config.yml)
        generalFile = new File(plugin.getDataFolder(), "config.yml");
        if (!generalFile.exists()) {
            plugin.saveDefaultConfig();
        }

        // Other config files - all in the main plugin folder
        particleFile = new File(plugin.getDataFolder(), "particles.yml");
        fuelFile = new File(plugin.getDataFolder(), "fuel.yml");
        economyFile = new File(plugin.getDataFolder(), "economy.yml");
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        // Create default files if they don't exist
        createDefaultConfig(particleFile, "particles.yml");
        createDefaultConfig(fuelFile, "fuel.yml");
        createDefaultConfig(economyFile, "economy.yml");
        createDefaultConfig(messagesFile, "messages.yml");
    }

    private void createDefaultConfig(File file, String resourcePath) throws IOException {
        if (!file.exists()) {
            plugin.getLogger().info("Creating default config file: " + file.getName());
            try (InputStream inputStream = plugin.getResource(resourcePath)) {
                if (inputStream != null) {
                    Files.copy(inputStream, file.toPath());
                    plugin.getLogger().info("Successfully created " + file.getName() + " from resource");
                } else {
                    plugin.getLogger().warning("Resource " + resourcePath + " not found in plugin jar, creating empty file");
                    file.createNewFile();
                }
            }
        } else {
            plugin.getLogger().info("Config file already exists: " + file.getName());
        }
    }

    private void loadAllConfigs() {
        generalConfig = plugin.getConfig();
        particleConfig = YamlConfiguration.loadConfiguration(particleFile);
        fuelConfig = YamlConfiguration.loadConfiguration(fuelFile);
        economyConfig = YamlConfiguration.loadConfiguration(economyFile);
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public boolean reloadConfigurations() {
        try {
            plugin.reloadConfig();
            loadAllConfigs();
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to reload configurations", e);
            return false;
        }
    }

    public boolean saveConfiguration(String configName) {
        try {
            switch (configName.toLowerCase()) {
                case "general":
                    plugin.saveConfig();
                    break;
                case "particles":
                    particleConfig.save(particleFile);
                    break;
                case "fuel":
                    fuelConfig.save(fuelFile);
                    break;
                case "economy":
                    economyConfig.save(economyFile);
                    break;
                case "messages":
                    messagesConfig.save(messagesFile);
                    break;
                default:
                    return false;
            }
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save configuration: " + configName, e);
            return false;
        }
    }

    // Getters for configurations
    public FileConfiguration getGeneralConfig() {
        return generalConfig;
    }

    public FileConfiguration getParticleConfig() {
        return particleConfig;
    }

    public FileConfiguration getFuelConfig() {
        return fuelConfig;
    }

    public FileConfiguration getEconomyConfig() {
        return economyConfig;
    }

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }
}