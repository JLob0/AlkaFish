package com.alkacode.fish.gui;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.model.Fish;
import com.alkacode.fish.model.FishRarity;
import com.alkacode.fish.model.PlayerFishStats;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class CodexGui extends FishGui {

    public CodexGui(AlkaFishPlugin plugin, Player player) {
        super(plugin, player, "📖 Codex de Peixes", 6, "alkafish-codex");
    }

    @Override
    public void render() {
        fillBlack();
        PlayerFishStats stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());

        // Cabeçalho com contagem
        int total = plugin.getFishManager().getAllFish().size();
        int caught = (int) stats.getCaughtByFishId().values().stream().filter(c -> c > 0).count();
        setItem(4, createItem(Material.BOOK, "<yellow>📖 Codex de Peixes",
            "<gray>Peixes descobertos: <green>" + caught + "<gray>/" + total), e -> {});

        // Renderizar por raridade, começando no slot 10 (pulando borda)
        int slot = 10;
        for (FishRarity rarity : FishRarity.values()) {
            List<Fish> fishList = plugin.getFishManager().getFishByRarity(rarity);
            if (fishList.isEmpty()) continue;

            // Separador de raridade (se couber)
            if (slot <= 43 && slot % 9 != 0) {
                setItem(slot, createItem(rarity.getSeparatorMaterial(), rarity.coloredName()), e -> {});
                slot++;
            }

            for (Fish fish : fishList) {
                if (slot > 43) break;
                // Pular se chegou no final da linha (slot 17, 26, 35, 44 são bordas)
                if (slot % 9 == 8) slot++;
                if (slot > 43) break;

                int count = stats.getCaughtByFishId().getOrDefault(fish.getId(), 0);
                setItem(slot, createFishDisplay(fish, count), e -> {});
                slot++;
            }

            // Pular linha entre raridades
            if (slot % 9 != 1) {
                slot = ((slot / 9) + 1) * 9 + 1;
            }
            if (slot > 43) break;
        }

        setItem(49, createItem(Material.ARROW, "<yellow>⬅ Voltar"), e -> new FishBagGui(plugin, player).open());
    }

    private ItemStack createFishDisplay(Fish fish, int count) {
        if (count > 0) {
            List<String> lore = new ArrayList<>(fish.getLore());
            lore.add("");
            lore.add("<green>✔ Já pescado (" + count + "x)");
            lore.add("<gray>Raridade: " + fish.getRarity().coloredName());
            lore.add("<gray>Peso: " + fish.getMinWeight() + "-" + fish.getMaxWeight() + "kg");
            return createItem(fish.getRarity().getDisplayMaterial(), "<white>" + fish.getDisplayName(), lore.toArray(new String[0]));
        }
        return createItem(Material.GRAY_DYE, "<dark_gray>???",
            "<gray>Peixe não descoberto",
            "<gray>Raridade: " + fish.getRarity().coloredName());
    }
}
