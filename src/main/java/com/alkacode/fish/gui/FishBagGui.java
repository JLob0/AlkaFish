package com.alkacode.fish.gui;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.database.entity.FishBagEntryEntity;
import com.alkacode.fish.model.Fish;
import com.alkacode.fish.model.PlayerFishStats;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** Sacola de peixes no banco: mostra stacks por tipo, vender stack ou vender tudo. */
public final class FishBagGui extends FishGui {

    public FishBagGui(AlkaFishPlugin plugin, Player player) {
        super(plugin, player, "🎣 Sacola de Peixes", Category.BRAND, 6, "alkafish-bag");
    }

    @Override
    public void render() {
        fillBlack();
        PlayerFishStats stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());
        List<FishBagEntryEntity> bag = plugin.getFishBagService().getBag(player);
        double totalBagWeight = bag.stream().mapToDouble(FishBagEntryEntity::totalWeight).sum();

        int totalItems = bag.stream().mapToInt(FishBagEntryEntity::amount).sum();
        setItem(4, createItem(Material.BUCKET, "<aqua>📊 Sacola",
                        "<gray>Peixes: <green>" + totalItems + " <gray>/ <green>" + (int) stats.getBagCapacity(),
                        "<gray>Peso total: <green>" + String.format("%.2f kg", totalBagWeight)),
                e -> {});

        // Linha inferior (slots 46-51): ações da sacola, entre os painéis de vidro da
        // borda (45 e 52/53). Vara e Classes saíram daqui - já têm entrada própria no
        // menu principal (FishingAreaGui), não precisam duplicar aqui.
        boolean canUpgradeBag = plugin.getFishBagService().canUpgradeBagLimit(player);
        double bagUpgradeCost = plugin.getFishBagService().bagUpgradeCost(player);
        double bagLimitPerLevel = plugin.getConfig().getDouble("fishing-area.bag-upgrade.limit-per-level", 100);
        setItem(46, createItem(canUpgradeBag ? Material.GOLD_INGOT : Material.IRON_INGOT,
                        "<yellow>📈 Aumentar Limite",
                        "<gray>Limite atual: <white>" + (int) stats.getBagCapacity(),
                        "<gray>Próximo: <white>" + (int) (stats.getBagCapacity() + bagLimitPerLevel)
                                + " <gray>(+" + (int) bagLimitPerLevel + ")",
                        "<gray>Custo: <green>" + String.format("%.0f", bagUpgradeCost) + " coins",
                        "",
                        canUpgradeBag ? "<yellow>Clique para upar" : "<red>Coins insuficientes"),
                e -> {
                    if (plugin.getFishBagService().upgradeBagLimit(player)) {
                        player.sendMessage(plugin.getMessages().parse("bag.upgraded",
                                java.util.Map.of("limit", String.valueOf((int) plugin.getPlayerDataManager()
                                        .getStats(player.getUniqueId()).getBagCapacity()))));
                        new FishBagGui(plugin, player).open();
                    } else {
                        player.sendMessage(plugin.getMessages().parse("bag.upgrade-not-enough-coins"));
                    }
                });

