package com.alkacode.fish.gui;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.model.FishingRod;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public final class RodGui extends FishGui {

    public RodGui(AlkaFishPlugin plugin, Player player) {
        super(plugin, player, "🎣 Sua Vara", Category.PROGRESSION, 3, "alkafish-rod");
    }

    @Override
    public void render() {
        var layout = applyBorder("rod");
        var stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());
        FishingRod rod = plugin.getRodManager().getRodById(stats.getRodId());
        // Fallback: se o id não existir (ex.: id antigo/inexistente no stats), usa a vara padrão
        // para o menu nunca ficar vazio.
        if (rod == null) rod = plugin.getRodManager().getDefaultRod();
        final FishingRod fRod = rod;
        FishingRod next = fRod != null ? plugin.getRodManager().getNextRod(fRod) : null;

        // R: vara atual (sempre)
        if (fRod != null) {
            setAt(layout, 'R', fRod.toItemStack(plugin, stats.getRodEnchantLevels(),
                    stats.getRodNacarEarned(), plugin.getRodManager().nextUpgradeCostNacar(fRod)));
        }

        // U: upgrade (sempre visível se existe próxima vara)
        if (next != null) {
            boolean canUpgrade = plugin.getRodManager().canUpgrade(player, fRod);
            setAt(layout, 'U', canUpgrade ? createUpgradeItem(next) : createUpgradeBlockedItem(next), e -> {
                if (canUpgrade) {
                    plugin.getRodManager().upgradeRod(player, fRod);
                    player.closeInventory();
                } else {
                    player.sendMessage(plugin.getMessages().parse("rod.not-enough-nacar",
                        java.util.Map.of("cost", String.format("%.0f", next.getUpgradeCostCoins()))));
                }
            });
        }

        // D: reparar ou status da vara
        if (fRod != null) {
            if (stats.isRodBroken()) {
                boolean canRepair = plugin.getRodManager().canRepair(player, fRod);
                setAt(layout, 'D', canRepair ? createRepairItem(fRod) : createRepairBlockedItem(fRod), e -> {
                    if (canRepair) {
                        plugin.getRodManager().repairRod(player);
                        player.closeInventory();
                    } else {
                        player.sendMessage(plugin.getMessages().parse("rod.not-enough-nacar",
                            java.util.Map.of("cost", String.format("%.0f", fRod.getRepairCostCoins()))));
                    }
                });
            } else {
                setAt(layout, 'D', icon("rod.em-boas-condicoes"));
            }
        }

        // K: skins - troca a textura entre varas já desbloqueadas sem mudar stats
        if (fRod != null && fRod.getLevel() > 0) {
            setAt(layout, 'K', icon("rod.skins"), e -> new RodSkinGui(plugin, player).open());
        }

        // A: auto-upgrade (feature de VIP - via Perk Tree do AlkaVips)
        boolean hasAutoUpgradePerk = plugin.getAlkaVipsHook() != null
                && plugin.getAlkaVipsHook().isAvailable()
                && plugin.getAlkaVipsHook().hasPerk(player.getUniqueId(), "auto-upgrade-rod");
        if (hasAutoUpgradePerk) {
            boolean enabled = stats.isAutoUpgradeEnabled();
            setAt(layout, 'A', createAutoUpgradeItem(enabled), e -> {
                plugin.getRodManager().toggleAutoUpgrade(player);
                refresh();
            });
        } else {
            setAt(layout, 'A', createAutoUpgradeLockedItem());
        }

        // V: voltar, F: fechar
        setAt(layout, 'V', icon("common.voltar"), e -> new FishingAreaGui(plugin, player).open());
        setAt(layout, 'F', icon("rod.fechar"), e -> player.closeInventory());
    }

    private ItemStack createAutoUpgradeItem(boolean enabled) {
        return icon(enabled ? "rod.auto-upgrade-on" : "rod.auto-upgrade-off");
    }

    private ItemStack createAutoUpgradeLockedItem() {
        return icon("rod.auto-upgrade-bloqueado");
    }

    private ItemStack createUpgradeItem(FishingRod next) {
        return icon("rod.upar", Map.of(
                "proxima", next.getDisplayName(),
                "custo-coins", String.format("%.0f", next.getUpgradeCostCoins()),
                "custo-nacar", String.format("%.0f", next.getUpgradeCostNacar())));
    }

    private ItemStack createUpgradeBlockedItem(FishingRod next) {
        return icon("rod.upar-bloqueado", Map.of(
                "proxima", next.getDisplayName(),
                "custo-coins", String.format("%.0f", next.getUpgradeCostCoins()),
                "custo-nacar", String.format("%.0f", next.getUpgradeCostNacar())));
    }

    private ItemStack createRepairItem(FishingRod rod) {
        return icon("rod.reparar", Map.of(
                "custo-coins", String.format("%.0f", rod.getRepairCostCoins()),
                "custo-nacar", String.format("%.0f", rod.getRepairCostNacar())));
    }

    private ItemStack createRepairBlockedItem(FishingRod rod) {
        return icon("rod.reparar-bloqueado", Map.of(
                "custo-coins", String.format("%.0f", rod.getRepairCostCoins()),
                "custo-nacar", String.format("%.0f", rod.getRepairCostNacar())));
    }
}
