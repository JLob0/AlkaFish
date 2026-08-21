package com.alkacode.fish.gui;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.model.Tournament;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Info do torneio ativo. */
public final class TournamentGui extends FishGui {

    public TournamentGui(AlkaFishPlugin plugin, Player player) {
        super(plugin, player, "🏆 Torneio de Pesca", Category.HIGHLIGHT, 3, "alkafish-tournament");
    }

    @Override
    public void render() {
        var layout = applyBorder("tournament");
        Tournament tournament = plugin.getTournamentManager().getActiveTournament();

        if (tournament == null) {
            setAt(layout, 'T', icon("tournament.sem-torneio"));
        } else {
            // top3 e uma lista de tamanho variavel (0-3 entradas) - fica como lore extra
            // anexada depois do template fixo (Tempo/Tipo/cabecalho "Top 3"), nao da pra
            // representar num template de placeholder por linha.
            List<Component> top3Lore = new ArrayList<>();
            var top3 = plugin.getTournamentManager().getTopScores(3);
            if (top3.isEmpty()) {
                top3Lore.add(MiniMessage.miniMessage().deserialize("<gray>Ninguém pontuou ainda."));
            } else {
                String[] medals = {"<yellow>🥇", "<gray>🥈", "<gold>🥉"};
                for (int i = 0; i < top3.size(); i++) {
                    var entry = top3.get(i);
                    var offline = org.bukkit.Bukkit.getOfflinePlayer(entry.uuid());
                    String name = offline.getName() != null ? offline.getName() : "Desconhecido";
                    top3Lore.add(MiniMessage.miniMessage().deserialize(medals[i] + " <white>" + name
                            + " <gray>- <aqua>" + String.format("%.2f", entry.score())));
                }
            }
            setAt(layout, 'T', icon("tournament.info", Map.of(
                    "tipo", tournament.getType().getDisplayName(),
                    "tempo-restante", tournament.getTimeLeftFormatted(),
                    "tipo-descricao", tournament.getType().getDescription()), top3Lore));

            int pos = plugin.getTournamentManager().getPlayerPosition(player);
            double score = plugin.getTournamentManager().getPlayerScore(player);
            setAt(layout, 'P', icon("tournament.posicao", Map.of("posicao", pos > 0 ? String.valueOf(pos) : "-")));
            setAt(layout, 'S', icon("tournament.pontuacao", Map.of("pontuacao", String.format("%.2f", score))));
        }

        setAt(layout, 'V', icon("common.voltar"), e -> new FishingAreaGui(plugin, player).open());
        setAt(layout, 'F', icon("tournament.fechar"), e -> player.closeInventory());
    }
}
