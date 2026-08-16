package com.alkacode.fish.gui;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.model.Fish;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/** GUI de venda de peixes com integração AlkaEconomy + multiplicadores. */
public final class SellGui extends FishGui {

    private double totalValue = 0;
    private double totalWeight = 0;
    private final List<ItemStack> fishToSell = new ArrayList<>();

    public SellGui(AlkaFishPlugin plugin, Player player) {
        super(plugin, player, "💰 Vender Peixes", 6, "alkafish-sell");
    }

    @Override
    public void render() {
        fishToSell.clear();
        totalValue = 0;
        totalWeight = 0;
        scanInventory();

        fillBlack();
        setItem(4, createItem(Material.PAPER, "<yellow>📋 Resumo da Venda",
                        "<gray>Peixes: <green>" + fishToSell.size(),
                        "<gray>Peso total: <green>" + String.format("%.2f kg", totalWeight),
                        "<gray>Valor total: <green>$" + String.format("%.2f", totalValue),
                        bonusLore()),
                e -> {});
        setItem(49, createItem(Material.EMERALD_BLOCK, "<green>✔ Confirmar Venda",
                        "<gray>Receba <green>$" + String.format("%.2f", totalValue) + " <gray>em coins"),
                e -> {
                    executeSell();
                    player.closeInventory();
                });
        setItem(53, createItem(Material.REDSTONE_BLOCK, "<red>✖ Cancelar"),
                e -> player.closeInventory());

        int slot = 9;
        for (ItemStack fish : fishToSell) {
            if (slot > 44) break;
            setItem(slot, fish, e -> {});
            slot++;
        }
    }

    private String bonusLore() {
        double mult = 1.0;
        if (plugin.getAlkaRankUpHook() != null && plugin.getAlkaRankUpHook().isAvailable()) {
            mult *= plugin.getAlkaRankUpHook().getSellMultiplier(player.getUniqueId());
        }
        mult *= plugin.getTournamentManager().getActiveSellMultiplier();
        mult *= (1 + plugin.getFishingClassManager().getSellBonus(player) / 100.0);
        return "<gray>Multiplicador total: <green>" + String.format("%.0f%%", (mult - 1) * 100);
    }

    private void scanInventory() {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || !isFishItem(item)) continue;
            String fishId = item.getItemMeta().getPersistentDataContainer()
                    .get(new NamespacedKey(plugin, "alkafish_id"), PersistentDataType.STRING);
            Double length = item.getItemMeta().getPersistentDataContainer()
                    .get(new NamespacedKey(plugin, "alkafish_length"), PersistentDataType.DOUBLE);
            Double weight = item.getItemMeta().getPersistentDataContainer()
                    .get(new NamespacedKey(plugin, "alkafish_weight"), PersistentDataType.DOUBLE);
            if (fishId == null || length == null || weight == null) continue;

            Fish fish = plugin.getFishManager().getFishById(fishId);
            if (fish == null) continue;

            double price = fish.calculatePrice(length);
            if (plugin.getAlkaRankUpHook() != null && plugin.getAlkaRankUpHook().isAvailable()) {
                price *= plugin.getAlkaRankUpHook().getSellMultiplier(player.getUniqueId());
            }
            price *= plugin.getTournamentManager().getActiveSellMultiplier();
            price *= (1 + plugin.getFishingClassManager().getSellBonus(player) / 100.0);

            totalValue += price * item.getAmount();
            totalWeight += weight * item.getAmount();
            fishToSell.add(item);
        }
    }

    private void executeSell() {
        if (fishToSell.isEmpty()) {
            player.sendMessage(plugin.getMessages().parse("sell.no-fish"));
            return;
        }

        for (ItemStack fish : fishToSell) {
            player.getInventory().removeItem(fish);
        }

        plugin.getEconomyBridge().deposit(player.getUniqueId(), "coins", totalValue);

        var stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());
        stats.removeFromBag(totalWeight);
        plugin.getPlayerDataManager().save(player.getUniqueId());

        player.sendMessage(plugin.getMessages().parse("sell.success", java.util.Map.of(
                "count", String.valueOf(fishToSell.size()),
                "amount", String.format("%.2f", totalValue))));
    }

    private boolean isFishItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(
                new NamespacedKey(plugin, "alkafish_id"));
    }
}
