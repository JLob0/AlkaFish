package com.alkacode.fish.gui;

import com.alkacode.fish.AlkaFishPlugin;
import org.bukkit.entity.Player;

import java.util.Map;

/** Menu principal de pesca: ir para a área + acesso às funções premium.
 * Layout em hierarquia: linha 1 = ação principal (ir/sair da área), linha 2 = as 5
 * funções (Vara/Encantamentos/Classes/Torneio/Sacola), linha 3 = utilidades
 * (Codex/Invisibilidade), linha 4 = fechar. */
public final class FishingAreaGui extends FishGui {

    public FishingAreaGui(AlkaFishPlugin plugin, Player player) {
        super(plugin, player, "🎣 AlkaFish", Category.BRAND, 5, "alkafish-area");
    }

    @Override
    public void render() {
        var layout = applyBorder("fishing-area");

        boolean inArea = plugin.getFishingAreaManager().isInArea(player);
        boolean configured = plugin.getFishingAreaManager().isConfigured();
        var stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());
        var rod = plugin.getRodManager().getRodById(stats.getRodId());
        double nacar = plugin.getEconomyBridge().getBalance(player.getUniqueId(), "nacar");

        // P: header - resumo rápido do progresso do jogador.
        String rodInfo = String.valueOf(stats.getRodLevel())
                + (rod != null ? " <gray>(<white>" + String.format("%.0f", rod.getSupportedWeight())
                        + "kg<gray>, <white>" + rod.getDelaySeconds() + "s<gray>)" : "");
        setAt(layout, 'P', headIcon("fishing-area.perfil", player.getName(), Map.of(
                "rod-info", rodInfo,
                "caught", String.valueOf(stats.getTotalCaught()),
                "weight", String.format("%.2f kg", stats.getTotalWeight()),
                "fishing-time", plugin.getFishingAreaManager().getFishingTime(player),
                "nacar", String.format("%.0f", nacar))));

        // A: ação principal - ir ou sair da área.
        String acaoPath = !configured ? "fishing-area.acao-nao-configurada"
                : (inArea ? "fishing-area.acao-sair" : "fishing-area.acao-ir");
        setAt(layout, 'A', icon(acaoPath),
                e -> {
                    if (!configured) return;
                    if (inArea) {
                        plugin.getFishingAreaManager().teleportExit(player);
                    } else {
                        plugin.getFishingAreaManager().teleportTo(player);
                    }
                    player.closeInventory();
                });

        // V/E/C/T/S: as 5 funções principais.
        setAt(layout, 'V', icon("fishing-area.vara"), e -> new RodGui(plugin, player).open());
        setAt(layout, 'E', icon("fishing-area.encantamentos"), e -> new EnchantGui(plugin, player).open());
        setAt(layout, 'C', icon("fishing-area.classes"), e -> new ClassGui(plugin, player).open());
        setAt(layout, 'T', icon("fishing-area.torneio"), e -> new TournamentGui(plugin, player).open());
        setAt(layout, 'S', icon("fishing-area.sacola"), e -> new FishBagGui(plugin, player).open());

        // X/R/G/I: utilidades.
        setAt(layout, 'X', icon("fishing-area.codex"), e -> new CodexGui(plugin, player).open());
        setAt(layout, 'R', icon("fishing-area.top"), e -> TopGui.openAsync(plugin, player, "fished"));
        boolean pvpEnabled = plugin.getFishingAreaManager().isPvpEnabled(player);
        setAt(layout, 'G', icon(pvpEnabled ? "fishing-area.pvp-on" : "fishing-area.pvp-off"),
                e -> {
                    plugin.getFishingAreaManager().togglePvp(player);
                    refresh();
                });
        setAt(layout, 'I', icon("fishing-area.invisibilidade"),
                e -> plugin.getFishingAreaManager().toggleInvisibility(player));

        // F: fechar.
        setAt(layout, 'F', icon("fishing-area.fechar-x"), e -> player.closeInventory());
    }
}
