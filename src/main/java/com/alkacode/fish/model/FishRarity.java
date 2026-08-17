package com.alkacode.fish.model;

import net.kyori.adventure.text.format.TextColor;

/** Raridades dos peixes do AlkaFish, com multiplicador de preço e chance base. */
public enum FishRarity {
    COMMON("Comum", TextColor.fromHexString("#7F7F7F"), 1.0, 0.50),
    UNCOMMON("Incomum", TextColor.fromHexString("#55FF55"), 1.5, 0.30),
    RARE("Raro", TextColor.fromHexString("#5555FF"), 2.5, 0.15),
    EPIC("Épico", TextColor.fromHexString("#AA00AA"), 4.0, 0.04),
    LEGENDARY("Lendário", TextColor.fromHexString("#FFAA00"), 7.0, 0.009),
    MYTHIC("Mítico", TextColor.fromHexString("#FF5555"), 15.0, 0.001);

    private final String displayName;
    private final TextColor color;
    private final double priceMultiplier;
    private final double baseChance;

    FishRarity(String displayName, TextColor color, double priceMultiplier, double baseChance) {
        this.displayName = displayName;
        this.color = color;
        this.priceMultiplier = priceMultiplier;
        this.baseChance = baseChance;
    }

    public String getDisplayName() { return displayName; }
    public TextColor getColor() { return color; }
    public double getPriceMultiplier() { return priceMultiplier; }
    public double getBaseChance() { return baseChance; }

    /** Tag MiniMessage com a cor da raridade, ex: "<#FFAA00>Lendário</#FFAA00>". */
    public String coloredName() {
        return "<#" + color.asHexString().substring(1) + ">" + displayName + "</#" + color.asHexString().substring(1) + ">";
    }

    /** Material do item que representa um peixe desta raridade (CodexGui). */
    public org.bukkit.Material getDisplayMaterial() {
        return switch (this) {
            case COMMON -> org.bukkit.Material.COD;
            case UNCOMMON -> org.bukkit.Material.SALMON;
            case RARE -> org.bukkit.Material.PUFFERFISH;
            case EPIC -> org.bukkit.Material.TROPICAL_FISH;
            case LEGENDARY -> org.bukkit.Material.GOLDEN_APPLE;
            case MYTHIC -> org.bukkit.Material.NETHER_STAR;
        };
    }

    /** Material do separador de raridade (CodexGui). */
    public org.bukkit.Material getSeparatorMaterial() {
        return switch (this) {
            case COMMON -> org.bukkit.Material.GRAY_STAINED_GLASS_PANE;
            case UNCOMMON -> org.bukkit.Material.LIME_STAINED_GLASS_PANE;
            case RARE -> org.bukkit.Material.BLUE_STAINED_GLASS_PANE;
            case EPIC -> org.bukkit.Material.MAGENTA_STAINED_GLASS_PANE;
            case LEGENDARY -> org.bukkit.Material.YELLOW_STAINED_GLASS_PANE;
            case MYTHIC -> org.bukkit.Material.RED_STAINED_GLASS_PANE;
        };
    }
}
