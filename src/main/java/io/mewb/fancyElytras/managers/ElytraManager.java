
package io.mewb.fancyElytras.managers;

import io.mewb.fancyElytras.FancyElytras;
import io.mewb.fancyElytras.data.ElytraData;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class ElytraManager {

    private final FancyElytras plugin;
    private final Map<UUID, ElytraData> playerElytras;

    // NBT Keys
    private final NamespacedKey fancyElytraKey;
    private final NamespacedKey particleKey;
    private final NamespacedKey fuelKey;
    private final NamespacedKey maxFuelKey;

    // Configuration cache
    private boolean allElytrasFancy;
    private String defaultElytraName;
    private List<String> defaultElytraLore;

    public ElytraManager(FancyElytras plugin) {
        this.plugin = plugin;
        this.playerElytras = new ConcurrentHashMap<>();

        // Initialize NBT keys
        this.fancyElytraKey = new NamespacedKey(plugin, "fancy_elytra");
        this.particleKey = new NamespacedKey(plugin, "particle");
        this.fuelKey = new NamespacedKey(plugin, "fuel");
        this.maxFuelKey = new NamespacedKey(plugin, "max_fuel");

        loadConfiguration();
    }

    private void loadConfiguration() {
        this.allElytrasFancy = plugin.getConfigManager().getGeneralConfig()
                .getBoolean("elytra.all-elytras-fancy", false);
        this.defaultElytraName = plugin.getConfigManager().getGeneralConfig()
                .getString("elytra.default.name", "&6&lFancy Elytra");
        this.defaultElytraLore = plugin.getConfigManager().getGeneralConfig()
                .getStringList("elytra.default.lore");
    }

    public ItemStack createFancyElytra(String particleType, int fuel) {
        ItemStack elytra = new ItemStack(Material.ELYTRA);
        ItemMeta meta = elytra.getItemMeta();

        if (meta != null) {
            // Set basic properties
            meta.setDisplayName(formatText(defaultElytraName));

            // Set NBT data
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(fancyElytraKey, PersistentDataType.BYTE, (byte) 1);
            container.set(particleKey, PersistentDataType.STRING, particleType);
            container.set(fuelKey, PersistentDataType.INTEGER, fuel);
            container.set(maxFuelKey, PersistentDataType.INTEGER,
                    plugin.getFuelManager().getMaxFuelCapacity());

            // Update lore with current data
            updateElytraLore(meta, particleType, fuel);

            elytra.setItemMeta(meta);
        }

        return elytra;
    }

    public boolean isFancyElytra(ItemStack item) {
        if (item == null || item.getType() != Material.ELYTRA) {
            return false;
        }

        // If all elytras are fancy, return true
        if (allElytrasFancy) {
            return true;
        }

        // Check for NBT data
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(fancyElytraKey, PersistentDataType.BYTE);
    }

    public ElytraData getElytraData(ItemStack item) {
        if (!isFancyElytra(item)) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }

        try {
            PersistentDataContainer container = meta.getPersistentDataContainer();

            String particle = container.getOrDefault(particleKey, PersistentDataType.STRING,
                    plugin.getParticleManager().getDefaultParticle());
            int fuel = container.getOrDefault(fuelKey, PersistentDataType.INTEGER,
                    plugin.getFuelManager().getMaxFuelCapacity());
            int maxFuel = container.getOrDefault(maxFuelKey, PersistentDataType.INTEGER,
                    plugin.getFuelManager().getMaxFuelCapacity());

            return new ElytraData(particle, fuel, maxFuel);

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to read elytra data", e);
            return null;
        }
    }

    public void setElytraData(ItemStack item, ElytraData data) {
        if (!isFancyElytra(item)) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        try {
            PersistentDataContainer container = meta.getPersistentDataContainer();

            container.set(particleKey, PersistentDataType.STRING, data.getParticleType());
            container.set(fuelKey, PersistentDataType.INTEGER, data.getFuel());
            container.set(maxFuelKey, PersistentDataType.INTEGER, data.getMaxFuel());

            // Update lore
            updateElytraLore(meta, data.getParticleType(), data.getFuel());

            item.setItemMeta(meta);

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to set elytra data", e);
        }
    }

    private void updateElytraLore(ItemMeta meta, String particleType, int fuel) {
        List<String> lore = new ArrayList<>();

        // Add particle info
        lore.add("§7Particle: §e" + particleType);

        // Get fuel display configuration
        ConfigurationSection displayConfig = plugin.getConfigManager().getFuelConfig().getConfigurationSection("display.format");

        if (displayConfig != null) {
            // Get max fuel capacity
            int maxFuel = plugin.getConfigManager().getFuelConfig().getInt("fuel.max-capacity", 1000);

            // Fuel bar
            if (displayConfig.getBoolean("bar.enabled", true)) {
                int length = displayConfig.getInt("bar.length", 20);
                String filledChar = displayConfig.getString("bar.filled-char", "█");
                String emptyChar = displayConfig.getString("bar.empty-char", "░");
                String filledColor = displayConfig.getString("bar.filled-color", "&a");
                String emptyColor = displayConfig.getString("bar.empty-color", "&7");

                // Calculate percentage
                double percentage = (double) fuel / maxFuel;
                int filledBars = (int) (percentage * length);

                // Build fuel bar
                StringBuilder fuelBar = new StringBuilder();
                fuelBar.append(plugin.getMessageUtil().formatMessage(filledColor));
                for (int i = 0; i < filledBars; i++) {
                    fuelBar.append(filledChar);
                }
                fuelBar.append(plugin.getMessageUtil().formatMessage(emptyColor));
                for (int i = filledBars; i < length; i++) {
                    fuelBar.append(emptyChar);
                }

                lore.add("§7Fuel: " + fuelBar.toString());
            }

            // Numeric display
            if (displayConfig.getBoolean("numeric.enabled", true)) {
                String format = displayConfig.getString("numeric.format", "&7Fuel: &a{current}&7/&a{max}");
                String numericDisplay = format
                        .replace("{current}", String.valueOf(fuel))
                        .replace("{max}", String.valueOf(maxFuel));
                lore.add(plugin.getMessageUtil().formatMessage(numericDisplay));
            }

            // Percentage display
            if (displayConfig.getBoolean("percentage.enabled", false)) {
                String format = displayConfig.getString("percentage.format", "&7Fuel: &a{percentage}%");
                int percentage = (int) Math.round(((double) fuel / maxFuel) * 100);
                String percentageDisplay = format.replace("{percentage}", String.valueOf(percentage));
                lore.add(plugin.getMessageUtil().formatMessage(percentageDisplay));
            }
        }

        meta.setLore(lore);
    }


    public ItemStack getPlayerElytra(Player player) {
        // Check chestplate slot first
        ItemStack chestplate = player.getInventory().getChestplate();
        if (isFancyElytra(chestplate)) {
            return chestplate;
        }

        // Check main hand
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (isFancyElytra(mainHand)) {
            return mainHand;
        }

        // Check off hand
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (isFancyElytra(offHand)) {
            return offHand;
        }

        return null;
    }

    public boolean updatePlayerElytra(Player player, ElytraData data) {
        ItemStack elytra = getPlayerElytra(player);
        if (elytra == null) {
            return false;
        }

        setElytraData(elytra, data);
        return true;
    }

    public void convertToFancyElytra(ItemStack item) {
        if (item == null || item.getType() != Material.ELYTRA) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            meta = plugin.getServer().getItemFactory().getItemMeta(Material.ELYTRA);
        }

        // Set as fancy elytra
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(fancyElytraKey, PersistentDataType.BYTE, (byte) 1);
        container.set(particleKey, PersistentDataType.STRING,
                plugin.getParticleManager().getDefaultParticle());
        container.set(fuelKey, PersistentDataType.INTEGER,
                plugin.getFuelManager().getMaxFuelCapacity());
        container.set(maxFuelKey, PersistentDataType.INTEGER,
                plugin.getFuelManager().getMaxFuelCapacity());

        // Set display name and lore
        meta.setDisplayName(formatText(defaultElytraName));
        updateElytraLore(meta, plugin.getParticleManager().getDefaultParticle(),
                plugin.getFuelManager().getMaxFuelCapacity());

        item.setItemMeta(meta);
    }

    public void removeFancyElytra(ItemStack item) {
        if (!isFancyElytra(item)) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        // Remove NBT data
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.remove(fancyElytraKey);
        container.remove(particleKey);
        container.remove(fuelKey);
        container.remove(maxFuelKey);

        // Reset display name and lore
        meta.setDisplayName(null);
        meta.setLore(null);

        item.setItemMeta(meta);
    }

    public int getElytraSlot(Player player) {
        // Check if elytra is in chestplate slot
        if (isFancyElytra(player.getInventory().getChestplate())) {
            return 38; // Chestplate slot
        }

        // Check main hand
        if (isFancyElytra(player.getInventory().getItemInMainHand())) {
            return player.getInventory().getHeldItemSlot();
        }

        // Check off hand
        if (isFancyElytra(player.getInventory().getItemInOffHand())) {
            return 40; // Off hand slot
        }

        return -1; // Not found
    }

    public void cachePlayerElytra(Player player) {
        ItemStack elytra = getPlayerElytra(player);
        if (elytra != null) {
            ElytraData data = getElytraData(elytra);
            if (data != null) {
                playerElytras.put(player.getUniqueId(), data);
            }
        }
    }

    public void removeCachedElytra(UUID playerId) {
        playerElytras.remove(playerId);
    }

    public ElytraData getCachedElytraData(UUID playerId) {
        return playerElytras.get(playerId);
    }

    private String formatText(String text) {
        return text.replace('&', '§');
    }

    public void reload() {
        loadConfiguration();
        playerElytras.clear();
    }

    public void cleanup() {
        playerElytras.clear();
    }

    // Statistics
    public int getCachedElytraCount() {
        return playerElytras.size();
    }

    public Collection<ElytraData> getAllCachedElytras() {
        return new ArrayList<>(playerElytras.values());
    }
}