package com.alkacode.fish.gui;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.model.PlayerFishStats;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Fish Bag: inventário próprio com estatísticas e acesso à venda/codex. */
public final class FishBagGui extends FishGui {

    public FishBagGui(AlkaFishPlugin plugin, Player player) {
        super(plugin, player, "🎣 Sua Fish Bag", 6, "alkafish-bag");
    }

    @Override
    public void render() {
        fillBlack();
        PlayerFishStats stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());

        setItem(4, createItem(Material.BUCKET, "<aqua>📊 Estatísticas",
                        "<gray>Nível: <green>" + stats.getLevel(),
                        "<gray>XP: <green>" + String.format("%.1f", stats.getXp()) + " / "
                                + String.format("%.1f", PlayerFishStats.getXpForLevel(stats.getLevel() + 1)),
                        "<gray>Total pescado: <green>" + stats.getTotalCaught(),
                        "<gray>Maior peixe: <green>" + String.format("%.1f cm", stats.getBiggestLength()),
                        "<gray>Peso na bag: <green>" + String.format("%.2f", stats.getCurrentBagWeight())
                                + " <gray>/ <green>" + String.format("%.2f", stats.getBagCapacity()) + " kg"),
                e -> {});

        setItem(49, createItem(Material.EMERALD, "<green>💰 Vender Tudo",
                        "<gray>Clique para vender todos os peixes"),
                e -> new SellGui(plugin, player).open());
        setItem(50, createItem(Material.BOOK, "<yellow>📖 Codex de Peixes",
                        "<gray>Veja todos os peixes do servidor"),
                e -> new CodexGui(plugin, player).open());
        setItem(51, createItem(Material.FISHING_ROD, "<aqua>🎣 Sua Vara",
                        "<gray>Gerenciar vara e encantamentos"),
                e -> new RodGui(plugin, player).open());
        setItem(52, createItem(Material.LEATHER_CHESTPLATE, "<gold>🛡 Classes",
                        "<gray>Classes de armadura de pesca"),
                e -> new ClassGui(plugin, player).open());
        setItem(53, createItem(Material.BARRIER, "<red>❌ Fechar"),
                e -> player.closeInventory());

        int slot = 9;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || slot > 44) continue;
            if (isFishItem(item)) {
                setItem(slot, item, e -> {});
                slot++;
            }
        }
    }

    private boolean isFishItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(
                new NamespacedKey(plugin, "alkafish_id"));
    }
}
