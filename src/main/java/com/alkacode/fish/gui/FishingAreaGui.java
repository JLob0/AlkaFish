package com.alkacode.fish.gui;

import com.alkacode.fish.AlkaFishPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Menu principal de pesca: ir para a área + acesso às funções premium. */
public final class FishingAreaGui extends FishGui {

    public FishingAreaGui(AlkaFishPlugin plugin, Player player) {
        super(plugin, player, "🎣 AlkaFish", 5, "alkafish-area");
    }

    @Override
    public void render() {
        fillBlack();

        boolean inArea = plugin.getFishingAreaManager().isInArea(player);
        boolean configured = plugin.getFishingAreaManager().isConfigured();

        // Ir / voltar da área
        setItem(22, createItem(configured
                        ? (inArea ? Material.RED_BED : Material.SEA_LANTERN)
                        : Material.BARRIER,
                        configured ? (inArea ? "<red>⬅ Sair da Área de Pesca" : "<aqua>🏝 Ir para a Área de Pesca")
                                : "<red>Área não configurada",
                        configured
                                ? (inArea ? "<gray>Voltar para o spawn" : "<gray>Teleportar e ganhar a vara")
                                : "<gray>Configure com /alkafish setarea"),
                e -> {
                    if (!configured) return;
                    if (inArea) {
                        plugin.getFishingAreaManager().teleportExit(player);
                    } else {
                        plugin.getFishingAreaManager().teleportTo(player);
                    }
                    player.closeInventory();
                });

        // Funções premium
        setItem(11, createItem(Material.FISHING_ROD, "<aqua>🎣 Sua Vara",
                        "<gray>Upar, reparar e encantar"),
                e -> new RodGui(plugin, player).open());
        setItem(12, createItem(Material.ENCHANTED_BOOK, "<dark_purple>✨ Encantamentos",
                        "<gray>Sortudo, Multiplicador, Chaveiro"),
                e -> new EnchantGui(plugin, player).open());
        setItem(13, createItem(Material.LEATHER_CHESTPLATE, "<gold>🛡 Classes de Pesca",
                        "<gray>Armaduras com bônus"),
                e -> new ClassGui(plugin, player).open());
        setItem(14, createItem(Material.TROPICAL_FISH, "<gold>🏆 Torneio",
                        "<gray>Info do torneio ativo"),
                e -> new TournamentGui(plugin, player).open());
        setItem(15, createItem(Material.EMERALD, "<green>💰 Vender Peixes",
                        "<gray>Venda seus peixes"),
                e -> new SellGui(plugin, player).open());

        setItem(31, createItem(Material.BOOK, "<yellow>📖 Codex de Peixes",
                        "<gray>Coleção de peixes"),
                e -> new CodexGui(plugin, player).open());
        setItem(33, createItem(Material.GLASS, "<gray>👻 Invisibilidade",
                        "<gray>Toggle invisibilidade na área"),
                e -> plugin.getFishingAreaManager().toggleInvisibility(player));

        setItem(40, createItem(Material.BARRIER, "<red>❌ Fechar"),
                e -> player.closeInventory());
    }
}
