package io.mewb.fancyElytras.data;

public class ElytraData {

    private String particleType;
    private int fuel;
    private int maxFuel;
    private long lastUsed;
    private boolean modified;

    public ElytraData(String particleType, int fuel, int maxFuel) {
        this.particleType = particleType;
        this.fuel = fuel;
        this.maxFuel = maxFuel;
        this.lastUsed = System.currentTimeMillis();
        this.modified = false;
    }

    public String getParticleType() {
        return particleType;
    }

    public void setParticleType(String particleType) {
        // Fix: Handle null values properly
        if ((this.particleType == null && particleType != null) ||
                (this.particleType != null && !this.particleType.equals(particleType))) {
            this.particleType = particleType;
            this.modified = true;
        }
    }




    public int getFuel() {
        return fuel;
    }

    public void setFuel(int fuel) {
        if (this.fuel != fuel) {
            this.fuel = Math.max(0, Math.min(fuel, maxFuel));
            this.modified = true;
        }
    }

    public int getMaxFuel() {
        return maxFuel;
    }

    public void setMaxFuel(int maxFuel) {
        if (this.maxFuel != maxFuel) {
            this.maxFuel = Math.max(1, maxFuel);
            this.fuel = Math.min(this.fuel, this.maxFuel);
            this.modified = true;
        }
    }

    public long getLastUsed() {
        return lastUsed;
    }

    public void updateLastUsed() {
        this.lastUsed = System.currentTimeMillis();
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public boolean consumeFuel(int amount) {
        if (fuel >= amount) {
            setFuel(fuel - amount);
            updateLastUsed();
            return true;
        }
        return false;
    }

    public void addFuel(int amount) {
        setFuel(fuel + amount);
    }

    public boolean hasFuel() {
        return fuel > 0;
    }

    public boolean hasEnoughFuel(int required) {
        return fuel >= required;
    }

    public double getFuelPercentage() {
        return maxFuel > 0 ? (double) fuel / maxFuel : 0.0;
    }

    public boolean isFuelLow(int threshold) {
        return fuel <= threshold;
    }

    public boolean isFuelCritical(int threshold) {
        return fuel <= threshold;
    }

    @Override
    public String toString() {
        return "ElytraData{" +
                "particleType='" + particleType + '\'' +
                ", fuel=" + fuel +
                ", maxFuel=" + maxFuel +
                ", modified=" + modified +
                '}';
    }

    @Override
    public ElytraData clone() {
        ElytraData clone = new ElytraData(particleType, fuel, maxFuel);
        clone.lastUsed = this.lastUsed;
        clone.modified = this.modified;
        return clone;
    }
}