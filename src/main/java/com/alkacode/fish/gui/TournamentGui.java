package com.alkacode.fish.gui;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.model.Tournament;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Info do torneio ativo. */
public final class TournamentGui extends FishGui {

    public TournamentGui(AlkaFishPlugin plugin, Player player) {
        super(plugin, player, "🏆 Torneio de Pesca", 3, "alkafish-tournament");
    }

    @Override
    public void render() {
        fillBlack();
        Tournament tournament = plugin.getTournamentManager().getActiveTournament();

        if (tournament == null) {
            setItem(13, createItem(Material.BARRIER, "<red>Nenhum Torneio Ativo",
                    "<gray>Aguarde o próximo torneio!"), e -> {});
        } else {
            setItem(13, createItem(Material.TROPICAL_FISH, "<gold>" + tournament.getType().getDisplayName(),
                            "<gray>Tempo restante: <yellow>" + tournament.getTimeLeftFormatted(),
                            "<gray>Tipo: <yellow>" + tournament.getType().getDescription()),
                    e -> {});

            int pos = plugin.getTournamentManager().getPlayerPosition(player);
            double score = plugin.getTournamentManager().getPlayerScore(player);
            setItem(11, createItem(Material.GOLD_INGOT, "<yellow>Sua Posição",
                            "<gold>#" + (pos > 0 ? pos : "-")), e -> {});
            setItem(15, createItem(Material.EMERALD, "<green>Sua Pontuação",
                            "<green>" + String.format("%.2f", score)), e -> {});
        }

        setItem(26, createItem(Material.BARRIER, "<red>Fechar"), e -> player.closeInventory());
    }
}
