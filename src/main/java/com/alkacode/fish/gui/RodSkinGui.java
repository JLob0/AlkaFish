package com.alkacode.fish.gui;

import com.alkacode.fish.AlkaFishPlugin;
import com.alkacode.fish.model.FishingRod;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Sub-menu de skins da vara: troca só a textura (custom-model-data do ItemsAdder) entre
 * as varas que o jogador já passou na progressão, sem mexer no nível/stats reais. */
public final class RodSkinGui extends FishGui {

    private static final int PER_PAGE = 6; // slots 11-16

    private final int page;

    public RodSkinGui(AlkaFishPlugin plugin, Player player) {
        this(plugin, player, 0);
    }

    public RodSkinGui(AlkaFishPlugin plugin, Player player, int page) {
        super(plugin, player, "🎨 Skins da Vara", Category.PROGRESSION, 3, "alkafish-rod-skin");
        this.page = page;
    }

    @Override
    public void render() {
        fillBlack();
        var stats = plugin.getPlayerDataManager().getStats(player.getUniqueId());
        FishingRod current = plugin.getRodManager().getRodById(stats.getRodId());
        if (current == null) current = plugin.getRodManager().getDefaultRod();
        if (current == null) return;
        final FishingRod fCurrent = current;

        String activeSkin = stats.getRodSkinId();
        boolean usingDefault = activeSkin == null || activeSkin.isEmpty();

        // Slot 10: reset pra skin própria da vara atual (fixo em toda página)
        setItem(10, createItem(usingDefault ? Material.LIME_DYE : Material.GRAY_DYE,
                usingDefault ? "<green>✔ Skin Padrão" : "<gray>Skin Padrão",
                "<gray>Usa a textura da sua vara atual:",
                "<white>" + fCurrent.getDisplayName(),
                "",
                usingDefault ? "<green>Selecionada" : "<yellow>Clique para usar"), e -> {
            plugin.getRodManager().setRodSkin(player, "");
            refresh();
        });

        List<FishingRod> options = new ArrayList<>();
        for (FishingRod rod : plugin.getRodManager().getUnlockedRods(fCurrent)) {
            if (!rod.getId().equals(fCurrent.getId())) options.add(rod);
        }

        int totalPages = Math.max(1, (int) Math.ceil(options.size() / (double) PER_PAGE));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        int from = safePage * PER_PAGE;
        int to = Math.min(from + PER_PAGE, options.size());

        int slot = 11;
        for (FishingRod rod : options.subList(from, to)) {
            boolean selected = rod.getId().equals(activeSkin);
            final String rodId = rod.getId();
            setItem(slot, createItem(selected ? Material.LIME_DYE : Material.CYAN_DYE,
                    (selected ? "<green>✔ " : "<aqua>") + rod.getDisplayName(),
                    "<gray>Nível: <aqua>" + rod.getLevel(),
                    "",
                    selected ? "<green>Selecionada" : "<yellow>Clique para usar essa skin"), e -> {
                plugin.getRodManager().setRodSkin(player, rodId);
                refresh();
            });
            slot++;
        }

        if (safePage > 0) {
            setItem(19, createItem(Material.ARROW, "<yellow>◀ Página Anterior"),
                    e -> new RodSkinGui(plugin, player, safePage - 1).open());
        }
        if (safePage < totalPages - 1) {
            setItem(25, createItem(Material.ARROW, "<yellow>Próxima Página ▶"),
                    e -> new RodSkinGui(plugin, player, safePage + 1).open());
        }

        setItem(18, createItem(Material.ARROW, "<yellow>⬅ Voltar"), e -> new RodGui(plugin, player).open());
        setItem(26, createItem(Material.BARRIER, "<red>Fechar"), e -> player.closeInventory());
    }
}
