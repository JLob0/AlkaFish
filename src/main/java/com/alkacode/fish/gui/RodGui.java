package com.alkacode.fish.gui;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.model.FishingRod;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** GUI de gerenciamento da vara (upar/reparar). */
public final class RodGui extends FishGui {

    public RodGui(AlkaFishPlugin plugin, Player player) {
        super(plugin, player, "🎣 Sua Vara", 3, "alkafish-rod");
    }

    @Override
    public void render() {
        fillBlack();
        var stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());
        FishingRod rod = plugin.getRodManager().getRodById(stats.getRodId());
        FishingRod next = rod != null ? plugin.getRodManager().getNextRod(rod) : null;

        if (rod != null) {
            setItem(11, rod.toItemStack(plugin, stats.getRodEnchantLevels(), stats.getNacar(), stats.getNacarNext()), e -> {});
        }

        if (next != null && plugin.getRodManager().canUpgrade(player, rod)) {
            setItem(13, createUpgradeItem(next), e -> {
                plugin.getRodManager().upgradeRod(player, rod);
                player.closeInventory();
            });
        }

        if (stats.isRodBroken() && rod != null && plugin.getRodManager().canRepair(player, rod)) {
            setItem(15, createRepairItem(rod), e -> {
                plugin.getRodManager().repairRod(player);
                player.closeInventory();
            });
        }

        setItem(26, createItem(Material.BARRIER, "<red>Fechar"), e -> player.closeInventory());
    }

    private ItemStack createUpgradeItem(FishingRod next) {
        return createItem(Material.ANVIL, "<green>⬆ Upgradar Vara",
                "<gray>Próxima: " + next.getDisplayName(),
                "<gray>Custo: <green>" + String.format("%.0f", next.getUpgradeCostCoins())
                        + " coins <gray>+ <aqua>" + String.format("%.0f", next.getUpgradeCostNacar()) + " nacar");
    }

    private ItemStack createRepairItem(FishingRod rod) {
        return createItem(Material.IRON_INGOT, "<yellow>🔧 Reparar Vara",
                "<gray>Custo: <green>" + String.format("%.0f", rod.getRepairCostCoins())
                        + " coins <gray>+ <aqua>" + String.format("%.0f", rod.getRepairCostNacar()) + " nacar");
    }
}
