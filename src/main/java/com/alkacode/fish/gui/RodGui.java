package com.alkacode.fish.gui;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.model.FishingRod;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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
                setAt(layout, 'D', createItem(Material.LIME_DYE, "<green>✔ Vara em boas condições",
                    "<gray>Sua vara não precisa de reparos."));
            }
        }

        // K: skins - troca a textura entre varas já desbloqueadas sem mudar stats
        if (fRod != null && fRod.getLevel() > 0) {
            setAt(layout, 'K', createItem(Material.PAINTING, "<light_purple>🎨 Skins da Vara",
                    "<gray>Troque a aparência da sua vara",
                    "<gray>entre as que você já desbloqueou.",
                    "",
                    "<yellow>Clique para abrir"), e -> new RodSkinGui(plugin, player).open());
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
        setAt(layout, 'V', createItem(Material.ARROW, "<yellow>⬅ Voltar"), e -> new FishingAreaGui(plugin, player).open());
        setAt(layout, 'F', createItem(Material.BARRIER, "<red>Fechar"), e -> player.closeInventory());
    }

    private ItemStack createAutoUpgradeItem(boolean enabled) {
        return createItem(enabled ? Material.LIME_DYE : Material.GRAY_DYE,
                enabled ? "<green>⚙ Auto-Upgrade: <bold>ON" : "<gray>⚙ Auto-Upgrade: <bold>OFF",
                "<gray>Upa a vara sozinha assim que",
                "<gray>tiver Gold + nacar suficientes.",
                "",
                "<yellow>Clique para " + (enabled ? "desligar" : "ligar"));
    }

    private ItemStack createAutoUpgradeLockedItem() {
        return createItem(Material.GRAY_DYE, "<gray>⚙ Auto-Upgrade <dark_gray>[VIP]",
                "<gray>Upa a vara sozinha assim que",
                "<gray>tiver Gold + nacar suficientes.",
                "",
                "<red>Perk VIP não desbloqueado.");
    }

    private ItemStack createUpgradeItem(FishingRod next) {
        return createItem(Material.ANVIL, "<green>⬆ Upgradar Vara",
            "<gray>Próxima: " + next.getDisplayName(),
            "<gray>Custo: <green>" + String.format("%.0f", next.getUpgradeCostCoins())
                + " Gold <gray>+ <aqua>" + String.format("%.0f", next.getUpgradeCostNacar()) + " nacar",
            "",
            "<yellow>Clique para upar");
    }

    private ItemStack createUpgradeBlockedItem(FishingRod next) {
        return createItem(Material.RED_STAINED_GLASS_PANE, "<green>⬆ Upgradar Vara (Bloqueado)",
            "<gray>Próxima: " + next.getDisplayName(),
            "<gray>Custo: <green>" + String.format("%.0f", next.getUpgradeCostCoins())
                + " Gold <gray>+ <aqua>" + String.format("%.0f", next.getUpgradeCostNacar()) + " nacar",
            "<red>Você não tem recursos suficientes.");
    }

    private ItemStack createRepairItem(FishingRod rod) {
        return createItem(Material.IRON_INGOT, "<yellow>🔧 Reparar Vara",
            "<gray>Custo: <green>" + String.format("%.0f", rod.getRepairCostCoins())
                + " Gold <gray>+ <aqua>" + String.format("%.0f", rod.getRepairCostNacar()) + " nacar",
            "",
            "<yellow>Clique para reparar");
    }

    private ItemStack createRepairBlockedItem(FishingRod rod) {
        return createItem(Material.RED_STAINED_GLASS_PANE, "<yellow>🔧 Reparar Vara (Bloqueado)",
            "<gray>Custo: <green>" + String.format("%.0f", rod.getRepairCostCoins())
                + " Gold <gray>+ <aqua>" + String.format("%.0f", rod.getRepairCostNacar()) + " nacar",
            "<red>Você não tem recursos suficientes.");
    }
}
