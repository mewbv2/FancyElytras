package io.mewb.fancyElytras.listeners;

import io.mewb.fancyElytras.FancyElytras;
import io.mewb.fancyElytras.data.ElytraData;
import io.mewb.fancyElytras.data.ParticleType;
import io.mewb.fancyElytras.gui.ParticleSelectionGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ElytraListener implements Listener {

    private final FancyElytras plugin;
    private final Map<UUID, BukkitRunnable> takeoffCountdowns;
    private final Map<UUID, Long> lastTakeoffAttempt;
    private final Map<UUID, Long> lastParticleSpawn;

    // Acceleration system tracking
    private final Map<UUID, Double> currentSpeedMultiplier;
    private final Map<UUID, BukkitTask> accelerationTasks;
    private final Map<UUID, Boolean> isAccelerating;

    public ElytraListener(FancyElytras plugin) {
        this.plugin = plugin;
        this.takeoffCountdowns = new HashMap<>();
        this.lastTakeoffAttempt = new HashMap<>();
        this.lastParticleSpawn = new HashMap<>();
        this.currentSpeedMultiplier = new HashMap<>();
        this.accelerationTasks = new HashMap<>();
        this.isAccelerating = new HashMap<>();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() != Material.ELYTRA) {
            return;
        }

        // Check if it's a fancy elytra
        ElytraData elytraData = plugin.getElytraManager().getElytraData(item);
        if (elytraData == null) return;

        // Right-click handling
        if (event.getAction().toString().contains("RIGHT_CLICK")) {
            event.setCancelled(true);

            // Check if player is holding fuel item
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (mainHand != null && plugin.getFuelManager().isFuelItem(mainHand.getType())) {
                // Attempt to refuel with held item
                if (plugin.getFuelManager().refuelElytra(player, elytraData, mainHand.getType(), 1)) {
                    // Update the elytra item
                    plugin.getElytraManager().setElytraData(item, elytraData);
                    plugin.getMessageUtil().sendMessage(player, "fuel.refueled");
                } else {
                    plugin.getMessageUtil().sendMessage(player, "fuel.refuel-failed");
                }
                return;
            }

            // If not holding fuel, open particle GUI
            if (!player.hasPermission("fancyelytra.particles")) {
                plugin.getMessageUtil().sendMessage(player, "general.no-permission");
                return;
            }

            new ParticleSelectionGUI(plugin, player, item).open();
        }
    }


    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        ItemStack chestplate = player.getInventory().getChestplate();
        if (chestplate == null || chestplate.getType() != Material.ELYTRA) {
            return;
        }

        ElytraData elytraData = plugin.getElytraManager().getElytraData(chestplate);
        if (elytraData == null) return;

        if (player.isGliding()) {
            // Handle acceleration while gliding
            if (event.isSneaking()) {
                // Start acceleration
                startAcceleration(player, elytraData);
            } else {
                // Stop acceleration
                stopAcceleration(player);
            }
        } else {
            // Handle takeoff when not gliding
            if (event.isSneaking()) {
                // Check if player has permission
                if (!player.hasPermission("fancyelytra.takeoff")) {
                    plugin.getMessageUtil().sendMessage(player, "general.no-permission");
                    return;
                }

                // Check if world is disabled
                if (isWorldDisabled(player)) {
                    plugin.getMessageUtil().sendMessage(player, "general.world-disabled");
                    return;
                }

                // Check takeoff cooldown
                if (hasTakeoffCooldown(player)) {
                    long remaining = getTakeoffCooldownRemaining(player);
                    String message = plugin.getMessageUtil().getMessage("elytra.takeoff.cooldown")
                            .replace("{time}", String.valueOf(remaining / 1000));
                    plugin.getMessageUtil().sendMessage(player, message);
                    return;
                }


                // Check if there's enough space above
                int minSpace = plugin.getConfigManager().getGeneralConfig().getInt("takeoff.min-space-above", 3);
                if (!hasEnoughSpaceAbove(player, minSpace)) {
                    plugin.getMessageUtil().sendMessage(player, "elytra.takeoff.insufficient-space");
                    return;
                }

                // Check fuel requirements
                int minFuelForTakeoff = plugin.getConfigManager().getFuelConfig().getInt("fuel.consumption.min-fuel-for-takeoff", 20);
                if (elytraData.getFuel() < minFuelForTakeoff) {
                    plugin.getMessageUtil().sendMessage(player, "fuel.insufficient-for-takeoff");
                    return;
                }

                // Start takeoff sequence
                startTakeoffSequence(player, elytraData);
            } else {
                // Cancel takeoff if player releases shift
                cancelTakeoff(player);
            }
        }
    }

    private void startAcceleration(Player player, ElytraData elytraData) {
        UUID playerId = player.getUniqueId();

        // Mark as accelerating
        isAccelerating.put(playerId, true);

        // Initialize speed multiplier if not exists
        currentSpeedMultiplier.putIfAbsent(playerId, 1.0);

        // Cancel existing acceleration task
        BukkitTask existingTask = accelerationTasks.get(playerId);
        if (existingTask != null) {
            existingTask.cancel();
        }

        // Get configuration values
        double maxSpeed = plugin.getConfigManager().getFuelConfig().getDouble("acceleration.max-speed-multiplier", 2.5);
        double accelerationRate = plugin.getConfigManager().getFuelConfig().getDouble("acceleration.acceleration-rate", 0.05);
        double fuelMultiplier = plugin.getConfigManager().getFuelConfig().getDouble("acceleration.fuel-consumption-multiplier", 1.2);

        // Start acceleration task
        BukkitTask accelerationTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !player.isGliding() || !isAccelerating.getOrDefault(playerId, false)) {
                    cancel();
                    return;
                }

                // Check fuel
                if (elytraData.getFuel() <= 0) {
                    cancel();
                    return;
                }

                // Get current speed multiplier
                double currentSpeed = currentSpeedMultiplier.getOrDefault(playerId, 1.0);

                // Increase speed gradually
                if (currentSpeed < maxSpeed) {
                    currentSpeed = Math.min(maxSpeed, currentSpeed + accelerationRate);
                    currentSpeedMultiplier.put(playerId, currentSpeed);
                }

                // Apply speed boost
                applySpeedBoost(player, currentSpeed);

                // Consume fuel based on speed
                double baseFuelConsumption = plugin.getConfigManager().getFuelConfig().getDouble("fuel.consumption.flight-per-tick", 0.2);
                double speedBasedConsumption = baseFuelConsumption * (1 + (currentSpeed - 1.0) * fuelMultiplier);

                // Convert to int and consume fuel
                int fuelToConsume = (int) Math.ceil(speedBasedConsumption);
                if (fuelToConsume > 0) {
                    elytraData.consumeFuel(fuelToConsume);

                    // Update elytra item - Fixed method call
                    plugin.getElytraManager().setElytraData(player.getInventory().getChestplate(), elytraData);
                }

                // Show speed indicator (optional visual feedback)
                if (currentSpeed > 1.1) { // Only show when noticeably faster
                    String speedBar = createSpeedBar(currentSpeed, maxSpeed);
                    player.sendActionBar("§a▶ §7Speed: " + speedBar);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L); // Run every tick

        accelerationTasks.put(playerId, accelerationTask);
    }

    private void stopAcceleration(Player player) {
        UUID playerId = player.getUniqueId();

        // Mark as not accelerating
        isAccelerating.put(playerId, false);

        // Start deceleration
        startDeceleration(player);
    }

    private void startDeceleration(Player player) {
        UUID playerId = player.getUniqueId();

        // Cancel existing acceleration task
        BukkitTask existingTask = accelerationTasks.get(playerId);
        if (existingTask != null) {
            existingTask.cancel();
        }

        double decelerationRate = plugin.getConfigManager().getFuelConfig().getDouble("acceleration.deceleration-rate", 0.03);
        double minSpeed = plugin.getConfigManager().getFuelConfig().getDouble("acceleration.min-speed-multiplier", 1.0);

        // Start deceleration task
        BukkitTask decelerationTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !player.isGliding()) {
                    // Reset speed and cancel
                    currentSpeedMultiplier.put(playerId, minSpeed);
                    cancel();
                    return;
                }

                // If player started accelerating again, stop deceleration
                if (isAccelerating.getOrDefault(playerId, false)) {
                    cancel();
                    return;
                }

                // Get current speed multiplier
                double currentSpeed = currentSpeedMultiplier.getOrDefault(playerId, minSpeed);

                // Decrease speed gradually
                if (currentSpeed > minSpeed) {
                    currentSpeed = Math.max(minSpeed, currentSpeed - decelerationRate);
                    currentSpeedMultiplier.put(playerId, currentSpeed);

                    // Apply speed boost (or remove it)
                    applySpeedBoost(player, currentSpeed);
                } else {
                    // Reached minimum speed, stop task
                    currentSpeedMultiplier.put(playerId, minSpeed);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L); // Run every tick

        accelerationTasks.put(playerId, decelerationTask);
    }

    private void applySpeedBoost(Player player, double speedMultiplier) {
        Vector velocity = player.getVelocity();

        // Only boost if player is moving forward
        if (velocity.length() > 0.1) {
            // Calculate boost based on current velocity direction
            Vector direction = velocity.normalize();
            double currentSpeed = velocity.length();

            // Apply speed multiplier
            Vector newVelocity = direction.multiply(currentSpeed * speedMultiplier);

            // Cap maximum velocity to prevent excessive speed
            double maxVelocity = 3.0; // Adjust as needed
            if (newVelocity.length() > maxVelocity) {
                newVelocity = newVelocity.normalize().multiply(maxVelocity);
            }

            player.setVelocity(newVelocity);
        }
    }

    private String createSpeedBar(double currentSpeed, double maxSpeed) {
        int barLength = 10;
        double percentage = (currentSpeed - 1.0) / (maxSpeed - 1.0);
        int filledBars = (int) (percentage * barLength);

        StringBuilder bar = new StringBuilder("§a");
        for (int i = 0; i < barLength; i++) {
            if (i < filledBars) {
                bar.append("█");
            } else {
                bar.append("§7░");
            }
        }
        return bar.toString();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityToggleGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        UUID playerId = player.getUniqueId();

        if (!event.isGliding()) {
            // Player stopped gliding - clean up acceleration
            cleanupPlayerAcceleration(playerId);

            // Stop particle trail
            stopParticleTrail(player);
        } else {
            // Player started gliding - start particle trail
            startParticleTrail(player);
        }
    }

    private void cleanupPlayerAcceleration(UUID playerId) {
        // Cancel acceleration task
        BukkitTask task = accelerationTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }

        // Reset values
        currentSpeedMultiplier.remove(playerId);
        isAccelerating.remove(playerId);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (!player.isGliding()) return;

        ItemStack chestplate = player.getInventory().getChestplate();
        if (chestplate == null || chestplate.getType() != Material.ELYTRA) return;

        ElytraData elytraData = plugin.getElytraManager().getElytraData(chestplate);
        if (elytraData == null) return;

        // Regular fuel consumption for basic flight
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        // Only consume fuel every few ticks to reduce consumption
        Long lastConsumption = lastParticleSpawn.get(playerId);
        if (lastConsumption == null || currentTime - lastConsumption > 50) { // Every 50ms (1 tick)

            // Store previous fuel for comparison
            int previousFuel = elytraData.getFuel();

            // Only consume fuel if not accelerating (acceleration handles its own fuel consumption)
            if (!isAccelerating.getOrDefault(playerId, false)) {
                double baseFuelConsumption = plugin.getConfigManager().getFuelConfig().getDouble("fuel.consumption.flight-per-tick", 0.2);
                int fuelToConsume = (int) Math.ceil(baseFuelConsumption);

                if (fuelToConsume > 0) {
                    elytraData.consumeFuel(fuelToConsume);
                }
            }

            // Try auto-refuel if fuel was consumed
            if (elytraData.getFuel() < previousFuel) {
                plugin.getFuelManager().attemptAutoRefuel(player, elytraData);
            }

            // Check for fuel warnings
            plugin.getFuelManager().checkFuelWarnings(player, elytraData);

            // Update the elytra item
            plugin.getElytraManager().setElytraData(chestplate, elytraData);

            lastParticleSpawn.put(playerId, currentTime);
        }
    }


    private void startParticleTrail(Player player) {
        plugin.getParticleManager().startParticleTrail(player);
    }

    private void stopParticleTrail(Player player) {
        plugin.getParticleManager().stopParticleTrail(player);
    }

    private void startTakeoffSequence(Player player, ElytraData elytraData) {
        UUID playerId = player.getUniqueId();

        // Cancel existing countdown
        cancelTakeoff(player);

        // Check fuel requirements
        int takeoffCost = plugin.getConfigManager().getFuelConfig().getInt("fuel.consumption.takeoff-cost", 5);
        if (elytraData.getFuel() < takeoffCost) {
            plugin.getMessageUtil().sendMessage(player, "fuel.insufficient-for-takeoff");
            return;
        }

        // Start countdown
        player.sendMessage("§6⟨ §7Takeoff in §a3 §7seconds... §6⟩");

        BukkitRunnable countdown = new BukkitRunnable() {
            int count = 3;

            @Override
            public void run() {
                if (!player.isOnline() || player.isGliding()) {
                    cancel();
                    return;
                }

                count--;
                if (count > 0) {
                    player.sendMessage("§6⟨ §7Takeoff in §a" + count + " §7seconds... §6⟩");
                } else {
                    player.sendMessage("§6⟨ §a§lTAKEOFF! §6⟩");
                    performTakeoff(player, elytraData);
                    cancel();
                }
            }
        };

        countdown.runTaskTimer(plugin, 20L, 20L);
        takeoffCountdowns.put(playerId, countdown);
    }

    private void performTakeoff(Player player, ElytraData elytraData) {
        // Consume takeoff fuel
        int takeoffCost = plugin.getConfigManager().getFuelConfig().getInt("fuel.consumption.takeoff-cost", 5);
        elytraData.consumeFuel(takeoffCost);

        // Apply upward velocity
        Vector velocity = new Vector(0, 1.5, 0);
        player.setVelocity(velocity);

        // Start gliding
        player.setGliding(true);

        // Update elytra item - Fixed method call
        plugin.getElytraManager().setElytraData(player.getInventory().getChestplate(), elytraData);

        // Set cooldown
        lastTakeoffAttempt.put(player.getUniqueId(), System.currentTimeMillis());

        // Play sound
        playSound(player, "takeoff");
    }

    private void cancelTakeoff(Player player) {
        UUID playerId = player.getUniqueId();
        BukkitRunnable countdown = takeoffCountdowns.remove(playerId);
        if (countdown != null) {
            countdown.cancel();
            player.sendMessage("§c⟨ §7Takeoff cancelled §c⟩");
        }
    }

    private boolean hasTakeoffCooldown(Player player) {
        Long lastAttempt = lastTakeoffAttempt.get(player.getUniqueId());
        if (lastAttempt == null) return false;

        long cooldown = plugin.getConfigManager().getGeneralConfig().getLong("takeoff.cooldown", 5000);
        return System.currentTimeMillis() - lastAttempt < cooldown;
    }

    private long getTakeoffCooldownRemaining(Player player) {
        Long lastAttempt = lastTakeoffAttempt.get(player.getUniqueId());
        if (lastAttempt == null) return 0;

        long cooldown = plugin.getConfigManager().getGeneralConfig().getLong("takeoff.cooldown", 5000);
        return Math.max(0, cooldown - (System.currentTimeMillis() - lastAttempt));
    }

    private boolean hasEnoughSpaceAbove(Player player, int minSpace) {
        for (int i = 1; i <= minSpace; i++) {
            if (!player.getLocation().add(0, i, 0).getBlock().getType().isAir()) {
                return false;
            }
        }
        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onElytraDamage(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (!player.isGliding()) return;

        ItemStack chestplate = player.getInventory().getChestplate();
        if (chestplate == null || chestplate.getType() != Material.ELYTRA) return;

        ElytraData elytraData = plugin.getElytraManager().getElytraData(chestplate);
        if (elytraData == null) return;

        // Prevent durability damage if fuel system is enabled
        if (plugin.getFuelManager().isFuelSystemEnabled()) {
            // Reset durability to prevent damage
            if (chestplate.getType().getMaxDurability() > 0) {
                short currentDurability = chestplate.getDurability();
                if (currentDurability > 0) {
                    chestplate.setDurability((short) 0);
                }
            }
        }
    }


    private void playSound(Player player, String soundName) {
        // Implementation for playing sounds based on config
        // This would use the sound configuration from your config files
    }

    private boolean isWorldDisabled(Player player) {
        // Check if elytras are disabled in this world
        return plugin.getConfigManager().getGeneralConfig().getStringList("disabled-worlds")
                .contains(player.getWorld().getName());
    }

    public void cleanup() {
        // Cancel all running tasks
        takeoffCountdowns.values().forEach(BukkitRunnable::cancel);
        accelerationTasks.values().forEach(BukkitTask::cancel);

        // Clear all maps
        takeoffCountdowns.clear();
        lastTakeoffAttempt.clear();
        lastParticleSpawn.clear();
        currentSpeedMultiplier.clear();
        accelerationTasks.clear();
        isAccelerating.clear();
    }
}