package com.alkacode.fish.model;

import org.bukkit.inventory.ItemStack;

import java.util.Map;

/** Definição de uma isca carregada do baits.yml. */
public class Bait {
    private final String id;
    private final String displayName;
    private final ItemStack baseItem;
    private final int durationSeconds;
    private final int radius;
    private final Map<FishRarity, Double> rarityBonus;
    private final double totalLuckBonus;

    public Bait(String id, String displayName, ItemStack baseItem, int durationSeconds,
                int radius, Map<FishRarity, Double> rarityBonus, double totalLuckBonus) {
        this.id = id;
        this.displayName = displayName;
        this.baseItem = baseItem;
        this.durationSeconds = durationSeconds;
        this.radius = radius;
        this.rarityBonus = rarityBonus;
        this.totalLuckBonus = totalLuckBonus;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public ItemStack getBaseItem() { return baseItem; }
    public int getDurationSeconds() { return durationSeconds; }
    public int getRadius() { return radius; }
    public Map<FishRarity, Double> getRarityBonus() { return rarityBonus; }
    public double getTotalLuckBonus() { return totalLuckBonus; }
}
