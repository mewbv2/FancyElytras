
package io.mewb.fancyElytras.data;

import org.bukkit.Particle;
import java.util.List;

public class ParticleType {

    private final String name;
    private final Particle bukkitParticle;
    private final String displayName;
    private final List<String> description;
    private final String guiItem;
    private final String permission;
    private final int spawnRate;

    public ParticleType(String name, Particle bukkitParticle, String displayName,
                        List<String> description, String guiItem, String permission, int spawnRate) {
        this.name = name;
        this.bukkitParticle = bukkitParticle;
        this.displayName = displayName;
        this.description = description;
        this.guiItem = guiItem;
        this.permission = permission;
        this.spawnRate = spawnRate;
    }

    public String getName() {
        return name;
    }

    public Particle getBukkitParticle() {
        return bukkitParticle;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getDescription() {
        return description;
    }

    public String getGuiItem() {
        return guiItem;
    }

    public String getPermission() {
        return permission;
    }

    public int getSpawnRate() {
        return spawnRate;
    }

    @Override
    public String toString() {
        return "ParticleType{" +
                "name='" + name + '\'' +
                ", bukkitParticle=" + bukkitParticle +
                ", displayName='" + displayName + '\'' +
                '}';
    }
}