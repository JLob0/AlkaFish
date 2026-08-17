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
        super(plugin, player, "🎣 Sacola de Peixes", 6, "alkafish-bag");
    }

    @Override
    public void render() {
        fillBlack();
        PlayerFishStats stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());
        List<FishBagEntryEntity> bag = plugin.getFishBagService().getBag(player);
        double totalBagWeight = bag.stream().mapToDouble(FishBagEntryEntity::totalWeight).sum();

        setItem(4, createItem(Material.BUCKET, "<aqua>📊 Sacola",
                        "<gray>Peixes: <green>" + bag.stream().mapToInt(FishBagEntryEntity::amount).sum(),
                        "<gray>Peso total: <green>" + String.format("%.2f kg", totalBagWeight)
                                + " <gray>/ <green>" + String.format("%.2f", stats.getBagCapacity()) + " kg"),
                e -> {});

        setItem(49, createItem(Material.EMERALD_BLOCK, "<green>💰 Vender Tudo",
                        "<gray>Vende todos os peixes da sacola"),
                e -> {
                    double total = plugin.getFishBagService().sellAll(player);
                    player.sendMessage(plugin.getMessages().parse("bag.sold_all",
                            java.util.Map.of("price", String.format("%.2f", total))));
                    new FishBagGui(plugin, player).open();
                });
        setItem(50, createItem(Material.BOOK, "<yellow>📖 Codex de Peixes"),
                e -> new CodexGui(plugin, player).open());
        setItem(51, createItem(Material.FISHING_ROD, "<aqua>🎣 Sua Vara"),
                e -> new RodGui(plugin, player).open());
        setItem(52, createItem(Material.LEATHER_CHESTPLATE, "<gold>🛡 Classes"),
                e -> new ClassGui(plugin, player).open());
        setItem(53, createItem(Material.BARRIER, "<red>❌ Fechar"),
                e -> player.closeInventory());

        int slot = 9;
        for (FishBagEntryEntity entry : bag) {
            if (slot > 44) break;
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
                    "<green>Clique para vender este peixe");
            setItem(slot, item, e -> {
                double sold = plugin.getFishBagService().sell(player, entry.fishId());
                player.sendMessage(plugin.getMessages().parse("bag.sold", java.util.Map.of(
                        "amount", String.valueOf(entry.amount()),
                        "fish", fish.getDisplayName(),
                        "price", String.format("%.2f", sold))));
                new FishBagGui(plugin, player).open();
            });
            slot++;
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
