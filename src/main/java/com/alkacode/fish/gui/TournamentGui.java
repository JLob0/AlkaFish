package com.alkacode.fish.gui;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.model.Tournament;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Info do torneio ativo. */
public final class TournamentGui extends FishGui {

    public TournamentGui(AlkaFishPlugin plugin, Player player) {
        super(plugin, player, "🏆 Torneio de Pesca", Category.HIGHLIGHT, 3, "alkafish-tournament");
    }

    @Override
    public void render() {
        fillBlack();
        Tournament tournament = plugin.getTournamentManager().getActiveTournament();

        if (tournament == null) {
            setItem(13, createItem(Material.BARRIER, "<red>Nenhum Torneio Ativo",
                    "<gray>Aguarde o próximo torneio!"), e -> {});
        } else {
            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add("<gray>Tempo restante: <yellow>" + tournament.getTimeLeftFormatted());
            lore.add("<gray>Tipo: <yellow>" + tournament.getType().getDescription());
            lore.add("");
            lore.add("<gold>Top 3 ao vivo:");
            var top3 = plugin.getTournamentManager().getTopScores(3);
            if (top3.isEmpty()) {
                lore.add("<gray>Ninguém pontuou ainda.");
            } else {
                String[] medals = {"<yellow>🥇", "<gray>🥈", "<gold>🥉"};
                for (int i = 0; i < top3.size(); i++) {
                    var entry = top3.get(i);
                    var offline = org.bukkit.Bukkit.getOfflinePlayer(entry.uuid());
                    String name = offline.getName() != null ? offline.getName() : "Desconhecido";
                    lore.add(medals[i] + " <white>" + name + " <gray>- <aqua>" + String.format("%.2f", entry.score()));
                }
            }
            setItem(13, createItem(Material.TROPICAL_FISH, "<gold>" + tournament.getType().getDisplayName(),
                            lore.toArray(new String[0])),
                    e -> {});

            int pos = plugin.getTournamentManager().getPlayerPosition(player);
            double score = plugin.getTournamentManager().getPlayerScore(player);
            setItem(11, createItem(Material.GOLD_INGOT, "<yellow>Sua Posição",
                            "<gold>#" + (pos > 0 ? pos : "-")), e -> {});
            setItem(15, createItem(Material.EMERALD, "<green>Sua Pontuação",
                            "<green>" + String.format("%.2f", score)), e -> {});
        }

        setItem(18, createItem(Material.ARROW, "<yellow>⬅ Voltar"), e -> new FishingAreaGui(plugin, player).open());
        setItem(26, createItem(Material.BARRIER, "<red>Fechar"), e -> player.closeInventory());
    }
}
