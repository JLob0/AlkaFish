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
        super(plugin, player, "📖 Codex de Peixes", Category.PROGRESSION, 6, "alkafish-codex");
    }

    @Override
    public void render() {
        var layout = applyBorder("codex");
        PlayerFishStats stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());

        // Cabeçalho com contagem
        int total = plugin.getFishManager().getAllFish().size();
        int caught = (int) stats.getCaughtByFishId().values().stream().filter(c -> c > 0).count();
        setAt(layout, 'H', createItem(Material.BOOK, "<yellow>📖 Codex de Peixes",
            "<gray>Peixes descobertos: <green>" + caught + "<gray>/" + total));

        // Renderizar por raridade - 7 slots de conteudo por linha do layout.
        var contentSlots = layout.findSlots('0');
        int idx = 0;
        for (FishRarity rarity : FishRarity.values()) {
            List<Fish> fishList = plugin.getFishManager().getFishByRarity(rarity);
            if (fishList.isEmpty()) continue;
            if (idx >= contentSlots.size()) break;

            // Separador de raridade
            setItem(contentSlots.get(idx++), createItem(rarity.getSeparatorMaterial(), rarity.coloredName()));

            for (Fish fish : fishList) {
                if (idx >= contentSlots.size()) break;
                int count = stats.getCaughtByFishId().getOrDefault(fish.getId(), 0);
                setItem(contentSlots.get(idx++), createFishDisplay(fish, count));
            }

            // Pular pro começo da próxima linha entre raridades.
            if (idx % 7 != 0) {
                idx = ((idx / 7) + 1) * 7;
            }
        }

        setAt(layout, 'V', createItem(Material.ARROW, "<yellow>⬅ Voltar"), e -> new FishBagGui(plugin, player).open());
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
