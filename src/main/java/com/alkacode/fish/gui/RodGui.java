package com.alkacode.fish.gui;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.model.FishingRod;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class RodGui extends FishGui {

    public RodGui(AlkaFishPlugin plugin, Player player) {
        super(plugin, player, "🎣 Sua Vara", 3, "alkafish-rod");
    }

    @Override
    public void render() {
        fillBlack();
        var stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());
        FishingRod rod = plugin.getRodManager().getRodById(stats.getRodId());
        // Fallback: se o id não existir (ex.: id antigo/inexistente no stats), usa a vara padrão
        // para o menu nunca ficar vazio.
        if (rod == null) rod = plugin.getRodManager().getDefaultRod();
        final FishingRod fRod = rod;
        FishingRod next = fRod != null ? plugin.getRodManager().getNextRod(fRod) : null;

        // Slot 11: vara atual (sempre)
        if (fRod != null) {
            setItem(11, fRod.toItemStack(plugin, stats.getRodEnchantLevels(), stats.getNacar(), stats.getNacarNext()), e -> {});
        }

        // Slot 13: upgrade (sempre visível se existe próxima vara)
        if (next != null) {
            boolean canUpgrade = plugin.getRodManager().canUpgrade(player, fRod);
            setItem(13, canUpgrade ? createUpgradeItem(next) : createUpgradeBlockedItem(next), e -> {
                if (canUpgrade) {
                    plugin.getRodManager().upgradeRod(player, fRod);
                    player.closeInventory();
                } else {
                    player.sendMessage(plugin.getMessages().parse("rod.not-enough-nacar",
                        java.util.Map.of("cost", String.format("%.0f", next.getUpgradeCostCoins()))));
                }
            });
        }

        // Slot 15: reparar ou status da vara
        if (fRod != null) {
            if (stats.isRodBroken()) {
                boolean canRepair = plugin.getRodManager().canRepair(player, fRod);
                setItem(15, canRepair ? createRepairItem(fRod) : createRepairBlockedItem(fRod), e -> {
                    if (canRepair) {
                        plugin.getRodManager().repairRod(player);
                        player.closeInventory();
                    } else {
                        player.sendMessage(plugin.getMessages().parse("rod.not-enough-nacar",
                            java.util.Map.of("cost", String.format("%.0f", fRod.getRepairCostCoins()))));
                    }
                });
            } else {
                setItem(15, createItem(Material.LIME_DYE, "<green>✔ Vara em boas condições",
                    "<gray>Sua vara não precisa de reparos."), e -> {});
            }
        }

        // Slot 18: voltar
        setItem(18, createItem(Material.ARROW, "<yellow>⬅ Voltar"), e -> new FishingAreaGui(plugin, player).open());
        setItem(26, createItem(Material.BARRIER, "<red>Fechar"), e -> player.closeInventory());
    }

    private ItemStack createUpgradeItem(FishingRod next) {
        return createItem(Material.ANVIL, "<green>⬆ Upgradar Vara",
            "<gray>Próxima: " + next.getDisplayName(),
            "<gray>Custo: <green>" + String.format("%.0f", next.getUpgradeCostCoins())
                + " coins <gray>+ <aqua>" + String.format("%.0f", next.getUpgradeCostNacar()) + " nacar");
    }

    private ItemStack createUpgradeBlockedItem(FishingRod next) {
        return createItem(Material.RED_STAINED_GLASS_PANE, "<green>⬆ Upgradar Vara (Bloqueado)",
            "<gray>Próxima: " + next.getDisplayName(),
            "<gray>Custo: <green>" + String.format("%.0f", next.getUpgradeCostCoins())
                + " coins <gray>+ <aqua>" + String.format("%.0f", next.getUpgradeCostNacar()) + " nacar",
            "<red>Você não tem recursos suficientes.");
    }

    private ItemStack createRepairItem(FishingRod rod) {
        return createItem(Material.IRON_INGOT, "<yellow>🔧 Reparar Vara",
            "<gray>Custo: <green>" + String.format("%.0f", rod.getRepairCostCoins())
                + " coins <gray>+ <aqua>" + String.format("%.0f", rod.getRepairCostNacar()) + " nacar");
    }

    private ItemStack createRepairBlockedItem(FishingRod rod) {
        return createItem(Material.RED_STAINED_GLASS_PANE, "<yellow>🔧 Reparar Vara (Bloqueado)",
            "<gray>Custo: <green>" + String.format("%.0f", rod.getRepairCostCoins())
                + " coins <gray>+ <aqua>" + String.format("%.0f", rod.getRepairCostNacar()) + " nacar",
            "<red>Você não tem recursos suficientes.");
    }
}
