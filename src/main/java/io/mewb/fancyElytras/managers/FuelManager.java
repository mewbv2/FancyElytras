package io.mewb.fancyElytras.managers;

import io.mewb.fancyElytras.FancyElytras;
import io.mewb.fancyElytras.data.ElytraData;
import io.mewb.fancyElytras.data.FuelItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class FuelManager {

    private final FancyElytras plugin;
    private final Map<Material, FuelItem> fuelItems;
    private final Map<UUID, Long> lastFuelWarning;

    // Configuration cache
    private boolean fuelSystemEnabled;
    private int maxFuelCapacity;
    private int startingFuel;
    private int flightConsumptionPerTick;
    private int takeoffFuelCost;
    private double highSpeedMultiplier;
    private int minFuelForTakeoff;

    // Auto-refuel settings
    private boolean autoRefuelEnabled;
    private int autoRefuelThreshold;
    private List<Material> fuelPriority;

    // Warning settings
    private boolean lowFuelWarning;
    private boolean criticalFuelWarning;
    private boolean noFuelWarning;
    private int lowFuelThreshold;
    private int criticalFuelThreshold;

    public FuelManager(FancyElytras plugin) {
        this.plugin = plugin;
        this.fuelItems = new ConcurrentHashMap<>();
        this.lastFuelWarning = new ConcurrentHashMap<>();

        loadConfiguration();
    }

    private void loadConfiguration() {
        ConfigurationSection fuelConfig = plugin.getConfigManager().getFuelConfig();

        // General fuel settings
        this.fuelSystemEnabled = fuelConfig.getBoolean("fuel.enabled", true);
        this.maxFuelCapacity = fuelConfig.getInt("fuel.max-capacity", 1000);
        this.startingFuel = fuelConfig.getInt("fuel.starting-fuel", 1000);
        this.flightConsumptionPerTick = fuelConfig.getInt("fuel.consumption.flight-per-tick", 1);
        this.takeoffFuelCost = fuelConfig.getInt("fuel.consumption.takeoff-cost", 10);
        this.highSpeedMultiplier = fuelConfig.getDouble("fuel.consumption.high-speed-multiplier", 2.0);
        this.minFuelForTakeoff = fuelConfig.getInt("fuel.consumption.min-fuel-for-takeoff", 50);

        // Auto-refuel settings
        ConfigurationSection autoRefuelSection = fuelConfig.getConfigurationSection("refueling.items.auto-refuel");
        if (autoRefuelSection != null) {
            this.autoRefuelEnabled = autoRefuelSection.getBoolean("enabled", true);
            this.autoRefuelThreshold = autoRefuelSection.getInt("auto-threshold", 100);
            this.fuelPriority = new ArrayList<>();

            for (String materialName : autoRefuelSection.getStringList("priority")) {
                try {
                    Material material = Material.valueOf(materialName.toUpperCase());
                    fuelPriority.add(material);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid fuel priority material: " + materialName);
                }
            }
        }

        // Warning settings
        ConfigurationSection warningSection = fuelConfig.getConfigurationSection("display.warnings");
        if (warningSection != null) {
            this.lowFuelWarning = warningSection.getBoolean("low-fuel.enabled", true);
            this.lowFuelThreshold = warningSection.getInt("low-fuel.threshold", 100);
            this.criticalFuelWarning = warningSection.getBoolean("critical-fuel.enabled", true);
            this.criticalFuelThreshold = warningSection.getInt("critical-fuel.threshold", 25);
            this.noFuelWarning = warningSection.getBoolean("no-fuel.enabled", true);
        }

        // Load fuel items
        loadFuelItems();
    }

    private void loadFuelItems() {
        fuelItems.clear();

        ConfigurationSection itemsSection = plugin.getConfigManager().getFuelConfig()
                .getConfigurationSection("refueling.items.fuel-items");

        if (itemsSection == null) {
            plugin.getLogger().warning("No fuel items configured!");
            return;
        }

        for (String key : itemsSection.getKeys(false)) {
            try {
                Material material = Material.valueOf(key.toUpperCase());
                ConfigurationSection itemConfig = itemsSection.getConfigurationSection(key);

                if (itemConfig != null) {
                    int fuelValue = itemConfig.getInt("fuel-value", 10);
                    String displayName = itemConfig.getString("display-name", key);
                    String returnItem = itemConfig.getString("return-item", null);

                    Material returnMaterial = null;
                    if (returnItem != null && !returnItem.isEmpty()) {
                        try {
                            returnMaterial = Material.valueOf(returnItem.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Invalid return item for " + key + ": " + returnItem);
                        }
                    }

                    FuelItem fuelItem = new FuelItem(material, fuelValue, displayName, returnMaterial);
                    fuelItems.put(material, fuelItem);
                }

            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid fuel item material: " + key);
            }
        }

        plugin.getLogger().info("Loaded " + fuelItems.size() + " fuel item types");
    }

    public boolean isFuelSystemEnabled() {
        return fuelSystemEnabled;
    }

    public int getMaxFuelCapacity() {
        return maxFuelCapacity;
    }

    public int getStartingFuel() {
        return startingFuel;
    }

    public boolean consumeFuelForFlight(Player player, ElytraData elytraData) {
        if (!fuelSystemEnabled) {
            return true;
        }

        if (!elytraData.hasFuel()) {
            handleNoFuel(player);
            return false;
        }

        // Calculate consumption based on speed
        double speed = player.getVelocity().length();
        double normalSpeed = 0.5; // Approximate normal elytra speed
        double consumption = flightConsumptionPerTick;

        if (speed > normalSpeed) {
            consumption *= highSpeedMultiplier;
        }

        // Consume fuel
        if (!elytraData.consumeFuel((int) Math.ceil(consumption))) {
            handleNoFuel(player);
            return false;
        }

        // Check for warnings
        checkFuelWarnings(player, elytraData);

        // Auto-refuel if enabled
        if (autoRefuelEnabled && elytraData.getFuel() <= autoRefuelThreshold) {
            attemptAutoRefuel(player, elytraData);
        }

        return true;
    }

    public boolean consumeFuelForTakeoff(Player player, ElytraData elytraData) {
        if (!fuelSystemEnabled) {
            return true;
        }

        if (elytraData.getFuel() < minFuelForTakeoff) {
            String message = plugin.getMessageUtil().getMessage("elytra.takeoff.insufficient-fuel")
                    .replace("{required}", String.valueOf(minFuelForTakeoff))
                    .replace("{current}", String.valueOf(elytraData.getFuel()));
            plugin.getMessageUtil().sendMessage(player, message);
            return false;
        }

        return elytraData.consumeFuel(takeoffFuelCost);
    }

    public void checkFuelWarnings(Player player, ElytraData elytraData) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        // Check if we should show a warning (throttle warnings)
        Long lastWarning = lastFuelWarning.get(playerId);
        if (lastWarning != null && currentTime - lastWarning < 5000) { // 5 second cooldown
            return;
        }

        int currentFuel = elytraData.getFuel();

        if (currentFuel <= 0 && noFuelWarning) {
            String message = plugin.getMessageUtil().getMessage("display.warnings.no-fuel");
            plugin.getMessageUtil().sendMessage(player, message);
            lastFuelWarning.put(playerId, currentTime);
        } else if (currentFuel <= criticalFuelThreshold && criticalFuelWarning) {
            String message = plugin.getMessageUtil().getMessage("display.warnings.critical-fuel")
                    .replace("{fuel}", String.valueOf(currentFuel));
            plugin.getMessageUtil().sendMessage(player, message);
            lastFuelWarning.put(playerId, currentTime);
        } else if (currentFuel <= lowFuelThreshold && lowFuelWarning) {
            String message = plugin.getMessageUtil().getMessage("display.warnings.low-fuel")
                    .replace("{fuel}", String.valueOf(currentFuel));
            plugin.getMessageUtil().sendMessage(player, message);
            lastFuelWarning.put(playerId, currentTime);
        }
    }


    private void handleNoFuel(Player player) {
        if (noFuelWarning) {
            String message = plugin.getMessageUtil().getMessage("elytra.fuel.empty");
            plugin.getMessageUtil().sendMessage(player, message);
        }

        // Remove player from warning cache to allow immediate warning when fuel is restored
        lastFuelWarning.remove(player.getUniqueId());
    }

    public boolean refuelElytra(Player player, ElytraData elytraData, Material fuelMaterial, int amount) {
        FuelItem fuelItem = fuelItems.get(fuelMaterial);
        if (fuelItem == null) {
            return false;
        }

        // Check if player has the required items
        if (!hasEnoughItems(player, fuelMaterial, amount)) {
            return false;
        }

        // Calculate fuel to add
        int fuelToAdd = fuelItem.getFuelValue() * amount;
        int currentFuel = elytraData.getFuel();
        int newFuel = Math.min(currentFuel + fuelToAdd, maxFuelCapacity);
        int actualFuelAdded = newFuel - currentFuel;

        if (actualFuelAdded <= 0) {
            return false; // Already full
        }

        // Calculate actual items needed (in case we can't fill completely)
        int actualItemsNeeded = (int) Math.ceil((double) actualFuelAdded / fuelItem.getFuelValue());

        // Remove items from inventory
        removeItems(player, fuelMaterial, actualItemsNeeded);

        // Add return items if applicable
        if (fuelItem.getReturnItem() != null) {
            addItems(player, fuelItem.getReturnItem(), actualItemsNeeded);
        }

        // Update fuel
        elytraData.setFuel(newFuel);

        // Send success message
        String message = plugin.getMessageUtil().getMessage("commands.fuel.refueled")
                .replace("{amount}", String.valueOf(actualFuelAdded))
                .replace("{items}", String.valueOf(actualItemsNeeded))
                .replace("{item}", fuelItem.getDisplayName());
        plugin.getMessageUtil().sendMessage(player, message);

        return true;
    }

    public void attemptAutoRefuel(Player player, ElytraData elytraData) {
        if (!autoRefuelEnabled || elytraData.getFuel() >= autoRefuelThreshold) {
            return;
        }

        for (Material fuelMaterial : fuelPriority) {
            FuelItem fuelItem = fuelItems.get(fuelMaterial);
            if (fuelItem == null) continue;

            int available = countItems(player, fuelMaterial);
            if (available > 0) {
                // Calculate how much fuel we can add
                int maxCanAdd = maxFuelCapacity - elytraData.getFuel();
                int fuelPerItem = fuelItem.getFuelValue();
                int itemsNeeded = Math.min(available, (maxCanAdd + fuelPerItem - 1) / fuelPerItem);

                if (itemsNeeded > 0) {
                    // Remove items from inventory
                    removeItems(player, fuelMaterial, itemsNeeded);

                    // Add fuel
                    int fuelToAdd = itemsNeeded * fuelPerItem;
                    elytraData.addFuel(Math.min(fuelToAdd, maxCanAdd));

                    // Handle return items (like empty buckets)
                    if (fuelItem.getReturnItem() != null) {
                        addItems(player, fuelItem.getReturnItem(), itemsNeeded);
                    }

                    // Notify player
                    String message = plugin.getMessageUtil().getMessage("fuel.auto-refueled")
                            .replace("{amount}", String.valueOf(fuelToAdd))
                            .replace("{item}", fuelItem.getDisplayName());
                    plugin.getMessageUtil().sendMessage(player, message);

                    return; // Stop after first successful refuel
                }
            }
        }
    }


    private boolean hasEnoughItems(Player player, Material material, int amount) {
        return countItems(player, material) >= amount;
    }

    private int countItems(Player player, Material material) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }



    private void removeItems(Player player, Material material, int amount) {
        int remaining = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material && remaining > 0) {
                int itemAmount = item.getAmount();
                if (itemAmount <= remaining) {
                    remaining -= itemAmount;
                    item.setAmount(0);
                } else {
                    item.setAmount(itemAmount - remaining);
                    remaining = 0;
                }
            }
        }
    }

    private void addItems(Player player, Material material, int amount) {
        ItemStack itemsToAdd = new ItemStack(material, amount);
        HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(itemsToAdd);

        // Drop overflow items
        for (ItemStack item : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        }
    }

    public FuelItem getFuelItem(Material material) {
        return fuelItems.get(material);
    }

    public Collection<FuelItem> getAllFuelItems() {
        return new ArrayList<>(fuelItems.values());
    }

    public boolean isFuelItem(Material material) {
        return fuelItems.containsKey(material);
    }

    public void reload() {
        loadConfiguration();
        lastFuelWarning.clear();
    }

    public void cleanup() {
        fuelItems.clear();
        lastFuelWarning.clear();
    }
}

