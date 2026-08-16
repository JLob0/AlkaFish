package com.alkacode.fish.model;

import com.alkacode.fish.util.FishUtil;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Map;

/** Definição de uma classe de armadura de pesca carregada do classes.yml. */
public class FishingClass {
    private final String id;
    private final int order;
    private final String displayName;
    private final Color armorColor;
    private final String permission;
    private final List<ClassEffect> effects;
    private final double fishChanceBonus;
    private final double coinBonus;
    private final double sellBonus;
    private final double priceCoins;
    private final double priceNacar;
    private final String activatorName;
    private final List<String> activatorLore;
    private final Map<String, MenuItem> menuItems;

    public FishingClass(String id, int order, String displayName, Color armorColor, String permission,
                        List<ClassEffect> effects, double fishChanceBonus, double coinBonus, double sellBonus,
                        double priceCoins, double priceNacar, String activatorName, List<String> activatorLore,
                        Map<String, MenuItem> menuItems) {
        this.id = id;
        this.order = order;
        this.displayName = displayName;
        this.armorColor = armorColor;
        this.permission = permission;
        this.effects = effects;
        this.fishChanceBonus = fishChanceBonus;
        this.coinBonus = coinBonus;
        this.sellBonus = sellBonus;
        this.priceCoins = priceCoins;
        this.priceNacar = priceNacar;
        this.activatorName = activatorName;
        this.activatorLore = activatorLore;
        this.menuItems = menuItems;
    }

    public String getId() { return id; }
    public int getOrder() { return order; }
    public String getDisplayName() { return displayName; }
    public Color getArmorColor() { return armorColor; }
    public String getPermission() { return permission; }
    public List<ClassEffect> getEffects() { return effects; }
    public double getFishChanceBonus() { return fishChanceBonus; }
    public double getCoinBonus() { return coinBonus; }
    public double getSellBonus() { return sellBonus; }
    public double getPriceCoins() { return priceCoins; }
    public double getPriceNacar() { return priceNacar; }
    public String getActivatorName() { return activatorName; }
    public List<String> getActivatorLore() { return activatorLore; }
    public Map<String, MenuItem> getMenuItems() { return menuItems; }

    public ItemStack getMenuItem(String state) {
        MenuItem mi = menuItems.get(state);
        if (mi == null) return null;
        ItemStack item = new ItemStack(mi.material());
        item.editMeta(meta -> {
            meta.displayName(FishUtil.parse(mi.name()));
            if (mi.color() != null && meta instanceof org.bukkit.inventory.meta.LeatherArmorMeta leather) {
                leather.setColor(mi.color());
            }
            List<String> lore = mi.lore().stream()
                    .map(l -> l
                            .replace("{fish-chance}", String.format("%.0f", fishChanceBonus))
                            .replace("{coin-bonus}", String.format("%.0f", coinBonus))
                            .replace("{sell-bonus}", String.format("%.0f", sellBonus))
                            .replace("{coins}", String.format("%.0f", priceCoins))
                            .replace("{nacar}", String.format("%.0f", priceNacar)))
                    .toList();
            meta.lore(lore.stream().map(FishUtil::parse).toList());
        });
        return item;
    }

    public record ClassEffect(PotionEffectType type, int level) {}
    public record MenuItem(Material material, String name, Color color, List<String> lore) {}
}
