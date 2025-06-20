
package io.mewb.fancyElytras.managers;

import io.mewb.fancyElytras.FancyElytras;
import io.mewb.fancyElytras.data.ElytraData;
import io.mewb.fancyElytras.data.ParticleType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ParticleManager {

    private final FancyElytras plugin;
    private final Map<String, ParticleType> particleTypes;
    private final Map<UUID, BukkitTask> activeTrails;
    private final Map<UUID, Long> lastParticleSpawn;
    private final AtomicInteger currentParticleCount;
    private final AtomicInteger totalParticlesSpawned;

    public ParticleManager(FancyElytras plugin) {
        this.plugin = plugin;
        this.particleTypes = new HashMap<>();
        this.activeTrails = new ConcurrentHashMap<>();
        this.lastParticleSpawn = new ConcurrentHashMap<>();
        this.currentParticleCount = new AtomicInteger(0);
        this.totalParticlesSpawned = new AtomicInteger(0);
        loadParticleTypes();
    }

    public void loadParticleTypes() {
        particleTypes.clear();

        if (plugin.getConfigManager() == null || plugin.getConfigManager().getParticleConfig() == null) {
            plugin.getLogger().severe("Config manager or particle config is null!");
            return;
        }

        ConfigurationSection particlesSection = plugin.getConfigManager().getParticleConfig()
                .getConfigurationSection("particles");

        if (particlesSection == null) {
            plugin.getLogger().warning("No particles section found in particle config!");
            createDefaultParticles();
            return;
        }

        for (String key : particlesSection.getKeys(false)) {
            try {
                ConfigurationSection particleSection = particlesSection.getConfigurationSection(key);
                if (particleSection == null) {
                    continue;
                }

                boolean enabled = particleSection.getBoolean("enabled", true);
                if (!enabled) {
                    continue;
                }

                String displayName = particleSection.getString("display-name", key);
                String permission = particleSection.getString("permission", "fancyelytra.particle." + key.toLowerCase());
                String guiItem = particleSection.getString("gui-item", "BLAZE_POWDER");
                List<String> description = particleSection.getStringList("description");
                int spawnRate = particleSection.getInt("spawn-rate", 1);

                // For custom particles, use a default Bukkit particle (we'll handle the custom logic in spawnSingleParticle)
                Particle bukkitParticle = getDefaultParticleForCustom(key);

                String internalKey = key.toLowerCase();
                ParticleType particleType = new ParticleType(internalKey, bukkitParticle, displayName, description, guiItem, permission, spawnRate);

                particleTypes.put(internalKey, particleType);

            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load particle type " + key + ": " + e.getMessage());
            }
        }

        plugin.getLogger().info("Loaded " + particleTypes.size() + " particle types");

        if (particleTypes.isEmpty()) {
            createDefaultParticles();
        }
    }


    private void createDefaultParticles() {
        ParticleType flame = new ParticleType(
                "flame",
                Particle.FLAME,
                "§cFlame",
                List.of("Classic flame particles"),
                "BLAZE_POWDER",
                "fancyelytra.particle.flame",
                1
        );

        ParticleType heart = new ParticleType(
                "heart",
                Particle.HEART,
                "§dHeart",
                List.of("Lovely heart particles"),
                "RED_DYE",
                "fancyelytra.particle.heart",
                1
        );

        ParticleType portal = new ParticleType(
                "portal",
                Particle.PORTAL,
                "§5Portal",
                List.of("Mysterious portal particles"),
                "ENDER_PEARL",
                "fancyelytra.particle.portal",
                1
        );

        particleTypes.put("flame", flame);
        particleTypes.put("heart", heart);
        particleTypes.put("portal", portal);

        plugin.getLogger().info("Created " + particleTypes.size() + " default particles");
    }

    public void reload() {
        stopAllTrails();
        loadParticleTypes();
        plugin.getLogger().info("ParticleManager reloaded successfully!");
    }

    public String getDefaultParticle() {
        String defaultParticle = plugin.getConfigManager().getGeneralConfig()
                .getString("particles.default-particle", "flame").toLowerCase();

        if (particleTypes.containsKey(defaultParticle)) {
            return defaultParticle;
        }

        if (!particleTypes.isEmpty()) {
            return particleTypes.keySet().iterator().next();
        }

        return "none";
    }

    public void resetParticleCount() {
        currentParticleCount.set(0);
    }

    public int getActiveParticleCount() {
        return currentParticleCount.get();
    }

    public double getAverageParticlesPerPlayer() {
        int onlinePlayers = plugin.getServer().getOnlinePlayers().size();
        if (onlinePlayers == 0) return 0.0;
        return (double) currentParticleCount.get() / onlinePlayers;
    }

    public int getTotalParticlesSpawned() {
        return totalParticlesSpawned.get();
    }

    public void spawnParticleEffect(Player player, String particleTypeName) {
        ParticleType particleType = particleTypes.get(particleTypeName);
        if (particleType != null && hasParticlePermission(player, particleTypeName)) {
            spawnParticles(player, particleType);
        }
    }

    public void spawnParticleEffect(Player player, ParticleType particleType) {
        if (particleType != null && hasParticlePermission(player, particleType.getName())) {
            spawnParticles(player, particleType);
        }
    }

    public boolean isValidParticleType(String particleTypeName) {
        return particleTypes.containsKey(particleTypeName);
    }

    public Set<String> getParticleTypeNames() {
        return new HashSet<>(particleTypes.keySet());
    }

    public void startParticleTrail(Player player) {
        UUID playerId = player.getUniqueId();

        // Stop any existing trail
        stopParticleTrail(player);

        // Get player's elytra data
        ItemStack elytra = plugin.getElytraManager().getPlayerElytra(player);
        if (elytra == null) {
            return;
        }

        ElytraData elytraData = plugin.getElytraManager().getElytraData(elytra);
        if (elytraData == null) {
            return;
        }

        String particleTypeName = elytraData.getParticleType();
        if (particleTypeName == null || "NONE".equalsIgnoreCase(particleTypeName)) {
            return;
        }

        particleTypeName = particleTypeName.toLowerCase();
        ParticleType particleType = particleTypes.get(particleTypeName);
        if (particleType == null || !hasParticlePermission(player, particleTypeName)) {
            return;
        }

        // Start continuous particle trail
        BukkitTask trailTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Check if player is still online and gliding
                if (!player.isOnline() || !player.isGliding()) {
                    stopParticleTrail(player);
                    return;
                }

                // Check if player still has the elytra
                ItemStack currentElytra = plugin.getElytraManager().getPlayerElytra(player);
                if (currentElytra == null) {
                    stopParticleTrail(player);
                    return;
                }

                // Check fuel if required
                ElytraData currentData = plugin.getElytraManager().getElytraData(currentElytra);
                if (currentData != null && !currentData.hasFuel()) {
                    boolean showWithoutFuel = plugin.getConfigManager().getGeneralConfig()
                            .getBoolean("elytra.show-particles-without-fuel", false);
                    if (!showWithoutFuel) {
                        return;
                    }
                }

                // Spawn particles
                spawnParticles(player, particleType);
            }
        }.runTaskTimer(plugin, 0L, getParticleInterval());

        activeTrails.put(playerId, trailTask);
        lastParticleSpawn.put(playerId, System.currentTimeMillis());
    }

    public void stopParticleTrail(Player player) {
        UUID playerId = player.getUniqueId();
        BukkitTask task = activeTrails.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        lastParticleSpawn.remove(playerId);
    }

    public void spawnParticleTrail(Player player, String particleTypeName) {
        ParticleType particleType = particleTypes.get(particleTypeName);
        if (particleType != null && hasParticlePermission(player, particleTypeName)) {
            spawnParticles(player, particleType);
        }
    }

    public void spawnParticleTrail(Player player, ParticleType particleType) {
        if (particleType != null && hasParticlePermission(player, particleType.getName())) {
            spawnParticles(player, particleType);
        }
    }

    private void spawnParticles(Player player, ParticleType particleType) {
        try {
            int trailLength = plugin.getConfigManager().getGeneralConfig()
                    .getInt("particles.trail-length", 3);
            double spacing = plugin.getConfigManager().getGeneralConfig()
                    .getDouble("particles.spacing", 0.5);

            Location playerLoc = player.getLocation();

            // Spawn particles behind the player
            for (int i = 1; i <= trailLength; i++) {
                Location particleLoc = playerLoc.clone();

                // Calculate position behind player based on their movement
                if (player.getVelocity().length() > 0.1) {
                    particleLoc = particleLoc.subtract(player.getVelocity().normalize().multiply(spacing * i));
                } else {
                    particleLoc = particleLoc.subtract(playerLoc.getDirection().multiply(spacing * i));
                }

                // Add some randomness to make it look more natural
                double offsetX = 0.1;
                double offsetY = 0.1;
                double offsetZ = 0.1;

                particleLoc.add(
                        (Math.random() - 0.5) * offsetX,
                        (Math.random() - 0.5) * offsetY,
                        (Math.random() - 0.5) * offsetZ
                );

                spawnSingleParticle(particleLoc, particleType);
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Error spawning particles for " + player.getName() + ": " + e.getMessage());
        }
    }

    private void spawnSingleParticle(Location location, ParticleType particleType) {
        try {
            String particleName = particleType.getName().toLowerCase();

            // Handle special spectacular effects
            switch (particleName) {
                case "firework":
                    spawnFireworkEffect(location, particleType);
                    break;
                case "galaxy":
                    spawnGalaxyEffect(location, particleType);
                    break;
                case "lightning":
                    spawnLightningEffect(location, particleType);
                    break;
                case "phoenix":
                    spawnPhoenixEffect(location, particleType);
                    break;
                case "void":
                    spawnVoidEffect(location, particleType);
                    break;
                case "aurora":
                    spawnAuroraEffect(location, particleType);
                    break;
                case "divine":
                    spawnDivineEffect(location, particleType);
                    break;
                case "storm":
                    spawnStormEffect(location, particleType);
                    break;
                case "crystal":
                    spawnCrystalEffect(location, particleType);
                    break;
                case "meteor":
                    spawnMeteorEffect(location, particleType);
                    break;
                default:
                    spawnStandardParticle(location, particleType);
                    break;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error spawning particle: " + e.getMessage());
        }
    }

    private void spawnStandardParticle(Location location, ParticleType particleType) {
        Particle particle = particleType.getBukkitParticle();

        ConfigurationSection particleConfig = plugin.getConfigManager().getParticleConfig()
                .getConfigurationSection("particles." + particleType.getName());

        int count = 1;
        double offsetX = 0.1;
        double offsetY = 0.1;
        double offsetZ = 0.1;
        double speed = 0.1;

        if (particleConfig != null) {
            count = particleConfig.getInt("count", 1);
            offsetX = particleConfig.getDouble("offset-x", 0.1);
            offsetY = particleConfig.getDouble("offset-y", 0.1);
            offsetZ = particleConfig.getDouble("offset-z", 0.1);
            speed = particleConfig.getDouble("speed", 0.1);
        }

        location.getWorld().spawnParticle(
                particle,
                location,
                count,
                offsetX,
                offsetY,
                offsetZ,
                speed
        );

        currentParticleCount.addAndGet(count);
        totalParticlesSpawned.addAndGet(count);
    }

    private Particle getDefaultParticleForCustom(String particleName) {
        // Map custom particles to a base Bukkit particle
        switch (particleName.toLowerCase()) {
            case "firework":
            case "lightning":
                return Particle.FIREWORKS_SPARK;
            case "galaxy":
            case "aurora":
            case "divine":
            case "crystal":
                return Particle.ENCHANTMENT_TABLE;
            case "phoenix":
            case "meteor":
                return Particle.FLAME;
            case "void":
                return Particle.PORTAL;
            case "storm":
                return Particle.CLOUD;
            default:
                // Try to parse as Bukkit particle first
                try {
                    return Particle.valueOf(particleName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return Particle.FLAME; // Default fallback
                }
        }
    }


        private long getParticleInterval() {
        return plugin.getConfigManager().getGeneralConfig()
                .getLong("particles.spawn-interval", 2L);
    }

    public boolean hasParticlePermission(Player player, String particleTypeName) {
        if (player.hasPermission("fancyelytra.particle.all") || player.hasPermission("fancyelytra.particle.*")) {
            return true;
        }

        ParticleType particleType = particleTypes.get(particleTypeName);
        if (particleType == null) {
            return false;
        }

        return player.hasPermission(particleType.getPermission());
    }

    public Collection<ParticleType> getAvailableParticles() {
        return particleTypes.values();
    }

    public List<ParticleType> getParticlesForPlayer(Player player) {
        List<ParticleType> availableParticles = new ArrayList<>();

        for (ParticleType particleType : particleTypes.values()) {
            if (hasParticlePermission(player, particleType.getName())) {
                availableParticles.add(particleType);
            }
        }

        return availableParticles;
    }



    private void spawnFireworkEffect(Location location, ParticleType particleType) {
        location.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, location, 5, 0.4, 0.3, 0.4, 0.1);

        if (Math.random() < 0.3) {
            location.getWorld().spawnParticle(Particle.FLAME, location, 2, 0.2, 0.2, 0.2, 0.05);
        }

        currentParticleCount.addAndGet(7);
        totalParticlesSpawned.addAndGet(7);
    }

    private void spawnGalaxyEffect(Location location, ParticleType particleType) {
        location.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, location, 3, 0.5, 0.3, 0.5, 0.02);
        location.getWorld().spawnParticle(Particle.PORTAL, location, 2, 0.3, 0.2, 0.3, 0.01);

        if (Math.random() < 0.2) {
            try {
                location.getWorld().spawnParticle(Particle.END_ROD, location, 1, 0.1, 0.1, 0.1, 0.01);
            } catch (Exception e) {
                location.getWorld().spawnParticle(Particle.SPELL, location, 1, 0.1, 0.1, 0.1, 0.01);
            }
        }

        currentParticleCount.addAndGet(6);
        totalParticlesSpawned.addAndGet(6);
    }

    private void spawnLightningEffect(Location location, ParticleType particleType) {
        location.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, location, 4, 0.3, 0.4, 0.3, 0.08);
        location.getWorld().spawnParticle(Particle.SPELL, location, 2, 0.2, 0.3, 0.2, 0.05);

        currentParticleCount.addAndGet(6);
        totalParticlesSpawned.addAndGet(6);
    }

    private void spawnPhoenixEffect(Location location, ParticleType particleType) {
        location.getWorld().spawnParticle(Particle.FLAME, location, 4, 0.4, 0.3, 0.4, 0.05);
        location.getWorld().spawnParticle(Particle.LAVA, location, 2, 0.3, 0.2, 0.3, 0.02);
        location.getWorld().spawnParticle(Particle.SMOKE_LARGE, location, 2, 0.5, 0.3, 0.5, 0.03);

        if (Math.random() < 0.4) {
            location.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, location, 2, 0.3, 0.2, 0.3, 0.06);
        }

        currentParticleCount.addAndGet(10);
        totalParticlesSpawned.addAndGet(10);
    }

    private void spawnVoidEffect(Location location, ParticleType particleType) {
        location.getWorld().spawnParticle(Particle.SMOKE_LARGE, location, 3, 0.3, 0.3, 0.3, 0.03);
        location.getWorld().spawnParticle(Particle.PORTAL, location, 2, 0.2, 0.2, 0.2, 0.02);

        currentParticleCount.addAndGet(5);
        totalParticlesSpawned.addAndGet(5);
    }

    private void spawnAuroraEffect(Location location, ParticleType particleType) {
        location.getWorld().spawnParticle(Particle.SPELL_MOB, location, 4, 0.4, 0.2, 0.4, 0.04);
        location.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, location, 2, 0.3, 0.1, 0.3, 0.02);

        currentParticleCount.addAndGet(6);
        totalParticlesSpawned.addAndGet(6);
    }

    private void spawnDivineEffect(Location location, ParticleType particleType) {
        location.getWorld().spawnParticle(Particle.SPELL, location, 3, 0.3, 0.4, 0.3, 0.06);
        location.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, location, 3, 0.2, 0.3, 0.2, 0.04);

        if (Math.random() < 0.3) {
            location.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, location, 2, 0.2, 0.2, 0.2, 0.05);
        }

        currentParticleCount.addAndGet(8);
        totalParticlesSpawned.addAndGet(8);
    }

    private void spawnStormEffect(Location location, ParticleType particleType) {
        location.getWorld().spawnParticle(Particle.CLOUD, location, 4, 0.5, 0.3, 0.5, 0.07);
        location.getWorld().spawnParticle(Particle.WATER_DROP, location, 3, 0.4, 0.2, 0.4, 0.03);

        currentParticleCount.addAndGet(7);
        totalParticlesSpawned.addAndGet(7);
    }

    private void spawnCrystalEffect(Location location, ParticleType particleType) {
        location.getWorld().spawnParticle(Particle.SPELL, location, 3, 0.3, 0.3, 0.3, 0.05);
        location.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, location, 2, 0.2, 0.2, 0.2, 0.03);

        currentParticleCount.addAndGet(5);
        totalParticlesSpawned.addAndGet(5);
    }

    private void spawnMeteorEffect(Location location, ParticleType particleType) {
        location.getWorld().spawnParticle(Particle.FLAME, location, 3, 0.3, 0.1, 0.3, 0.08);
        location.getWorld().spawnParticle(Particle.SMOKE_LARGE, location, 2, 0.4, 0.2, 0.4, 0.06);
        location.getWorld().spawnParticle(Particle.LAVA, location, 2, 0.2, 0.1, 0.2, 0.05);
        location.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, location, 2, 0.3, 0.2, 0.3, 0.07);

        currentParticleCount.addAndGet(9);
        totalParticlesSpawned.addAndGet(9);
    }

    public ParticleType getParticleType(String name) {
        return particleTypes.get(name);
    }

    public Collection<ParticleType> getAllParticleTypes() {
        return particleTypes.values();
    }

    public void cleanup() {
        for (BukkitTask task : activeTrails.values()) {
            task.cancel();
        }
        activeTrails.clear();
        lastParticleSpawn.clear();
    }

    public void stopAllTrails() {
        cleanup();
    }
}