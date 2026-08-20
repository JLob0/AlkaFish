package com.alkacode.fish.gui;

import com.alkacode.fish.AlkaFishPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;

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
        setAt(layout, 'P', head(player.getName(), "<aqua>Suas Informações",
                "",
                "<gray>❘ Nível da vara: <white>" + stats.getRodLevel()
                        + (rod != null ? " <gray>(<white>" + String.format("%.0f", rod.getSupportedWeight())
                                + "kg<gray>, <white>" + rod.getDelaySeconds() + "s<gray>)" : ""),
                "<gray>❘ Peixes pescados: <white>" + stats.getTotalCaught(),
                "<gray>❘ Peso total pescado: <white>" + String.format("%.2f kg", stats.getTotalWeight()),
                "",
                "<gray>❘ Tempo pescando: <white>" + plugin.getFishingAreaManager().getFishingTime(player),
                "<gray>❘ Nacar: <aqua>" + String.format("%.0f", nacar)));

        // A: ação principal - ir ou sair da área.
        setAt(layout, 'A', createItem(configured
                        ? (inArea ? Material.RED_BED : Material.SEA_LANTERN)
                        : Material.BARRIER,
                        configured ? (inArea ? "<red>⬅ Sair da Área de Pesca" : "<aqua>🏝 Ir para a Área de Pesca")
                                : "<red>Área não configurada",
                        configured
                                ? (inArea ? "<gray>Voltar para o spawn" : "<gray>Teleportar e ganhar a vara")
                                : "<gray>Configure com /alkafish setarea",
                        "",
                        configured ? (inArea ? "<yellow>Clique para sair" : "<yellow>Clique para entrar") : ""),
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
        setAt(layout, 'V', createItem(Material.FISHING_ROD, "<aqua>🎣 Sua Vara",
                        "<gray>Informações, upar e reparar",
                        "",
                        "<yellow>Clique para abrir"),
                e -> new RodGui(plugin, player).open());
        setAt(layout, 'E', createItem(Material.ENCHANTED_BOOK, "<dark_purple>✨ Encantamentos",
                        "<gray>Sortudo, Multiplicador, Chaveiro",
                        "",
                        "<yellow>Clique para abrir"),
                e -> new EnchantGui(plugin, player).open());
        setAt(layout, 'C', createItem(Material.LEATHER_CHESTPLATE, "<gold>🛡 Classes de Pesca",
                        "<gray>Armaduras com bônus",
                        "",
                        "<yellow>Clique para abrir"),
                e -> new ClassGui(plugin, player).open());
        setAt(layout, 'T', createItem(Material.TROPICAL_FISH, "<gold>🏆 Torneio",
                        "<gray>Info do torneio ativo",
                        "",
                        "<yellow>Clique para abrir"),
                e -> new TournamentGui(plugin, player).open());
        setAt(layout, 'S', createItem(Material.EMERALD, "<green>💰 Sacola de Peixes",
                        "<gray>Veja e venda seus peixes",
                        "",
                        "<yellow>Clique para abrir"),
                e -> new FishBagGui(plugin, player).open());

        // X/R/G/I: utilidades.
        setAt(layout, 'X', createItem(Material.BOOK, "<yellow>📖 Codex de Peixes",
                        "<gray>Coleção de peixes",
                        "",
                        "<yellow>Clique para abrir"),
                e -> new CodexGui(plugin, player).open());
        setAt(layout, 'R', createItem(Material.COMPASS, "<gold>🏆 TOP Pescaria",
                        "<gray>Ranking geral (peixes, nácar, peso, tempo)",
                        "",
                        "<yellow>Clique para abrir"),
                e -> TopGui.openAsync(plugin, player, "fished"));
        boolean pvpEnabled = plugin.getFishingAreaManager().isPvpEnabled(player);
        setAt(layout, 'G', createItem(pvpEnabled ? Material.IRON_SWORD : Material.SHIELD,
                        (pvpEnabled ? "<red>⚔ PvP: <bold>ON" : "<green>🛡 PvP: <bold>OFF"),
                        "<gray>Só toma dano de outro jogador",
                        "<gray>se os dois tiverem PvP ligado.",
                        "",
                        "<yellow>Clique para " + (pvpEnabled ? "desligar" : "ligar")),
                e -> {
                    plugin.getFishingAreaManager().togglePvp(player);
                    refresh();
                });
        setAt(layout, 'I', createItem(Material.GLASS, "<gray>👻 Invisibilidade",
                        "<gray>Toggle invisibilidade na área",
                        "",
                        "<yellow>Clique para alternar"),
                e -> plugin.getFishingAreaManager().toggleInvisibility(player));

        // F: fechar.
        setAt(layout, 'F', createItem(Material.BARRIER, "<red>❌ Fechar"),
                e -> player.closeInventory());
    }
}
