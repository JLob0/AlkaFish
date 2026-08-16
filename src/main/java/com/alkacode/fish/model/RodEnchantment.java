package com.alkacode.fish.model;

import com.alkacode.fish.util.FishUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Definição de um encantamento de vara carregado do enchantments.yml. */
public class RodEnchantment {
    private final String id;
    private final String displayName;
    private final String description;
    private final int defaultLevel;
    private final int maxLevel; // -1 = infinito
    private final double percentagePerLevel;
    private final double multiplierPerLevel;
    private final int costBase;
    private final int costPerLevel;
    private final List<EnchantCommand> commands;
    private final EnchantMessages messages;
    private final Map<String, MenuItem> menuItems;

    public RodEnchantment(String id, String displayName, String description, int defaultLevel, int maxLevel,
                          double percentagePerLevel, double multiplierPerLevel, int costBase, int costPerLevel,
                          List<EnchantCommand> commands, EnchantMessages messages, Map<String, MenuItem> menuItems) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.defaultLevel = defaultLevel;
        this.maxLevel = maxLevel;
        this.percentagePerLevel = percentagePerLevel;
        this.multiplierPerLevel = multiplierPerLevel;
        this.costBase = costBase;
        this.costPerLevel = costPerLevel;
        this.commands = commands;
        this.messages = messages;
        this.menuItems = menuItems;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public int getDefaultLevel() { return defaultLevel; }
    public int getMaxLevel() { return maxLevel; }
    public double getPercentagePerLevel() { return percentagePerLevel; }
    public double getMultiplierPerLevel() { return multiplierPerLevel; }
    public int getCostBase() { return costBase; }
    public int getCostPerLevel() { return costPerLevel; }
    public List<EnchantCommand> getCommands() { return commands; }
    public EnchantMessages getMessages() { return messages; }
    public Map<String, MenuItem> getMenuItems() { return menuItems; }

    public int costForLevel(int currentLevel) {
        return costBase + (currentLevel * costPerLevel);
    }

    /** Bônus percentual (lucky/keychain) para o nível dado. */
    public double percentBonus(int level) {
        return percentagePerLevel * level;
    }

    /** Bônus multiplicativo (multiplier) para o nível dado. */
    public double multiplierBonus(int level) {
        return 1.0 + (multiplierPerLevel * level);
    }

    public ItemStack getMenuItem(String state, int current, int max, double bonus, int cost) {
        MenuItem mi = menuItems.get(state);
        if (mi == null) return null;
        ItemStack item = new ItemStack(mi.material());
        if (mi.customModelData() > 0) item.editMeta(m -> m.setCustomModelData(mi.customModelData()));
        ItemStack finalItem = item;
        finalItem.editMeta(meta -> {
            String maxDisplay = max == -1 ? "∞" : String.valueOf(max);
            meta.displayName(FishUtil.parse(mi.name()));
            List<String> lore = new ArrayList<>();
            for (String line : mi.lore()) {
                lore.add(line
                        .replace("{current}", String.valueOf(current))
                        .replace("{max}", maxDisplay)
                        .replace("{bonus}", String.format("%.1f", bonus))
                        .replace("{cost}", String.valueOf(cost)));
            }
            meta.lore(lore.stream().map(FishUtil::parse).toList());
        });
        return finalItem;
    }

    public record EnchantCommand(double chance, String command) {}
    public record EnchantMessages(String title, String actionbar, List<String> chat) {}
    public record MenuItem(Material material, int customModelData, String name, List<String> lore) {
        public static MenuItem from(ConfigurationSection section) {
            if (section == null) return null;
            Material mat = Material.valueOf(section.getString("material", "ENCHANTED_BOOK").toUpperCase());
            return new MenuItem(mat, section.getInt("custom-model-data", 0),
                    section.getString("name", "Encantamento"), section.getStringList("lore"));
        }
    }
}
