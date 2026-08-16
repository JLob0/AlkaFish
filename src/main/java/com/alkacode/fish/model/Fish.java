package com.alkacode.fish.model;

import com.alkacode.fish.AlkaFishPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/** Definição de um peixe carregado do fish.yml. */
public class Fish {
    private final String id;
    private final String displayName;
    private final FishRarity rarity;
    private final List<String> biomes;
    private final double minLength;
    private final double maxLength;
    private final double minWeight;
    private final double maxWeight;
    private final double basePrice;
    private final String itemsAdderId;
    private final int customModelData;
    private final boolean requiresNight;
    private final boolean requiresRain;
    private final int minDepth;
    private final double xpReward;
    private final List<String> lore;

    public Fish(String id, String displayName, FishRarity rarity, List<String> biomes,
                double minLength, double maxLength, double minWeight, double maxWeight,
                double basePrice, String itemsAdderId, int customModelData,
                boolean requiresNight, boolean requiresRain, int minDepth,
                double xpReward, List<String> lore) {
        this.id = id;
        this.displayName = displayName;
        this.rarity = rarity;
        this.biomes = biomes;
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.minWeight = minWeight;
        this.maxWeight = maxWeight;
        this.basePrice = basePrice;
        this.itemsAdderId = itemsAdderId;
        this.customModelData = customModelData;
        this.requiresNight = requiresNight;
        this.requiresRain = requiresRain;
        this.minDepth = minDepth;
        this.xpReward = xpReward;
        this.lore = lore;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public FishRarity getRarity() { return rarity; }
    public List<String> getBiomes() { return biomes; }
    public double getMinLength() { return minLength; }
    public double getMaxLength() { return maxLength; }
    public double getMinWeight() { return minWeight; }
    public double getMaxWeight() { return maxWeight; }
    public double getBasePrice() { return basePrice; }
    public String getItemsAdderId() { return itemsAdderId; }
    public int getCustomModelData() { return customModelData; }
    public boolean isRequiresNight() { return requiresNight; }
    public boolean isRequiresRain() { return requiresRain; }
    public int getMinDepth() { return minDepth; }
    public double getXpReward() { return xpReward; }
    public List<String> getLore() { return lore; }

    public double calculatePrice(double length) {
        return basePrice + (length * rarity.getPriceMultiplier());
    }

    public ItemStack toItemStack(AlkaFishPlugin plugin, double length, double weight) {
        ItemStack item;

        if (itemsAdderId != null && !itemsAdderId.isEmpty() && plugin.getItemsAdderHook() != null) {
            item = plugin.getItemsAdderHook().getItem(itemsAdderId);
            if (item == null) {
                item = new ItemStack(org.bukkit.Material.COD);
            }
        } else {
            item = new ItemStack(org.bukkit.Material.COD);
        }

        ItemStack finalItem = item;
        finalItem.editMeta(meta -> {
            meta.displayName(com.alkacode.fish.util.FishUtil.parse(displayName));
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "alkafish_id"), PersistentDataType.STRING, id);
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "alkafish_length"), PersistentDataType.DOUBLE, length);
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "alkafish_weight"), PersistentDataType.DOUBLE, weight);
            if (customModelData > 0) {
                meta.setCustomModelData(customModelData);
            }
            meta.lore(lore.stream().map(com.alkacode.fish.util.FishUtil::parse).toList());
        });

        return finalItem;
    }
}
