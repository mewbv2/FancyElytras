package io.mewb.fancyElytras.data;

import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private String defaultParticle;
    private final long createdAt;
    private long lastLogin;

    public PlayerData(UUID uuid, String defaultParticle, long createdAt, long lastLogin) {
        this.uuid = uuid;
        this.defaultParticle = defaultParticle;
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getDefaultParticle() {
        return defaultParticle;
    }

    public void setDefaultParticle(String defaultParticle) {
        this.defaultParticle = defaultParticle;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(long lastLogin) {
        this.lastLogin = lastLogin;
    }

    @Override
    public String toString() {
        return "PlayerData{" +
                "uuid=" + uuid +
                ", defaultParticle='" + defaultParticle + '\'' +
                ", createdAt=" + createdAt +
                ", lastLogin=" + lastLogin +
                '}';
    }
}