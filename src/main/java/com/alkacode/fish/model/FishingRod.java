package com.alkacode.fish.model;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.util.FishUtil;
import com.alkacode.fish.util.ProgressBarUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Definição de uma vara de pesca carregada do rods.yml. */
public class FishingRod {
    private final String id;
    private final int level;
    private final String displayName;
    private final Material material;
    private final int customModelData;
    private final double supportedWeight;
    private final int delaySeconds;
    private final boolean breakOnHeavy;
    private final boolean allowAutoSell;
    private final double repairCostCoins;
    private final double repairCostNacar;
    private final double upgradeCostCoins;
    private final double upgradeCostNacar;
    private final int enchantBaseCost;
    private final List<RodReward> rewards;
    private final String itemName;
    private final List<String> itemLore;

    public FishingRod(String id, int level, String displayName, Material material, int customModelData,
                      double supportedWeight, int delaySeconds, boolean breakOnHeavy, boolean allowAutoSell,
                      double repairCostCoins, double repairCostNacar, double upgradeCostCoins, double upgradeCostNacar,
                      int enchantBaseCost, List<RodReward> rewards, String itemName, List<String> itemLore) {
        this.id = id;
        this.level = level;
        this.displayName = displayName;
        this.material = material;
        this.customModelData = customModelData;
        this.supportedWeight = supportedWeight;
        this.delaySeconds = delaySeconds;
        this.breakOnHeavy = breakOnHeavy;
        this.allowAutoSell = allowAutoSell;
        this.repairCostCoins = repairCostCoins;
        this.repairCostNacar = repairCostNacar;
        this.upgradeCostCoins = upgradeCostCoins;
        this.upgradeCostNacar = upgradeCostNacar;
        this.enchantBaseCost = enchantBaseCost;
        this.rewards = rewards;
        this.itemName = itemName;
        this.itemLore = itemLore;
    }

    public String getId() { return id; }
    public int getLevel() { return level; }
    public String getDisplayName() { return displayName; }
    public Material getMaterial() { return material; }
    public int getCustomModelData() { return customModelData; }
    public double getSupportedWeight() { return supportedWeight; }
    public int getDelaySeconds() { return delaySeconds; }
    public boolean isBreakOnHeavy() { return breakOnHeavy; }
    public boolean isAllowAutoSell() { return allowAutoSell; }
    public double getRepairCostCoins() { return repairCostCoins; }
    public double getRepairCostNacar() { return repairCostNacar; }
    public double getUpgradeCostCoins() { return upgradeCostCoins; }
    public double getUpgradeCostNacar() { return upgradeCostNacar; }
    public int getEnchantBaseCost() { return enchantBaseCost; }
    public List<RodReward> getRewards() { return rewards; }
    public String getItemName() { return itemName; }
    public List<String> getItemLore() { return itemLore; }

    /** Monta o ItemStack da vara com PDC e lore processada. */
    public ItemStack toItemStack(AlkaFishPlugin plugin, Map<String, Integer> enchants, double nacarHas, double nacarNext) {
        ItemStack item = new ItemStack(material);
        if (customModelData > 0) item.editMeta(m -> m.setCustomModelData(customModelData));

        ItemStack finalItem = item;
        finalItem.editMeta(meta -> {
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "alkafish_rod_id"), PersistentDataType.STRING, id);
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "alkafish_rod_level"), PersistentDataType.INTEGER, level);
            String name = itemName.replace("{level}", String.valueOf(level));
            meta.displayName(FishUtil.parse(name));

            List<String> lore = new ArrayList<>();
            for (String line : itemLore) {
                lore.add(processLore(plugin, line, enchants, nacarHas, nacarNext));
            }
            meta.lore(lore.stream().map(FishUtil::parse).toList());
        });
        return finalItem;
    }

    private String processLore(AlkaFishPlugin plugin, String line, Map<String, Integer> enchants, double nacarHas, double nacarNext) {
        String out = line
                .replace("{weight}", String.format("%.1f", supportedWeight))
                .replace("{delay}", String.valueOf(delaySeconds))
                .replace("{nacar_has}", String.format("%.0f", nacarHas))
                .replace("{nacar_next}", String.format("%.0f", nacarNext))
                .replace("{progressbar}", ProgressBarUtil.create(nacarHas, nacarNext, 20, '■', "<aqua>", "<dark_gray>"));
        if (line.contains("{enchants}")) {
            return plugin.getEnchantmentManager().formatEnchants(enchants);
        }
        return out;
    }

    /** Recompensa vinculada a chance + id (rewards.yml). */
    public record RodReward(double chance, String rewardId) {}
}
