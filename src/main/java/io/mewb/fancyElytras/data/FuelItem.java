package io.mewb.fancyElytras.data;

import org.bukkit.Material;

public class FuelItem {

    private final Material material;
    private final int fuelValue;
    private final String displayName;
    private final Material returnItem;

    public FuelItem(Material material, int fuelValue, String displayName, Material returnItem) {
        this.material = material;
        this.fuelValue = fuelValue;
        this.displayName = displayName;
        this.returnItem = returnItem;
    }

    public Material getMaterial() {
        return material;
    }

    public int getFuelValue() {
        return fuelValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getReturnItem() {
        return returnItem;
    }

    public boolean hasReturnItem() {
        return returnItem != null;
    }

    @Override
    public String toString() {
        return "FuelItem{" +
                "material=" + material +
                ", fuelValue=" + fuelValue +
                ", displayName='" + displayName + '\'' +
                ", returnItem=" + returnItem +
                '}';
    }
}