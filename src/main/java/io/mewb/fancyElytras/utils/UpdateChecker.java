package io.mewb.fancyElytras.utils;

import io.mewb.fancyElytras.FancyElytras;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class UpdateChecker {

    private final FancyElytras plugin;
    private final int resourceId;
    private String latestVersion;
    private boolean updateAvailable = false;

    public UpdateChecker(FancyElytras plugin, int resourceId) {
        this.plugin = plugin;
        this.resourceId = resourceId;
    }

    public CompletableFuture<Boolean> checkForUpdates() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Check SpigotMC API for latest version
                String url = "https://api.spigotmc.org/legacy/update.php?resource=" + resourceId;

                try (InputStream inputStream = new URL(url).openStream();
                     Scanner scanner = new Scanner(inputStream)) {

                    if (scanner.hasNext()) {
                        latestVersion = scanner.next();
                        String currentVersion = plugin.getDescription().getVersion();

                        updateAvailable = !currentVersion.equals(latestVersion);

                        if (updateAvailable) {
                            plugin.getLogger().info("Update available! Current: " + currentVersion +
                                    ", Latest: " + latestVersion);
                        } else {
                            plugin.getLogger().info("Plugin is up to date!");
                        }

                        return updateAvailable;
                    }
                }

            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to check for updates", e);
            }

            return false;
        });
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public String getCurrentVersion() {
        return plugin.getDescription().getVersion();
    }

    public String getDownloadUrl() {
        return "https://www.spigotmc.org/resources/" + resourceId + "/";
    }
}