        int pendingRewards = plugin.getPendingRewardService().getPending(player).size();
        setItem(47, createItem(Material.CHEST, "<gold>🎁 Recompensas <yellow>(" + pendingRewards + ")",
                        "<gray>Itens e chaves de crate ganhos pescando",
                        "",
                        "<yellow>Clique para abrir"),
                e -> new RewardClaimGui(plugin, player).open());
        setItem(48, createItem(Material.BOOK, "<yellow>📖 Codex de Peixes",
                        "<gray>Sua coleção de peixes descobertos",
                        "",
                        "<yellow>Clique para abrir"),
                e -> new CodexGui(plugin, player).open());
        setItem(49, createItem(Material.EMERALD_BLOCK, "<green>💰 Vender Tudo",
                        "<gray>Vende todos os peixes da sacola",
                        "",
                        "<yellow>Clique para vender tudo"),
                e -> {
                    if (bag.isEmpty()) {
                        player.sendMessage(plugin.getMessages().parse("bag.empty"));
                        return;
                    }
                    int count = bag.stream().mapToInt(FishBagEntryEntity::amount).sum();
                    double total = plugin.getFishBagService().sellAll(player);
                    player.sendMessage(plugin.getMessages().parse("bag.sold_all",
                            java.util.Map.of("count", String.valueOf(count), "price", String.format("%.2f", total))));
                    String bonusLine = plugin.getFishBagService().getSellBonusLine(player);
                    if (!bonusLine.isEmpty()) {
                        player.sendMessage(plugin.getMessages().parse("bag.bonus_line",
                                java.util.Map.of("bonuses", bonusLine)));
                    }
                    new FishBagGui(plugin, player).open();
                });
        // Slot 50: auto-venda ao encher (feature de VIP - via Perk Tree do AlkaVips,
        // mesmo padrão do auto-upgrade da vara).
        boolean hasAutoSellPerk = plugin.getAlkaVipsHook() != null
                && plugin.getAlkaVipsHook().isAvailable()
                && plugin.getAlkaVipsHook().hasPerk(player.getUniqueId(), "auto-sell-bag-full");
        if (hasAutoSellPerk) {
            boolean enabled = stats.isAutoSellOnFull();
            setItem(50, createItem(enabled ? Material.LIME_DYE : Material.GRAY_DYE,
                    enabled ? "<green>⚙ Auto-Venda: <bold>ON" : "<gray>⚙ Auto-Venda: <bold>OFF",
                    "<gray>Vende a sacola inteira sozinha",
                    "<gray>assim que ela encher.",
                    "",
                    "<yellow>Clique para " + (enabled ? "desligar" : "ligar")),
                    e -> {
                        plugin.getFishBagService().toggleAutoSellOnFull(player);
                        new FishBagGui(plugin, player).open();
                    });
        } else {
            setItem(50, createItem(Material.GRAY_DYE, "<gray>⚙ Auto-Venda <dark_gray>[VIP]",
                    "<gray>Vende a sacola inteira sozinha",
                    "<gray>assim que ela encher.",
                    "",
                    "<red>Perk VIP não desbloqueado."), e -> {});
        }

        setItem(51, createItem(Material.ARROW, "<yellow>⬅ Voltar"),
                e -> new FishingAreaGui(plugin, player).open());

        int slot = 10;
        for (FishBagEntryEntity entry : bag) {
            if (slot > 43) break;
            Fish fish = plugin.getFishManager().getFishById(entry.fishId());
            if (fish == null) continue;
            double unitWeight = entry.amount() > 0 ? entry.totalWeight() / entry.amount() : fish.getMinWeight();
            double unitPrice = plugin.getFishBagService().unitPrice(player, fish, unitWeight);
            double totalPrice = unitPrice * entry.amount();

            ItemStack item = createItem(fish.getRarity().ordinal() >= 4 ? Material.COD : materialFor(fish),
                    fish.getDisplayName(),
                    "<gray>Raridade: " + fish.getRarity().coloredName(),
                    "<gray>Quantia: <green>" + entry.amount() + "x",
                    "<gray>Peso total: <green>" + String.format("%.2f kg", entry.totalWeight()),
                    "<gray>Preço unitário: <green>$" + String.format("%.2f", unitPrice),
                    "<gray>Preço total: <green>$" + String.format("%.2f", totalPrice),
                    "",
                    "<yellow>Clique para vender este peixe");
            setItem(slot, item, e -> {
                double sold = plugin.getFishBagService().sell(player, entry.fishId());
                player.sendMessage(plugin.getMessages().parse("bag.sold", java.util.Map.of(
                        "amount", String.valueOf(entry.amount()),
                        "fish", fish.getDisplayName(),
                        "price", String.format("%.2f", sold))));
                String bonusLine = plugin.getFishBagService().getSellBonusLine(player);
                if (!bonusLine.isEmpty()) {
                    player.sendMessage(plugin.getMessages().parse("bag.bonus_line",
                            java.util.Map.of("bonuses", bonusLine)));
                }
                new FishBagGui(plugin, player).open();
            });
            slot++;
            if (slot % 9 == 8) slot += 2;
        }
    }

    private Material materialFor(Fish fish) {
        return switch (fish.getRarity()) {
            case COMMON -> Material.COD;
            case UNCOMMON -> Material.SALMON;
            case RARE -> Material.TROPICAL_FISH;
            case EPIC -> Material.PUFFERFISH;
            default -> Material.COD;
        };
    }
}
