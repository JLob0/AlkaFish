package com.alkacode.fish.gui;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.model.Fish;
import com.alkacode.fish.model.PlayerFishStats;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Codex de peixes: mostra todos os peixes e quais o jogador já pescou. */
public final class CodexGui extends FishGui {

    public CodexGui(AlkaFishPlugin plugin, Player player) {
        super(plugin, player, "📖 Codex de Peixes", 6, "alkafish-codex");
    }

    @Override
    public void render() {
        fillBlack();
        PlayerFishStats stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());

        int slot = 9;
        for (Fish fish : plugin.getFishManager().getAllFish()) {
            if (slot > 44) break;
            int count = stats.getCaughtByFishId().getOrDefault(fish.getId(), 0);
            setItem(slot, createFishDisplay(fish, count), e -> {});
            slot++;
        }

        setItem(49, createItem(Material.ARROW, "<yellow>⬅ Voltar"),
                e -> new FishBagGui(plugin, player).open());
    }

    private ItemStack createFishDisplay(Fish fish, int count) {
        if (count > 0) {
            List<String> lore = new ArrayList<>(fish.getLore());
            lore.add("");
            lore.add("<green>✔ Já pescado (" + count + "x)");
            lore.add("<gray>Raridade: " + fish.getRarity().getDisplayName());
            return createItem(Material.COD, fish.getDisplayName(), lore.toArray(new String[0]));
        }
        return createItem(Material.GRAY_DYE, "<dark_gray>???",
                "<gray>Peixe não descoberto",
                "<dark_gray>Raridade: " + fish.getRarity().getDisplayName());
    }
}
